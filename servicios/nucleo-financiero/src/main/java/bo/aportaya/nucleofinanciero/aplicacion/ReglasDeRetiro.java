package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.dominio.CondicionesDeRetiro;
import bo.aportaya.nucleofinanciero.dominio.CondicionesDeRetiro.Situacion;
import bo.aportaya.nucleofinanciero.dominio.CondicionesDeRetiro.Veredicto;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio.Cuenta;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRetiroRepositorio.Instrumento;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRetiroRepositorio.Orden;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;

/** Lo que CU-11 comprueba antes de escribir, sin tocar la base. */
final class ReglasDeRetiro {

    private ReglasDeRetiro() {}

    /**
     * Repetir la clave es repetir <b>la misma</b> solicitud: mismo destino, mismo importe y misma
     * cotizacion aceptada. Si algo de eso cambia, la clave ya identifica otra cosa y devolver la orden
     * vieja le diria a la persona que retiro lo que no pidio.
     *
     * <p>El costo NO se compara: el replay devuelve el costo ALMACENADO en la orden, no el que trae la
     * entrada repetida (si el cotizador cambio entretanto, recotizar le mostraria a la persona un costo
     * distinto del que se le cobro). El costo lo fija el servidor desde {@code tarifas}, nunca el cliente.
     */
    static void exigirMismaSolicitud(Orden orden, CU11RetirarSaldo.EntradaRetiro entrada) {
        boolean igual = orden.cuentaId().equals(entrada.cuentaBilleteraId())
                && orden.instrumentoDestinoId().equals(entrada.instrumentoDestinoId())
                && orden.solicitado().equals(entrada.monto())
                && orden.cotizacionId().equals(entrada.cotizacionId());
        if (!igual) {
            throw new ErrorDeNegocio(CodigoError.de(11, 13), "La clave de idempotencia ya identifica otro retiro.");
        }
    }

    /** Las condiciones duras del retiro (R-BIL-09, R-BIL-11b), en el orden en que se le explican a la persona. */
    static Veredicto evaluarCondiciones(
            Cuenta cuenta,
            Instrumento instrumento,
            CU11RetirarSaldo.EntradaRetiro entrada,
            boolean hayBloqueoDeAutoridad,
            boolean encajeCumplido,
            OffsetDateTime ahora) {
        return CondicionesDeRetiro.evaluar(
                new Situacion(
                        entrada.mfaVerificado(),
                        entrada.evidenciaMfaProvista(),
                        cuenta.disponible(),
                        entrada.monto(),
                        instrumento.usuarioId().equals(cuenta.usuarioId()) && instrumento.titularCoincide(),
                        instrumento.verificado(),
                        instrumento.bloqueadoHasta(),
                        hayBloqueoDeAutoridad,
                        encajeCumplido),
                ahora);
    }
}
