package bo.aportaya.auditoria.web;

import bo.aportaya.auditoria.aplicacion.GestionarCatalogosOperativos;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoOperativoDetalle;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoOperativoResumen;
import bo.aportaya.auditoria.web.generado.modelo.ElementoCatalogoOperativo;
import bo.aportaya.auditoria.web.generado.modelo.EntradaActualizarElementoCatalogo;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * H5.S2 · Controlador REST para la consulta de catálogos operativos y
 * la gestión de elementos activos/inactivos con bitácora inmutable.
 */
@RestController
public class CatalogosOperativosController {

    private final GestionarCatalogosOperativos casoDeUso;
    private final SesionDeLaPeticion sesion;

    public CatalogosOperativosController(GestionarCatalogosOperativos casoDeUso, SesionDeLaPeticion sesion) {
        this.casoDeUso = casoDeUso;
        this.sesion = sesion;
    }

    @GetMapping(value = "/auditoria/catalogos", produces = "application/json")
    @Permiso("AUDITORIA_LEER")
    @PermisoAlternativo({"CATALOGO_EDITAR", "CATALOGO_DATOS_EDITAR"})
    public ResponseEntity<List<CatalogoOperativoResumen>> listarCatalogosOperativos() {
        return ResponseEntity.ok(casoDeUso.listarCatalogos());
    }

    @GetMapping(value = "/auditoria/catalogos/{codigo}", produces = "application/json")
    @Permiso("AUDITORIA_LEER")
    @PermisoAlternativo({"CATALOGO_EDITAR", "CATALOGO_DATOS_EDITAR"})
    public ResponseEntity<CatalogoOperativoDetalle> consultarCatalogoOperativo(@PathVariable("codigo") String codigo) {
        return casoDeUso.consultarCatalogo(codigo).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound()
                .build());
    }

    @GetMapping(value = "/auditoria/catalogos/{codigo}/elementos", produces = "application/json")
    @Permiso("AUDITORIA_LEER")
    @PermisoAlternativo({"CATALOGO_EDITAR", "CATALOGO_DATOS_EDITAR"})
    public ResponseEntity<List<ElementoCatalogoOperativo>> listarElementosCatalogo(
            @PathVariable("codigo") String codigo,
            @RequestParam(value = "activo", required = false) Boolean activo,
            @RequestParam(value = "termino", required = false) String termino) {
        if (casoDeUso.consultarCatalogo(codigo).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(casoDeUso.listarElementos(codigo, activo, termino));
    }

    @PatchMapping(
            value = "/auditoria/catalogos/{codigo}/elementos/{id}",
            produces = "application/json",
            consumes = "application/json")
    @Permiso("CATALOGO_EDITAR")
    @PermisoAlternativo({"AUDITORIA_GESTIONAR", "SISTEMAS_ADMIN"})
    public ResponseEntity<ElementoCatalogoOperativo> actualizarEstadoElementoCatalogo(
            @PathVariable("codigo") String codigo,
            @PathVariable("id") String id,
            @Valid @RequestBody EntradaActualizarElementoCatalogo entrada) {
        if (entrada.getMotivo() == null || entrada.getMotivo().trim().length() < 5) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "El motivo del cambio es obligatorio y debe tener al menos 5 caracteres.");
        }

        Traza.marcarCasoDeUso("H5.S2", codigo + ":" + id);

        return casoDeUso
                .actualizarEstado(
                        codigo,
                        id,
                        Boolean.TRUE.equals(entrada.getActivo()),
                        entrada.getMotivo().trim(),
                        sesion.actual())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
