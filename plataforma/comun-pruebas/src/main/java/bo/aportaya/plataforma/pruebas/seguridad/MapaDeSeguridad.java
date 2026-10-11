package bo.aportaya.plataforma.pruebas.seguridad;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.yaml.snakeyaml.Yaml;

/**
 * Cruza lo que cada contrato OpenAPI promete con lo que cada controlador decide sobre
 * el acceso: la matriz de autorizacion de los quince servicios, leida de las fuentes.
 *
 * <p>No arranca ningun servicio. Las comprobaciones dinamicas (401, 403, el cuerpo del
 * error) las hace la sabana de cada servicio; esto hace lo que ella no puede: ver los
 * quince a la vez, y encontrar la operacion que el contrato promete y el codigo no
 * protege, o el permiso que el codigo exige y el catalogo no tiene.
 */
public final class MapaDeSeguridad {

    private static final List<String> VERBOS = List.of("get", "post", "put", "patch", "delete");
    private static final Pattern ANOTACION = Pattern.compile("@(?:[A-Za-z_]\\w*\\.)*(Permiso|Publico)\\b");
    private static final Pattern DECLARACION_DE_TIPO = Pattern.compile("\\b(?:class|interface|record)\\s+\\w+");
    private static final Pattern CADENA = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final Path raiz;
    private final Map<String, List<FuenteJava>> fuentesPorServicio = new LinkedHashMap<>();

    private MapaDeSeguridad(Path raiz) {
        this.raiz = raiz;
    }

