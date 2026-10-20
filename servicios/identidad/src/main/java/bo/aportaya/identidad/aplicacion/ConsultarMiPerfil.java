package bo.aportaya.identidad.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-07: el correo de la propia cuenta, para que el perfil lo muestre, y el estado de la cuenta,
 * para que la app sepa si pedir la verificacion antes de una operacion con dinero. Se lee con el usuario de la
 * sesion y nada mas: no hay forma de pedir el de otra persona.
 */
@Service
public class ConsultarMiPerfil {

    private final Datos datos;

    public ConsultarMiPerfil(Datos datos) {
        this.datos = datos;
    }

    @Transactional(readOnly = true)
    public Perfil ejecutar(ContextoSesion ctx) {
        UUID usuarioId = ctx.usuarioId();
        return datos.conContexto(ctx, dsl -> {
            var fila = dsl.fetchOne(
                    """
                    SELECT correo, correo_verificado_en IS NOT NULL AS verificado, estado, nombres, apellidos, telefono_e164
                      FROM identidad.usuario WHERE id = ?
                    """,
                    usuarioId);
            if (fila == null) {
                throw new ErrorDeNegocio(CodigoError.de(7, 11), "No encontramos tu cuenta.");
            }
            return new Perfil(
                    usuarioId,
                    fila.get("correo", String.class),
                    Boolean.TRUE.equals(fila.get("verificado", Boolean.class)),
                    fila.get("estado", String.class),
                    nombreVisible(fila.get("nombres", String.class), fila.get("apellidos", String.class)),
                    nombreCompleto(fila.get("nombres", String.class), fila.get("apellidos", String.class)),
                    fila.get("telefono_e164", String.class));
        });
    }

    /**
     * Primer nombre e inicial del apellido («Rosa Q.»): lo minimo para que un organizador reconozca a quien pide
     * entrar a su grupo, sin exponer el nombre completo (regla 90.2). Null si no hay nombre.
     */
    public static String nombreVisible(String nombres, String apellidos) {
        if (nombres == null || nombres.isBlank()) return null;
        String primero = nombres.trim().split("\\s+")[0];
        if (apellidos == null || apellidos.isBlank()) return primero;
        return primero + " " + Character.toUpperCase(apellidos.trim().charAt(0)) + ".";
    }

    /**
     * Nombres y apellidos tal como se cargaron, con los espacios normalizados: lo que ve el organizador en la
     * solicitud de quien pide unirse (decision del dueño 2026-10-20). Null si no hay nombre.
     */
    public static String nombreCompleto(String nombres, String apellidos) {
        if (nombres == null || nombres.isBlank()) return null;
        String completo = nombres.trim() + (apellidos == null || apellidos.isBlank() ? "" : " " + apellidos.trim());
        return completo.replaceAll("\\s+", " ");
    }

    /** {@code estadoDeCuenta}: si todavia falta verificar la identidad, si esta en revision o si opera. */
    public record Perfil(
            UUID usuarioId,
            String correo,
            boolean correoVerificado,
            String estadoDeCuenta,
            String nombreVisible,
            String nombreCompleto,
            String telefono) {}
}
