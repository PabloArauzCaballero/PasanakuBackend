package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.nucleofinanciero.aplicacion.QrInterno;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaLecturaQr;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaPagoQr;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaQr;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaPagoQr;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaQrEmitido;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaQrLeido;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Los QR internos de la billetera, del contrato al caso de uso.
 *
 * <p>El contenido del QR viaja siempre en el cuerpo: en la URL quedaria en los registros de cada
 * salto. Un QR de otro sistema de pagos llega hasta el caso de uso y alli se rechaza como tal.
 */
@Component
class QrDeLaBilletera {

    private final QrInterno qrs;

    QrDeLaBilletera(QrInterno qrs) {
        this.qrs = qrs;
    }

    ResponseEntity<SalidaQrEmitido> emitir(EntradaQr cuerpo, ContextoSesion ctx) {
        var emitido = qrs.emitir(
                new QrInterno.EntradaQr(
                        cuerpo.getCuentaBilleteraId(),
                        cuerpo.getModalidad().getValue(),
                        MapeoDeBilletera.dineroOpcional(cuerpo.getMonto()),
                        Optional.ofNullable(cuerpo.getConcepto())),
                ctx);

        var respuesta = new SalidaQrEmitido();
        respuesta.setQrId(emitido.qrId());
        respuesta.setContenido(emitido.contenido());
        respuesta.setModalidad(SalidaQrEmitido.ModalidadEnum.fromValue(emitido.modalidad()));
        emitido.monto().map(MapeoDeBilletera::dinero).ifPresent(respuesta::setMonto);
        emitido.expiraEn().ifPresent(respuesta::setExpiraEn);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    ResponseEntity<SalidaQrLeido> leer(EntradaLecturaQr cuerpo, ContextoSesion ctx) {
        var leido = qrs.leer(cuerpo.getContenido(), ctx);

        var respuesta = new SalidaQrLeido();
        respuesta.setQrId(leido.qrId());
        respuesta.setClase(SalidaQrLeido.ClaseEnum.INTERNO);
        respuesta.setModalidad(SalidaQrLeido.ModalidadEnum.fromValue(leido.modalidad()));
        leido.monto().map(MapeoDeBilletera::dinero).ifPresent(respuesta::setMonto);
        respuesta.setDestinatario(leido.destinatario());
        leido.concepto().ifPresent(respuesta::setConcepto);
        leido.expiraEn().ifPresent(respuesta::setExpiraEn);
        return ResponseEntity.ok(respuesta);
    }

    ResponseEntity<SalidaPagoQr> pagar(UUID clave, EntradaPagoQr cuerpo, ContextoSesion ctx) {
        var comprobante = qrs.pagar(
                new QrInterno.EntradaPagoQr(
                        cuerpo.getContenido(),
                        cuerpo.getCuentaOrigenId(),
                        MapeoDeBilletera.dineroOpcional(cuerpo.getMonto()),
                        clave.toString()),
                ctx);

        var respuesta = new SalidaPagoQr();
        respuesta.setTransaccionId(comprobante.transaccionId());
        respuesta.setQrId(comprobante.qrId());
        respuesta.setMonto(MapeoDeBilletera.dinero(comprobante.monto()));
        respuesta.setDestinatario(comprobante.destinatario());
        comprobante.concepto().ifPresent(respuesta::setConcepto);
        respuesta.setSaldoDespues(MapeoDeBilletera.dinero(comprobante.saldoDespues()));
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
