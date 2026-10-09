package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CotizarOperacion;
import bo.aportaya.nucleofinanciero.aplicacion.CuentaPropia;
import bo.aportaya.nucleofinanciero.aplicacion.OrdenesExistentes;
import bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor;
import bo.aportaya.nucleofinanciero.dominio.puertos.SegundoFactor;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaCotizacionOperacion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaRecarga;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaRetiro;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaAcreditacion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaCotizacionOperacion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaRecarga;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaRetiro;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Por donde entra y sale el dinero de la billetera: recargas y retiros.
 *
 * <p><b>Todo lo que este servicio no sabe se resuelve antes de abrir la transaccion.</b> El costo
 * lo fija {@code tarifas}, el segundo factor lo comprueba quien guarda las credenciales y el
 * proveedor confirma por su lado: son llamadas de red, y una llamada de red dentro de la
 * transaccion que mueve plata deja el dinero bloqueado esperando a un tercero (invariante 6).
 *
 * <p>Cuando alguna de esas preguntas no obtiene respuesta, la operacion se rechaza. Cobrar cero
 * porque {@code tarifas} no contesto es regalar plata en silencio, y denegar por omision es el
 * invariante 9.
 *
 * <p>Vive aparte del controlador porque el generador agrupa todas las operaciones de
 * {@code /billetera} en una sola interfaz: dos {@code @RestController} registrarian dos veces cada
 * mapeo. Lo que si se puede separar es a donde delega.
 */
@Component
class FondeoDeLaBilletera {

    private final RecargasConProveedor recargas;
    private final CU11RetirarSaldo cu11;
    private final CotizarOperacion cotizaciones;
    private final CuentaPropia cuentaPropia;
    private final OrdenesExistentes existentes;
    private final SegundoFactor segundoFactor;
    private final BigDecimal desdeCuandoSonDosFirmas;

    @SuppressWarnings("checkstyle:ParameterNumber")
    FondeoDeLaBilletera(
            RecargasConProveedor recargas,
            CU11RetirarSaldo cu11,
            CotizarOperacion cotizaciones,
            CuentaPropia cuentaPropia,
            OrdenesExistentes existentes,
            SegundoFactor segundoFactor,
            @Value("${aportaya.retiro.doble-aprobacion-desde}") BigDecimal desdeCuandoSonDosFirmas) {
        this.recargas = recargas;
        this.cu11 = cu11;
        this.cotizaciones = cotizaciones;
        this.cuentaPropia = cuentaPropia;
        this.existentes = existentes;
        this.segundoFactor = segundoFactor;
        this.desdeCuandoSonDosFirmas = desdeCuandoSonDosFirmas;
    }

    /** El precio, antes de confirmar: base, comision, impuesto, total, neto y hasta cuando vale. */
    ResponseEntity<SalidaCotizacionOperacion> cotizar(
            UUID clave, EntradaCotizacionOperacion cuerpo, ContextoSesion ctx) {
        cuentaPropia.exigirTitular(cuerpo.getCuentaBilleteraId(), ctx);
        var vista = cotizaciones.cotizar(
                CotizarOperacion.Operacion.valueOf(cuerpo.getOperacion().getValue()),
                cuerpo.getCuentaBilleteraId(),
                MapeoDeBilletera.dinero(cuerpo.getMonto()),
                clave);

        var respuesta = new SalidaCotizacionOperacion();
        vista.cotizacionId().ifPresent(respuesta::setCotizacionId);
        respuesta.setBase(MapeoDeBilletera.dinero(vista.base()));
        respuesta.setComision(MapeoDeBilletera.dinero(vista.comision()));
        respuesta.setImpuesto(MapeoDeBilletera.dinero(vista.impuesto()));
        respuesta.setCostoTotal(MapeoDeBilletera.dinero(vista.total()));
        respuesta.setNeto(MapeoDeBilletera.dinero(vista.neto()));
        vista.validaHasta().ifPresent(respuesta::setValidaHasta);
        respuesta.setGratuita(vista.gratuita());
        return ResponseEntity.ok(respuesta);
    }

