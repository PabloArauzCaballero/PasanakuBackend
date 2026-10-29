package bo.aportaya.tarifas.infraestructura;

import bo.aportaya.tarifas.dominio.CostoDeProveedor;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/** Suma {@code tarifas.costo_proveedor_operacion} por proveedor y moneda en un periodo. Solo lectura. */
@Component
public class CostosDeProveedorRepositorio {

    public List<CostoDeProveedor> delPeriodo(DSLContext dsl, String periodo) {
        return dsl.fetch(
                        """
                        select proveedor_id, periodo, moneda, sum(costo_total) as total, count(*)::int as operaciones
                          from tarifas.costo_proveedor_operacion
                         where periodo = ?
                         group by proveedor_id, periodo, moneda
                         order by proveedor_id, moneda
                         limit 500
                        """,
                        periodo)
                .map(r -> new CostoDeProveedor(
                        r.get("proveedor_id", UUID.class),
                        r.get("periodo", String.class),
                        r.get("moneda", String.class),
                        r.get("total", BigDecimal.class),
                        r.get("operaciones", Integer.class)));
    }
}
