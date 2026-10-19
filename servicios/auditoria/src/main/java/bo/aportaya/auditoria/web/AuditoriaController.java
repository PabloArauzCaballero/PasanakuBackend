package bo.aportaya.auditoria.web;

import bo.aportaya.auditoria.aplicacion.CU07EjercerDerechos;
import bo.aportaya.auditoria.aplicacion.ConsultarBitacora;
import bo.aportaya.auditoria.aplicacion.GestionarCatalogosOperativos;
import bo.aportaya.auditoria.web.generado.AuditoriaApi;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoOperativoDetalle;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoOperativoResumen;
import bo.aportaya.auditoria.web.generado.modelo.ElementoCatalogoOperativo;
import bo.aportaya.auditoria.web.generado.modelo.EntradaActualizarElementoCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.EntradaDerechos;
import bo.aportaya.auditoria.web.generado.modelo.EventoBitacora;
import bo.aportaya.auditoria.web.generado.modelo.PaginaDeBitacora;
import bo.aportaya.auditoria.web.generado.modelo.SalidaDerechos;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador transversal de auditoría que implementa el contrato OpenAPI {@link AuditoriaApi}.
 * Las URLs y métodos HTTP provienen del contrato transversal generado por OpenAPI.
 */
@RestController
public class AuditoriaController implements AuditoriaApi {

    private final GestionarCatalogosOperativos catalogosCasoDeUso;
    private final ConsultarBitacora bitacoraCasoDeUso;
    private final CU07EjercerDerechos cu07;
    private final SesionDeLaPeticion sesion;

    public AuditoriaController(
            GestionarCatalogosOperativos catalogosCasoDeUso,
            ConsultarBitacora bitacoraCasoDeUso,
            CU07EjercerDerechos cu07,
            SesionDeLaPeticion sesion) {
        this.catalogosCasoDeUso = catalogosCasoDeUso;
        this.bitacoraCasoDeUso = bitacoraCasoDeUso;
        this.cu07 = cu07;
        this.sesion = sesion;
    }

    // ==========================================
    // Catálogos Operativos (H5.S2)
    // ==========================================

    @Override
    @Permiso("AUDITORIA_LEER")
    @PermisoAlternativo({"CATALOGO_EDITAR", "CATALOGO_DATOS_EDITAR"})
    public ResponseEntity<List<CatalogoOperativoResumen>> listarCatalogosOperativos() {
        return ResponseEntity.ok(catalogosCasoDeUso.listarCatalogos());
    }

    @Override
    @Permiso("AUDITORIA_LEER")
    @PermisoAlternativo({"CATALOGO_EDITAR", "CATALOGO_DATOS_EDITAR"})
    public ResponseEntity<CatalogoOperativoDetalle> consultarCatalogoOperativo(String codigo) {
        return catalogosCasoDeUso
                .consultarCatalogo(codigo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @Permiso("AUDITORIA_LEER")
    @PermisoAlternativo({"CATALOGO_EDITAR", "CATALOGO_DATOS_EDITAR"})
    public ResponseEntity<List<ElementoCatalogoOperativo>> listarElementosCatalogo(
            String codigo, Boolean activo, String termino) {
        if (catalogosCasoDeUso.consultarCatalogo(codigo).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(catalogosCasoDeUso.listarElementos(codigo, activo, termino));
    }

    @Override
    @Permiso("CATALOGO_EDITAR")
    @PermisoAlternativo({"AUDITORIA_GESTIONAR", "SISTEMAS_ADMIN"})
    public ResponseEntity<ElementoCatalogoOperativo> actualizarEstadoElementoCatalogo(
            String codigo, String id, EntradaActualizarElementoCatalogo entrada) {
        if (entrada.getMotivo() == null || entrada.getMotivo().trim().length() < 5) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "El motivo del cambio es obligatorio y debe tener al menos 5 caracteres.");
        }

        Traza.marcarCasoDeUso("H5.S2", codigo + ":" + id);

        return catalogosCasoDeUso
                .actualizarEstado(
                        codigo,
                        id,
                        Boolean.TRUE.equals(entrada.getActivo()),
                        entrada.getMotivo().trim(),
                        sesion.actual())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ==========================================
    // Bitácora de Auditoría Inmutable
    // ==========================================

    @Override
    @Permiso("AUDITORIA_LEER")
    public ResponseEntity<PaginaDeBitacora> consultarBitacora(
            String entidad,
            String accion,
            UUID actorUsuarioId,
            OffsetDateTime desde,
            OffsetDateTime hasta,
            Integer limite,
            OffsetDateTime antesDe,
            UUID antesDeId) {
        PaginaDeBitacora pagina = bitacoraCasoDeUso.listar(
                entidad, accion, actorUsuarioId, desde, hasta, limite, antesDe, antesDeId, sesion.actual());
        return ResponseEntity.ok(pagina);
    }

    @Override
    @Permiso("AUDITORIA_LEER")
    public ResponseEntity<EventoBitacora> consultarEventoBitacora(UUID id) {
        return bitacoraCasoDeUso
                .buscarPorId(id, sesion.actual())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ==========================================
    // Ejercicio de Derechos (CU-07)
    // ==========================================

    @Override
    @Permiso("DATOS_SENSIBLES_LEER")
    public ResponseEntity<SalidaDerechos> ejercerDerechos(UUID idempotencyKey, EntradaDerechos cuerpo) {
        Traza.marcarCasoDeUso("CU-07", String.valueOf(cuerpo.getUsuarioId()));

        var salida = cu07.ejecutar(
                new CU07EjercerDerechos.EntradaDerechos(
                        cuerpo.getUsuarioId(),
                        cuerpo.getTipo().getValue(),
                        cuerpo.getDescripcion(),
                        cuerpo.getUltimaActividad()),
                sesion.actual());

        SalidaDerechos respuesta = new SalidaDerechos(
                salida.solicitudId(),
                salida.fechaLimiteLegal(),
                SalidaDerechos.EstadoEnum.fromValue(salida.estado()),
                salida.datosRetenidosPorLey());
        salida.procesoAnonimizacionId().ifPresent(respuesta::setProcesoAnonimizacionId);
        if (salida.estrategia() != null) {
            respuesta.setEstrategia(SalidaDerechos.EstrategiaEnum.fromValue(salida.estrategia()));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
