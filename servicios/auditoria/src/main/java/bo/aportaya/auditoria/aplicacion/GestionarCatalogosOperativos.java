package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.infraestructura.CatalogosOperativosRepositorio;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoOperativoDetalle;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoOperativoResumen;
import bo.aportaya.auditoria.web.generado.modelo.ElementoCatalogoOperativo;
import bo.aportaya.plataforma.datos.Datos;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * H5.S2 · Caso de uso: Gestión de catálogos operativos y elementos activos/inactivos.
 * Orquesta la consulta del inventario de catálogos, la visualización de elementos y la
 * mutación del estado con registro inmutable en bitácora.
 */
@Service
public class GestionarCatalogosOperativos {

    private final CatalogosOperativosRepositorio repositorio;
    private final Datos datos;

    public GestionarCatalogosOperativos(CatalogosOperativosRepositorio repositorio, Datos datos) {
        this.repositorio = repositorio;
        this.datos = datos;
    }

    public List<CatalogoOperativoResumen> listarCatalogos() {
        return repositorio.listarCatalogos();
    }

    public Optional<CatalogoOperativoDetalle> consultarCatalogo(String codigo) {
        return repositorio.consultarCatalogo(codigo);
    }

    public List<ElementoCatalogoOperativo> listarElementos(String codigo, Boolean activo, String termino) {
        return repositorio.listarElementos(codigo, activo, termino);
    }

    public Optional<ElementoCatalogoOperativo> actualizarEstado(
            String codigo,
            String id,
            boolean nuevoActivo,
            String motivo,
            bo.aportaya.plataforma.dominio.ContextoSesion contexto) {
        UUID usuarioId = contexto != null ? contexto.usuarioId() : null;
        String rol = contexto != null ? contexto.rol() : "ADMIN";
        UUID correlationId = UUID.randomUUID();

        return datos.conContexto(
                contexto,
                dsl -> repositorio.actualizarEstadoElemento(
                        dsl, codigo, id, nuevoActivo, motivo, usuarioId, rol, correlationId));
    }
}