    public static MapaDeSeguridad delRepositorio() {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null && !Files.isDirectory(actual.resolve("servicios"))) {
            actual = actual.getParent();
        }
        if (actual == null) {
            throw new IllegalStateException("no encontre la raiz del repositorio (carpeta servicios/)");
        }
        return new MapaDeSeguridad(actual);
    }

    public static MapaDeSeguridad de(Path raiz) {
        return new MapaDeSeguridad(raiz);
    }

    public Path raiz() {
        return raiz;
    }

    /** Todas las operaciones de todos los contratos, en orden estable. */
    public List<Operacion> operaciones() {
        List<Operacion> todas = new ArrayList<>();
        for (Path contrato : contratos()) {
            String servicio = contrato.getParent()
                    .getParent()
                    .getParent()
                    .getParent()
                    .getParent()
                    .getFileName()
                    .toString();
            todas.addAll(leerContrato(servicio, contrato));
        }
        return todas;
    }

    public Decision decisionDe(Operacion operacion) {
        Pattern declaracion =
                Pattern.compile("\\bpublic\\s+[^;{}=()]*?\\b" + Pattern.quote(operacion.id()) + "\\s*\\(");
        for (FuenteJava fuente : fuentesDe(operacion.servicio())) {
            Matcher m = declaracion.matcher(fuente.enmascarado());
            while (m.find()) {
                String bloque = fuente.anotacionesEnmascaradasAntesDe(m.start());
                if (bloque.contains("@Override")) {
                    return decidir(fuente, m.start(), bloque);
                }
            }
        }
        return Decision.sinMetodo();
    }

    private Decision decidir(FuenteJava fuente, int posicion, String bloque) {
        Matcher propia = ANOTACION.matcher(bloque);
        if (propia.find()) {
            return construir(fuente, posicion, propia.group(1), fuente.anotacionesAntesDe(posicion));
        }
        Matcher tipo = DECLARACION_DE_TIPO.matcher(fuente.enmascarado());
        if (tipo.find()) {
            String deClase = fuente.anotacionesEnmascaradasAntesDe(tipo.start());
            Matcher heredada = ANOTACION.matcher(deClase);
            if (heredada.find()) {
                return construir(fuente, posicion, heredada.group(1), fuente.anotacionesAntesDe(tipo.start()));
            }
        }
        return new Decision(
                Decision.Tipo.NINGUNA, "", fuente.archivo(), fuente.linea(posicion), fuente.cuerpoDesde(posicion));
    }

    private Decision construir(FuenteJava fuente, int posicion, String anotacion, String bloqueOriginal) {
        Decision.Tipo tipo = "Permiso".equals(anotacion) ? Decision.Tipo.PERMISO : Decision.Tipo.PUBLICO;
        return new Decision(
                tipo,
                valorDe(bloqueOriginal, anotacion),
                fuente.archivo(),
                fuente.linea(posicion),
                fuente.cuerpoDesde(posicion));
    }

    /** El texto de la anotacion: el codigo del permiso, o la justificacion entera del publico. */
    private static String valorDe(String bloque, String anotacion) {
        Matcher inicio = Pattern.compile("@(?:[A-Za-z_]\\w*\\.)*" + anotacion + "\\s*\\(")
                .matcher(bloque);
        if (!inicio.find()) {
            return "";
        }
        // La justificacion puede llevar parentesis y estar partida con «+»: se juntan las
        // cadenas consecutivas y se corta en lo primero que no sea otra cadena.
        Matcher cadenas = CADENA.matcher(bloque);
        StringBuilder texto = new StringBuilder();
        int cursor = inicio.end();
        while (cadenas.find(cursor)) {
            String entre = bloque.substring(cursor, cadenas.start());
            if (!entre.isBlank() && !entre.strip().equals("+")) {
                break;
            }
            texto.append(cadenas.group(1));
            cursor = cadenas.end();
        }
        return texto.toString();
    }

    private List<FuenteJava> fuentesDe(String servicio) {
        return fuentesPorServicio.computeIfAbsent(servicio, s -> {
            Path codigo = raiz.resolve("servicios").resolve(s).resolve("src/main/java");
            try (Stream<Path> arbol = Files.walk(codigo)) {
                return arbol.filter(p -> p.getFileName().toString().endsWith(".java"))
                        .filter(p -> p.toString().replace('\\', '/').contains("/web/"))
                        .filter(p -> !p.toString().replace('\\', '/').contains("/generado/"))
                        .sorted()
                        .map(FuenteJava::leer)
                        .toList();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private List<Path> contratos() {
        Path servicios = raiz.resolve("servicios");
        try (Stream<Path> arbol = Files.walk(servicios, 6)) {
            return arbol.filter(Files::isRegularFile)
                    .filter(p -> p.getParent().getFileName().toString().equals("openapi"))
                    .filter(p -> p.toString().endsWith(".yaml"))
                    .filter(p -> p.toString().replace('\\', '/').contains("/src/main/resources/"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Operacion> leerContrato(String servicio, Path contrato) {
        Map<String, Object> documento;
        try {
            documento = new Yaml().load(Files.readString(contrato));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, Object> rutas = (Map<String, Object>) documento.getOrDefault("paths", Map.of());
        List<Operacion> operaciones = new ArrayList<>();
        for (Map.Entry<String, Object> ruta : rutas.entrySet()) {
            Map<String, Object> item = (Map<String, Object>) ruta.getValue();
            for (String verbo : VERBOS) {
                if (item.get(verbo) instanceof Map<?, ?> operacion) {
                    Map<String, Object> op = (Map<String, Object>) operacion;
                    List<String> enRuta = new ArrayList<>(parametrosDeRuta(documento, item));
                    enRuta.addAll(parametrosDeRuta(documento, op));
                    operaciones.add(new Operacion(
                            servicio,
                            verbo,
                            ruta.getKey(),
                            String.valueOf(op.get("operationId")),
                            enRuta,
                            propiedadesDelCuerpo(documento, op)));
                }
            }
        }
        return operaciones;
    }

    @SuppressWarnings("unchecked")
    private List<String> parametrosDeRuta(Map<String, Object> documento, Map<String, Object> nodo) {
        List<String> nombres = new ArrayList<>();
        for (Object p : (List<Object>) nodo.getOrDefault("parameters", List.of())) {
            Map<String, Object> parametro = resolver(documento, (Map<String, Object>) p);
            if ("path".equals(parametro.get("in"))) {
                nombres.add(String.valueOf(parametro.get("name")));
            }
        }
        return nombres;
    }

    @SuppressWarnings("unchecked")
    private List<String> propiedadesDelCuerpo(Map<String, Object> documento, Map<String, Object> operacion) {
        try {
            Map<String, Object> cuerpo = (Map<String, Object>) operacion.get("requestBody");
            Map<String, Object> contenido = (Map<String, Object>) cuerpo.get("content");
            Map<String, Object> json = (Map<String, Object>) contenido.get("application/json");
            Map<String, Object> esquema = resolver(documento, (Map<String, Object>) json.get("schema"));
            Object propiedades = esquema.get("properties");
            return propiedades instanceof Map<?, ?> m
                    ? m.keySet().stream().map(String::valueOf).toList()
                    : List.of();
        } catch (ClassCastException | NullPointerException e) {
            // Sin cuerpo JSON (multipart, vacio): no hay propiedades que mirar.
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolver(Map<String, Object> documento, Map<String, Object> nodo) {
        Object ref = nodo.get("$ref");
        if (ref == null) {
            return nodo;
        }
        Object actual = documento;
        for (String paso : String.valueOf(ref).replaceFirst("^#/", "").split("/")) {
            actual = ((Map<String, Object>) actual).get(paso);
        }
        return (Map<String, Object>) actual;
    }
}
