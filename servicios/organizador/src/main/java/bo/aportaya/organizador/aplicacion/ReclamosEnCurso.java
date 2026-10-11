package bo.aportaya.organizador.aplicacion;

import bo.aportaya.organizador.dominio.EfectoDeReclamos;
import bo.aportaya.organizador.dominio.EfectoDeReclamos.Decision;
import bo.aportaya.organizador.dominio.EfectoDeReclamos.Que;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * H13.S1.M6 · Antes de dejar firme una sancion: que la persona sancionada no tenga un reclamo en curso que lo
 * impida. Un reclamo en revision NO es una sancion firme (regla 91.4).
 *
 * <p>La pregunta a {@code cumplimiento} va FUERA de toda transaccion (invariante 6): esta clase no abre ninguna;
 * la lectura del usuario del organizador es una transaccion de solo lectura aparte ({@link UsuarioDelOrganizador})
 * y la escritura que sigue (CU-93) abre la suya. Si cumplimiento no contesta con algo entendible, frena:
 * "no se pudo saber" nunca se lee como "no hay reclamo".
 *
 * <p>Que nada frene es condicion necesaria, NO suficiente: la causal, el descargo y la apelacion siguen siendo del
 * proceso que aplica la sancion.
 */
@Service
public class ReclamosEnCurso {

    private final UsuarioDelOrganizador organizadores;
    private final EfectoDeReclamos efecto;

    public ReclamosEnCurso(UsuarioDelOrganizador organizadores, EfectoDeReclamos efecto) {
        this.organizadores = organizadores;
        this.efecto = efecto;
    }

    /** @throws ErrorDeNegocio {@code AP-CU93-07} si hay un reclamo que frena o si no se pudo saber */
    public void exigirQueNoFrene(UUID organizadorId, ContextoSesion ctx) {
        // Un organizador que no existe lo rechaza CU-93 con SU codigo (AP-CU93-01); aca no hay a quien preguntarle.
        var usuario = organizadores.usuarioDe(organizadorId, ctx);
        if (usuario.isEmpty()) {
            return;
        }
        if (efecto.consultar(usuario.get(), Que.SANCION_FIRME) != Decision.PROCEDE) {
            throw new ErrorDeNegocio(
                    CodigoError.de(93, 7),
                    "Hay un reclamo en curso de esa persona, o no se pudo confirmar que no lo haya: la sancion no "
                            + "queda firme mientras no se resuelva.");
        }
    }
}
