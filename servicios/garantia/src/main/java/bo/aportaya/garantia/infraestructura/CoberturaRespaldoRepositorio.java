package bo.aportaya.garantia.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code cobertura_respaldo} (una por turno), sus lineas (una por obligacion faltante)
 * y {@code recuperacion_respaldo} (una por pago, append-only).
 *
 * <p>La cobertura es el puente entre una reserva y un pozo: dice cuanto no llego, de
 * quien era cada pedazo, y despues cuanto volvio. Las lineas existen para que un aporte
 * tardio sepa exactamente a que adelanto de la empresa corresponde.
 */
@Component
public class CoberturaRespaldoRepositorio {

    public record Cobertura(
            UUID id,
            UUID reservaId,
            UUID grupoId,
            UUID turnoId,
            String estado,
            Dinero faltante,
            Dinero recuperado,
            String clave) {}

    public record Linea(UUID id, UUID coberturaId, UUID obligacionId, Dinero cubierto, Dinero recuperado) {}

    public Optional<Cobertura> porClave(DSLContext dsl, String clave) {
        return leer(dsl, DSL.field("clave_idempotencia", String.class).eq(clave), false);
    }

    /** La cobertura viva del turno (una sola, R-GAR-10), bloqueada. */
    public Optional<Cobertura> bloquearVivaDelTurno(DSLContext dsl, UUID turnoId) {
        return leer(
                dsl,
                DSL.field("turno_id", UUID.class)
                        .eq(turnoId)
                        .and(DSL.field("estado", String.class).ne("REVERSADA")),
                true);
    }

    /** Sin candado: sirve para saber de que reserva es, y bloquear primero la reserva. */
    public Optional<Cobertura> ver(DSLContext dsl, UUID coberturaId) {
        return leer(dsl, DSL.field("id", UUID.class).eq(coberturaId), false);
    }

    public Optional<Cobertura> bloquear(DSLContext dsl, UUID coberturaId) {
        return leer(dsl, DSL.field("id", UUID.class).eq(coberturaId), true);
    }

