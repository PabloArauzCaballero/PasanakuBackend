package bo.aportaya.organizador.aplicacion;

import bo.aportaya.organizador.infraestructura.OrganizadorRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * De quien es un organizador, para preguntarle a otro servicio por esa persona ANTES de abrir la transaccion de la
 * escritura. Vive en su propia clase porque {@code @Transactional} solo tiene efecto cuando la llamada entra por el
 * proxy (invariante 3: sin transaccion la politica de fila no se aplica).
 */
@Service
public class UsuarioDelOrganizador {

    private final Datos datos;
    private final OrganizadorRepositorio organizadores;

    public UsuarioDelOrganizador(Datos datos, OrganizadorRepositorio organizadores) {
        this.datos = datos;
        this.organizadores = organizadores;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> usuarioDe(UUID organizadorId, ContextoSesion ctx) {
        return datos.conContexto(
                ctx, dsl -> organizadores.ver(dsl, organizadorId).map(o -> o.usuarioId()));
    }
}
