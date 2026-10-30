package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.nucleofinanciero.aplicacion.CU13LiberarRetencionDelMercado;
import bo.aportaya.nucleofinanciero.web.generado.BilleteraApi;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaCierreRetencion;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * CU-13 · lo que {@code entregas} le pide a la billetera por el mercado y la subasta del cupo, aparte de
 * {@link BilleteraController} (que ya esta al limite de largo del barrido, 300 lineas).
 */
abstract class RutasDelMercadoDeLaBilletera implements BilleteraApi {

    private SesionDeLaPeticion sesionDelMercado;
    private CU13LiberarRetencionDelMercado liberacion;

    /** Por setter: el constructor de {@link BilleteraController} no cambia y Spring lo inyecta antes de atender. */
    @Autowired
    void conLiberacionDelMercado(SesionDeLaPeticion sesion, CU13LiberarRetencionDelMercado liberacion) {
        this.sesionDelMercado = sesion;
        this.liberacion = liberacion;
    }

    @Override
    @Permiso("SERVICIO_ENTREGAS")
    public ResponseEntity<SalidaCierreRetencion> liberarRetencionDelMercado(UUID retencionId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-13", retencionId.toString());
        return ResponseEntity.ok(MapeoDeBilletera.cierre(
                liberacion.liberar(retencionId, IdentidadDeServicio.como(sesionDelMercado, "SERVICIO_ENTREGAS"))));
    }
}
