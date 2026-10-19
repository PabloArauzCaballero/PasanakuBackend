package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.dominio.FichaDeGobierno;
import bo.aportaya.auditoria.dominio.OpcionDeRetencion;
import bo.aportaya.auditoria.infraestructura.BitacoraRepositorio;
import bo.aportaya.auditoria.infraestructura.FichaGobiernoRepositorio;
import bo.aportaya.auditoria.infraestructura.PoliticaRetencionRepositorio;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-130 · Gobernar la ficha de cada tabla del catalogo de datos.
 *
 * <p>Que cada tabla del modelo tenga dicho si esta en uso, si alguien ya reviso su descripcion, quien responde por
 * ella y que retencion le aplica — como el catalogo de datos del portal interno de Atlas.
 *
 * <p>El catalogo (estructura y narrativa) sigue viniendo del recurso generado; esta clase le superpone lo unico que
 * cambia por decision de una persona: la ficha de gobierno. Una tabla que no esta en el catalogo no se puede gobernar:
 * guardar su ficha crearia una fila sobre algo que el modelo no tiene.
 */
@Service
public class CU130GobernarFichaDeTabla {

    static final CodigoError VERSION_VIEJA = CodigoError.de(130, 1);
    static final CodigoError TABLA_DESCONOCIDA = CodigoError.de(130, 2);
    static final CodigoError RETENCION_DESCONOCIDA = CodigoError.de(130, 3);

    private final Datos datos;
    private final FichaGobiernoRepositorio fichas;
    private final PoliticaRetencionRepositorio politicas;
    private final BitacoraRepositorio bitacora;
    private final CatalogoDeDatosVigente catalogo;
    private final Reloj reloj;
    private final ObjectMapper mapper;
    private final Set<String> tablasDelCatalogo;

    public CU130GobernarFichaDeTabla(
            Datos datos,
            FichaGobiernoRepositorio fichas,
            PoliticaRetencionRepositorio politicas,
            BitacoraRepositorio bitacora,
            CatalogoDeDatosVigente catalogo,
            Reloj reloj,
            ObjectMapper mapper) {
        this.datos = datos;
        this.fichas = fichas;
        this.politicas = politicas;
        this.bitacora = bitacora;
        this.catalogo = catalogo;
        this.reloj = reloj;
        this.mapper = mapper;
        this.tablasDelCatalogo = new HashSet<>();
        for (JsonNode t : catalogo.catalogo().path("tablas")) {
            tablasDelCatalogo.add(FichaDeGobierno.clave(
                    t.path("esquema").asText(), t.path("tabla").asText()));
        }
    }

    /** Las fichas guardadas por {@code esquema.tabla} y las politicas de retencion que se pueden asignar. */
    @Transactional(readOnly = true)
    public Vista consultar(ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            Map<String, FichaDeGobierno> guardadas = new LinkedHashMap<>();
            fichas.todas(dsl).forEach((clave, g) -> guardadas.put(clave, g.ficha()));
            return new Vista(guardadas, politicas.opciones(dsl));
        });
    }

    public JsonNode catalogo() {
        return catalogo.catalogo();
    }

    @Transactional
    public FichaDeGobierno guardar(String esquema, String tabla, FichaDeGobierno pedida, ContextoSesion ctx) {
        if (!tablasDelCatalogo.contains(FichaDeGobierno.clave(esquema, tabla))) {
            throw new ErrorDeNegocio(
                    TABLA_DESCONOCIDA, "La tabla " + esquema + "." + tabla + " no esta en el catalogo de datos.");
        }
        OffsetDateTime ahora = OffsetDateTime.ofInstant(reloj.ahora(), ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            pedida.politicaRetencionId()
                    .filter(id -> !politicas.existe(dsl, id))
                    .ifPresent(id -> {
                        throw new ErrorDeNegocio(RETENCION_DESCONOCIDA, "La politica de retencion indicada no existe.");
                    });
            FichaDeGobierno anterior = fichas.de(dsl, esquema, tabla)
                    .map(FichaGobiernoRepositorio.Guardada::ficha)
                    .orElse(null);
            UUID id = fichas.guardar(dsl, esquema, tabla, pedida, pedida.version(), ctx.usuarioId(), ahora)
                    .orElseThrow(() -> new ErrorDeNegocio(
                            VERSION_VIEJA,
                            "Otra persona guardo esta ficha antes. Recarga para ver lo ultimo y vuelve a intentar."));
            FichaDeGobierno guardada = fichas.de(dsl, esquema, tabla)
                    .map(FichaGobiernoRepositorio.Guardada::ficha)
                    .orElseThrow();
            bitacora.registrarCambioDeConfiguracion(
                    dsl,
                    "ficha_gobierno_tabla",
                    id,
                    ctx.usuarioId(),
                    ctx.rol(),
                    UUID.fromString(ctx.traza().id()),
                    anterior == null ? null : json(esquema, tabla, anterior),
                    json(esquema, tabla, guardada),
                    ahora);
            return guardada;
        });
    }

    /** Lo que se guarda en la bitacora: la ficha, sin datos de personas (el revisor va como id). */
    private String json(String esquema, String tabla, FichaDeGobierno f) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("esquema", esquema);
        v.put("tabla", tabla);
        v.put("estado", f.estado());
        v.put("estadoRevision", f.estadoRevision());
        v.put("responsable", f.responsable().orElse(null));
        v.put("politicaRetencionId", f.politicaRetencionId().map(UUID::toString).orElse(null));
        v.put("criticaParaAuditoria", f.criticaParaAuditoria());
        v.put("notaRevision", f.notaRevision().orElse(null));
        v.put("version", f.version());
        try {
            return mapper.writeValueAsString(v);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la ficha para la bitacora", e);
        }
    }

    public record Vista(Map<String, FichaDeGobierno> fichas, List<OpcionDeRetencion> politicas) {}
}
