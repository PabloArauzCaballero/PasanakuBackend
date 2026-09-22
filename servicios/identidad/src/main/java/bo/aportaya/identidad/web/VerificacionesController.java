package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CU02RevisarExpediente;
import bo.aportaya.identidad.dominio.ExpedienteDeIdentidad;
import bo.aportaya.identidad.web.generado.IdentidadApi;
import bo.aportaya.identidad.web.generado.modelo.DecisionDeVerificacion;
import bo.aportaya.identidad.web.generado.modelo.EnlaceDeFoto;
import bo.aportaya.identidad.web.generado.modelo.ExpedienteEnRevision;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * El portal de riesgo: la cola de expedientes, las fotos y la decision.
 *
 * <p>Las tres operaciones exigen permiso y ninguna es publica. Ver la foto de la
 * cedula de alguien es leer un dato personal sensible; decidir sobre su identidad le
 * abre o le cierra la billetera.
 */
@RestController
public class VerificacionesController implements IdentidadApi {

    private final CU02RevisarExpediente revision;
    private final SesionDeLaPeticion sesion;

    public VerificacionesController(CU02RevisarExpediente revision, SesionDeLaPeticion sesion) {
        this.revision = revision;
        this.sesion = sesion;
    }

    /**
     * El mismo permiso que decidir, y no `DATOS_SENSIBLES_LEER`. Separarlos dejaba a
     * quien tiene que resolver sin poder abrir el expediente: podia decidir sin
     * mirar, que es lo contrario de lo que se busca. Mirar el expediente y resolverlo
     * son una sola capacidad — trabajar la cola de verificacion.
     */
    @Override
    @Permiso("VERIFICACION_RESOLVER")
    public ResponseEntity<List<ExpedienteEnRevision>> listarVerificaciones(String estado) {
        List<ExpedienteEnRevision> cola = revision.cola(estado, sesion.actual()).stream()
                .map(VerificacionesController::aSalida)
                .toList();
        return ResponseEntity.ok(cola);
    }

    /** Las tres caras que el contrato declara; cualquier otra no existe. */
    private static final Set<String> CARAS = Set.of("ANVERSO", "REVERSO", "SELFIE");

    @Override
    @Permiso("VERIFICACION_RESOLVER")
    public ResponseEntity<EnlaceDeFoto> verFotoDelExpediente(UUID verificacionId, String cara) {
        // El contrato declara el enum, pero la interfaz generada lo entrega como texto: sin
        // esta guarda una cara inventada llegaba hasta la consulta y salia un 500 —un dato
        // que el borde tenia que rechazar, convertido en error del servidor—.
        if (!CARAS.contains(cara)) {
            throw new ErrorDeDominio("La cara del documento no existe: " + cara);
        }
        var enlace = revision.foto(verificacionId, cara, sesion.actual());
        return ResponseEntity.ok(new EnlaceDeFoto().url(enlace.url()).vigenteHasta(enlace.vigenteHasta()));
    }

    /**
     * Los bytes, no el enlace. El almacen no es publico —red interna, sin puerto ni
     * dominio— asi que la URL prefirmada de arriba sirve entre servicios y es
     * inservible en un navegador. Aca la sirve el servicio dueno, despues de validar
     * sesion y permiso, que es lo que manda el ADR-034.
     */
    @Override
    @Permiso("VERIFICACION_RESOLVER")
    public ResponseEntity<org.springframework.core.io.Resource> verContenidoDeFoto(UUID verificacionId, String cara) {
        if (!CARAS.contains(cara)) {
            throw new ErrorDeDominio("La cara del documento no existe: " + cara);
        }
        var contenido = revision.contenido(verificacionId, cara, sesion.actual());
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        contenido.tipoMime() == null ? "application/octet-stream" : contenido.tipoMime()))
                .contentLength(contenido.bytes())
                // `no-store`: una cedula no se queda en la cache del navegador ni en la
                // de ningun intermediario. El enlace muere con la respuesta.
                .header("Cache-Control", "no-store")
                .body(new org.springframework.core.io.InputStreamResource(contenido.datos()));
    }

    @Override
    @Permiso("VERIFICACION_RESOLVER")
    public ResponseEntity<ExpedienteEnRevision> resolverVerificacion(
            UUID verificacionId, UUID idempotencyKey, DecisionDeVerificacion cuerpo) {
        revision.resolver(verificacionId, cuerpo.getDecision().getValue(), cuerpo.getMotivo(), sesion.actual());
        var resuelto = revision.cola(null, sesion.actual()).stream()
                .filter(e -> e.verificacionId().equals(verificacionId))
                .findFirst()
                .orElseThrow();
        return ResponseEntity.ok(aSalida(resuelto));
    }

    private static ExpedienteEnRevision aSalida(ExpedienteDeIdentidad e) {
        var salida = new ExpedienteEnRevision()
                .verificacionId(e.verificacionId())
                .usuarioId(e.usuarioId())
                .nombreCompleto(e.nombreCompleto())
                .documento(e.documento())
                .estado(ExpedienteEnRevision.EstadoEnum.fromValue(e.estado()))
                .iniciadaEn(e.iniciadaEn())
                .resueltaEn(e.resueltaEn())
                .motivoRechazo(e.motivoRechazo());
        e.fotos().forEach(f -> salida.addFotosItem(ExpedienteEnRevision.FotosEnum.fromValue(f)));
        return salida;
    }
}
