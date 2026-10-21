package bo.aportaya.identidad.infraestructura;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * El telefono de un integrante para el perfil del grupo (2026-10-21). Lo lee el proceso interno porque la politica de
 * fila no le deja a un participante ver la fila de otra persona: por eso solo se llama DESPUES de que grupos confirmo
 * que comparten el grupo ({@code ConsultarContactoEnGrupo}). Devuelve el telefono y nada mas.
 */
@Repository
public class ContactoEnGrupoRepositorio {

    private final Datos datos;

    public ContactoEnGrupoRepositorio(Datos datos) {
        this.datos = datos;
    }

    @Transactional(readOnly = true)
    public Optional<String> telefono(UUID usuarioId, ContextoSesion ctx) {
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(interno, dsl -> Optional.ofNullable(
                        dsl.fetchOne("SELECT telefono_e164 FROM identidad.usuario WHERE id = ?", usuarioId))
                .map(fila -> fila.get(0, String.class)));
    }
}
