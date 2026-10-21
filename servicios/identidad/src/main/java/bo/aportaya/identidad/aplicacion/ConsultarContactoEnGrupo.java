package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.ContactoEnGrupoRepositorio;
import bo.aportaya.identidad.infraestructura.clientes.GruposPorHttp;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Perfil del integrante (2026-10-21): el telefono de una persona, solo para quien esta en el mismo grupo (decision del
 * dueño). La pertenencia es de grupos y se le pregunta FUERA de la transaccion (invariante 6); si no responde o dice
 * que no, es el mismo 403 que para quien no comparte el grupo, asi que la respuesta no cuenta si la persona existe.
 */
@Service
public class ConsultarContactoEnGrupo {

    private final GruposPorHttp grupos;
    private final ContactoEnGrupoRepositorio contactos;

    public ConsultarContactoEnGrupo(GruposPorHttp grupos, ContactoEnGrupoRepositorio contactos) {
        this.grupos = grupos;
        this.contactos = contactos;
    }

    public String ejecutar(UUID grupoId, UUID usuarioId, ContextoSesion ctx) {
        if (!grupos.compartenGrupo(grupoId, usuarioId)) {
            throw new AccessDeniedException("no comparte el grupo");
        }
        return contactos
                .telefono(grupoId, usuarioId, ctx)
                .orElseThrow(() -> new AccessDeniedException("no comparte el grupo"));
    }
}
