package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo;
import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.nucleofinanciero.dominio.puertos.SegundoFactor;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaRetiro;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.util.UUID;

/** Resuelve el costo y el segundo factor antes de abrir la transaccion del retiro. */
final class PreparacionDeRetiro {
    private final CotizadorDeComision cotizador;
    private final SegundoFactor segundoFactor;
    private final BigDecimal desdeCuandoSonDosFirmas;

    PreparacionDeRetiro(
            CotizadorDeComision cotizador, SegundoFactor segundoFactor, BigDecimal desdeCuandoSonDosFirmas) {
        this.cotizador = cotizador;
        this.segundoFactor = segundoFactor;
        this.desdeCuandoSonDosFirmas = desdeCuandoSonDosFirmas;
    }

    // factorMfa se acepta hasta 2026-12-31 por compatibilidad con clientes viejos.
    @SuppressWarnings("deprecation")
    CU11RetirarSaldo.EntradaRetiro preparar(UUID idempotencyKey, EntradaRetiro cuerpo, ContextoSesion sesion) {
        var monto = MapeoDeBilletera.dinero(cuerpo.getMonto());
        var costo = cotizador
                .costoDe("RETIRO_ACREDITADO", cuerpo.getCuentaBilleteraId(), monto, idempotencyKey.toString())
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(11, 1), "No se pudo cotizar el costo del retiro: intentalo de nuevo."));

        String evidencia = cuerpo.getEvidenciaMfa() != null ? cuerpo.getEvidenciaMfa() : cuerpo.getFactorMfa();
        boolean evidenciaProvista = evidencia != null && !evidencia.isBlank();

        return new CU11RetirarSaldo.EntradaRetiro(
                idempotencyKey.toString(),
                cuerpo.getCuentaBilleteraId(),
                monto,
                costo,
                cuerpo.getInstrumentoDestinoId(),
                segundoFactor.verificado(sesion.usuarioId(), evidencia),
                evidenciaProvista,
                monto.monto().compareTo(desdeCuandoSonDosFirmas) >= 0);
    }
}
