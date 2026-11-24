package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.USUARIO_INTERNO;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Nombres y apellidos de los usuarios internos ACTIVOS de un lote. Lee dos columnas y nada mas: ni correo, ni
 * telefono, ni documento, ni roles (regla 90.1.8). El JOIN con {@code usuario_interno} es el filtro que importa:
 * un participante (cliente) nunca sale de aca, aunque su id venga en la lista.
 */
@Component
public class NombresDeUsuariosInternosRepositorio {

    public Map<UUID, Nombre> activos(DSLContext dsl, Collection<UUID> usuarioIds) {
        Map<UUID, Nombre> resultado = new LinkedHashMap<>();
        if (usuarioIds.isEmpty()) {
            return resultado;
        }
        dsl.select(USUARIO_INTERNO.USUARIO_ID, USUARIO.NOMBRES, USUARIO.APELLIDOS)
                .from(USUARIO_INTERNO)
                .join(USUARIO)
                .on(USUARIO.ID.eq(USUARIO_INTERNO.USUARIO_ID))
                .where(USUARIO_INTERNO.USUARIO_ID.in(usuarioIds))
                .and(USUARIO.ESTADO.eq("ACTIVO"))
                .fetch()
                .forEach(r -> resultado.put(
                        r.get(USUARIO_INTERNO.USUARIO_ID),
                        new Nombre(r.get(USUARIO.NOMBRES), r.get(USUARIO.APELLIDOS))));
        return resultado;
    }

    /** Lo que guarda la base, sin formatear: el formato lo decide el caso de uso. */
    public record Nombre(String nombres, String apellidos) {}
}
