package bo.aportaya.aportes.infraestructura;

import bo.aportaya.aportes.dominio.ConciliacionTripartita.MovimientoDelBanco;
import bo.aportaya.aportes.dominio.ConciliacionTripartita.PagoDelLibro;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code conciliacion}, {@code excepcion_conciliacion} y la lectura del libro y del extracto.
 *
 * <p>Una conciliacion por pago ({@code uq_conciliacion_pago_id}): correr el lote dos veces no abre
 * dos excepciones por el mismo hallazgo. Aca no se acredita, no se descuenta y no se toca un
 * pago: solo se LEE el libro y se ANOTA lo que se encontro.
 */
@Component
public class ConciliacionRepositorio {

    public List<PagoDelLibro> pagosAcreditados(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("referencia_proveedor", String.class),
                        DSL.field("monto", BigDecimal.class),
                        DSL.field("moneda", String.class))
                .from(DSL.table(DSL.name("aportes", "pago")))
                .where(DSL.field("estado", String.class).eq("ACREDITADO"))
                .and(DSL.field("fecha_hora_acreditacion", OffsetDateTime.class)
                        .ge(desde.atStartOfDay().atOffset(java.time.ZoneOffset.UTC)))
                .and(DSL.field("fecha_hora_acreditacion", OffsetDateTime.class)
                        .lt(hasta.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC)))
                .orderBy(DSL.field("referencia_proveedor"), DSL.field("id"))
                .fetch(f -> new PagoDelLibro(
                        f.get("id", UUID.class),
                        f.get("referencia_proveedor", String.class),
                        Dinero.de(f.get("monto", BigDecimal.class), Moneda.valueOf(f.get("moneda", String.class)))));
    }

    public List<MovimientoDelBanco> movimientosDelExtracto(DSLContext dsl, UUID extractoId) {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("referencia_banco", String.class),
                        DSL.field("monto", BigDecimal.class),
                        DSL.field("moneda", String.class))
                .from(DSL.table(DSL.name("aportes", "movimiento_bancario")))
                .where(DSL.field("extracto_id", UUID.class).eq(extractoId))
                .orderBy(DSL.field("referencia_banco"), DSL.field("id"))
                .fetch(f -> new MovimientoDelBanco(
                        f.get("id", UUID.class),
                        f.get("referencia_banco", String.class),
                        Dinero.de(f.get("monto", BigDecimal.class), Moneda.valueOf(f.get("moneda", String.class)))));
    }

    /** @return el id de la conciliacion NUEVA, o vacio si ese pago ya estaba conciliado. */
    public Optional<UUID> registrar(
            DSLContext dsl,
            UUID pagoId,
            UUID movimientoId,
            String estado,
            BigDecimal diferencia,
            OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        int filas = dsl.insertInto(DSL.table(DSL.name("aportes", "conciliacion")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("pago_id", UUID.class), pagoId)
                .set(DSL.field("movimiento_bancario_id", UUID.class), movimientoId)
                .set(DSL.field("estado", String.class), estado)
                .set(DSL.field("metodo", String.class), "REFERENCIA_EXACTA")
                .set(DSL.field("diferencia_monto", BigDecimal.class), diferencia)
                .set(
                        DSL.field("fecha_conciliacion", OffsetDateTime.class),
                        "CONCILIADO_AUTOMATICO".equals(estado) ? ahora : null)
                .onConflictDoNothing()
                .execute();
        return filas == 1 ? Optional.of(id) : Optional.empty();
    }

    public void abrirExcepcion(
            DSLContext dsl, UUID conciliacionId, String tipo, String descripcion, BigDecimal diferencia) {
        dsl.insertInto(DSL.table(DSL.name("aportes", "excepcion_conciliacion")))
                .set(DSL.field("id", UUID.class), UUID.randomUUID())
                .set(DSL.field("conciliacion_id", UUID.class), conciliacionId)
                .set(DSL.field("tipo", String.class), tipo)
                .set(
                        DSL.field("descripcion", String.class),
                        descripcion.length() > 300 ? descripcion.substring(0, 300) : descripcion)
                .set(DSL.field("monto_diferencia", BigDecimal.class), diferencia)
                .set(DSL.field("estado", String.class), "ABIERTA")
                .execute();
    }

    public void marcarConciliado(DSLContext dsl, UUID movimientoId) {
        dsl.update(DSL.table(DSL.name("aportes", "movimiento_bancario")))
                .set(DSL.field("conciliado", Boolean.class), true)
                .where(DSL.field("id", UUID.class).eq(movimientoId))
                .execute();
    }
}
