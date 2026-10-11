package bo.aportaya.garantia.aplicacion;

import bo.aportaya.garantia.dominio.puertos.EfectoDeReclamos;
import bo.aportaya.garantia.dominio.puertos.EfectoDeReclamos.Decision;
import bo.aportaya.garantia.dominio.puertos.EfectoDeReclamos.Que;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * H13.S1.M6 · Antes de volver ejecutable una deuda o dejar firme una restriccion: que la persona no tenga un
 * reclamo en curso que lo impida.
 *
 * <p>Un reclamo en revision NO es una sancion firme ni una deuda ejecutable (regla 91.4). La pregunta va a
 * {@code cumplimiento} FUERA de toda transaccion (invariante 6): esta clase no abre ninguna, y la escritura que
 * sigue (CU-26 o CU-27) es la que la abre. Si cumplimiento no contesta con algo entendible, frena: "no se pudo
 * saber" nunca se lee como "no hay reclamo".
 *
 * <p>Que nada frene es condicion necesaria, NO suficiente: la causal, la notificacion probada, el descargo y la
 * apelacion siguen siendo del proceso que aplica el efecto.
 */
@Service
public class ReclamosEnCurso {

    private final UsuarioDelExpediente expedientes;
    private final EfectoDeReclamos efecto;

    public ReclamosEnCurso(UsuarioDelExpediente expedientes, EfectoDeReclamos efecto) {
        this.expedientes = expedientes;
        this.efecto = efecto;
    }

    /**
     * @param casoDeUso 26 (ejecutar el aval) o 27 (restringir): el numero del codigo {@code AP-CU<nn>-07}
     * @throws ErrorDeNegocio {@code AP-CU<nn>-07} si hay un reclamo que frena o si no se pudo saber
     */
    public void exigirQueNoFrene(UUID expedienteId, int casoDeUso, ContextoSesion ctx) {
        // Un expediente que no existe lo rechaza el caso de uso con SU codigo; aca no hay a quien preguntarle.
        var usuario = expedientes.usuarioDe(expedienteId, ctx);
        if (usuario.isEmpty()) {
            return;
        }
        if (efecto.consultar(usuario.get(), Que.DEUDA_EJECUTABLE) != Decision.PROCEDE) {
            throw new ErrorDeNegocio(
                    CodigoError.de(casoDeUso, 7),
                    "Hay un reclamo en curso de esa persona, o no se pudo confirmar que no lo haya: no se vuelve "
                            + "ejecutable la deuda mientras no se resuelva.");
        }
    }
}
