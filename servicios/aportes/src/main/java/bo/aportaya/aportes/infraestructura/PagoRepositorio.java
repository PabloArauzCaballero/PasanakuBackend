package bo.aportaya.aportes.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** {@code pago} y {@code reembolso}, con su historial de estados (R-BIL-30). */
@Component
public class PagoRepositorio {

    /**
     * F1.L1.M08 · R-BIL-30: el estado vigente de un pago es la ultima fila de su historial; sin historial, el
     * de alta. Se usa sobre {@code FROM aportes.pago} sin alias.
     */
    static final Field<String> ESTADO_VIGENTE = DSL.field(
            """
            COALESCE((SELECT h.estado_nuevo FROM aportes.historial_estado_pago h WHERE h.pago_id = pago.id \
            ORDER BY h.fecha_hora DESC, h.id DESC LIMIT 1), pago.estado)""",
            String.class);

    public UUID registrar(
            DSLContext dsl,
            UUID obligacionId,
            Optional<UUID> proveedorId,
            Dinero monto,
            Dinero comision,
            Dinero neto,
            String canal,
            String referenciaProveedor,
            String claveIdempotencia,
            boolean esManual,
            Optional<UUID> registradoPor,
            OffsetDateTime ahora) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("aportes", "pago")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("obligacion_id", UUID.class), obligacionId)
                .set(DSL.field("proveedor_id", UUID.class), proveedorId.orElse(null))
                .set(DSL.field("monto", BigDecimal.class), monto.monto())
                .set(DSL.field("moneda", String.class), monto.moneda().name())
                .set(DSL.field("monto_comision_proveedor", BigDecimal.class), comision.monto())
                .set(DSL.field("monto_neto_acreditado", BigDecimal.class), neto.monto())
                .set(DSL.field("canal", String.class), canal)
                .set(DSL.field("estado", String.class), "ACREDITADO")
                .set(DSL.field("fecha_hora_pago", OffsetDateTime.class), ahora)
                .set(DSL.field("fecha_hora_acreditacion", OffsetDateTime.class), ahora)
                .set(DSL.field("referencia_proveedor", String.class), referenciaProveedor)
                .set(DSL.field("registrado_por", UUID.class), registradoPor.orElse(null))
                .set(DSL.field("es_manual", Boolean.class), esManual)
                .set(DSL.field("clave_idempotencia", String.class), claveIdempotencia)
                .execute();
        return id;
    }

    /**
     * Busca el pago por la MISMA identidad que ampara {@code uq_pago_idem
     * (obligacion_id, clave_idempotencia)}.
     *
     * <p>Filtrar solo por {@code clave_idempotencia} (como hacia antes) es mas ancho
     * que el indice: dos obligaciones distintas que por coincidencia usan la misma
     * clave (una app movil que reusa un UUID de plantilla, dos participantes que piden
     * el mismo instante) colisionaban en Java aunque la base las tenia por separado, y
     * el segundo pago devolvia el {@code pagoId} del primero — la persona equivocada
     * (hallazgo H1.S1, PLAN.md hallazgo C).
     */
    public Optional<UUID> porClaveIdempotencia(DSLContext dsl, UUID obligacionId, String clave) {
        return Optional.ofNullable(dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("aportes", "pago")))
                .where(DSL.field("obligacion_id", UUID.class).eq(obligacionId))
                .and(DSL.field("clave_idempotencia").eq(clave))
                .fetchOne(DSL.field("id", UUID.class)));
    }

    /**
     * El pago que la pasarela dice haber confirmado, por la referencia que ella misma
     * asigno (`CU21CobrarAporte.registrar` la guarda en el mismo INSERT).
     *
     * <p><b>Simplificacion declarada</b> (ver {@code idempotencia-scope.md} y
     * {@code carriles/PR4-seguridad.md} §H1.S2): el modelo completo de
     * {@code orden_cobro → intento_pago → webhook_pasarela → pago} que describe la
     * skill de pagos QR no esta implementado en Java todavia — {@code CU21CobrarAporte}
     * acredita en la misma llamada, sin esperar confirmacion async. Esta lectura conecta
     * el webhook con el pago YA registrado por su referencia de proveedor, que es el
     * unico eslabon que existe hoy; no reemplaza construir la cadena completa, que
     * queda fuera de este carril de seguridad transversal (hallazgo de alcance, no de
     * seguridad).
     */
    public Optional<PagoParaConciliar> porReferenciaProveedor(DSLContext dsl, String referenciaProveedor) {
        Record fila = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("monto", BigDecimal.class),
                        DSL.field("moneda", String.class),
                        ESTADO_VIGENTE.as("estado"))
                .from(DSL.table(DSL.name("aportes", "pago")))
                .where(DSL.field("referencia_proveedor", String.class).eq(referenciaProveedor))
                .orderBy(DSL.field("fecha_hora_pago").desc())
                .limit(1)
                .fetchOne();
        return Optional.ofNullable(fila)
                .map(f -> new PagoParaConciliar(
                        f.get("id", UUID.class),
                        Dinero.de(f.get("monto", BigDecimal.class), Moneda.valueOf(f.get("moneda", String.class))),
                        f.get("estado", String.class)));
    }

    public record PagoParaConciliar(UUID id, Dinero monto, String estado) {}

    /**
     * Compensacion de un aporte que el libro no pudo aplicar: {@code ACREDITADO} pasa a {@code REVERSADO}. La precondicion va
     * en la escritura (no en un {@code if} previo): un pago en disputa, ya reversado o de otro estado no se toca.
     */
    public boolean reversar(DSLContext dsl, UUID pagoId) {
        // F1.L1.M08 · R-BIL-30: el pago es append-only; su estado cambia con una fila de historial.
        // La precondicion va en la escritura y uq_historial_estado_pago_salida decide la carrera.
        return dsl.execute(
                        """
                        INSERT INTO aportes.historial_estado_pago
                            (id, pago_id, estado_anterior, estado_nuevo, motivo, fecha_hora)
                        SELECT gen_random_uuid(), pago.id, 'ACREDITADO', 'REVERSADO',
                               'Compensacion: el libro no pudo aplicar el aporte', clock_timestamp()
                          FROM aportes.pago
                         WHERE pago.id = ?
                           AND COALESCE((SELECT h.estado_nuevo FROM aportes.historial_estado_pago h
                                          WHERE h.pago_id = pago.id ORDER BY h.fecha_hora DESC, h.id DESC LIMIT 1),
                                        pago.estado) = 'ACREDITADO'
                        ON CONFLICT (pago_id, estado_anterior) DO NOTHING
                        """,
                        pagoId)
                == 1;
    }

    public Optional<Pago> ver(DSLContext dsl, UUID pagoId) {
        Record fila = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("obligacion_id", UUID.class),
                        DSL.field("proveedor_id", UUID.class),
                        DSL.field("monto", BigDecimal.class),
                        DSL.field("moneda", String.class),
                        ESTADO_VIGENTE.as("estado"))
                .from(DSL.table(DSL.name("aportes", "pago")))
                .where(DSL.field("id", UUID.class).eq(pagoId))
                .fetchOne();
        return Optional.ofNullable(fila)
                .map(f -> new Pago(
                        f.get("id", UUID.class),
                        f.get("obligacion_id", UUID.class),
                        Optional.ofNullable(f.get("proveedor_id", UUID.class)),
                        Dinero.de(f.get("monto", BigDecimal.class), Moneda.valueOf(f.get("moneda", String.class))),
                        f.get("estado", String.class)));
    }

    /** Lo ya reembolsado de un pago: es contra lo que se compara el nuevo pedido. */
    public Dinero reembolsadoDe(DSLContext dsl, UUID pagoId, Moneda moneda) {
        // SQL-SEGURO: literal de texto (bloque triple-comillas), `?` con bind real —
        // revisado en H2.S2.M2 del carril PR4-seguridad, sin entrada interpolada.
        BigDecimal total = (BigDecimal) dsl.fetchOne(
                        """
                        SELECT COALESCE(SUM(r.monto), 0) FROM aportes.reembolso r
                         WHERE r.pago_id = ?
                           AND COALESCE((SELECT h.estado_nuevo FROM aportes.historial_estado_reembolso h
                                          WHERE h.reembolso_id = r.id ORDER BY h.fecha_hora DESC, h.id DESC LIMIT 1),
                                        r.estado) <> 'RECHAZADO'
                        """,
                        pagoId)
                .get(0);
        return Dinero.de(total, moneda);
    }

    public UUID solicitarReembolso(
            DSLContext dsl,
            UUID pagoId,
            Dinero monto,
            String motivo,
            UUID solicitadoPor,
            String claveIdempotencia,
            OffsetDateTime ahora) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("aportes", "reembolso")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("pago_id", UUID.class), pagoId)
                .set(DSL.field("monto", BigDecimal.class), monto.monto())
                // F1.L1.M12 · R-BIL-29: la moneda al lado del importe, la del pago (CU-19 ya la comparo).
                .set(DSL.field("moneda", String.class), monto.moneda().name())
                .set(DSL.field("motivo", String.class), motivo)
                .set(DSL.field("estado", String.class), "SOLICITADO")
                .set(DSL.field("solicitado_por", UUID.class), solicitadoPor)
                .set(DSL.field("clave_idempotencia", String.class), claveIdempotencia)
                .set(DSL.field("fecha_solicitud", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    /**
     * Aprueba y ejecuta. El {@code WHERE estado = 'SOLICITADO'} decide la carrera: dos
     * aprobaciones simultaneas no devuelven la plata dos veces.
     */
    public boolean ejecutarReembolso(DSLContext dsl, UUID reembolsoId, UUID aprobadoPor, OffsetDateTime ahora) {
        // F1.L1.M08 · R-BIL-30: el reembolso es append-only; quien aprueba y cuando quedan en su historial.
        return dsl.execute(
                        """
                        INSERT INTO aportes.historial_estado_reembolso
                            (id, reembolso_id, estado_anterior, estado_nuevo, ejecutado_por, fecha_hora)
                        SELECT gen_random_uuid(), r.id, 'SOLICITADO', 'EJECUTADO', ?, ?::timestamptz
                          FROM aportes.reembolso r
                         WHERE r.id = ?
                           AND COALESCE((SELECT h.estado_nuevo FROM aportes.historial_estado_reembolso h
                                          WHERE h.reembolso_id = r.id ORDER BY h.fecha_hora DESC, h.id DESC LIMIT 1),
                                        r.estado) = 'SOLICITADO'
                        ON CONFLICT (reembolso_id, estado_anterior) DO NOTHING
                        """,
                        aprobadoPor,
                        ahora,
                        reembolsoId)
                == 1;
    }

    /** Quien ejecuto el reembolso, segun su historial (R-BIL-30); vacio si sigue solicitado. */
    public Optional<UUID> ejecutadoPor(DSLContext dsl, UUID reembolsoId) {
        return dsl.select(DSL.field("ejecutado_por", UUID.class))
                .from(DSL.table(DSL.name("aportes", "historial_estado_reembolso")))
                .where(DSL.field("reembolso_id", UUID.class).eq(reembolsoId))
                .and(DSL.field("estado_nuevo", String.class).eq("EJECUTADO"))
                .fetchOptional(r -> r.value1());
    }

    /** F1.L1.M09 · el candado de (pago, clave) hasta el fin de la transaccion: dos reintentos no corren a la vez. */
    public void bloquearIdempotenciaDeReembolso(DSLContext dsl, UUID pagoId, String clave) {
        dsl.execute(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0::bigint))", "reembolso|" + pagoId + "|" + clave);
    }

    /** La solicitud que ya uso esa clave sobre ese pago, si la hay (R-BIL-28). */
    public Optional<Reembolso> reembolsoPorClave(DSLContext dsl, UUID pagoId, String clave) {
        return dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("aportes", "reembolso")))
                .where(DSL.field("pago_id", UUID.class).eq(pagoId))
                .and(DSL.field("clave_idempotencia", String.class).eq(clave))
                .fetchOptional(r -> r.value1())
                .flatMap(id -> verReembolso(dsl, id));
    }

    /**
     * F1.L1.M12 · la moneda se lee de la fila; una fila de antes de R-BIL-29 no la tiene y toma la de su pago,
     * nunca una fija.
     */
    public Optional<Reembolso> verReembolso(DSLContext dsl, UUID reembolsoId) {
        Record fila = dsl.resultQuery(
                        """
                        SELECT r.id, r.pago_id, r.monto, COALESCE(r.moneda, p.moneda) AS moneda,
                               COALESCE((SELECT h.estado_nuevo FROM aportes.historial_estado_reembolso h
                                          WHERE h.reembolso_id = r.id ORDER BY h.fecha_hora DESC, h.id DESC LIMIT 1),
                                        r.estado) AS estado,
                               r.solicitado_por
                          FROM aportes.reembolso r
                          JOIN aportes.pago p ON p.id = r.pago_id
                         WHERE r.id = ?
                        """,
                        reembolsoId)
                .fetchOne();
        return Optional.ofNullable(fila)
                .map(f -> new Reembolso(
                        f.get("id", UUID.class),
                        f.get("pago_id", UUID.class),
                        Dinero.de(f.get("monto", BigDecimal.class), Moneda.valueOf(f.get("moneda", String.class))),
                        f.get("estado", String.class),
                        f.get("solicitado_por", UUID.class)));
    }

    public record Pago(UUID id, UUID obligacionId, Optional<UUID> proveedorId, Dinero monto, String estado) {}

    public record Reembolso(UUID id, UUID pagoId, Dinero monto, String estado, UUID solicitadoPor) {}
}
