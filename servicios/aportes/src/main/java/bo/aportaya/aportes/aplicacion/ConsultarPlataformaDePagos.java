package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.dominio.PlataformaDePagos.Proveedor;
import bo.aportaya.aportes.dominio.PlataformaDePagos.Webhook;
import bo.aportaya.aportes.infraestructura.PlataformaDePagosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sistemas › Proveedores y Webhooks: lo que aportes sabe de sus integraciones, solo lectura. */
@Service
public class ConsultarPlataformaDePagos {

    private final Datos datos;
    private final PlataformaDePagosRepositorio repositorio;

    public ConsultarPlataformaDePagos(Datos datos, PlataformaDePagosRepositorio repositorio) {
        this.datos = datos;
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<Proveedor> proveedores(ContextoSesion ctx) {
        return datos.conContexto(ctx, repositorio::proveedores);
    }

    @Transactional(readOnly = true)
    public List<Webhook> webhooks(int limite, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> repositorio.webhooks(dsl, limite));
    }
}
