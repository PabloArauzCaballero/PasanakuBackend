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
 *
 * <p><b>El puntaje exacto solo lo ve su dueño</b> (decision del dueño 2026-10-30, regla 90.2.8): si quien pregunta no
 * es {@code usuarioId}, el perfil sale sin {@code puntaje}, igual que {@link ConsultarPuntaje} lo niega con 403. Sin
 * esto, esta ruta devolvia el mismo numero que aquella niega.
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
        PerfilDeReputacionEnGrupo perfil = perfiles.de(usuarioId, ctx);
        return usuarioId.equals(ctx.usuarioId()) ? perfil : perfil.sinPuntaje();
    }
}
