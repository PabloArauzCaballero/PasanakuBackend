package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.dominio.CostoDeOperacion;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRecargaRepositorio.Instrumento;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRecargaRepositorio.Orden;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Objects;
import java.util.Optional;

/**
 * Lo que CU-10 comprueba antes de escribir, sin tocar la base: la misma solicitud y un
 * medio de fondeo que es de quien dice ser.
 */
final class ReglasDeRecarga {

    private ReglasDeRecarga() {}

    /**
     * Repetir la clave es repetir <b>la misma</b> solicitud. Si cambia el importe, el
     * costo, el medio, el instrumento o la cotizacion aceptada, la clave ya identifica
     * otra cosa y devolver la orden vieja le diria a la persona que pidio lo que no pidio.
     *
     * <p>El medio de una orden anterior a que se guardara (nulo) no se compara: no hay con
     * que.
     */
    static void exigirMismaSolicitud(Orden orden, CU10RecargarSaldo.EntradaSolicitud entrada) {
        boolean igual = orden.cuentaId().equals(entrada.cuentaBilleteraId())
                && orden.bruto().equals(entrada.monto())
                && orden.acreditado().equals(CostoDeOperacion.acreditacion(entrada.monto(), entrada.costoProveedor()))
                && orden.instrumentoId().equals(entrada.instrumentoFondeoId())
                && (orden.medio() == null || Objects.equals(orden.medio(), entrada.medio()))
                && orden.cotizacionId().equals(entrada.cotizacionId());
        if (!igual) {
            throw new ErrorDeNegocio(CodigoError.de(10, 5), "La clave de idempotencia ya identifica otra solicitud.");
        }
    }

    /** AP-CU10-03. Un medio de fondeo sin verificar puede no ser de quien dice. */
    static void exigirInstrumentoDelTitular(Optional<Instrumento> instrumento, ContextoSesion ctx) {
        var medio = instrumento.orElseThrow(
                () -> new ErrorDeNegocio(CodigoError.de(10, 3), "Ese medio de fondeo no existe."));
        if (!medio.verificado() || !medio.titularCoincide() || !ctx.usuarioId().equals(medio.usuarioId())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(10, 3), "Ese medio de fondeo no esta verificado a nombre del titular.");
        }
    }
}
