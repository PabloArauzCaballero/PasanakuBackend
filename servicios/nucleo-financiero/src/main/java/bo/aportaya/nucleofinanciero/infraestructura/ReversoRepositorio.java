package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** {@code reverso_transaccion} y la lectura de la transaccion que se reversa. */
@Component
public class ReversoRepositorio {

    /** La transaccion original con sus patas, para poder invertirlas exactamente. */
    public Optional<Original> original(DSLContext dsl, UUID transaccionId) {
        Record cabecera = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("tipo", String.class),
                        DSL.field("estado", String.class),
                        DSL.field("moneda", String.class),
                        DSL.field("monto_total", BigDecimal.class),
                        DSL.field("origen_tipo", String.class),
                        DSL.field("origen_id", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "transaccion_billetera")))
                .where(DSL.field("id", UUID.class).eq(transaccionId))
                .fetchOne();
        if (cabecera == null) {
            return Optional.empty();
        }
        Moneda moneda = Moneda.valueOf(cabecera.get("moneda", String.class));

        List<Movimiento> patas = dsl.select(
                        DSL.field("cuenta_billetera_id", UUID.class),
                        DSL.field("sentido", String.class),
                        DSL.field("monto", BigDecimal.class),
                        DSL.field("glosa", String.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "movimiento_billetera")))
                .where(DSL.field("transaccion_id", UUID.class).eq(transaccionId))
                .orderBy(DSL.field("orden").asc())
                .fetch(f -> new Movimiento(
                        f.get("cuenta_billetera_id", UUID.class),
                        f.get("sentido", String.class),
                        Dinero.de(f.get("monto", BigDecimal.class), moneda),
                        f.get("glosa", String.class)));

        return Optional.of(new Original(
                cabecera.get("id", UUID.class),
                cabecera.get("tipo", String.class),
                cabecera.get("estado", String.class),
                Dinero.de(cabecera.get("monto_total", BigDecimal.class), moneda),
                patas,
                cabecera.get("origen_tipo", String.class),
                cabecera.get("origen_id", UUID.class)));
    }

    /**
     * El cargo que esa transaccion llevaba: la orden que la origino, con su costo y la cotizacion que lo
     * respaldo. Solo las recargas y los retiros cobran; para cualquier otro origen no hay cargo.
     */
    public Optional<Cargo> cargoDe(DSLContext dsl, Original original) {
        boolean recarga = "ORDEN_RECARGA".equals(original.origenTipo());
        if (!recarga && !"ORDEN_RETIRO".equals(original.origenTipo())) {
            return Optional.empty();
        }
        String tabla = recarga ? "orden_recarga" : "orden_retiro";
        String columnaCosto = recarga ? "costo_proveedor" : "costo_retiro";
        Record fila = dsl.select(
                        DSL.field(columnaCosto, BigDecimal.class).as("costo"),
                        DSL.field("moneda", String.class),
                        DSL.field("cotizacion_id", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", tabla)))
                .where(DSL.field("id", UUID.class).eq(original.origenId()))
                .fetchOne();
        if (fila == null) {
            return Optional.empty();
        }
        return Optional.of(new Cargo(
                original.origenTipo(),
                original.origenId(),
                Dinero.de(fila.get("costo", BigDecimal.class), Moneda.valueOf(fila.get("moneda", String.class))),
                Optional.ofNullable(fila.get("cotizacion_id", UUID.class))));
    }

    /**
     * La recarga reversada deja de figurar como acreditada. El retiro no se marca: su UPDATE de estado dispara
     * el control de encaje (R-BIL-11b), y deshacer una salida no puede quedar frenado por el encaje.
     */
    public void marcarRecargaReversada(DSLContext dsl, UUID ordenId) {
        dsl.update(DSL.table(DSL.name("nucleo_financiero", "orden_recarga")))
                .set(DSL.field("estado", String.class), "REVERSADA")
                .where(DSL.field("id", UUID.class).eq(ordenId))
                .and(DSL.field("estado").eq("ACREDITADA"))
                .execute();
    }

    /**
     * ¿Ya se reverso? R-BIL-15 lo garantiza con un unico parcial, pero preguntarlo
     * antes permite devolver el codigo del contrato en vez de una violacion cruda.
     */
    public boolean yaFueReversada(DSLContext dsl, UUID transaccionOriginalId) {
        return dsl.fetchCount(
                        DSL.table(DSL.name("nucleo_financiero", "reverso_transaccion")),
                        DSL.field("transaccion_original_id", UUID.class).eq(transaccionOriginalId),
                        DSL.field("estado").ne("RECHAZADO"))
                > 0;
    }

    public UUID registrar(
            DSLContext dsl,
            UUID originalId,
            UUID transaccionReversoId,
            UUID autorizadaPor,
            String tipo,
            String motivo,
            Dinero monto,
            OffsetDateTime ahora) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("nucleo_financiero", "reverso_transaccion")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("transaccion_original_id", UUID.class), originalId)
                .set(DSL.field("transaccion_reverso_id", UUID.class), transaccionReversoId)
                .set(DSL.field("autorizada_por", UUID.class), autorizadaPor)
                .set(DSL.field("tipo", String.class), tipo)
                .set(DSL.field("motivo", String.class), motivo)
                .set(DSL.field("monto_reversado", BigDecimal.class), monto.monto())
                .set(DSL.field("estado", String.class), "EJECUTADO")
                .set(DSL.field("solicitada_en", OffsetDateTime.class), ahora)
                .set(DSL.field("ejecutada_en", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    /*
     * No hay `marcarOriginalReversada`, y no es un olvido.
     *
     * `transaccion_billetera` es append-only y R-AUD-01 rechaza cualquier UPDATE,
     * tambien sobre la cabecera. Esta bien que sea asi: la transaccion original es un
     * hecho que ocurrio, y cambiarle el estado seria reescribir el pasado. El vinculo
     * entre lo que paso y su correccion lo lleva `reverso_transaccion`, que es donde
     * corresponde, y de ahi sale la respuesta a «¿esto ya se reverso?».
     */

    public record Cargo(String ordenTipo, UUID ordenId, Dinero costo, Optional<UUID> cotizacionId) {}

    public record Original(
            UUID id,
            String tipo,
            String estado,
            Dinero montoTotal,
            List<Movimiento> patas,
            String origenTipo,
            UUID origenId) {

        /** Solo se reversa lo aplicado: lo rechazado o ya reversado no tiene efecto que deshacer. */
        public boolean esReversable() {
            return "APLICADA".equals(estado);
        }
    }

    public record Movimiento(UUID cuentaId, String sentido, Dinero monto, String glosa) {

        /** El sentido opuesto: reversar es escribir el espejo, no borrar el original. */
        public String sentidoInverso() {
            return "DEBITO".equals(sentido) ? "CREDITO" : "DEBITO";
        }
    }
}
