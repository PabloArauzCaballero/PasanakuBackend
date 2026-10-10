package bo.aportaya.plataforma.pruebas.seguridad;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * H13.S1.M3 · Barrido de las SALIDAS del codigo —logs, URLs, eventos y mensajes de
 * error— en busca de datos personales y credenciales (reglas 90.2.1, 90.2.2 y 90.2.9).
 *
 * <p>Lee fuentes, no ejecuta. Mira los cuatro lugares por donde un dato sale del proceso
 * sin que nadie lo decida con cuidado: lo que se escribe en un log, lo que viaja en una
 * URL, el payload que el outbox publica a todos los consumidores, y el texto de una
 * excepcion que termina en la respuesta. La regla es la de la casa: se loguean
 * identificadores, no contenidos.
 *
 * <p>Es una heuristica sobre nombres, a proposito estrecha: una regla que grita seguido
 * se apaga. Lo que encuentra, lo encuentra con archivo y linea; lo que NO encuentra no
 * queda demostrado como limpio —un dato con un nombre inocente pasa—, y eso se declara
 * en la evidencia.
 */
public final class SalidasSensibles {

    public enum Salida {
        LOG,
        URL,
        EVENTO,
        ERROR,
        CONSOLA
    }

    /** Un dato sensible que sale por donde no debe. {@code dato} es el nombre que lo delata. */
    public record Hallazgo(Path archivo, int linea, Salida salida, String dato) {

        public String clave(Path raiz) {
            return "%s|%s|%s".formatted(raiz.relativize(archivo).toString().replace('\\', '/'), salida, dato);
        }

        public String describir(Path raiz) {
            return "%s:%d  %s  «%s»"
                    .formatted(raiz.relativize(archivo).toString().replace('\\', '/'), linea, salida, dato);
        }
    }

    private static final Pattern LLAMADA_DE_LOG =
            Pattern.compile("\\b(?:BITACORA|LOGGER|LOG|log|logger)\\s*\\.\\s*(?:trace|debug|info|warn|error)\\s*\\(");
    private static final Pattern LLAMADA_DE_URL = Pattern.compile(
            "\\.\\s*(?:uri|consultar|pedir)\\s*\\(|\\bURI\\s*\\.\\s*create\\s*\\(|\\bUriComponentsBuilder\\b");
    private static final Pattern LLAMADA_DE_EVENTO = Pattern.compile("\\bnew\\s+EventoDominio\\s*\\(");
    private static final Pattern LLAMADA_DE_ERROR = Pattern.compile(
            "\\bnew\\s+(?:ErrorDeNegocio|ErrorDeDominio|IllegalStateException|IllegalArgumentException)\\s*\\(");
    private static final Pattern CONSOLA =
            Pattern.compile("\\bSystem\\s*\\.\\s*(?:out|err)\\s*\\.\\s*print|\\.printStackTrace\\s*\\(");
    private static final Pattern IDENTIFICADOR = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern CADENA = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern PARAMETRO_SENSIBLE = Pattern.compile(
            "(?i)[?&](telefono|celular|correo|email|token|secreto|clave|password|contrasena|documento|otp|nit)=");
    /**
     * Un texto de log que dice que el valor va a continuacion: «el token {}», «clave={}». Un
     * identificador (tokenId={}) no cuenta: es el nombre del dato, no el dato.
     */
    private static final Pattern VALOR_EN_EL_TEXTO =
            Pattern.compile("(?i)\\b(telefono|correo|email|token|secreto|clave|password|contrasena|otp)"
                    + "\\s*(?:[=:]|\\bes\\b|\\bfue\\b)?\\s*(\\{\\}|%s)|\\b(c[oó]digo)\\s+(es|fue)\\s*\\{\\}");

    private static final Pattern NOMBRE_SENSIBLE = Pattern.compile(
            ".*(telefono|celular|correo|email|password|contrasena|secreto|secret|pimienta|otp|documento|iban|cvv"
                    + "|token|clave|apellido|direccion|numerocuenta).*");
    private static final Pattern NOMBRE_DE_MEDIDA = Pattern.compile(".*(largo|longitud|maximo|minimo|max_|min_).*");
    private static final Pattern NOMBRE_INOFENSIVO = Pattern.compile(
            ".*(id|ids|hash|tipo|estado|enmascarad\\w*|redactad\\w*|len|largo|longitud|count|size|vigencia|ttl|expira\\w*"
                    + "|proposito|valido|existe|presente|activo|uuid|clase|nombre|fecha|total|cantidad|intentos)$");

    private static final Set<String> PALABRAS_DE_JAVA =
            Set.of("new", "null", "true", "false", "this", "var", "final", "return", "String", "Map", "List", "of");

    private SalidasSensibles() {}

