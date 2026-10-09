package bo.aportaya.garantia.web;

import bo.aportaya.garantia.aplicacion.CU23CubrirFaltanteDelCorte;
import bo.aportaya.garantia.aplicacion.CU23RecuperarRespaldo;
import bo.aportaya.garantia.aplicacion.CU23ReservarRespaldo;
import bo.aportaya.garantia.aplicacion.CU23ReversarCobertura;
import bo.aportaya.garantia.web.generado.modelo.EntradaAmpliacionRespaldo;
import bo.aportaya.garantia.web.generado.modelo.EntradaCoberturaRespaldo;
import bo.aportaya.garantia.web.generado.modelo.EntradaRecuperacionRespaldo;
import bo.aportaya.garantia.web.generado.modelo.EntradaReservaRespaldo;
import bo.aportaya.garantia.web.generado.modelo.SalidaAmpliacionRespaldo;
import bo.aportaya.garantia.web.generado.modelo.SalidaCoberturaRespaldo;
import bo.aportaya.garantia.web.generado.modelo.SalidaLiberacionRespaldo;
import bo.aportaya.garantia.web.generado.modelo.SalidaRecuperacionRespaldo;
import bo.aportaya.garantia.web.generado.modelo.SalidaReservaRespaldo;
import bo.aportaya.garantia.web.generado.modelo.SalidaReversaRespaldo;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * La traduccion web de {@code /garantia/respaldo}: la reserva de la empresa, su uso y su recuperacion.
 *
 * <p>El generador de contratos agrupa las rutas por su primer segmento, asi que estas seis
 * operaciones pertenecen a {@code GarantiaApi} y las expone {@link GarantiaController}: dos
 * controladores sobre la misma interfaz duplicarian el mapeo. Esta clase solo traduce; el
 * permiso de cada operacion lo declara el controlador.
 *
 * <p>Reservar, ampliar, liberar, reversar y recuperar son decisiones de tesoreria
 * ({@code ADMIN_PLATAFORMA}); cubrir un faltante lo dispara quien ejecuta la entrega
 * ({@code ENTREGA_EJECUTAR}). Ninguna de las dos cosas la puede hacer el organizador del
 * grupo: el organizador no es caja (RN-18).
 */
@Component
public class RespaldoWeb {

    private final CU23ReservarRespaldo reservar;
    private final CU23CubrirFaltanteDelCorte cubrir;
    private final CU23ReversarCobertura reversar;
    private final CU23RecuperarRespaldo recuperar;
    private final SesionDeLaPeticion sesion;

    public RespaldoWeb(
            CU23ReservarRespaldo reservar,
            CU23CubrirFaltanteDelCorte cubrir,
            CU23ReversarCobertura reversar,
            CU23RecuperarRespaldo recuperar,
            SesionDeLaPeticion sesion) {
        this.reservar = reservar;
        this.cubrir = cubrir;
        this.reversar = reversar;
        this.recuperar = recuperar;
        this.sesion = sesion;
    }