    ResponseEntity<SalidaRecarga> solicitarRecarga(UUID clave, EntradaRecarga cuerpo, ContextoSesion ctx) {
        var salida = recargas.solicitar(
                new CU10RecargarSaldo.EntradaSolicitud(
                        clave.toString(),
                        cuerpo.getCuentaBilleteraId(),
                        MapeoDeBilletera.dinero(cuerpo.getMonto()),
                        bo.aportaya.plataforma.dominio.Dinero.cero(
                                MapeoDeBilletera.dinero(cuerpo.getMonto()).moneda()),
                        cuerpo.getMedio(),
                        Optional.ofNullable(cuerpo.getInstrumentoFondeoId()),
                        Optional.ofNullable(cuerpo.getCotizacionId())),
                ctx);

        var respuesta = new SalidaRecarga();
        respuesta.setOrdenRecargaId(salida.ordenRecargaId());
        respuesta.setEstado(salida.estado());
        respuesta.setExpiraEn(salida.expiraEn());
        respuesta.setAcreditara(MapeoDeBilletera.dinero(salida.acreditara()));
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    ResponseEntity<SalidaAcreditacion> acreditarRecarga(UUID ordenId, ContextoSesion ctx) {
        var salida = recargas.confirmar(ordenId, ctx);

        var respuesta = new SalidaAcreditacion();
        respuesta.setOrdenRecargaId(salida.ordenRecargaId());
        respuesta.setTransaccionId(salida.transaccionId());
        respuesta.setSaldoDespues(MapeoDeBilletera.dinero(salida.saldoDespues()));
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Si el costo no se pudo cotizar, se rechaza: cobrar cero porque tarifas no respondio es regalar plata.
     *
     * <p>{@code evidenciaMfa} (el JWT de step-up de {@code identidad}) reemplaza a {@code factorMfa}, que se
     * acepta hasta 2026-12-31 por compatibilidad con clientes viejos (H2.S2.M5).
     */
    // factorMfa se acepta hasta 2026-12-31 por compatibilidad con clientes viejos.
    @SuppressWarnings("deprecation")
    ResponseEntity<SalidaRetiro> retirar(UUID clave, EntradaRetiro cuerpo, ContextoSesion ctx) {
        var monto = MapeoDeBilletera.dinero(cuerpo.getMonto());
        var costo = cotizaciones.confirmar(
                CotizarOperacion.Operacion.RETIRO,
                cuerpo.getCuentaBilleteraId(),
                monto,
                clave.toString(),
                Optional.ofNullable(cuerpo.getCotizacionId()),
                existentes.retiro(cuerpo.getCuentaBilleteraId(), clave.toString(), ctx));

        String evidencia = cuerpo.getEvidenciaMfa() != null ? cuerpo.getEvidenciaMfa() : cuerpo.getFactorMfa();
        var salida = cu11.solicitar(
                new CU11RetirarSaldo.EntradaRetiro(
                        clave.toString(),
                        cuerpo.getCuentaBilleteraId(),
                        monto,
                        costo.costo(),
                        cuerpo.getInstrumentoDestinoId(),
                        segundoFactor.verificado(ctx.usuarioId(), evidencia),
                        evidencia != null && !evidencia.isBlank(),
                        monto.monto().compareTo(desdeCuandoSonDosFirmas) >= 0,
                        costo.cotizacionId()),
                ctx);

        var respuesta = new SalidaRetiro();
        respuesta.setOrdenRetiroId(salida.ordenRetiroId());
        respuesta.setEstado(SalidaRetiro.EstadoEnum.fromValue(salida.estado()));
        respuesta.setCostoRetiro(MapeoDeBilletera.dinero(salida.costoRetiro()));
        respuesta.setMontoNeto(MapeoDeBilletera.dinero(salida.montoNeto()));
        respuesta.setRetencionId(salida.retencionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
