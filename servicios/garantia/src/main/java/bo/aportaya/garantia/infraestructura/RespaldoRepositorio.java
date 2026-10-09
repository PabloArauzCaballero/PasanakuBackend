package bo.aportaya.garantia.infraestructura;

import bo.aportaya.garantia.dominio.RespaldoEmpresarial;
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
 * {@code capacidad_respaldo}, {@code reserva_respaldo} y su libro {@code movimiento_reserva}.
 *
 * <p>El organismo <b>inserta el movimiento</b> y la base mantiene los contadores
 * ({@code tg_movimiento_reserva_aplica}, R-GAR-09): las cifras de la reserva son cache
 * del libro, igual que el saldo de la billetera. Por eso aca no hay ningun
 * {@code UPDATE} de montos.
 */
@Component
public class RespaldoRepositorio {

    public record Capacidad(UUID id, Dinero tope, Dinero comprometido) {

        public Dinero disponible() {
            return tope.menos(comprometido);
        }
    }

    public record Reserva(
            UUID id,
            UUID capacidadId,
            UUID grupoId,
            int ciclo,
            UUID responsableId,
            String estado,
            RespaldoEmpresarial.Reserva cifras) {}

    /** Con candado: es el punto de serializacion de dos grupos que activan a la vez. */
    public Optional<Capacidad> bloquearCapacidad(DSLContext dsl, String ambito, Moneda moneda) {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("monto_tope", BigDecimal.class),
                        DSL.field("monto_comprometido", BigDecimal.class))
                .from(DSL.table(DSL.name("garantia", "capacidad_respaldo")))
                .where(DSL.field("ambito", String.class).eq(ambito))
                .and(DSL.field("moneda", String.class).eq(moneda.name()))
                .forUpdate()
                .fetchOptional(f -> new Capacidad(
                        f.get("id", UUID.class),
                        Dinero.de(f.get("monto_tope", BigDecimal.class), moneda),
                        Dinero.de(f.get("monto_comprometido", BigDecimal.class), moneda)));
    }

    public Optional<Reserva> porClave(DSLContext dsl, String clave) {
        return leer(dsl, DSL.field("clave_idempotencia", String.class).eq(clave), false);
    }

    /** La reserva viva de un grupo, bloqueada: dos coberturas no gastan el mismo saldo. */
    public Optional<Reserva> bloquearVigenteDelGrupo(DSLContext dsl, UUID grupoId) {
        return leer(
                dsl,
                DSL.field("grupo_id", UUID.class)
                        .eq(grupoId)
                        .and(DSL.field("estado", String.class).eq("VIGENTE")),
                true);
    }

    /** ¿Ya existe un movimiento con esa clave? La clave unica es la que decide; esto solo responde. */
    public boolean movimientoPorClave(DSLContext dsl, String clave) {
        return dsl.fetchExists(
                DSL.table(DSL.name("garantia", "movimiento_reserva")),
                DSL.field("clave_idempotencia", String.class).eq(clave));
    }

    public Optional<Reserva> bloquear(DSLContext dsl, UUID reservaId) {
        return leer(dsl, DSL.field("id", UUID.class).eq(reservaId), true);
    }

    private Optional<Reserva> leer(DSLContext dsl, org.jooq.Condition donde, boolean bloqueando) {
        var consulta = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("capacidad_respaldo_id", UUID.class),
                        DSL.field("grupo_id", UUID.class),
                        DSL.field("ciclo_numero", Short.class),
                        DSL.field("moneda", String.class),
                        DSL.field("responsable_id", UUID.class),
                        DSL.field("estado", String.class),
                        DSL.field("monto_reservado", BigDecimal.class),
                        DSL.field("monto_aplicado", BigDecimal.class),
                        DSL.field("monto_recuperado", BigDecimal.class),
                        DSL.field("monto_liberado", BigDecimal.class))
                .from(DSL.table(DSL.name("garantia", "reserva_respaldo")))
                .where(donde)
                .limit(1);
        Record fila = bloqueando ? consulta.forUpdate().fetchOne() : consulta.fetchOne();
        return Optional.ofNullable(fila).map(this::aReserva);
    }

    private Reserva aReserva(Record f) {
        Moneda moneda = Moneda.valueOf(f.get("moneda", String.class));
        return new Reserva(
                f.get("id", UUID.class),
                f.get("capacidad_respaldo_id", UUID.class),
                f.get("grupo_id", UUID.class),
                f.get("ciclo_numero", Short.class),
                f.get("responsable_id", UUID.class),
                f.get("estado", String.class),
                new RespaldoEmpresarial.Reserva(
                        Dinero.de(f.get("monto_reservado", BigDecimal.class), moneda),
                        Dinero.de(f.get("monto_aplicado", BigDecimal.class), moneda),
                        Dinero.de(f.get("monto_recuperado", BigDecimal.class), moneda),
                        Dinero.de(f.get("monto_liberado", BigDecimal.class), moneda)));
    }

    public UUID crear(
            DSLContext dsl,
            UUID capacidadId,
            UUID grupoId,
            int ciclo,
            Dinero monto,
            UUID responsableId,
            String clave,
            OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("garantia", "reserva_respaldo")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("capacidad_respaldo_id", UUID.class), capacidadId)
                .set(DSL.field("grupo_id", UUID.class), grupoId)
                .set(DSL.field("ciclo_numero", Short.class), (short) ciclo)
                .set(DSL.field("moneda", String.class), monto.moneda().name())
                .set(DSL.field("monto_reservado", BigDecimal.class), monto.monto())
                .set(DSL.field("estado", String.class), "VIGENTE")
                .set(DSL.field("responsable_id", UUID.class), responsableId)
                .set(DSL.field("clave_idempotencia", String.class), clave)
                .set(DSL.field("reservada_en", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    /** Un renglon del libro. La base actualiza los contadores en el mismo instante. */
    public UUID movimiento(
            DSLContext dsl,
            UUID reservaId,
            String tipo,
            Dinero monto,
            RespaldoEmpresarial.Reserva despues,
            String referenciaTipo,
            UUID referenciaId,
            String clave,
            UUID registradoPor,
            UUID responsableId,
            OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("garantia", "movimiento_reserva")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("reserva_respaldo_id", UUID.class), reservaId)
                .set(DSL.field("tipo", String.class), tipo)
                .set(DSL.field("monto", BigDecimal.class), monto.monto())
                .set(DSL.field("moneda", String.class), monto.moneda().name())
                .set(
                        DSL.field("disponible_resultante", BigDecimal.class),
                        despues.disponible().monto())
                .set(
                        DSL.field("exposicion_resultante", BigDecimal.class),
                        despues.exposicion().monto())
                .set(DSL.field("referencia_tipo", String.class), referenciaTipo)
                .set(DSL.field("referencia_id", UUID.class), referenciaId)
                .set(DSL.field("clave_idempotencia", String.class), clave)
                .set(DSL.field("registrado_por", UUID.class), registradoPor)
                .set(DSL.field("responsable_id", UUID.class), responsableId)
                .set(DSL.field("fecha", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }
}
