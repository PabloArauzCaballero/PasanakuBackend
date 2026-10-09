package bo.aportaya.aportes.web;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.ConsultarEstadoDelParticipante;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDelPeriodo;
import bo.aportaya.aportes.web.generado.AportesApi;
import bo.aportaya.aportes.web.generado.modelo.EntradaCobro;
import bo.aportaya.aportes.web.generado.modelo.EstadoDelParticipante;
import bo.aportaya.aportes.web.generado.modelo.Morosos;
import bo.aportaya.aportes.web.generado.modelo.ObligacionPendiente;
import bo.aportaya.aportes.web.generado.modelo.RecaudoDelPeriodo;
import bo.aportaya.aportes.web.generado.modelo.SalidaCobro;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * La pagina de {@code /aportes}: traduce y delega, sin logica.
 *
 * <p>La clave de idempotencia entra por cabecera y **se le pasa al caso de uso**, que
 * es quien la valida antes de escribir (invariante 7). Aca no se decide nada sobre
 * ella: comprobarla en la pagina dejaria la ventana entre la comprobacion y el
 * {@code INSERT} abierta para el segundo intento.
 */
@RestController
public class AportesController implements AportesApi {

    private final CU21CobrarAporte cu21;
    private final ConsultarEstadoDelParticipante estados;
    private final ConsultarRecaudoDelPeriodo recaudos;
    private final SesionDeLaPeticion sesion;
    private final bo.aportaya.aportes.aplicacion.HechosDeGrupos grupos;

    public AportesController(
            CU21CobrarAporte cu21,
            ConsultarEstadoDelParticipante estados,
            ConsultarRecaudoDelPeriodo recaudos,
            SesionDeLaPeticion sesion,
            bo.aportaya.aportes.aplicacion.HechosDeGrupos grupos) {
        this.grupos = grupos;
        this.cu21 = cu21;
        this.estados = estados;
        this.recaudos = recaudos;
        this.sesion = sesion;
    }

    /**
     * Lo que este servicio le contesta a los otros sobre un participante.
     *
     * <p>Existe para que {@code grupos} y {@code nucleo-financiero} decidan sin leer
     * este esquema (invariante 11), y para que el dato no lo afirme quien pide.
     */
    @Override
    @Permiso("BILLETERA_VER")
    public ResponseEntity<EstadoDelParticipante> consultarEstadoDelParticipante(UUID participanteId) {
        Traza.marcarCasoDeUso("CU-21", participanteId.toString());

        var estado = estados.ejecutar(participanteId, sesion.actual());

        var respuesta = new EstadoDelParticipante();
        respuesta.setAlDia(estado.alDia());
        respuesta.setTotalAportado(MapeoDeAportes.importe(estado.totalAportado()));
        respuesta.setDeudaVigente(MapeoDeAportes.importe(estado.deudaVigente()));
        respuesta.setPorAportar(MapeoDeAportes.importe(estado.porAportar()));
        respuesta.setObligacionesAbiertas(estado.obligacionesAbiertas());
        respuesta.setMoneda(EstadoDelParticipante.MonedaEnum.fromValue(estado.moneda()));
        return ResponseEntity.ok(respuesta);
    }

    @Override
    @Permiso("BILLETERA_VER")
    public ResponseEntity<Morosos> contarMorosos(UUID grupoId) {
        Traza.marcarCasoDeUso("CU-21", grupoId.toString());

        var respuesta = new Morosos();
        respuesta.setGrupoId(grupoId);
        respuesta.setMorosos(estados.morososDelGrupo(grupoId, sesion.actual()));
        return ResponseEntity.ok(respuesta);
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaCobro> cobrarAporte(UUID idempotencyKey, UUID obligacionId, EntradaCobro cuerpo) {
        Traza.marcarCasoDeUso("CU-21", obligacionId.toString());

        // Antes de tocar nada: la obligacion tiene que ser de quien paga y el periodo tiene que seguir
        // abierto (B15/B5). Se pregunta a `grupos` FUERA de la transaccion del cobro (invariante 6).
        var ctx = sesion.actual();
        var contexto = cu21.contextoDe(obligacionId, ctx)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(21, 1), "Esa obligacion no existe."));
        var admisibilidad = grupos.admisibilidad(contexto.participanteId(), contexto.periodoId())
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(21, 7), "No pudimos verificar esa obligacion: intentalo de nuevo."));
        if (!admisibilidad.esDelUsuario()) {
            throw new ErrorDeNegocio(CodigoError.de(21, 6), "Esa obligacion no es tuya.");
        }

        var salida = cu21.acreditar(
                new CU21CobrarAporte.EntradaCobro(
                        idempotencyKey.toString(),
                        obligacionId,
                        MapeoDeAportes.dinero(cuerpo.getMonto()),
                        cuerpo.getRecargo() == null
                                ? bo.aportaya.plataforma.dominio.Dinero.cero(
                                        MapeoDeAportes.dinero(cuerpo.getMonto()).moneda())
                                : MapeoDeAportes.dinero(cuerpo.getRecargo()),
                        cuerpo.getCanal().getValue(),
                        cuerpo.getReferenciaProveedor(),
                        Optional.ofNullable(cuerpo.getProveedorId()),
                        Boolean.TRUE.equals(cuerpo.getEsManual()),
                        admisibilidad.periodoAbierto()),
                ctx);

        var respuesta = new SalidaCobro();
        respuesta.setPagoId(salida.pagoId());
        respuesta.setObligacionId(salida.obligacionId());
        respuesta.setEstadoObligacion(SalidaCobro.EstadoObligacionEnum.fromValue(salida.estadoObligacion()));
        respuesta.setPendiente(MapeoDeAportes.dinero(salida.pendiente()));
        respuesta.setEsNuevo(salida.esNuevo());

        // 201 cuando el cobro se acredito ahora; 200 cuando el reintento devolvio el
        // que ya existia. Que el cliente pueda distinguirlos es lo que hace que
        // reintentar sea seguro sin adivinar.
        var estado = salida.esNuevo() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(respuesta);
    }

    /**
     * Cuanto del pozo del periodo es caja confirmada.
     *
     * <p>Lo pide {@code entregas} antes de liquidar. El permiso es el de quien ejecuta la
     * entrega: el token del usuario viaja en la llamada entre servicios.
     */
    @Override
    @Permiso("ENTREGA_EJECUTAR")
    public ResponseEntity<RecaudoDelPeriodo> consultarRecaudoDelPeriodo(UUID periodoId) {
        Traza.marcarCasoDeUso("CU-21", periodoId.toString());

        var salida = recaudos.ejecutar(periodoId, sesion.actual());

        var respuesta = new RecaudoDelPeriodo();
        respuesta.setPeriodoId(salida.periodoId());
        respuesta.setGrupoId(salida.grupoId());
        respuesta.setCorteEn(salida.corteEn());
        respuesta.setPozo(MapeoDeAportes.dinero(salida.recaudo().pozo()));
        respuesta.setConfirmado(MapeoDeAportes.dinero(salida.recaudo().confirmado()));
        respuesta.setCubiertoMutual(MapeoDeAportes.dinero(salida.recaudo().cubiertoMutual()));
        respuesta.setFaltante(MapeoDeAportes.dinero(salida.recaudo().faltante()));
        respuesta.setPendientes(salida.recaudo().pendientes().stream()
                .map(p -> {
                    var o = new ObligacionPendiente();
                    o.setObligacionId(p.obligacionId());
                    o.setMonto(MapeoDeAportes.dinero(p.monto()));
                    return o;
                })
                .toList());
        return ResponseEntity.ok(respuesta);
    }
}
