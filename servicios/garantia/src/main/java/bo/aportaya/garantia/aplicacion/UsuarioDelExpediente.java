package bo.aportaya.garantia.aplicacion;

import bo.aportaya.garantia.infraestructura.ExpedienteRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * De quien es un expediente, para preguntarle a otro servicio por esa persona ANTES de abrir la transaccion de la
 * escritura. Vive en su propia clase porque {@code @Transactional} solo tiene efecto cuando la llamada entra por el
 * proxy (invariante 3: sin transaccion la politica de fila no se aplica).
 */
@Service
public class UsuarioDelExpediente {

    private final Datos datos;
    private final ExpedienteRepositorio expedientes;

    public UsuarioDelExpediente(Datos datos, ExpedienteRepositorio expedientes) {
        this.datos = datos;
        this.expedientes = expedientes;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> usuarioDe(UUID expedienteId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> expedientes.ver(dsl, expedienteId).map(e -> e.usuarioId()));
    }
}
