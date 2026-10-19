package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.dominio.RevisionDeColumna;
import bo.aportaya.auditoria.infraestructura.BitacoraRepositorio;
import bo.aportaya.auditoria.infraestructura.RevisionColumnaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-131 · Revisar cada columna del catalogo de datos.
 *
 * <p>Lo que el catalogo dice de una columna lo escribio alguien una vez desde el modelo; que otra persona lo
 * apruebe, lo rechace con motivo o lo deje pendiente es lo que vuelve confiable la ficha. Una columna que no esta
 * en el catalogo no se puede revisar: seria una fila sobre algo que el modelo no tiene.
 */
@Service
public class CU131RevisarColumna {

    static final CodigoError COLUMNA_DESCONOCIDA = CodigoError.de(131, 1);
    static final CodigoError RECHAZO_SIN_MOTIVO = CodigoError.de(131, 2);
    static final CodigoError VERSION_VIEJA = CodigoError.de(131, 3);

    private final Datos datos;
    private final RevisionColumnaRepositorio revisiones;
    private final BitacoraRepositorio bitacora;
    private final Reloj reloj;
    private final ObjectMapper mapper;
    private final Set<String> columnasDelCatalogo;

    public CU131RevisarColumna(
            Datos datos,
            RevisionColumnaRepositorio revisiones,
            BitacoraRepositorio bitacora,
            CatalogoDeDatosVigente catalogo,
            Reloj reloj,
            ObjectMapper mapper) {
        this.datos = datos;
        this.revisiones = revisiones;
        this.bitacora = bitacora;
        this.reloj = reloj;
        this.mapper = mapper;
        this.columnasDelCatalogo = new HashSet<>();
        for (JsonNode t : catalogo.catalogo().path("tablas")) {
            for (JsonNode c : t.path("columnas")) {
                columnasDelCatalogo.add(RevisionDeColumna.clave(
                        t.path("esquema").asText(),
                        t.path("tabla").asText(),
                        c.path("nombre").asText()));
            }
        }
    }

    /** Las revisiones guardadas por {@code esquema.tabla.columna}. */
    @Transactional(readOnly = true)
    public Map<String, RevisionDeColumna> consultar(ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            Map<String, RevisionDeColumna> guardadas = new LinkedHashMap<>();
            revisiones.todas(dsl).forEach((clave, g) -> guardadas.put(clave, g.revision()));
            return guardadas;
        });
    }

    @Transactional
    public RevisionDeColumna guardar(
            String esquema, String tabla, String columna, RevisionDeColumna pedida, ContextoSesion ctx) {
        if (!columnasDelCatalogo.contains(RevisionDeColumna.clave(esquema, tabla, columna))) {
            throw new ErrorDeNegocio(
                    COLUMNA_DESCONOCIDA,
                    "La columna " + esquema + "." + tabla + "." + columna + " no esta en el catalogo de datos.");
        }
        if (pedida.exigeMotivo()) {
            throw new ErrorDeNegocio(RECHAZO_SIN_MOTIVO, "Para rechazar una columna hay que decir por que.");
        }
        OffsetDateTime ahora = OffsetDateTime.ofInstant(reloj.ahora(), ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            RevisionDeColumna anterior = revisiones
                    .de(dsl, esquema, tabla, columna)
                    .map(RevisionColumnaRepositorio.Guardada::revision)
                    .orElse(null);
            UUID id = revisiones
                    .guardar(dsl, esquema, tabla, columna, pedida, pedida.version(), ctx.usuarioId(), ahora)
                    .orElseThrow(() -> new ErrorDeNegocio(
                            VERSION_VIEJA,
                            "Otra persona reviso esta columna antes. Recarga para ver lo ultimo y vuelve a intentar."));
            RevisionDeColumna guardada = revisiones
                    .de(dsl, esquema, tabla, columna)
                    .map(RevisionColumnaRepositorio.Guardada::revision)
                    .orElseThrow();
            bitacora.registrarCambioDeConfiguracion(
                    dsl,
                    "revision_columna",
                    id,
                    ctx.usuarioId(),
                    ctx.rol(),
                    UUID.fromString(ctx.traza().id()),
                    anterior == null ? null : json(esquema, tabla, columna, anterior),
                    json(esquema, tabla, columna, guardada),
                    ahora);
            return guardada;
        });
    }

    private String json(String esquema, String tabla, String columna, RevisionDeColumna r) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("esquema", esquema);
        v.put("tabla", tabla);
        v.put("columna", columna);
        v.put("decision", r.decision());
        v.put("confianza", r.confianza().orElse(null));
        v.put("motivo", r.motivo().orElse(null));
        v.put("version", r.version());
        try {
            return mapper.writeValueAsString(v);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la revision para la bitacora", e);
        }
    }
}
