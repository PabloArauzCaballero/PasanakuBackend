package bo.aportaya.aportes.infraestructura;

import bo.aportaya.aportes.dominio.RecaudoDelPeriodo;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Las obligaciones periodicas de un periodo, para saber cuanto del pozo es caja.
 *
 * <p>Lee {@code obligacion_aporte}, que es de este servicio. {@code monto_pagado} solo lo
 * mueve CU-21 despues de la confirmacion del proveedor, en la misma transaccion que el
 * pago: por eso es la fuente de lo «confirmado». Contrastarlo contra el mayor es trabajo
 * de la conciliacion (H13), no de esta consulta.
 */
@Component
public class RecaudoRepositorio {

    public record Fila(UUID grupoId, Moneda moneda, List<RecaudoDelPeriodo.Obligacion> obligaciones) {}

    /** Orden estable por identificador: dos consultas seguidas devuelven la misma lista. */
    public Fila obligacionesDelPeriodo(DSLContext dsl, UUID periodoId) {
        var filas = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("grupo_id", UUID.class),
                        DSL.field("moneda", String.class),
                        DSL.field("monto_esperado", BigDecimal.class),
                        DSL.field("monto_pagado", BigDecimal.class),
                        DSL.field("monto_cubierto_garantia", BigDecimal.class))
                .from(DSL.table(DSL.name("aportes", "obligacion_aporte")))
                .where(DSL.field("periodo_id", UUID.class).eq(periodoId))
                .and(DSL.field("tipo", String.class).eq("APORTE_PERIODICO"))
                .and(DSL.field("estado", String.class).ne("ANULADO"))
                .orderBy(DSL.field("id"))
                .fetch();
        if (filas.isEmpty()) {
            return new Fila(null, Moneda.BOB, List.of());
        }
        Moneda moneda = Moneda.valueOf(filas.get(0).get("moneda", String.class));
        UUID grupoId = filas.get(0).get("grupo_id", UUID.class);
        boolean variosGrupos = filas.stream().anyMatch(f -> !grupoId.equals(f.get("grupo_id", UUID.class)));
        if (variosGrupos) {
            throw new bo.aportaya.plataforma.dominio.ErrorDeNegocio(
                    bo.aportaya.plataforma.dominio.CodigoError.de(21, 5),
                    "Las obligaciones del periodo son de grupos distintos.");
        }
        var obligaciones = filas.stream()
                .map(f -> {
                    Moneda m = Moneda.valueOf(f.get("moneda", String.class));
                    return new RecaudoDelPeriodo.Obligacion(
                            f.get("id", UUID.class),
                            Dinero.de(f.get("monto_esperado", BigDecimal.class), m),
                            Dinero.de(f.get("monto_pagado", BigDecimal.class), m),
                            Dinero.de(f.get("monto_cubierto_garantia", BigDecimal.class), m));
                })
                .toList();
        return new Fila(grupoId, moneda, obligaciones);
    }
}
