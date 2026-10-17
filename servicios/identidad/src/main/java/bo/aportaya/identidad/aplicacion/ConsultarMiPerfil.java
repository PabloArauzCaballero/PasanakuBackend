package bo.aportaya.identidad.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-07: el correo de la propia cuenta, para que el perfil lo muestre. Se lee con el usuario de la
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
                    "SELECT correo, correo_verificado_en IS NOT NULL AS verificado FROM identidad.usuario WHERE id = ?",
                    usuarioId);
            if (fila == null) {
                throw new ErrorDeNegocio(CodigoError.de(7, 11), "No encontramos tu cuenta.");
            }
            return new Perfil(
                    usuarioId,
                    fila.get("correo", String.class),
                    Boolean.TRUE.equals(fila.get("verificado", Boolean.class)));
        });
    }

    public record Perfil(UUID usuarioId, String correo, boolean correoVerificado) {}
}
