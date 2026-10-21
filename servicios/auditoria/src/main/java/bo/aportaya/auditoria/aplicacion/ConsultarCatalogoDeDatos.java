package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.dominio.FichaDeGobierno;
import bo.aportaya.auditoria.dominio.OpcionDeRetencion;
import bo.aportaya.auditoria.dominio.RevisionDeColumna;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import com.fasterxml.jackson.databind.JsonNode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Catalogo de datos 2026-10-21 · H2 — el catalogo servido por partes, como el portal de Atlas: un resumen liviano, la
 * lista de tablas filtrada y paginada aca (no en el navegador) y la ficha de una tabla.
 *
 * <p>Antes la pantalla bajaba el catalogo entero (~2 MB, 391 tablas con sus 4.000 columnas) para mostrar 20 filas.
 * El catalogo es un recurso estatico del servicio ({@link CatalogoDeDatosVigente}): el indice se arma una vez al
 * arrancar y cada pedido solo filtra en memoria y superpone lo que cambia por decision de una persona — la ficha de
 * gobierno (CU-130) y la revision de cada columna (CU-131).
 */
@Service
public class ConsultarCatalogoDeDatos {

    public static final int LIMITE_MAXIMO = 100;

    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");

    private final CU130GobernarFichaDeTabla cu130;
    private final CU131RevisarColumna cu131;
    private final List<Indexada> tablas;
    private final Map<String, Indexada> porClave;
    private final List<Esquema> esquemas;

    public ConsultarCatalogoDeDatos(
            CatalogoDeDatosVigente vigente, CU130GobernarFichaDeTabla cu130, CU131RevisarColumna cu131) {
        this.cu130 = cu130;
        this.cu131 = cu131;
        List<Indexada> indice = new ArrayList<>();
        for (JsonNode t : vigente.catalogo().path("tablas")) {
            indice.add(Indexada.de(t));
        }
        // Orden fijo esquema y tabla: es lo que hace que una pagina sea la misma entre dos pedidos.
        indice.sort((a, b) -> a.clave().compareTo(b.clave()));
        this.tablas = List.copyOf(indice);
        Map<String, Indexada> mapa = new LinkedHashMap<>();
        indice.forEach(t -> mapa.put(t.clave(), t));
        this.porClave = Collections.unmodifiableMap(mapa);
        List<Esquema> detalle = new ArrayList<>();
        for (JsonNode e : vigente.catalogo().path("detalleDeEsquemas")) {
            detalle.add(Esquema.de(e));
        }
        this.esquemas = List.copyOf(detalle);
    }

    /** Las cifras del catalogo con las fichas y revisiones vigentes, sin una sola columna. */
    public Resumen resumen(ContextoSesion ctx) {
        CU130GobernarFichaDeTabla.Vista gobierno = cu130.consultar(ctx);
        Map<String, RevisionDeColumna> revisiones = cu131.consultar(ctx);
        Map<String, Integer> porEstado = conteoEnCero(ESTADOS);
        Map<String, Integer> porRevision = conteoEnCero(REVISIONES);
        Map<String, Modulo> modulos = new TreeMap<>();
        int columnas = 0, relaciones = 0, personales = 0, financieros = 0, criticas = 0, sinResponsable = 0;
        int deNegocio = 0;
        for (Indexada t : tablas) {
            FichaDeGobierno g = fichaDe(gobierno, t);
            porEstado.merge(g.estado(), 1, Integer::sum);
            porRevision.merge(g.estadoRevision(), 1, Integer::sum);
            columnas += t.columnas().size();
            relaciones += t.salientes();
            personales += t.clasificacion().personales() > 0 ? 1 : 0;
            financieros += t.clasificacion().financieros() > 0 ? 1 : 0;
            criticas += g.criticaParaAuditoria() ? 1 : 0;
            sinResponsable += g.responsable().isEmpty() ? 1 : 0;
            deNegocio += "NEGOCIO".equals(t.tipo()) ? 1 : 0;
            modulos.merge(
                    t.modulo(),
                    new Modulo(t.modulo(), t.nombreModulo(), 1),
                    (a, b) -> new Modulo(a.codigo(), a.nombre(), a.tablas() + 1));
        }
        int revisadas = (int) revisiones.keySet().stream()
                .filter(clave -> porClave.containsKey(clave.substring(0, clave.lastIndexOf('.'))))
                .count();
        return new Resumen(
                tablas.size(), deNegocio, tablas.size() - deNegocio, columnas, revisadas, relaciones, personales,
                financieros, criticas, sinResponsable, porEstado, porRevision, esquemas,
                List.copyOf(modulos.values()), gobierno.politicas());
    }

