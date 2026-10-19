package bo.aportaya.auditoria.infraestructura;

import bo.aportaya.auditoria.web.generado.modelo.CatalogoOperativoDetalle;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoOperativoResumen;
import bo.aportaya.auditoria.web.generado.modelo.ElementoCatalogoOperativo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * H5.S2 · Repositorio de catálogos operativos de la plataforma AportaYa.
 * Provee la lista de catálogos, sus elementos configurables y la persistencia
 * del estado activo/inactivo con auditoría inmutable en bitácora.
 */
@Component
public class CatalogosOperativosRepositorio {

    private static final String RECURSO_CATALOGOS = "/catalogo/catalogos-operativos.json";
    private static final OffsetDateTime VIGENCIA_DEFECTO = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private final BitacoraRepositorio bitacora;
    private final Map<String, CatalogoDefinicion> catalogos = new LinkedHashMap<>();
    private final Map<String, Map<String, ElementoCatalogoOperativo>> elementosPorCatalogo = new ConcurrentHashMap<>();

    public record CatalogoDefinicion(
            String codigo,
            String nombre,
            String dueno,
            String version,
            String estado,
            String descripcion,
            String tablaFisica,
            String baseNormativa,
            OffsetDateTime vigenteDesde) {}

    public CatalogosOperativosRepositorio(BitacoraRepositorio bitacora, ObjectMapper mapper) {
        this.bitacora = bitacora;
        cargarDesdeRecurso(mapper != null ? mapper : new ObjectMapper().findAndRegisterModules());
    }

    public CatalogosOperativosRepositorio(BitacoraRepositorio bitacora) {
        this(bitacora, new ObjectMapper().findAndRegisterModules());
    }

    private void cargarDesdeRecurso(ObjectMapper mapper) {
        try (InputStream is = getClass().getResourceAsStream(RECURSO_CATALOGOS)) {
            if (is == null) return;
            JsonNode root = mapper.readTree(is);
            JsonNode array = root.path("catalogos");
            if (!array.isArray()) return;

            for (JsonNode cNode : array) {
                String codigo = cNode.path("codigo").asText();
                CatalogoDefinicion def = new CatalogoDefinicion(
                        codigo,
                        cNode.path("nombre").asText(),
                        cNode.path("dueno").asText(),
                        cNode.path("version").asText(),
                        cNode.path("estado").asText("VIGENTE"),
                        cNode.path("descripcion").asText(),
                        cNode.path("tablaFisica").asText(),
                        cNode.path("baseNormativa").asText(),
                        VIGENCIA_DEFECTO);
                catalogos.put(codigo, def);
                Map<String, ElementoCatalogoOperativo> mapElem = new ConcurrentHashMap<>();
                elementosPorCatalogo.put(codigo, mapElem);

                JsonNode elems = cNode.path("elementos");
                if (elems.isArray()) {
                    for (JsonNode eNode : elems) {
                        ElementoCatalogoOperativo elem = new ElementoCatalogoOperativo();
                        String id = eNode.path("id").asText();
                        elem.setId(id);
                        elem.setCatalogoCodigo(codigo);
                        elem.setCodigo(eNode.path("codigo").asText());
                        elem.setNombre(eNode.path("nombre").asText());
                        elem.setDescripcion(eNode.path("descripcion").asText());
                        elem.setActivo(eNode.path("activo").asBoolean(true));
                        elem.setOrden(eNode.path("orden").asInt(1));
                        elem.setVigenteDesde(VIGENCIA_DEFECTO);
                        elem.setActualizadoEn(VIGENCIA_DEFECTO);
                        elem.setActualizadoPor("SISTEMA");

                        if (eNode.has("atributos")) {
                            Map<String, Object> attrs = mapper.convertValue(
                                    eNode.path("atributos"), new TypeReference<Map<String, Object>>() {});
                            elem.setAtributos(attrs);
                        }
                        mapElem.put(id, elem);
                    }
                }
            }
        } catch (Exception e) {
            // El servicio continúa con el catálogo disponible en memoria
        }
    }

    public List<CatalogoOperativoResumen> listarCatalogos() {
        List<CatalogoOperativoResumen> lista = new ArrayList<>();
        for (CatalogoDefinicion def : catalogos.values()) {
            Map<String, ElementoCatalogoOperativo> map =
                    elementosPorCatalogo.getOrDefault(def.codigo(), Collections.emptyMap());
            int activos = (int) map.values().stream()
                    .filter(ElementoCatalogoOperativo::getActivo)
                    .count();

            CatalogoOperativoResumen r = new CatalogoOperativoResumen();
            r.setCodigo(def.codigo());
            r.setNombre(def.nombre());
            r.setDueno(def.dueno());
            r.setVersion(def.version());
            r.setTotalElementos(map.size());
            r.setElementosActivos(activos);
            r.setVigenteDesde(def.vigenteDesde());
            r.setEstado(CatalogoOperativoResumen.EstadoEnum.fromValue(def.estado()));
            r.setDescripcion(def.descripcion());
            lista.add(r);
        }
        return lista;
    }

