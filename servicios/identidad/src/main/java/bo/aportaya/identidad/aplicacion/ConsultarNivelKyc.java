package bo.aportaya.identidad.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El nivel de KYC vigente de una persona, para quien lo necesita sin poder leer este esquema.
 *
 * <p>Cada quien lee el suyo. Leer el de otra persona exige backoffice o un proceso del sistema:
 * el nivel de verificación de alguien no se le cuenta a un tercero. La respuesta es el nivel y
 * nada más; la decisión de si alcanza es de quien pregunta.
 */
@Service
public class ConsultarNivelKyc {
    private static final Set<String> LECTORES_DE_OTROS = Set.of("BACKOFFICE", "ADMIN_PLATAFORMA", "sistema");

    private final Datos datos;

    public ConsultarNivelKyc(Datos datos) {
        this.datos = datos;
    }

    @Transactional(readOnly = true)
    public String ejecutar(UUID usuarioId, ContextoSesion ctx) {
        if (!usuarioId.equals(ctx.usuarioId()) && !LECTORES_DE_OTROS.contains(ctx.rol())) {
            throw new AccessDeniedException("sin permiso");
        }
        return datos.conContexto(ctx, dsl -> {
            var fila = dsl.fetchOne("SELECT nivel_kyc FROM identidad.usuario WHERE id=?", usuarioId);
            if (fila == null) {
                throw new ErrorDeNegocio(CodigoError.de(2, 9), "No hay un nivel de verificacion para esa persona.");
            }
            return fila.get(0, String.class);
        });
    }
}
