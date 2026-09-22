package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code orden_retiro} y el instrumento de destino.
 *
 * <p>La base tiene tres guardias sobre esta tabla y ninguna se replica en Java: el
 * trigger del instrumento (R-BIL-09), el del encaje (R-BIL-11b) y los CHECK de MFA y
 * doble aprobacion. Duplicarlas crearia dos verdades; lo que se hace es adelantar el
 * rechazo para dar un mensaje util y dejar que la base tenga la ultima palabra.
 */
@Component
public class OrdenRetiroRepositorio {

    public UUID crear(
            DSLContext dsl,
            UUID cuentaId,
            UUID instrumentoDestinoId,
            UUID retencionId,
            UUID solicitadaPor,
            Dinero solicitado,
            Dinero costo,
            Dinero neto,
            boolean mfaVerificado,
            boolean requiereDobleAprobacion,
            Optional<OffsetDateTime> enfriamientoHasta,
            String claveIdempotencia,
            // H3.S1: el estado inicial lo decide el CU (PENDIENTE si nadie mas la
            // toca todavia — hoy no pasa, pero la firma no lo prohibe — AUTORIZADA
            // automatica bajo el umbral, EN_REVISION si necesita doble aprobacion),
            // nunca hardcodeado aca: esta clase es infraestructura, no la maquina de
            // estados.
            String estadoInicial,
            OffsetDateTime ahora) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("nucleo_financiero", "orden_retiro")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("cuenta_billetera_id", UUID.class), cuentaId)
                .set(DSL.field("instrumento_destino_id", UUID.class), instrumentoDestinoId)
                .set(DSL.field("retencion_id", UUID.class), retencionId)
                .set(DSL.field("solicitada_por", UUID.class), solicitadaPor)
                .set(DSL.field("monto_solicitado", BigDecimal.class), solicitado.monto())
                .set(DSL.field("costo_retiro", BigDecimal.class), costo.monto())
                .set(DSL.field("monto_neto", BigDecimal.class), neto.monto())
                .set(DSL.field("moneda", String.class), solicitado.moneda().name())
                .set(DSL.field("estado", String.class), estadoInicial)
                .set(DSL.field("mfa_verificado", Boolean.class), mfaVerificado)
                .set(DSL.field("requiere_doble_aprobacion", Boolean.class), requiereDobleAprobacion)
                .set(DSL.field("ventana_enfriamiento_hasta", OffsetDateTime.class), enfriamientoHasta.orElse(null))
                .set(DSL.field("clave_idempotencia", String.class), claveIdempotencia)
                .set(DSL.field("solicitada_en", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    public Optional<Orden> ver(DSLContext dsl, UUID ordenId) {
        Record fila = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("cuenta_billetera_id", UUID.class),
                        DSL.field("retencion_id", UUID.class),
                        DSL.field("monto_solicitado", BigDecimal.class),
                        DSL.field("costo_retiro", BigDecimal.class),
                        DSL.field("monto_neto", BigDecimal.class),
                        DSL.field("moneda", String.class),
                        DSL.field("estado", String.class),
                        DSL.field("solicitada_por", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "orden_retiro")))
                .where(DSL.field("id", UUID.class).eq(ordenId))
                .fetchOne();
        return Optional.ofNullable(fila).map(f -> {
            Moneda moneda = Moneda.valueOf(f.get("moneda", String.class));
            return new Orden(
                    f.get("id", UUID.class),
                    f.get("cuenta_billetera_id", UUID.class),
                    Optional.ofNullable(f.get("retencion_id", UUID.class)),
                    Dinero.de(f.get("monto_solicitado", BigDecimal.class), moneda),
                    Dinero.de(f.get("costo_retiro", BigDecimal.class), moneda),
                    Dinero.de(f.get("monto_neto", BigDecimal.class), moneda),
                    f.get("estado", String.class),
                    f.get("solicitada_por", UUID.class));
        });
    }

    /**
     * R-BIL-06, scope de {@code uq_retiro_idem (cuenta_billetera_id, clave_idempotencia)}.
     *
     * <p>Sin el {@code cuenta_billetera_id} en el {@code WHERE}, dos titulares distintos
     * que coincidan en la clave comparten orden: el segundo recibe el retiro del
     * primero en vez del suyo.
     */
    public void bloquearIdempotencia(DSLContext dsl, UUID cuentaId, String clave) {
        BloqueoDeIdempotencia.tomar(dsl, "orden_retiro", cuentaId.toString(), clave);
    }

    public Optional<UUID> porClaveIdempotencia(DSLContext dsl, UUID cuentaId, String clave) {
        return Optional.ofNullable(dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "orden_retiro")))
                .where(DSL.field("cuenta_billetera_id", UUID.class).eq(cuentaId))
                .and(DSL.field("clave_idempotencia").eq(clave))
                .fetchOne(DSL.field("id", UUID.class)));
    }

    /** Transicion condicionada al estado previo: la carrera la decide el UPDATE. */
    public boolean pasarA(DSLContext dsl, UUID ordenId, String desde, String hacia, OffsetDateTime pagadaEn) {
        var paso = dsl.update(DSL.table(DSL.name("nucleo_financiero", "orden_retiro")))
                .set(DSL.field("estado", String.class), hacia)
                .set(DSL.field("pagada_en", OffsetDateTime.class), pagadaEn);
        return paso.where(DSL.field("id", UUID.class).eq(ordenId))
                        .and(DSL.field("estado").eq(desde))
                        .execute()
                > 0;
    }

    /**
     * EN_REVISION → AUTORIZADA (H3.S2): un aprobador DISTINTO del solicitante.
     *
     * <p>{@code aprobada_por <> solicitada_por} se repite aca aunque
     * {@code ck_retiro_doble_aprobacion} ya lo exige en la base: la base rechaza con
     * un error generico de restriccion, y {@code CU11.aprobar} necesita distinguir
     * "ya no esta en revision" (alguien mas la resolvio primero) de "sos el mismo que
     * la pidio" para dar el codigo de error correcto — R-SEG-04 no perdona, pero el
     * mensaje si puede ser util.
     */
    public boolean pasarAAutorizadaPorAprobacion(DSLContext dsl, UUID ordenId, UUID aprobadaPor) {
        return dsl.update(DSL.table(DSL.name("nucleo_financiero", "orden_retiro")))
                        .set(DSL.field("estado", String.class), "AUTORIZADA")
                        .set(DSL.field("aprobada_por", UUID.class), aprobadaPor)
                        .where(DSL.field("id", UUID.class).eq(ordenId))
                        .and(DSL.field("estado").eq("EN_REVISION"))
                        .and(DSL.field("solicitada_por", UUID.class).ne(aprobadaPor))
                        .execute()
                > 0;
    }

    /** EN_REVISION → RECHAZADA (H3.S2): el aprobador la rechaza, sin llegar a AUTORIZADA. */
    public boolean pasarARechazadaPorAprobacion(DSLContext dsl, UUID ordenId, UUID aprobadaPor) {
        return dsl.update(DSL.table(DSL.name("nucleo_financiero", "orden_retiro")))
                        .set(DSL.field("estado", String.class), "RECHAZADA")
                        .set(DSL.field("aprobada_por", UUID.class), aprobadaPor)
                        .where(DSL.field("id", UUID.class).eq(ordenId))
                        .and(DSL.field("estado").eq("EN_REVISION"))
                        .and(DSL.field("solicitada_por", UUID.class).ne(aprobadaPor))
                        .execute()
                > 0;
    }

    /**
     * AUTORIZADA → EN_PROCESO (H3.S1.M3/H4.S2): guarda la referencia del proveedor.
     *
     * <p>Separado de {@link #pasarA} porque esa fija {@code pagada_en} — aca no hay
     * pago todavia, solo la instruccion enviada. Poner una fecha de pago antes de que
     * el proveedor confirme seria mentir sobre cuando salio la plata.
     */
    public boolean pasarAEnProceso(DSLContext dsl, UUID ordenId, String referenciaProveedor) {
        return dsl.update(DSL.table(DSL.name("nucleo_financiero", "orden_retiro")))
                        .set(DSL.field("estado", String.class), "EN_PROCESO")
                        .set(DSL.field("referencia_proveedor", String.class), referenciaProveedor)
                        .where(DSL.field("id", UUID.class).eq(ordenId))
                        .and(DSL.field("estado").eq("AUTORIZADA"))
                        .execute()
                > 0;
    }

    /** El instrumento de destino, con lo que las condiciones duras necesitan mirar. */
    public Optional<Instrumento> instrumento(DSLContext dsl, UUID instrumentoId) {
        Record fila = dsl.select(
                        DSL.field("usuario_id", UUID.class),
                        DSL.field("estado_verificacion", String.class),
                        DSL.field("titular_coincide", Boolean.class),
                        DSL.field("bloqueado_hasta", OffsetDateTime.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "instrumento_fondeo")))
                .where(DSL.field("id", UUID.class).eq(instrumentoId))
                .fetchOne();
        return Optional.ofNullable(fila)
                .map(f -> new Instrumento(
                        f.get("usuario_id", UUID.class),
                        "VERIFICADO".equals(f.get("estado_verificacion", String.class)),
                        f.get("titular_coincide", Boolean.class),
                        Optional.ofNullable(f.get("bloqueado_hasta", OffsetDateTime.class))));
    }

    /** ¿Hay saldo inmovilizado por oficio sobre esa cuenta? */
    public boolean hayBloqueoDeAutoridad(DSLContext dsl, UUID cuentaId) {
        return dsl.fetchCount(
                        DSL.table(DSL.name("nucleo_financiero", "bloqueo_saldo")),
                        DSL.field("cuenta_billetera_id", UUID.class).eq(cuentaId),
                        DSL.field("estado").eq("VIGENTE"))
                > 0;
    }

    /**
     * R-BIL-11b · ¿la custodia cumple encaje?
     *
     * <p>Se mira la ULTIMA conciliacion de cada cuenta de custodia de la moneda. Con el
     * encaje roto no salen retiros nuevos: seguir pagando es el escenario clasico de la
     * corrida — cobran los primeros que llegan y no queda para los demas.
     */
    public boolean encajeCumplido(DSLContext dsl, String moneda) {
        Integer incumplen = (Integer) dsl.fetchOne(
                        """
                        SELECT count(*)::int FROM (
                          SELECT DISTINCT ON (c.cuenta_custodia_id) c.cumple_encaje
                            FROM nucleo_financiero.conciliacion_custodia c
                            JOIN nucleo_financiero.cuenta_custodia cc ON cc.id = c.cuenta_custodia_id
                           WHERE cc.moneda = ?
                           ORDER BY c.cuenta_custodia_id, c.fecha DESC
                        ) ultimas WHERE NOT cumple_encaje
                        """,
                        moneda)
                .get(0);
        return incumplen == null || incumplen == 0;
    }

    public record Orden(
            UUID id,
            UUID cuentaId,
            Optional<UUID> retencionId,
            Dinero solicitado,
            Dinero costo,
            Dinero neto,
            String estado,
            UUID solicitadaPor) {}

    public record Instrumento(
            UUID usuarioId, boolean verificado, boolean titularCoincide, Optional<OffsetDateTime> bloqueadoHasta) {}
}
