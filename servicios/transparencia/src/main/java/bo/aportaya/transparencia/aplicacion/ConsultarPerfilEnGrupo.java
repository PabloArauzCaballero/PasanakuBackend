package bo.aportaya.transparencia.aplicacion;

import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.transparencia.dominio.PerfilDeReputacionEnGrupo;
import bo.aportaya.transparencia.infraestructura.PerfilEnGrupoRepositorio;
import bo.aportaya.transparencia.infraestructura.clientes.GruposPorHttp;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Perfil del integrante (2026-10-21): la reputacion de una persona, solo para quien esta en el mismo grupo (decision
 * del dueño). La pertenencia es de grupos y se le pregunta FUERA de la transaccion (invariante 6); si no responde o
 * dice que no, 403. Quien no tiene historial no tiene ceros: el perfil lo dice y las cifras quedan vacias.
 */
@Service
public class ConsultarPerfilEnGrupo {

    private final GruposPorHttp grupos;
    private final PerfilEnGrupoRepositorio perfiles;

    public ConsultarPerfilEnGrupo(GruposPorHttp grupos, PerfilEnGrupoRepositorio perfiles) {
        this.grupos = grupos;
        this.perfiles = perfiles;
    }

    public PerfilDeReputacionEnGrupo ejecutar(UUID grupoId, UUID usuarioId, ContextoSesion ctx) {
        if (!grupos.compartenGrupo(grupoId, usuarioId)) {
            throw new AccessDeniedException("no comparte el grupo");
        }
        return perfiles.de(usuarioId, ctx);
    }
}