    /** Barre {@code src/main/java} de cada modulo bajo las raices dadas; ignora lo generado. */
    public static List<Hallazgo> revisar(List<Path> raices) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        for (Path raiz : raices) {
            for (Path archivo : fuentes(raiz)) {
                hallazgos.addAll(revisar(FuenteJava.leer(archivo)));
            }
        }
        return hallazgos;
    }

    public static List<Hallazgo> revisar(FuenteJava fuente) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        String m = fuente.enmascarado();
        buscar(fuente, m, LLAMADA_DE_LOG, Salida.LOG, hallazgos);
        buscar(fuente, m, LLAMADA_DE_URL, Salida.URL, hallazgos);
        buscar(fuente, m, LLAMADA_DE_EVENTO, Salida.EVENTO, hallazgos);
        buscar(fuente, m, LLAMADA_DE_ERROR, Salida.ERROR, hallazgos);
        Matcher consola = CONSOLA.matcher(m);
        while (consola.find()) {
            hallazgos.add(new Hallazgo(fuente.archivo(), fuente.linea(consola.start()), Salida.CONSOLA, "print"));
        }
        return hallazgos;
    }

    private static void buscar(
            FuenteJava fuente, String mascara, Pattern inicio, Salida salida, List<Hallazgo> destino) {
        Matcher llamada = inicio.matcher(mascara);
        while (llamada.find()) {
            int abre = mascara.indexOf('(', llamada.start());
            int cierra = cierre(mascara, abre);
            if (abre < 0 || cierra < 0) {
                continue;
            }
            String argumentos = mascara.substring(abre + 1, cierra);
            String conTextos = fuente.original().substring(abre + 1, cierra);
            int linea = fuente.linea(llamada.start());
            List<String> datos = new ArrayList<>();
            identificadoresSensibles(argumentos, datos);
            if (salida == Salida.URL) {
                parametrosEnLaUrl(conTextos, datos);
            }
            if (salida == Salida.LOG) {
                valoresEnElTexto(conTextos, datos);
            }
            if (salida == Salida.EVENTO) {
                clavesDelPayload(conTextos, datos);
            }
            datos.stream().distinct().forEach(d -> destino.add(new Hallazgo(fuente.archivo(), linea, salida, d)));
        }
    }

    private static void identificadoresSensibles(String argumentos, List<String> destino) {
        Matcher id = IDENTIFICADOR.matcher(argumentos);
        while (id.find()) {
            String nombre = id.group();
            String minuscula = nombre.toLowerCase(Locale.ROOT);
            if (!PALABRAS_DE_JAVA.contains(nombre)
                    && NOMBRE_SENSIBLE.matcher(minuscula).matches()
                    && !NOMBRE_DE_MEDIDA.matcher(minuscula).matches()
                    && !NOMBRE_INOFENSIVO.matcher(minuscula).matches()) {
                destino.add(nombre);
            }
        }
    }

    private static void parametrosEnLaUrl(String conTextos, List<String> destino) {
        Matcher p = PARAMETRO_SENSIBLE.matcher(conTextos);
        while (p.find()) {
            destino.add("?" + p.group(1).toLowerCase(Locale.ROOT));
        }
    }

    private static void valoresEnElTexto(String conTextos, List<String> destino) {
        Matcher cadenas = CADENA.matcher(conTextos);
        while (cadenas.find()) {
            Matcher v = VALOR_EN_EL_TEXTO.matcher(cadenas.group(1));
            if (v.find()) {
                destino.add("texto:" + (v.group(1) != null ? v.group(1) : v.group(3)).toLowerCase(Locale.ROOT));
            }
        }
    }

    private static void clavesDelPayload(String conTextos, List<String> destino) {
        Matcher cadenas = CADENA.matcher(conTextos);
        while (cadenas.find()) {
            String clave = cadenas.group(1);
            String minuscula = clave.toLowerCase(Locale.ROOT);
            if (NOMBRE_SENSIBLE.matcher(minuscula).matches()
                    && !NOMBRE_INOFENSIVO.matcher(minuscula).matches()) {
                destino.add("clave:" + clave);
            }
        }
    }

    private static int cierre(String texto, int abre) {
        if (abre < 0) {
            return -1;
        }
        int profundidad = 0;
        for (int i = abre; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '(') {
                profundidad++;
            } else if (c == ')' && --profundidad == 0) {
                return i;
            }
        }
        return -1;
    }

    private static List<Path> fuentes(Path raiz) {
        if (!Files.isDirectory(raiz)) {
            return List.of();
        }
        try (Stream<Path> arbol = Files.walk(raiz)) {
            return arbol.filter(p -> p.getFileName().toString().endsWith(".java"))
                    .filter(p -> {
                        String ruta = p.toString().replace('\\', '/');
                        return ruta.contains("/src/main/java/")
                                && !ruta.contains("/build/")
                                && !ruta.contains("/generado/");
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
