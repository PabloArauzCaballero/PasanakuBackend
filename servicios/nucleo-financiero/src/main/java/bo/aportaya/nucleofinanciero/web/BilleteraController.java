package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion;
import bo.aportaya.nucleofinanciero.aplicacion.CU15EmitirExtracto;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarMovimientosBilletera;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarSaldo;
import bo.aportaya.nucleofinanciero.web.generado.BilleteraApi;
import bo.aportaya.nucleofinanciero.web.generado.modelo.CuentaDeLaSesion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaAcreditacionRescate;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaAprobacionRetiro;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaBloqueo;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaCierre;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaCierreRetencion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaCotizacionOperacion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaDebitoInversion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaLecturaQr;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaPagoQr;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaQr;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaRecarga;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaRetencion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaRetiro;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaReverso;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaTransferencia;
import bo.aportaya.nucleofinanciero.web.generado.modelo.ListaMovimientosBilletera;
import bo.aportaya.nucleofinanciero.web.generado.modelo.PaginaRetirosEnRevision;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SaldoBilletera;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaAcreditacion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaAcreditacionRescate;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaAprobacionRetiro;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaBloqueo;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaCierreBilletera;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaCierreRetencion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaCotizacionOperacion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaEjecucionDeRetencion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaExtracto;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaPagoQr;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaQrEmitido;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaQrLeido;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaRecarga;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaRetencion;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaRetiro;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaReverso;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaTransferencia;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las paginas de {@code /billetera}.
 *
 * <p><b>Todo lo que este servicio no sabe se resuelve antes de abrir la transaccion.</b>
 * El costo de un retiro lo fija {@code tarifas}; a quien apunta un alias lo sabe
 * {@code grupos}; si quedan aportes pendientes lo sabe {@code aportes}. Los tres son
 * llamadas de red, y una llamada de red dentro de la transaccion que mueve plata deja
 * el dinero bloqueado esperando a un tercero (invariante 6).
 *
 * <p>Si alguna respuesta falta, la operacion se rechaza para evitar cobros o cierres
 * con datos incompletos.
 */
@RestController
@Permiso("BILLETERA_OPERAR")
public class BilleteraController extends RutasDelMercadoDeLaBilletera {

    private final FondeoDeLaBilletera fondeo;
    private final QrDeLaBilletera qr;
    private final CU13RetenerSaldo cu13;
    private final CU14ReversarTransaccion cu14;
    private final CU15EmitirExtracto cu15;
    private final bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad cu17;
    private final MovimientosDeLaBilletera movimientos;
    private final ConsultarSaldo saldos;
    private final ConsultarMovimientosBilletera consultasDeMovimientos;
    private final SesionDeLaPeticion sesion;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public BilleteraController(
            FondeoDeLaBilletera fondeo,
            QrDeLaBilletera qr,
            CU13RetenerSaldo cu13,
            CU14ReversarTransaccion cu14,
            CU15EmitirExtracto cu15,
            bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad cu17,
            MovimientosDeLaBilletera movimientos,
            ConsultarSaldo saldos,
            ConsultarMovimientosBilletera consultasDeMovimientos,
            SesionDeLaPeticion sesion) {
        this.fondeo = fondeo;
        this.qr = qr;
        this.cu13 = cu13;
        this.cu14 = cu14;
        this.cu15 = cu15;
        this.cu17 = cu17;
        this.movimientos = movimientos;
        this.saldos = saldos;
        this.consultasDeMovimientos = consultasDeMovimientos;
        this.sesion = sesion;
    }

    @Override
    @Permiso("BILLETERA_VER")
    public ResponseEntity<CuentaDeLaSesion> consultarCuentaPropia(String moneda) {
        return fondeo.cuentaPropia(moneda, sesion.actual());
    }

    @Override
    @Permiso("BILLETERA_VER")
    public ResponseEntity<SaldoBilletera> consultarSaldo(UUID cuentaId) {
        Traza.marcarCasoDeUso("CU-13", cuentaId.toString());

        return ResponseEntity.ok(MapeoDeBilletera.saldo(saldos.ejecutar(cuentaId, sesion.actual())));
    }