    public ResponseEntity<SalidaReservaRespaldo> reservarRespaldo(UUID idempotencyKey, EntradaReservaRespaldo cuerpo) {
        Traza.marcarCasoDeUso("CU-23", cuerpo.getGrupoId().toString());

        var salida = reservar.reservar(
                new CU23ReservarRespaldo.EntradaReserva(
                        cuerpo.getGrupoId(),
                        cuerpo.getCiclo(),
                        MapeoDeGarantia.dinero(cuerpo.getMonto()),
                        cuerpo.getResponsableId(),
                        clave("reserva", idempotencyKey)),
                sesion.actual());

        var respuesta = new SalidaReservaRespaldo();
        respuesta.setReservaId(salida.reservaId());
        respuesta.setReservado(MapeoDeGarantia.dinero(salida.reservado()));
        respuesta.setCapacidadDisponible(MapeoDeGarantia.dinero(salida.capacidadDisponible()));
        respuesta.setEsNueva(salida.esNueva());
        return ResponseEntity.status(salida.esNueva() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(respuesta);
    }

    public ResponseEntity<SalidaAmpliacionRespaldo> ampliarRespaldo(
            UUID reservaId, UUID idempotencyKey, EntradaAmpliacionRespaldo cuerpo) {
        Traza.marcarCasoDeUso("CU-23", reservaId.toString());

        var salida = reservar.ampliar(
                reservaId,
                MapeoDeGarantia.dinero(cuerpo.getMonto()),
                clave("ampliacion", idempotencyKey),
                sesion.actual());

        var respuesta = new SalidaAmpliacionRespaldo();
        respuesta.setReservaId(salida.reservaId());
        respuesta.setReservado(MapeoDeGarantia.dinero(salida.reservadoDespues()));
        respuesta.setCapacidadDisponible(MapeoDeGarantia.dinero(salida.capacidadDisponible()));
        respuesta.setEsNueva(salida.esNueva());
        return ResponseEntity.ok(respuesta);
    }

    public ResponseEntity<SalidaLiberacionRespaldo> liberarRespaldo(UUID reservaId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-23", reservaId.toString());

        var salida = reservar.liberar(reservaId, clave("liberacion", idempotencyKey), sesion.actual());

        var respuesta = new SalidaLiberacionRespaldo();
        respuesta.setReservaId(salida.reservaId());
        respuesta.setLiberado(MapeoDeGarantia.dinero(salida.liberado()));
        respuesta.setEsNueva(salida.esNueva());
        return ResponseEntity.ok(respuesta);
    }

    public ResponseEntity<SalidaCoberturaRespaldo> cubrirConRespaldo(
            UUID idempotencyKey, EntradaCoberturaRespaldo cuerpo) {
        Traza.marcarCasoDeUso("CU-23", cuerpo.getTurnoId().toString());

        var salida = cubrir.cubrir(
                new CU23CubrirFaltanteDelCorte.Pedido(
                        cuerpo.getGrupoId(),
                        cuerpo.getPeriodoId(),
                        cuerpo.getTurnoId(),
                        cuerpo.getFaltanteEsperado() == null
                                ? null
                                : MapeoDeGarantia.dinero(cuerpo.getFaltanteEsperado()),
                        clave("cobertura", idempotencyKey)),
                sesion.actual());

        var respuesta = new SalidaCoberturaRespaldo();
        respuesta.setResultado(SalidaCoberturaRespaldo.ResultadoEnum.fromValue(
                salida.resultado().name()));
        respuesta.setCoberturaId(salida.coberturaId());
        respuesta.setReservaId(salida.reservaId());
        respuesta.setCubierto(MapeoDeGarantia.dinero(salida.cubierto()));
        respuesta.setSinCubrir(MapeoDeGarantia.dinero(salida.sinCubrir()));
        respuesta.setDisponibleDespues(MapeoDeGarantia.dinero(salida.disponibleDespues()));
        respuesta.setExposicionDespues(MapeoDeGarantia.dinero(salida.exposicionDespues()));
        respuesta.setEsNueva(salida.esNueva());
        return ResponseEntity.ok(respuesta);
    }

    public ResponseEntity<SalidaReversaRespaldo> reversarCoberturaRespaldo(UUID coberturaId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-23", coberturaId.toString());

        var salida = reversar.reversar(coberturaId, clave("reversa", idempotencyKey), sesion.actual());

        var respuesta = new SalidaReversaRespaldo();
        respuesta.setCoberturaId(salida.coberturaId());
        respuesta.setRevertido(MapeoDeGarantia.dinero(salida.revertido()));
        respuesta.setEsNueva(salida.esNueva());
        return ResponseEntity.ok(respuesta);
    }

    public ResponseEntity<SalidaRecuperacionRespaldo> recuperarRespaldo(
            UUID idempotencyKey, EntradaRecuperacionRespaldo cuerpo) {
        Traza.marcarCasoDeUso("CU-23", cuerpo.getObligacionId().toString());

        var salida = recuperar.recuperar(
                new CU23RecuperarRespaldo.EntradaRecuperacion(
                        cuerpo.getPagoId(), cuerpo.getObligacionId(), MapeoDeGarantia.dinero(cuerpo.getPago())),
                sesion.actual());

        var respuesta = new SalidaRecuperacionRespaldo();
        respuesta.setResultado(SalidaRecuperacionRespaldo.ResultadoEnum.fromValue(
                salida.resultado().name()));
        respuesta.setRecuperado(MapeoDeGarantia.dinero(salida.recuperado()));
        respuesta.setExcedente(MapeoDeGarantia.dinero(salida.excedente()));
        respuesta.setExposicionDespues(MapeoDeGarantia.dinero(salida.exposicionDespues()));
        respuesta.setEsNueva(salida.esNueva());
        return ResponseEntity.ok(respuesta);
    }

    /** La clave de la cabecera, con el nombre de la operacion para que dos operaciones no choquen. */
    private static ClaveIdempotencia clave(String operacion, UUID idempotencyKey) {
        return ClaveIdempotencia.deHecho(operacion, idempotencyKey);
    }
}
