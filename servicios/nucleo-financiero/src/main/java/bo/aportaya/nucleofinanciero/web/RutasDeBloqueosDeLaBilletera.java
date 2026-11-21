package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad;
import bo.aportaya.nucleofinanciero.web.generado.BilleteraApi;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaLevantamientoBloqueo;
import bo.aportaya.plataforma.web.seguridad.CuatroOjos;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * CU-17 · levantar un bloqueo por autoridad (F1.L1.M10, R-BIL-32), aparte de {@link BilleteraController} (que ya
 * esta al limite de largo del barrido, 300 lineas).
 *
 * <p>Sin cuerpo: quien levanta es el actor del token, y tiene que ser una persona. El caso de uso compara esa
 * identidad con la de quien registro el bloqueo (AP-CU17-04) y la base lo exige tambien.
 */
abstract class RutasDeBloqueosDeLaBilletera extends RutasDeReversosDeLaBilletera implements BilleteraApi {

    private SesionDeLaPeticion sesionDelBloqueo;
    private CuatroOjos personaDelBloqueo;
    private CU17BloquearPorAutoridad bloqueos;

    /** Por setter: el constructor de {@link BilleteraController} no cambia y Spring lo inyecta antes de atender. */
    @Autowired
    void conBloqueos(SesionDeLaPeticion sesion, CuatroOjos cuatroOjos, CU17BloquearPorAutoridad bloqueos) {
        this.sesionDelBloqueo = sesion;
        this.personaDelBloqueo = cuatroOjos;
        this.bloqueos = bloqueos;
    }

    @Override
    @Permiso("BLOQUEO_AUTORIDAD_LEVANTAR")
    public ResponseEntity<SalidaLevantamientoBloqueo> levantarBloqueoPorAutoridad(UUID idempotencyKey, UUID bloqueoId) {
        Traza.marcarCasoDeUso("CU-17", bloqueoId.toString());
        // Levanta una persona, desde su token: una identidad de servicio no firma el levantamiento.
        personaDelBloqueo.actor();
        var salida = bloqueos.levantar(bloqueoId, sesionDelBloqueo.actual());
        var respuesta = new SalidaLevantamientoBloqueo();
        respuesta.setBloqueoId(salida.bloqueoId());
        respuesta.setEstado(SalidaLevantamientoBloqueo.EstadoEnum.fromValue(salida.estado()));
        return ResponseEntity.ok(respuesta);
    }
}
