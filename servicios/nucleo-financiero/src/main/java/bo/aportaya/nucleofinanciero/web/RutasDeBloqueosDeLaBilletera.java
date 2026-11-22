package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad;
import bo.aportaya.nucleofinanciero.web.generado.BilleteraApi;
import bo.aportaya.nucleofinanciero.web.generado.modelo.BloqueoEnLista;
import bo.aportaya.nucleofinanciero.web.generado.modelo.PaginaDeBloqueos;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaLevantamientoBloqueo;
import bo.aportaya.plataforma.web.seguridad.CuatroOjos;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * CU-17 · listar y levantar los bloqueos por autoridad (F1.L1.M10, R-BIL-32), aparte de {@link BilleteraController} (que ya
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

    /** Lista blanca del filtro: el generador deja el parametro como texto libre. */
    private static final Set<String> ESTADOS = Set.of("VIGENTE", "LEVANTADO", "VENCIDO");

    @Override
    @Permiso("BLOQUEO_AUTORIDAD_REGISTRAR")
    @PermisoAlternativo({"BLOQUEO_AUTORIDAD_LEVANTAR"})
    public ResponseEntity<PaginaDeBloqueos> listarBloqueosPorAutoridad(
            Integer limite, String estado, OffsetDateTime antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-17", "listado");
        if (estado != null && !ESTADOS.contains(estado)) {
            throw new ConstraintViolationException("estado: fuera de la lista del contrato", Set.of());
        }
        var pagina = bloqueos.listar(
                Optional.ofNullable(estado),
                Optional.ofNullable(antesDe),
                Optional.ofNullable(antesDeId),
                limite == null ? 25 : limite,
                sesionDelBloqueo.actual());
        var salida = new PaginaDeBloqueos();
        salida.setItems(pagina.items().stream()
                .map(RutasDeBloqueosDeLaBilletera::enLista)
                .toList());
        salida.setProximoAntesDe(pagina.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(pagina.proximoAntesDeId().orElse(null));
        return ResponseEntity.ok(salida);
    }

    private static BloqueoEnLista enLista(CU17BloquearPorAutoridad.FilaDelListado fila) {
        var b = fila;
        var r = new BloqueoEnLista();
        r.setBloqueoId(b.bloqueoId());
        r.setCuentaBilleteraId(b.cuentaBilleteraId());
        r.setAutoridad(BloqueoEnLista.AutoridadEnum.fromValue(b.autoridad()));
        r.setTipoOrden(BloqueoEnLista.TipoOrdenEnum.fromValue(b.tipoOrden()));
        r.setAlcance(BloqueoEnLista.AlcanceEnum.fromValue(b.alcance()));
        r.setEstado(BloqueoEnLista.EstadoEnum.fromValue(b.estado()));
        b.montoBloqueado().map(MapeoDeBilletera::dinero).ifPresent(r::setMontoBloqueado);
        r.setRegistradoPor(b.registradoPor().orElse(null));
        r.setLevantadoPor(b.levantadoPor().orElse(null));
        r.setRecibidoEn(b.recibidoEn());
        r.setVenceEn(b.venceEn().orElse(null));
        r.setLevantadoEn(b.levantadoEn().orElse(null));
        r.setPuedeLevantar(fila.puedeLevantar());
        return r;
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
