package bo.aportaya.inversiones.infraestructura;

import static bo.aportaya.inversiones.infraestructura.Sql.f;
import static bo.aportaya.inversiones.infraestructura.Sql.t;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/**
 * {@code distribucion_interes}: el interes que el emisor pago y que se reparte al titular,
 * conciliado contra el devengo propio. Append-only: una discrepancia se registra y queda a
 * la vista, no se corrige en silencio ({@code R-INV-09}).
 */
@Component
public class DistribucionRepositorio {

    public record Distribucion(
            UUID id,
            UUID posicionId,
            UUID rescateId,
            UUID usuarioId,
            LocalDate periodoDesde,
            LocalDate periodoHasta,
            BigDecimal interesDevengado,
            BigDecimal penalizacion,
            BigDecimal interesEsperado,
            BigDecimal interesPagado,
            BigDecimal retencion,
            BigDecimal interesNeto,
            String moneda,
            String estado,
            String motivoDiscrepancia,
            OffsetDateTime distribuidaEn) {}

    /** @return {@code true} si la inserto este actor; el UNIQUE por rescate hace idempotente el reparto. */
    public boolean registrar(DSLContext dsl, Distribucion d) {
        return dsl.insertInto(t("distribucion_interes"))
                        .set(f("id", UUID.class), d.id())
                        .set(f("posicion_inversion_id", UUID.class), d.posicionId())
                        .set(f("rescate_inversion_id", UUID.class), d.rescateId())
                        .set(f("usuario_id", UUID.class), d.usuarioId())
                        .set(f("periodo_desde", LocalDate.class), d.periodoDesde())
                        .set(f("periodo_hasta", LocalDate.class), d.periodoHasta())
                        .set(f("interes_devengado", BigDecimal.class), d.interesDevengado())
                        .set(f("penalizacion", BigDecimal.class), d.penalizacion())
                        .set(f("interes_esperado", BigDecimal.class), d.interesEsperado())
                        .set(f("interes_pagado", BigDecimal.class), d.interesPagado())
                        .set(f("retencion", BigDecimal.class), d.retencion())
                        .set(f("interes_neto", BigDecimal.class), d.interesNeto())
                        .set(f("moneda", String.class), d.moneda())
                        .set(f("estado", String.class), d.estado())
                        .set(f("motivo_discrepancia", String.class), d.motivoDiscrepancia())
                        .set(f("distribuida_en", OffsetDateTime.class), d.distribuidaEn())
                        .onConflictDoNothing()
                        .execute()
                == 1;
    }

    public List<Distribucion> deLaPosicion(DSLContext dsl, UUID posicionId) {
        return dsl.selectFrom(t("distribucion_interes"))
                .where(f("posicion_inversion_id", UUID.class).eq(posicionId))
                .orderBy(f("distribuida_en", OffsetDateTime.class), f("id", UUID.class))
                .fetch(this::distribucion);
    }

    /** Las del titular cuyo reparto cae en el periodo, para el reporte fiscal. */
    public List<Distribucion> delTitular(DSLContext dsl, UUID usuarioId, OffsetDateTime desde, OffsetDateTime hasta) {
        return dsl.selectFrom(t("distribucion_interes"))
                .where(f("usuario_id", UUID.class).eq(usuarioId))
                .and(f("distribuida_en", OffsetDateTime.class).ge(desde))
                .and(f("distribuida_en", OffsetDateTime.class).lt(hasta))
                .orderBy(f("distribuida_en", OffsetDateTime.class), f("id", UUID.class))
                .fetch(this::distribucion);
    }

    private Distribucion distribucion(Record r) {
        return new Distribucion(
                r.get("id", UUID.class),
                r.get("posicion_inversion_id", UUID.class),
                r.get("rescate_inversion_id", UUID.class),
                r.get("usuario_id", UUID.class),
                r.get("periodo_desde", LocalDate.class),
                r.get("periodo_hasta", LocalDate.class),
                r.get("interes_devengado", BigDecimal.class),
                r.get("penalizacion", BigDecimal.class),
                r.get("interes_esperado", BigDecimal.class),
                r.get("interes_pagado", BigDecimal.class),
                r.get("retencion", BigDecimal.class),
                r.get("interes_neto", BigDecimal.class),
                r.get("moneda", String.class),
                r.get("estado", String.class),
                r.get("motivo_discrepancia", String.class),
                r.get("distribuida_en", OffsetDateTime.class));
    }
}