    @Override
    @Permiso("BILLETERA_VER")
    public ResponseEntity<ListaMovimientosBilletera> listarMovimientosBilletera(UUID cuentaId, Integer limite) {
        Traza.marcarCasoDeUso("CU-13", cuentaId.toString());
        return ResponseEntity.ok(
                MapeoDeBilletera.movimientos(consultasDeMovimientos.ejecutar(cuentaId, limite, sesion.actual())));
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaCotizacionOperacion> cotizarOperacion(
            UUID idempotencyKey, EntradaCotizacionOperacion cuerpo) {
        Traza.marcarCasoDeUso("CU-30", cuerpo.getCuentaBilleteraId().toString());
        return fondeo.cotizar(idempotencyKey, cuerpo, sesion.actual());
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaRecarga> solicitarRecarga(UUID idempotencyKey, EntradaRecarga cuerpo) {
        Traza.marcarCasoDeUso("CU-10", cuerpo.getCuentaBilleteraId().toString());
        return fondeo.solicitarRecarga(idempotencyKey, cuerpo, sesion.actual());
    }

    // Acreditar es confirmar que la plata llego: no lo hace quien la pide (91.3). Hasta que
    // exista el webhook de la pasarela para recargas, lo confirma tesoreria.
    @Override
    @Permiso("TESORERIA")
    public ResponseEntity<SalidaAcreditacion> acreditarRecarga(UUID ordenId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-10", ordenId.toString());
        return fondeo.acreditarRecarga(ordenId, sesion.actual());
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaRetiro> solicitarRetiro(UUID idempotencyKey, EntradaRetiro cuerpo) {
        Traza.marcarCasoDeUso("CU-11", cuerpo.getCuentaBilleteraId().toString());
        return fondeo.retirar(idempotencyKey, cuerpo, sesion.actual());
    }

    // La cola la ve solo quien puede decidirla: el mismo permiso que aprobar (no se inventa otro, S-8).
    @Override
    @Permiso("RETIRO_APROBAR")
    public ResponseEntity<PaginaRetirosEnRevision> listarRetirosEnRevision(
            Integer limite, OffsetDateTime despuesDeFecha, UUID despuesDeId) {
        return fondeo.listarRetirosEnRevision(limite, despuesDeFecha, despuesDeId, sesion.actual());
    }

    // H3.S2 (ADR-049): RETIRO_APROBAR es la primera defensa (403); la segregacion
    // solicitante != aprobador es la SEGUNDA y vive en CU11.aprobar/rechazarRevision,
    // porque la base la exige igual y las dos tienen que decir lo mismo.
    @Override
    @Permiso("RETIRO_APROBAR")
    public ResponseEntity<SalidaAprobacionRetiro> aprobarRetiro(
            UUID ordenId, UUID idempotencyKey, EntradaAprobacionRetiro cuerpo) {
        return fondeo.aprobarRetiro(ordenId, idempotencyKey, cuerpo, sesion.actual());
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaRetencion> retenerSaldo(UUID idempotencyKey, EntradaRetencion cuerpo) {
        Traza.marcarCasoDeUso("CU-13", cuerpo.getCuentaBilleteraId().toString());

        var salida =
                cu13.retener(MapeoDeBilletera.entradaDeRetencion(cuerpo), idempotencyKey.toString(), sesion.actual());

        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeBilletera.retencion(salida));
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaCierreRetencion> cerrarRetencion(
            UUID retencionId, UUID idempotencyKey, EntradaCierreRetencion cuerpo) {
        Traza.marcarCasoDeUso("CU-13", retencionId.toString());

        // Liberar devuelve el saldo al titular; ejecutar lo entrega. Son dos actos
        // distintos y el desenlace es lo unico que los distingue.
        var salida = cuerpo.getDesenlace() == EntradaCierreRetencion.DesenlaceEnum.LIBERADA
                ? cu13.liberar(retencionId, sesion.actual())
                : cu13.ejecutar(retencionId, sesion.actual());

        return ResponseEntity.ok(MapeoDeBilletera.cierre(salida));
    }

    @Override
    @Permiso("SERVICIO_ENTREGAS")
    public ResponseEntity<SalidaEjecucionDeRetencion> pagarAlTercero(UUID retencionId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-13", retencionId.toString());
        return movimientos.pagarAlTercero(
                retencionId, idempotencyKey, IdentidadDeServicio.como(sesion, "SERVICIO_ENTREGAS"));
    }

    // Servicio a servicio (inversiones): una sesion de usuario final, aunque tenga BILLETERA_OPERAR, recibe 403.
    @Override
    @Permiso("SERVICIO_INVERSIONES")
    public ResponseEntity<SalidaEjecucionDeRetencion> debitarInversion(
            UUID idempotencyKey, EntradaDebitoInversion cuerpo) {
        Traza.marcarCasoDeUso("CU-13", cuerpo.getRetencionId().toString());
        return movimientos.debitarInversion(
                idempotencyKey, cuerpo, IdentidadDeServicio.como(sesion, "SERVICIO_INVERSIONES"));
    }

    // Acreditar es confirmar que la plata volvio: no lo hace quien la espera (91.3). Lo confirma el servicio.
    @Override
    @Permiso("SERVICIO_INVERSIONES")
    public ResponseEntity<SalidaAcreditacionRescate> acreditarRescate(
            UUID idempotencyKey, EntradaAcreditacionRescate cuerpo) {
        Traza.marcarCasoDeUso("CU-12", cuerpo.getCuentaBilleteraId().toString());
        return movimientos.acreditarRescate(
                idempotencyKey, cuerpo, IdentidadDeServicio.como(sesion, "SERVICIO_INVERSIONES"));
    }

    @Override
    @Permiso("REVERSO_AUTORIZAR")
    public ResponseEntity<SalidaReverso> reversarTransaccion(UUID idempotencyKey, EntradaReverso cuerpo) {
        Traza.marcarCasoDeUso("CU-14", cuerpo.getTransaccionOriginalId().toString());
        var salida =
                cu14.ejecutar(MapeoDeBilletera.entradaDeReverso(idempotencyKey.toString(), cuerpo), sesion.actual());
        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeBilletera.reverso(salida));
    }

    @Override
    @Permiso("BILLETERA_VER")
    public ResponseEntity<SalidaExtracto> emitirExtracto(UUID cuentaId, LocalDate desde, LocalDate hasta) {
        Traza.marcarCasoDeUso("CU-15", cuentaId.toString());

        // El permiso delegado sale del token, no del cuerpo: quien pide el extracto de
        // otro tiene que traerlo firmado, no afirmarlo.
        boolean delegado = sesion.permisos().contains("BILLETERA_VER_TERCEROS");

        var salida =
                cu15.emitir(new CU15EmitirExtracto.EntradaExtracto(cuentaId, desde, hasta, delegado), sesion.actual());

        return ResponseEntity.ok(MapeoDeBilletera.extracto(salida));
    }

    @Override
    @Permiso("DATOS_SENSIBLES_LEER")
    public ResponseEntity<SalidaBloqueo> bloquearPorAutoridad(UUID idempotencyKey, EntradaBloqueo cuerpo) {
        Traza.marcarCasoDeUso("CU-17", cuerpo.getNumeroOficio());
        var salida = cu17.ejecutar(MapeoDeBilletera.entradaDeBloqueo(cuerpo), sesion.actual());
        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeBilletera.bloqueo(salida));
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaTransferencia> transferirSaldo(UUID idempotencyKey, EntradaTransferencia cuerpo) {
        Traza.marcarCasoDeUso("CU-12", cuerpo.getCuentaOrigenId().toString());
        return movimientos.transferir(idempotencyKey, cuerpo, sesion.actual());
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaCierreBilletera> solicitarCierreBilletera(UUID idempotencyKey, EntradaCierre cuerpo) {
        Traza.marcarCasoDeUso("CU-16", cuerpo.getCuentaBilleteraId().toString());
        return movimientos.cerrar(cuerpo, sesion.actual());
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaQrEmitido> emitirQr(EntradaQr cuerpo) {
        Traza.marcarCasoDeUso("CU-12", cuerpo.getCuentaBilleteraId().toString());
        return qr.emitir(cuerpo, sesion.actual());
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaQrLeido> leerQr(EntradaLecturaQr cuerpo) {
        Traza.marcarCasoDeUso("CU-12", "lectura-qr");
        return qr.leer(cuerpo, sesion.actual());
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaPagoQr> pagarQr(UUID idempotencyKey, EntradaPagoQr cuerpo) {
        Traza.marcarCasoDeUso("CU-12", cuerpo.getCuentaOrigenId().toString());
        return qr.pagar(idempotencyKey, cuerpo, sesion.actual());
    }
}