    public Optional<CatalogoOperativoDetalle> consultarCatalogo(String codigo) {
        CatalogoDefinicion def = catalogos.get(codigo);
        if (def == null) return Optional.empty();

        Map<String, ElementoCatalogoOperativo> map = elementosPorCatalogo.getOrDefault(codigo, Collections.emptyMap());
        int activos = (int) map.values().stream()
                .filter(ElementoCatalogoOperativo::getActivo)
                .count();

        CatalogoOperativoDetalle d = new CatalogoOperativoDetalle();
        d.setCodigo(def.codigo());
        d.setNombre(def.nombre());
        d.setDueno(def.dueno());
        d.setVersion(def.version());
        d.setTotalElementos(map.size());
        d.setElementosActivos(activos);
        d.setVigenteDesde(def.vigenteDesde());
        d.setEstado(CatalogoOperativoDetalle.EstadoEnum.fromValue(def.estado()));
        d.setDescripcion(def.descripcion());
        d.setTablaFisica(def.tablaFisica());
        d.setBaseNormativa(def.baseNormativa());
        return Optional.of(d);
    }

    public List<ElementoCatalogoOperativo> listarElementos(String codigo, Boolean activo, String termino) {
        Map<String, ElementoCatalogoOperativo> map = elementosPorCatalogo.get(codigo);
        if (map == null) return Collections.emptyList();

        return map.values().stream()
                .filter(e -> activo == null || e.getActivo().equals(activo))
                .filter(e -> {
                    if (termino == null || termino.isBlank()) return true;
                    String t = termino.toLowerCase();
                    return e.getCodigo().toLowerCase().contains(t)
                            || e.getNombre().toLowerCase().contains(t)
                            || e.getDescripcion().toLowerCase().contains(t);
                })
                .sorted((a, b) -> Integer.compare(a.getOrden(), b.getOrden()))
                .toList();
    }

    public Optional<ElementoCatalogoOperativo> actualizarEstadoElemento(
            DSLContext dsl,
            String codigo,
            String id,
            boolean nuevoActivo,
            String motivo,
            UUID actorUsuarioId,
            String actorRol,
            UUID correlationId) {
        Map<String, ElementoCatalogoOperativo> map = elementosPorCatalogo.get(codigo);
        if (map == null) return Optional.empty();

        ElementoCatalogoOperativo actual = map.get(id);
        if (actual == null) return Optional.empty();

        boolean estadoAnterior = actual.getActivo();
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);

        actual.setActivo(nuevoActivo);
        actual.setActualizadoEn(ahora);
        actual.setActualizadoPor(actorUsuarioId != null ? actorUsuarioId.toString() : "ADMIN");

        if (dsl != null) {
            try {
                if ("HECHOS_GENERADORES".equals(codigo)) {
                    dsl.execute(
                            "UPDATE tarifas.catalogo_hecho_generador SET activo = ? WHERE codigo = ? OR id::varchar = ?",
                            nuevoActivo,
                            actual.getCodigo(),
                            id);
                } else if ("REPORTES_REGULATORIOS".equals(codigo)) {
                    dsl.execute(
                            "UPDATE cumplimiento.catalogo_reporte_regulatorio SET activo = ? WHERE codigo = ? OR id::varchar = ?",
                            nuevoActivo,
                            actual.getCodigo(),
                            id);
                }
            } catch (Exception ignored) {
            }

            try {
                String accion = nuevoActivo ? "ACTIVAR_ELEMENTO_CATALOGO" : "DESACTIVAR_ELEMENTO_CATALOGO";
                String valAnt = "{\"catalogo\":\"" + codigo + "\",\"id\":\"" + id + "\",\"codigo\":\""
                        + actual.getCodigo() + "\",\"activo\":" + estadoAnterior + "}";
                String valNue = "{\"catalogo\":\"" + codigo + "\",\"id\":\"" + id + "\",\"codigo\":\""
                        + actual.getCodigo() + "\",\"activo\":" + nuevoActivo + ",\"motivo\":\"" + motivo + "\"}";
                UUID entidadId = UUID.nameUUIDFromBytes((codigo + ":" + id).getBytes());

                bitacora.registrarCambioDeCatalogo(
                        dsl,
                        "catalogo_operativo",
                        entidadId,
                        accion,
                        actorUsuarioId,
                        actorRol != null ? actorRol : "ADMINISTRADOR",
                        correlationId != null ? correlationId : UUID.randomUUID(),
                        valAnt,
                        valNue,
                        motivo,
                        ahora);
            } catch (Exception ignored) {
            }
        }

        return Optional.of(actual);
    }
}