    /**
     * La pagina pedida de la lista filtrada. Una pagina que pasa del final viene vacia (con el total real): asi el
     * cliente sabe a cual volver. El limite ya viene validado por el contrato; se acota igual, por si llega de otro lado.
     */
    public Pagina tablas(Filtro filtro, int pagina, int limite, ContextoSesion ctx) {
        int tope = Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        int numero = Math.max(1, pagina);
        CU130GobernarFichaDeTabla.Vista gobierno = cu130.consultar(ctx);
        Map<String, RevisionDeColumna> revisiones = cu131.consultar(ctx);
        Predicate<Indexada> coincide = filtro.comoPredicado(t -> fichaDe(gobierno, t));
        List<Indexada> encontradas = tablas.stream().filter(coincide).toList();
        int total = encontradas.size();
        int desde = (int) Math.min((long) (numero - 1) * tope, total);
        List<Fila> items = encontradas.subList(desde, Math.min(desde + tope, total)).stream()
                .map(t -> t.fila(fichaDe(gobierno, t), revisadasDe(revisiones, t)))
                .toList();
        return new Pagina(items, numero, tope, total, (total + tope - 1) / tope);
    }

    /** La tabla tal como esta en el catalogo, con su ficha y las revisiones de sus columnas; vacio si no existe. */
    public Optional<Ficha> tabla(String esquema, String tabla, ContextoSesion ctx) {
        Indexada t = porClave.get(FichaDeGobierno.clave(esquema, tabla));
        if (t == null) {
            return Optional.empty();
        }
        Map<String, RevisionDeColumna> propias = new LinkedHashMap<>();
        cu131.consultar(ctx).forEach((clave, r) -> {
            if (clave.startsWith(t.clave() + ".")) propias.put(clave, r);
        });
        return Optional.of(new Ficha(t.nodo(), fichaDe(cu130.consultar(ctx), t), propias));
    }

    private static FichaDeGobierno fichaDe(CU130GobernarFichaDeTabla.Vista gobierno, Indexada t) {
        return gobierno.fichas().getOrDefault(t.clave(), FichaDeGobierno.porOmision());
    }

    private static int revisadasDe(Map<String, RevisionDeColumna> revisiones, Indexada t) {
        int n = 0;
        for (String columna : t.columnas()) {
            if (revisiones.containsKey(RevisionDeColumna.clave(t.esquema(), t.tabla(), columna))) n++;
        }
        return n;
    }

    private static final List<String> ESTADOS =
            List.of("ACTIVA", "PLANIFICADA", "CANDIDATA_A_RETIRO", "RETIRADA", "DESHABILITADA");
    private static final List<String> REVISIONES = List.of("DETECTADA", "NECESITA_REVISION", "APROBADA", "RECHAZADA");

