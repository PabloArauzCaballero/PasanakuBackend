package bo.aportaya.inversiones.infraestructura;

import static bo.aportaya.inversiones.infraestructura.Sql.f;
import static bo.aportaya.inversiones.infraestructura.Sql.t;

import bo.aportaya.inversiones.dominio.Descomposicion;
import bo.aportaya.inversiones.dominio.OrigenDatos;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code comprobante_inversion}, {@code comision_exito} y {@code conciliacion_interes}:
 * todo append-only. Es el "mayor" de las inversiones: el costo base de una posicion y lo
 * ya pagado salen de aca, no de un campo que alguien actualiza.
 */
@Component
public class ComprobanteRepositorio {

    public record Comprobante(
            UUID id,
            UUID posicionId,
            String tipo,
            String sentidoTitular,
            Descomposicion d,
            String origen,
            OffsetDateTime emitidoEn) {}

    /** @return {@code true} si lo inserto este actor; el UNIQUE por origen hace idempotente la emision. */
    public boolean emitir(
            DSLContext dsl,
            UUID posicionId,
            UUID usuarioId,
            String tipo,
            UUID origenId,
            Descomposicion d,
            String moneda,
            OrigenDatos origen,
            OffsetDateTime ahora) {
        return dsl.insertInto(t("comprobante_inversion"))
                        .set(f("id", UUID.class), UUID.randomUUID())
                        .set(f("posicion_inversion_id", UUID.class), posicionId)
                        .set(f("usuario_id", UUID.class), usuarioId)
                        .set(f("tipo", String.class), tipo)
                        .set(f("origen_id", UUID.class), origenId)
                        .set(f("sentido_titular", String.class), "SUSCRIPCION".equals(tipo) ? "DEBITO" : "CREDITO")
                        .set(f("principal", BigDecimal.class), d.principal())
                        .set(f("interes", BigDecimal.class), d.interes())
                        .set(f("impuesto", BigDecimal.class), d.impuesto())
                        .set(f("comision", BigDecimal.class), d.comision())
                        .set(f("perdida_realizada", BigDecimal.class), d.perdidaRealizada())
                        .set(f("neto", BigDecimal.class), d.neto())
                        .set(f("costo_base", BigDecimal.class), d.costoBase())
                        .set(f("moneda", String.class), moneda)
                        .set(f("origen_datos", String.class), origen.name())
                        .set(f("emitido_en", OffsetDateTime.class), ahora)
                        .onConflictDoNothing()
                        .execute()
                == 1;
    }

    public List<Comprobante> deLaPosicion(DSLContext dsl, UUID posicionId) {
        return dsl.selectFrom(t("comprobante_inversion"))
                .where(f("posicion_inversion_id", UUID.class).eq(posicionId))
                .orderBy(
                        f("emitido_en", OffsetDateTime.class),
                        f("tipo", String.class).desc(),
                        f("id", UUID.class))
                .fetch(this::comprobante);
    }

    public Optional<Comprobante> deOrigen(DSLContext dsl, UUID origenId) {
        return dsl.selectFrom(t("comprobante_inversion"))
                .where(f("origen_id", UUID.class).eq(origenId))
                .fetchOptional(this::comprobante);
    }

    /**
     * El costo que le queda a la posicion, DESDE el mayor: lo que creo la suscripcion
     * menos lo que liberaron las liquidaciones.
     */
    public BigDecimal costoBaseVigente(DSLContext dsl, UUID posicionId) {
        var costo = f("costo_base", BigDecimal.class);
        var tipo = f("tipo", String.class);
        BigDecimal neto = dsl.select(
                        DSL.sum(DSL.when(tipo.eq("SUSCRIPCION"), costo).otherwise(costo.neg())))
                .from(t("comprobante_inversion"))
                .where(f("posicion_inversion_id", UUID.class).eq(posicionId))
                .fetchOne(0, BigDecimal.class);
        return neto == null ? BigDecimal.ZERO : neto;
    }