    private Optional<Cobertura> leer(DSLContext dsl, org.jooq.Condition donde, boolean bloqueando) {
        var consulta = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("reserva_respaldo_id", UUID.class),
                        DSL.field("grupo_id", UUID.class),
                        DSL.field("turno_id", UUID.class),
                        DSL.field("estado", String.class),
                        DSL.field("moneda", String.class),
                        DSL.field("monto_faltante", BigDecimal.class),
                        DSL.field("monto_recuperado", BigDecimal.class),
                        DSL.field("clave_idempotencia", String.class))
                .from(DSL.table(DSL.name("garantia", "cobertura_respaldo")))
                .where(donde)
                .limit(1);
        var fila = bloqueando ? consulta.forUpdate().fetchOne() : consulta.fetchOne();
        return Optional.ofNullable(fila).map(f -> {
            Moneda moneda = Moneda.valueOf(f.get("moneda", String.class));
            return new Cobertura(
                    f.get("id", UUID.class),
                    f.get("reserva_respaldo_id", UUID.class),
                    f.get("grupo_id", UUID.class),
                    f.get("turno_id", UUID.class),
                    f.get("estado", String.class),
                    Dinero.de(f.get("monto_faltante", BigDecimal.class), moneda),
                    Dinero.de(f.get("monto_recuperado", BigDecimal.class), moneda),
                    f.get("clave_idempotencia", String.class));
        });
    }

    public record Alta(
            UUID reservaId,
            UUID grupoId,
            UUID periodoId,
            UUID turnoId,
            Dinero pozo,
            Dinero confirmado,
            Dinero cubiertoMutual,
            Dinero faltante,
            OffsetDateTime corte,
            String clave,
            UUID solicitadaPor,
            UUID responsableId) {}

    public UUID crear(DSLContext dsl, Alta d, OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("garantia", "cobertura_respaldo")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("reserva_respaldo_id", UUID.class), d.reservaId())
                .set(DSL.field("grupo_id", UUID.class), d.grupoId())
                .set(DSL.field("periodo_id", UUID.class), d.periodoId())
                .set(DSL.field("turno_id", UUID.class), d.turnoId())
                .set(DSL.field("moneda", String.class), d.pozo().moneda().name())
                .set(DSL.field("monto_pozo", BigDecimal.class), d.pozo().monto())
                .set(
                        DSL.field("monto_confirmado", BigDecimal.class),
                        d.confirmado().monto())
                .set(
                        DSL.field("monto_cubierto_mutual", BigDecimal.class),
                        d.cubiertoMutual().monto())
                .set(DSL.field("monto_faltante", BigDecimal.class), d.faltante().monto())
                .set(DSL.field("monto_recuperado", BigDecimal.class), BigDecimal.ZERO)
                .set(DSL.field("estado", String.class), "APLICADA")
                .set(DSL.field("corte_en", OffsetDateTime.class), d.corte())
                .set(DSL.field("clave_idempotencia", String.class), d.clave())
                .set(DSL.field("solicitada_por", UUID.class), d.solicitadaPor())
                .set(DSL.field("responsable_id", UUID.class), d.responsableId())
                .set(DSL.field("aplicada_en", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    public void agregarLinea(DSLContext dsl, UUID coberturaId, UUID obligacionId, Dinero monto) {
        dsl.insertInto(DSL.table(DSL.name("garantia", "cobertura_respaldo_linea")))
                .set(DSL.field("id", UUID.class), UUID.randomUUID())
                .set(DSL.field("cobertura_respaldo_id", UUID.class), coberturaId)
                .set(DSL.field("obligacion_id", UUID.class), obligacionId)
                .set(DSL.field("monto_cubierto", BigDecimal.class), monto.monto())
                .set(DSL.field("monto_recuperado", BigDecimal.class), BigDecimal.ZERO)
                .execute();
    }

    /**
     * La linea viva de una obligacion (la de una cobertura no reversada).
     *
     * <p>{@code bloqueando = false} sirve para averiguar de que reserva es: el orden de
     * candados es siempre reserva, cobertura, linea.
     */
    public Optional<Linea> lineaViva(DSLContext dsl, UUID obligacionId, Moneda moneda, boolean bloqueando) {
        var vivas = DSL.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("garantia", "cobertura_respaldo")))
                .where(DSL.field("estado", String.class).ne("REVERSADA"));
        var consulta = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("cobertura_respaldo_id", UUID.class),
                        DSL.field("monto_cubierto", BigDecimal.class),
                        DSL.field("monto_recuperado", BigDecimal.class))
                .from(DSL.table(DSL.name("garantia", "cobertura_respaldo_linea")))
                .where(DSL.field("obligacion_id", UUID.class).eq(obligacionId))
                .and(DSL.field("cobertura_respaldo_id", UUID.class).in(vivas));
        var fila = bloqueando ? consulta.forUpdate().fetchOne() : consulta.fetchOne();
        return Optional.ofNullable(fila)
                .map(f -> new Linea(
                        f.get("id", UUID.class),
                        f.get("cobertura_respaldo_id", UUID.class),
                        obligacionId,
                        Dinero.de(f.get("monto_cubierto", BigDecimal.class), moneda),
                        Dinero.de(f.get("monto_recuperado", BigDecimal.class), moneda)));
    }

    public List<Linea> lineasDe(DSLContext dsl, UUID coberturaId, Moneda moneda) {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("obligacion_id", UUID.class),
                        DSL.field("monto_cubierto", BigDecimal.class),
                        DSL.field("monto_recuperado", BigDecimal.class))
                .from(DSL.table(DSL.name("garantia", "cobertura_respaldo_linea")))
                .where(DSL.field("cobertura_respaldo_id", UUID.class).eq(coberturaId))
                .orderBy(DSL.field("obligacion_id"))
                .fetch(f -> new Linea(
                        f.get("id", UUID.class),
                        coberturaId,
                        f.get("obligacion_id", UUID.class),
                        Dinero.de(f.get("monto_cubierto", BigDecimal.class), moneda),
                        Dinero.de(f.get("monto_recuperado", BigDecimal.class), moneda)));
    }

    /** Suma lo recuperado de la linea y de la cobertura, con el estado que corresponde. */
    public void sumarRecuperado(DSLContext dsl, Linea linea, Cobertura cobertura, Dinero monto) {
        dsl.update(DSL.table(DSL.name("garantia", "cobertura_respaldo_linea")))
                .set(
                        DSL.field("monto_recuperado", BigDecimal.class),
                        DSL.field("monto_recuperado", BigDecimal.class).plus(monto.monto()))
                .where(DSL.field("id", UUID.class).eq(linea.id()))
                .execute();
        Dinero total = cobertura.recuperado().mas(monto);
        String estado = total.equals(cobertura.faltante()) ? "RECUPERADA_TOTAL" : "RECUPERADA_PARCIAL";
        dsl.update(DSL.table(DSL.name("garantia", "cobertura_respaldo")))
                .set(DSL.field("monto_recuperado", BigDecimal.class), total.monto())
                .set(DSL.field("estado", String.class), estado)
                .set(
                        DSL.field("version", Integer.class),
                        DSL.field("version", Integer.class).plus(1))
                .where(DSL.field("id", UUID.class).eq(cobertura.id()))
                .execute();
    }

    public void registrarRecuperacion(
            DSLContext dsl, UUID lineaId, UUID pagoId, UUID movimientoId, Dinero monto, OffsetDateTime ahora) {
        dsl.insertInto(DSL.table(DSL.name("garantia", "recuperacion_respaldo")))
                .set(DSL.field("id", UUID.class), UUID.randomUUID())
                .set(DSL.field("cobertura_respaldo_linea_id", UUID.class), lineaId)
                .set(DSL.field("pago_id", UUID.class), pagoId)
                .set(DSL.field("movimiento_reserva_id", UUID.class), movimientoId)
                .set(DSL.field("monto", BigDecimal.class), monto.monto())
                .set(DSL.field("moneda", String.class), monto.moneda().name())
                .set(DSL.field("recuperada_en", OffsetDateTime.class), ahora)
                .execute();
    }

    /** ¿Ya se uso este pago para recuperar? El indice unico decide; esto solo da la respuesta. */
    public Optional<BigDecimal> recuperacionDelPago(DSLContext dsl, UUID pagoId) {
        return dsl.select(DSL.field("monto", BigDecimal.class))
                .from(DSL.table(DSL.name("garantia", "recuperacion_respaldo")))
                .where(DSL.field("pago_id", UUID.class).eq(pagoId))
                .fetchOptional(f -> f.get("monto", BigDecimal.class));
    }

    public boolean marcarReversada(DSLContext dsl, UUID coberturaId) {
        return dsl.update(DSL.table(DSL.name("garantia", "cobertura_respaldo")))
                        .set(DSL.field("estado", String.class), "REVERSADA")
                        .set(
                                DSL.field("version", Integer.class),
                                DSL.field("version", Integer.class).plus(1))
                        .where(DSL.field("id", UUID.class).eq(coberturaId))
                        .and(DSL.field("estado", String.class).eq("APLICADA"))
                        .and(DSL.field("monto_recuperado", BigDecimal.class).eq(BigDecimal.ZERO))
                        .execute()
                == 1;
    }
}
