package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion;
import bo.aportaya.nucleofinanciero.web.generado.BilleteraApi;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaReverso;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaReverso;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaSolicitudReverso;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.web.seguridad.ActorAutenticado;
import bo.aportaya.plataforma.web.seguridad.CuatroOjos;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * CU-14 · reverso con cuatro ojos reales (P-13), aparte de {@link BilleteraController} (que ya esta al
 * limite de largo del barrido, 300 lineas).
 *
 * <p>Dos peticiones, dos tokens. La primera guarda a quien pide; la segunda la firma otra persona y
 * {@link CuatroOjos} compara las dos identidades, ambas tomadas de token. El contrato no declara ningun
 * autorizador: un cuerpo que lo traiga se rechaza como campo no declarado.
 */
abstract class RutasDeReversosDeLaBilletera implements BilleteraApi {

    private SesionDeLaPeticion sesionDelReverso;
    private CuatroOjos cuatroOjos;
    private CU14ReversarTransaccion cu14;

    /** Por setter: el constructor de {@link BilleteraController} no cambia y Spring lo inyecta antes de atender. */
    @Autowired
    void conReversos(SesionDeLaPeticion sesion, CuatroOjos cuatroOjos, CU14ReversarTransaccion cu14) {
        this.sesionDelReverso = sesion;
        this.cuatroOjos = cuatroOjos;
        this.cu14 = cu14;
    }

    @Override
    @Permiso("REVERSO_AUTORIZAR")
    public ResponseEntity<SalidaSolicitudReverso> solicitarReverso(UUID idempotencyKey, EntradaReverso cuerpo) {
        Traza.marcarCasoDeUso("CU-14", cuerpo.getTransaccionOriginalId().toString());
        // El solicitante es el actor del token; exige sesion para que la firma sea de una persona.
        cuatroOjos.actor();
        var salida = cu14.solicitar(
                new CU14ReversarTransaccion.EntradaReverso(
                        idempotencyKey.toString(),
                        cuerpo.getTransaccionOriginalId(),
                        cuerpo.getTipo().getValue(),
                        cuerpo.getMotivo()),
                sesionDelReverso.actual());
        var respuesta = new SalidaSolicitudReverso();
        respuesta.setSolicitudId(salida.solicitudId());
        respuesta.setEstado(SalidaSolicitudReverso.EstadoEnum.fromValue(salida.estado()));
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Override
    @Permiso("REVERSO_AUTORIZAR")
    public ResponseEntity<SalidaReverso> autorizarReverso(UUID solicitudId) {
        Traza.marcarCasoDeUso("CU-14", solicitudId.toString());
        var ctx = sesionDelReverso.actual();
        UUID solicitante = cu14.solicitadaPor(solicitudId, ctx);
        // AP-CU14-02: otra persona, con su propio token (R-SEG-04, R-BIL-27).
        cuatroOjos.segundaFirma(ActorAutenticado.registrado(solicitante, null), CodigoError.de(14, 2));
        var salida = cu14.autorizar(solicitudId, ctx);
        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeBilletera.reverso(salida));
    }
}
