package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** {@code conciliacion_custodia} y los cierres diarios contra los que compara. */
@Component
public class ConciliacionRepositorio {

    public boolean existeDelDia(DSLContext dsl, UUID cuentaCustodiaId, LocalDate fecha) {
        return dsl.fetchCount(
                        DSL.table(DSL.name("nucleo_financiero", "conciliacion_custodia")),
                        DSL.field("cuenta_custodia_id", UUID.class).eq(cuentaCustodiaId),
                        DSL.field("fecha", LocalDate.class).eq(fecha))
                > 0;
    }

    /** ¿Se cerraron los saldos de ese dia? Sin ellos no hay contra que comparar. */
    public boolean haySaldosDelDia(DSLContext dsl, LocalDate fecha) {
        return dsl.fetchCount(
                        DSL.table(DSL.name("nucleo_financiero", "saldo_diario_billetera")),
                        DSL.field("fecha", LocalDate.class).eq(fecha))
                > 0;
    }

    /**
     * Registra la conciliacion.
     *
     * <p>{@code diferencia} y {@code ratio_cobertura} son columnas GENERATED: las
     * calcula la base a partir de los saldos. No se envian — y esta bien que sea asi,
     * porque el numero que decide si hay encaje no puede depender de que la aplicacion
     * lo calcule igual que la consulta que despues lo audita.
     */
    public UUID registrar(
            DSLContext dsl,
            UUID cuentaCustodiaId,
            LocalDate fecha,
            Dinero emitido,
            Dinero custodia,
            Dinero enTransito,
            boolean cumpleEncaje,
            UUID ejecutadaPor,
            OffsetDateTime ahora) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("nucleo_financiero", "conciliacion_custodia")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("cuenta_custodia_id", UUID.class), cuentaCustodiaId)
                .set(DSL.field("ejecutada_por", UUID.class), ejecutadaPor)
                .set(DSL.field("fecha", LocalDate.class), fecha)
                .set(DSL.field("saldo_dinero_electronico", BigDecimal.class), emitido.monto())
                .set(DSL.field("saldo_custodia", BigDecimal.class), custodia.monto())
                .set(DSL.field("saldo_en_transito", BigDecimal.class), enTransito.monto())
                .set(DSL.field("cumple_encaje", Boolean.class), cumpleEncaje)
                .set(DSL.field("estado", String.class), cumpleEncaje ? "CUADRADA" : "DESCUADRADA")
                .set(DSL.field("ejecutada_en", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    /** Cierra el saldo del dia de una cuenta: es lo que el extracto y la conciliacion leen. */
    public UUID cerrarSaldoDelDia(
            DSLContext dsl,
            UUID cuentaId,
            LocalDate fecha,
            Dinero disponible,
            Dinero retenido,
            int movimientos,
            String hash,
            String hashAnterior,
            OffsetDateTime ahora) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("nucleo_financiero", "saldo_diario_billetera")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("cuenta_billetera_id", UUID.class), cuentaId)
                .set(DSL.field("fecha", LocalDate.class), fecha)
                .set(DSL.field("saldo_disponible", BigDecimal.class), disponible.monto())
                .set(DSL.field("saldo_retenido", BigDecimal.class), retenido.monto())
                .set(DSL.field("cantidad_movimientos", Integer.class), movimientos)
                .set(DSL.field("hash_registro", String.class), hash)
                .set(DSL.field("hash_anterior", String.class), hashAnterior)
                .set(DSL.field("cerrado_en", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    /** El sello del ultimo dia cerrado antes de {@code fecha}: el eslabon al que se encadena el nuevo. */
    public java.util.Optional<String> selloAnterior(DSLContext dsl, UUID cuentaId, LocalDate fecha) {
        return dsl.select(DSL.field("hash_registro", String.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "saldo_diario_billetera")))
                .where(DSL.field("cuenta_billetera_id", UUID.class).eq(cuentaId))
                .and(DSL.field("fecha", LocalDate.class).lt(fecha))
                .orderBy(DSL.field("fecha").desc())
                .limit(1)
                .fetchOptional(DSL.field("hash_registro", String.class));
    }

    /** La cadena de saldos diarios de una cuenta, del dia mas viejo al mas nuevo, con la moneda de la cuenta. */
    public java.util.List<bo.aportaya.nucleofinanciero.dominio.SelloDelDia.Eslabon> cadena(
            DSLContext dsl, UUID cuentaId) {
        return dsl.resultQuery(
                        """
                        SELECT s.cuenta_billetera_id, s.fecha, c.moneda, s.saldo_disponible, s.saldo_retenido,
                               s.hash_registro, s.hash_anterior
                          FROM nucleo_financiero.saldo_diario_billetera s
                          JOIN nucleo_financiero.cuenta_billetera c ON c.id = s.cuenta_billetera_id
                         WHERE s.cuenta_billetera_id = ?
                         ORDER BY s.fecha
                        """,
                        cuentaId)
                .fetch(r -> new bo.aportaya.nucleofinanciero.dominio.SelloDelDia.Eslabon(
                        r.get(0, UUID.class),
                        r.get(1, LocalDate.class),
                        r.get(2, String.class),
                        r.get(3, BigDecimal.class),
                        r.get(4, BigDecimal.class),
                        r.get(5, String.class),
                        r.get(6, String.class)));
    }
}
