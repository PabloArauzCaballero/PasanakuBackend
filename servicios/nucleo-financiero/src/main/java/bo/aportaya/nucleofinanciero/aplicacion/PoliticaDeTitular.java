package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Set;
import java.util.UUID;

/**
 * Quien puede operar sobre una billetera: su titular, el propio backend (trabajos y
 * consumidores) o un rol de backoffice con mandato. El rol correcto sobre el recurso ajeno
 * sigue siendo acceso indebido (BOLA): el permiso de la ruta no alcanza.
 *
 * <p>Los roles son los mismos que las politicas de fila de la base consideran privilegiados
 * (el auditor solo lee, por eso no esta).
 */
final class PoliticaDeTitular {

    private static final Set<String> CON_MANDATO = Set.of("BACKOFFICE", "CUMPLIMIENTO");

    private PoliticaDeTitular() {}

    static void exigir(ContextoSesion ctx, UUID titular, CodigoError codigo, String mensaje) {
        if (ctx.usuarioId().equals(titular) || ctx.esSistema() || CON_MANDATO.contains(ctx.rol())) {
            return;
        }
        throw new ErrorDeNegocio(codigo, mensaje);
    }
}