    // ------------------------------------------------------------------ comision de exito
    public void registrarComision(
            DSLContext dsl,
            UUID posicionId,
            UUID rescateId,
            BigDecimal cuotas,
            BigDecimal valorCuota,
            BigDecimal marcaPrevia,
            BigDecimal tasa,
            BigDecimal base,
            BigDecimal comision,
            BigDecimal cuotaNeta,
            BigDecimal marcaNueva,
            String moneda,
            OrigenDatos origen,
            OffsetDateTime ahora) {
        dsl.insertInto(t("comision_exito"))
                .set(f("id", UUID.class), UUID.randomUUID())
                .set(f("posicion_inversion_id", UUID.class), posicionId)
                .set(f("rescate_inversion_id", UUID.class), rescateId)
                .set(f("cuotas", BigDecimal.class), cuotas)
                .set(f("valor_cuota", BigDecimal.class), valorCuota)
                .set(f("marca_maxima_previa", BigDecimal.class), marcaPrevia)
                .set(f("tasa", BigDecimal.class), tasa)
                .set(f("base_elegible", BigDecimal.class), base)
                .set(f("comision", BigDecimal.class), comision)
                .set(f("cuota_neta", BigDecimal.class), cuotaNeta)
                .set(f("marca_maxima_nueva", BigDecimal.class), marcaNueva)
                .set(f("moneda", String.class), moneda)
                .set(f("origen_datos", String.class), origen.name())
                .set(f("calculada_en", OffsetDateTime.class), ahora)
                .onConflictDoNothing()
                .execute();
    }

    public BigDecimal comisionesCobradas(DSLContext dsl, UUID posicionId) {
        BigDecimal suma = dsl.select(DSL.sum(f("comision", BigDecimal.class)))
                .from(t("comision_exito"))
                .where(f("posicion_inversion_id", UUID.class).eq(posicionId))
                .fetchOne(0, BigDecimal.class);
        return suma == null ? BigDecimal.ZERO : suma;
    }

    // ------------------------------------------------------------------ conciliacion del interes
    public void registrarConciliacion(
            DSLContext dsl,
            UUID posicionId,
            UUID rescateId,
            java.time.LocalDate desde,
            java.time.LocalDate hasta,
            BigDecimal devengado,
            BigDecimal externo,
            BigDecimal retencionCalculada,
            BigDecimal retencionExterna,
            String moneda,
            OffsetDateTime ahora) {
        boolean coincide = devengado.compareTo(externo) == 0 && retencionCalculada.compareTo(retencionExterna) == 0;
        dsl.insertInto(t("conciliacion_interes"))
                .set(f("id", UUID.class), UUID.randomUUID())
                .set(f("posicion_inversion_id", UUID.class), posicionId)
                .set(f("rescate_inversion_id", UUID.class), rescateId)
                .set(f("periodo_desde", java.time.LocalDate.class), desde)
                .set(f("periodo_hasta", java.time.LocalDate.class), hasta)
                .set(f("interes_devengado", BigDecimal.class), devengado)
                .set(f("interes_externo", BigDecimal.class), externo)
                .set(f("retencion_calculada", BigDecimal.class), retencionCalculada)
                .set(f("retencion_externa", BigDecimal.class), retencionExterna)
                .set(f("moneda", String.class), moneda)
                .set(f("estado", String.class), coincide ? "CONCILIADA" : "DISCREPANCIA")
                .set(f("conciliada_en", OffsetDateTime.class), ahora)
                .onConflictDoNothing()
                .execute();
    }

    public Optional<String> estadoDeConciliacion(DSLContext dsl, UUID rescateId) {
        return dsl.select(f("estado", String.class))
                .from(t("conciliacion_interes"))
                .where(f("rescate_inversion_id", UUID.class).eq(rescateId))
                .fetchOptional(0, String.class);
    }

    private Comprobante comprobante(Record r) {
        var d = new Descomposicion(
                r.get("principal", BigDecimal.class),
                r.get("interes", BigDecimal.class),
                r.get("impuesto", BigDecimal.class),
                r.get("comision", BigDecimal.class),
                r.get("perdida_realizada", BigDecimal.class),
                r.get("neto", BigDecimal.class),
                r.get("costo_base", BigDecimal.class));
        return new Comprobante(
                r.get("id", UUID.class),
                r.get("posicion_inversion_id", UUID.class),
                r.get("tipo", String.class),
                r.get("sentido_titular", String.class),
                d,
                r.get("origen_datos", String.class),
                r.get("emitido_en", OffsetDateTime.class));
    }
}
