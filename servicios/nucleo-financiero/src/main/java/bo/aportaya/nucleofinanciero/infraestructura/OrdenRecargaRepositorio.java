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

/** {@code orden_recarga}: el pedido de ingreso de fondos y su ciclo. */
@Component
public class OrdenRecargaRepositorio {

    public UUID crear(
            DSLContext dsl,
            UUID cuentaId,
            Optional<UUID> instrumentoId,
            Dinero bruto,
            Dinero costoProveedor,
            Dinero acreditado,
            String claveIdempotencia,
            String medio,
            Optional<UUID> cotizacionId,
            OffsetDateTime ahora,
            OffsetDateTime expiraEn) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("nucleo_financiero", "orden_recarga")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("cuenta_billetera_id", UUID.class), cuentaId)
                .set(DSL.field("instrumento_fondeo_id", UUID.class), instrumentoId.orElse(null))
                .set(DSL.field("monto_bruto", BigDecimal.class), bruto.monto())
                .set(DSL.field("costo_proveedor", BigDecimal.class), costoProveedor.monto())
                .set(DSL.field("monto_acreditado", BigDecimal.class), acreditado.monto())
                .set(DSL.field("moneda", String.class), bruto.moneda().name())
                .set(DSL.field("estado", String.class), "PENDIENTE")
                .set(DSL.field("medio", String.class), medio)
                .set(DSL.field("cotizacion_id", UUID.class), cotizacionId.orElse(null))
                .set(DSL.field("clave_idempotencia", String.class), claveIdempotencia)
                .set(DSL.field("solicitada_en", OffsetDateTime.class), ahora)
                .set(DSL.field("expira_en", OffsetDateTime.class), expiraEn)
                .execute();
        return id;
    }

    public Optional<Orden> ver(DSLContext dsl, UUID ordenId) {
        return consultar(dsl, ordenId, false);
    }

    public Optional<Orden> bloquear(DSLContext dsl, UUID ordenId) {
        return consultar(dsl, ordenId, true);
    }

    private Optional<Orden> consultar(DSLContext dsl, UUID ordenId, boolean bloquear) {
        var consulta = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("cuenta_billetera_id", UUID.class),
                        DSL.field("monto_bruto", BigDecimal.class),
                        DSL.field("monto_acreditado", BigDecimal.class),
                        DSL.field("moneda", String.class),
                        DSL.field("estado", String.class),
                        DSL.field("transaccion_id", UUID.class),
                        DSL.field("referencia_externa", String.class),
                        DSL.field("instrumento_fondeo_id", UUID.class),
                        DSL.field("medio", String.class),
                        DSL.field("cotizacion_id", UUID.class),
                        DSL.field("expira_en", OffsetDateTime.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "orden_recarga")))
                .where(DSL.field("id", UUID.class).eq(ordenId));
        Record fila = bloquear ? consulta.forUpdate().fetchOne() : consulta.fetchOne();
        return Optional.ofNullable(fila).map(f -> {
            Moneda moneda = Moneda.valueOf(f.get("moneda", String.class));
            return new Orden(
                    f.get("id", UUID.class),
                    f.get("cuenta_billetera_id", UUID.class),
                    Dinero.de(f.get("monto_bruto", BigDecimal.class), moneda),
                    Dinero.de(f.get("monto_acreditado", BigDecimal.class), moneda),
                    f.get("estado", String.class),
                    enUtc(f.get("expira_en", OffsetDateTime.class)),
                    f.get("transaccion_id", UUID.class),
                    f.get("referencia_externa", String.class),
                    Optional.ofNullable(f.get("instrumento_fondeo_id", UUID.class)),
                    f.get("medio", String.class),
                    Optional.ofNullable(f.get("cotizacion_id", UUID.class)));
        });
    }

    /**
     * R-BIL-06, scope de {@code uq_recarga_idem (cuenta_billetera_id, clave_idempotencia)}.
     *
     * <p>Sin el {@code cuenta_billetera_id} en el {@code WHERE}, dos titulares distintos
     * que coincidan en la clave comparten orden: el segundo recibe la recarga del
     * primero en vez de la suya.
     */
    public void bloquearIdempotencia(DSLContext dsl, UUID cuentaId, String clave) {
        BloqueoDeIdempotencia.tomar(dsl, "orden_recarga", cuentaId.toString(), clave);
    }

    /**
     * El mismo instante, siempre en UTC. La base devuelve el desplazamiento de la sesion
     * (por ejemplo -04:00), y repetir una solicitud tiene que devolver la misma respuesta
     * que la primera vez, no la misma hora escrita distinto.
     */
    private static OffsetDateTime enUtc(OffsetDateTime instante) {
        return instante == null ? null : instante.withOffsetSameInstant(java.time.ZoneOffset.UTC);
    }

    /**
     * La orden de esa clave <b>en esa billetera</b>: la unicidad es {@code (cuenta, clave)}
     * (R-BIL-06). Buscar solo por clave devolveria la orden de otra persona que usara el
     * mismo valor, o reventaria con dos filas.
     */
    public Optional<UUID> porClaveIdempotencia(DSLContext dsl, UUID cuentaId, String clave) {
        return Optional.ofNullable(dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "orden_recarga")))
                .where(DSL.field("cuenta_billetera_id", UUID.class).eq(cuentaId))
                .and(DSL.field("clave_idempotencia").eq(clave))
                .fetchOne(DSL.field("id", UUID.class)));
    }

    /** La transaccion que acredito la orden, si ya se acredito (para repetir la respuesta original). */
    public Optional<UUID> transaccionDe(DSLContext dsl, UUID ordenId) {
        return Optional.ofNullable(dsl.select(DSL.field("transaccion_id", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "orden_recarga")))
                .where(DSL.field("id", UUID.class).eq(ordenId))
                .fetchOne(DSL.field("transaccion_id", UUID.class)));
    }

    /** El proveedor rechazo en firme: la orden se cierra, el libro no se toca. */
    public boolean rechazar(DSLContext dsl, UUID ordenId) {
        return dsl.update(DSL.table(DSL.name("nucleo_financiero", "orden_recarga")))
                        .set(DSL.field("estado", String.class), "RECHAZADA")
                        .where(DSL.field("id", UUID.class).eq(ordenId))
                        .and(DSL.field("estado").eq("PENDIENTE"))
                        .execute()
                > 0;
    }

    public Dinero saldoDeLaAcreditacion(DSLContext dsl, Orden orden) {
        BigDecimal saldo = dsl.select(DSL.field("saldo_disponible_posterior", BigDecimal.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "movimiento_billetera")))
                .where(DSL.field("transaccion_id", UUID.class).eq(orden.transaccionId()))
                .and(DSL.field("cuenta_billetera_id", UUID.class).eq(orden.cuentaId()))
                .and(DSL.field("sentido", String.class).eq("CREDITO"))
                .fetchSingle(DSL.field("saldo_disponible_posterior", BigDecimal.class));
        return Dinero.de(saldo, orden.bruto().moneda());
    }

    /**
     * Acredita la orden, **solo si sigue pendiente**.
     *
     * <p>El {@code WHERE estado = 'PENDIENTE'} es lo que impide acreditar dos veces
     * cuando el proveedor reenvia la confirmacion: sin el, el mismo pago sumaria saldo
     * dos veces y no habria forma de saber cual de los dos fue el bueno.
     */
    public boolean acreditar(
            DSLContext dsl, UUID ordenId, UUID transaccionId, String referenciaExterna, OffsetDateTime ahora) {
        return dsl.update(DSL.table(DSL.name("nucleo_financiero", "orden_recarga")))
                        .set(DSL.field("estado", String.class), "ACREDITADA")
                        .set(DSL.field("transaccion_id", UUID.class), transaccionId)
                        .set(DSL.field("referencia_externa", String.class), referenciaExterna)
                        .set(DSL.field("acreditada_en", OffsetDateTime.class), ahora)
                        .where(DSL.field("id", UUID.class).eq(ordenId))
                        .and(DSL.field("estado").eq("PENDIENTE"))
                        .execute()
                > 0;
    }

    public int expirarVencidas(DSLContext dsl, OffsetDateTime ahora) {
        return dsl.update(DSL.table(DSL.name("nucleo_financiero", "orden_recarga")))
                .set(DSL.field("estado", String.class), "EXPIRADA")
                .where(DSL.field("estado").eq("PENDIENTE"))
                .and(DSL.field("expira_en", OffsetDateTime.class).lt(ahora))
                .execute();
    }

    /** El instrumento de fondeo, si esta verificado y es del titular. */
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

    public record Orden(
            UUID id,
            UUID cuentaId,
            Dinero bruto,
            Dinero acreditado,
            String estado,
            OffsetDateTime expiraEn,
            UUID transaccionId,
            String referenciaExterna,
            Optional<UUID> instrumentoId,
            String medio,
            Optional<UUID> cotizacionId) {}

    public record Instrumento(
            UUID usuarioId, boolean verificado, boolean titularCoincide, Optional<OffsetDateTime> bloqueadoHasta) {}
}
