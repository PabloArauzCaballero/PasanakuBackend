package bo.aportaya.auditoria.web;

import bo.aportaya.auditoria.aplicacion.ConsultarBitacora;
import bo.aportaya.auditoria.web.generado.modelo.EventoBitacora;
import bo.aportaya.auditoria.web.generado.modelo.PaginaDeBitacora;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la consulta de la bitacora de auditoria inmutable (comun.bitacora_evento).
 * Protegido por el permiso regulatorio AUDITORIA_LEER.
 */
@RestController
@Validated
@Permiso("AUDITORIA_LEER")
public class BitacoraController {

    private final ConsultarBitacora casoDeUso;
    private final SesionDeLaPeticion sesion;

    public BitacoraController(ConsultarBitacora casoDeUso, SesionDeLaPeticion sesion) {
        this.casoDeUso = casoDeUso;
        this.sesion = sesion;
    }

    @GetMapping(value = "/auditoria/bitacora", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaginaDeBitacora> consultarBitacora(
            @Size(max = 50) @RequestParam(value = "entidad", required = false) @Nullable String entidad,
            @Size(max = 30) @RequestParam(value = "accion", required = false) @Nullable String accion,
            @RequestParam(value = "actorUsuarioId", required = false) @Nullable UUID actorUsuarioId,
            @RequestParam(value = "desde", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    @Nullable
                    OffsetDateTime desde,
            @RequestParam(value = "hasta", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    @Nullable
                    OffsetDateTime hasta,
            @Min(1) @Max(100) @RequestParam(value = "limite", required = false, defaultValue = "25") Integer limite,
            @RequestParam(value = "antesDe", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    @Nullable
                    OffsetDateTime antesDe,
            @RequestParam(value = "antesDeId", required = false) @Nullable UUID antesDeId) {
        PaginaDeBitacora pagina = casoDeUso.listar(
                entidad, accion, actorUsuarioId, desde, hasta, limite, antesDe, antesDeId, sesion.actual());
        return ResponseEntity.ok(pagina);
    }

    @GetMapping(value = "/auditoria/bitacora/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventoBitacora> consultarEventoBitacora(@PathVariable("id") UUID id) {
        return casoDeUso
                .buscarPorId(id, sesion.actual())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
