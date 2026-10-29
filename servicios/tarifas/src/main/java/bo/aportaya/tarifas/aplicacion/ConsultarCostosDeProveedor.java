package bo.aportaya.tarifas.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.tarifas.dominio.CostoDeProveedor;
import bo.aportaya.tarifas.infraestructura.CostosDeProveedorRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sistemas › Proveedores: el costo real de cada proveedor en un periodo. El formato del periodo lo valida el contrato. */
@Service
public class ConsultarCostosDeProveedor {

    private final Datos datos;
    private final CostosDeProveedorRepositorio repositorio;

    public ConsultarCostosDeProveedor(Datos datos, CostosDeProveedorRepositorio repositorio) {
        this.datos = datos;
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<CostoDeProveedor> delPeriodo(String periodo, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> repositorio.delPeriodo(dsl, periodo));
    }
}