    private static Map<String, Integer> conteoEnCero(List<String> claves) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        claves.forEach(c -> conteo.put(c, 0));
        return conteo;
    }

    /** Sin acentos ni mayusculas, y con el guion bajo como espacio: «obligacion aporte» encuentra la tabla. */
    static String normalizar(String texto) {
        String sinMarcas = MARCAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        return sinMarcas.toLowerCase(Locale.ROOT).replace('_', ' ').strip();
    }

    // --- lo que la pantalla puede pedir --------------------------------------------------------------------------

    /** Los filtros de la lista. Todos opcionales; los que vienen se combinan con Y. */
    public record Filtro(
            Optional<String> q,
            Optional<String> esquema,
            Optional<String> modulo,
            Optional<String> tipo,
            Optional<String> estado,
            Optional<String> revision,
            Optional<String> sensibilidad) {

        public static Filtro ninguno() {
            return new Filtro(
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty());
        }

        Predicate<Indexada> comoPredicado(java.util.function.Function<Indexada, FichaDeGobierno> ficha) {
            Optional<String> termino = q.map(ConsultarCatalogoDeDatos::normalizar).filter(s -> !s.isEmpty());
            return t -> esquema.map(t.esquema()::equals).orElse(true)
                    && modulo.map(t.modulo()::equals).orElse(true)
                    && tipo.map(t.tipo()::equals).orElse(true)
                    && estado.map(e -> e.equals(ficha.apply(t).estado())).orElse(true)
                    && revision.map(r -> r.equals(ficha.apply(t).estadoRevision())).orElse(true)
                    && sensibilidad.map(t.clasificacion()::tiene).orElse(true)
                    && termino.map(t.buscable()::contains).orElse(true);
        }
    }

    public record Clasificacion(int personales, int financieros, int secretos, int derivados) {

        boolean tiene(String sensibilidad) {
            return switch (sensibilidad) {
                case "PERSONAL" -> personales > 0;
                case "FINANCIERO" -> financieros > 0;
                case "SECRETO" -> secretos > 0;
                default -> false;
            };
        }

        static Clasificacion de(JsonNode c) {
            return new Clasificacion(
                    c.path("personales").asInt(), c.path("financieros").asInt(),
                    c.path("secretos").asInt(), c.path("derivados").asInt());
        }
    }

    public record Fila(
            String tabla,
            String esquema,
            String tipo,
            String modulo,
            String nombreModulo,
            Optional<String> servicio,
            Optional<String> queEs,
            boolean appendOnly,
            int columnas,
            int columnasRevisadas,
            int relacionesSalientes,
            int relacionesEntrantes,
            Clasificacion clasificacion,
            FichaDeGobierno gobierno) {}

    public record Pagina(List<Fila> items, int pagina, int limite, int total, int totalPaginas) {}

    public record Esquema(
            String nombre,
            Optional<String> servicio,
            String descripcion,
            int tablas,
            int tablasDeNegocio,
            int columnas,
            int relaciones,
            int appendOnly,
            int conDatosPersonales) {

        static Esquema de(JsonNode e) {
            return new Esquema(
                    e.path("nombre").asText(),
                    texto(e, "servicio"),
                    e.path("descripcion").asText(),
                    e.path("tablas").asInt(),
                    e.path("tablasDeNegocio").asInt(),
                    e.path("columnas").asInt(),
                    e.path("relaciones").asInt(),
                    e.path("appendOnly").asInt(),
                    e.path("conDatosPersonales").asInt());
        }
    }

    public record Modulo(String codigo, String nombre, int tablas) {}

    public record Resumen(
            int tablas,
            int tablasDeNegocio,
            int tablasDeInfraestructura,
            int columnas,
            int columnasRevisadas,
            int relaciones,
            int conDatosPersonales,
            int conDatosFinancieros,
            int criticasParaAuditoria,
            int sinResponsable,
            Map<String, Integer> porEstado,
            Map<String, Integer> porRevision,
            List<Esquema> esquemas,
            List<Modulo> modulos,
            List<OpcionDeRetencion> politicas) {}

    /** La tabla del recurso (estructura y narrativa) con lo que una persona decidio sobre ella. */
    public record Ficha(JsonNode tabla, FichaDeGobierno gobierno, Map<String, RevisionDeColumna> revisiones) {}

    /** Una tabla del recurso con lo que el filtro y la fila necesitan, calculado una sola vez. */
    record Indexada(
            JsonNode nodo,
            String clave,
            String tabla,
            String esquema,
            String tipo,
            String modulo,
            String nombreModulo,
            Optional<String> servicio,
            Optional<String> queEs,
            boolean appendOnly,
            List<String> columnas,
            int salientes,
            int entrantes,
            Clasificacion clasificacion,
            String buscable) {

        static Indexada de(JsonNode t) {
            String tabla = t.path("tabla").asText();
            String esquema = t.path("esquema").asText();
            List<String> columnas = new ArrayList<>();
            t.path("columnas").forEach(c -> columnas.add(c.path("nombre").asText()));
            Optional<String> queEs = texto(t.path("negocio"), "queEs");
            String buscable = normalizar(String.join(" ", esquema, tabla, String.join(" ", columnas), queEs.orElse("")));
            return new Indexada(
                    t,
                    FichaDeGobierno.clave(esquema, tabla),
                    tabla,
                    esquema,
                    t.path("tipo").asText("NEGOCIO"),
                    t.path("modulo").asText(),
                    t.path("nombreModulo").asText(),
                    texto(t, "servicio"),
                    queEs,
                    t.path("appendOnly").asBoolean(),
                    List.copyOf(columnas),
                    t.path("relaciones").path("salientes").size(),
                    t.path("relaciones").path("entrantes").size(),
                    Clasificacion.de(t.path("clasificacion")),
                    buscable);
        }

        Fila fila(FichaDeGobierno gobierno, int revisadas) {
            return new Fila(
                    tabla, esquema, tipo, modulo, nombreModulo, servicio, queEs, appendOnly, columnas.size(),
                    revisadas, salientes, entrantes, clasificacion, gobierno);
        }
    }

    private static Optional<String> texto(JsonNode nodo, String campo) {
        JsonNode v = nodo.path(campo);
        return v.isTextual() && !v.asText().isBlank() ? Optional.of(v.asText()) : Optional.empty();
    }
}
