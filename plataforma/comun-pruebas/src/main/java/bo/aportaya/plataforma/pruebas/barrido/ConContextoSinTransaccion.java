package bo.aportaya.plataforma.pruebas.barrido;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regla {@code con-contexto-sin-transaccion}.
 *
 * <p>{@code Datos.conContexto} fija el contexto de sesion con {@code SET LOCAL}, que fuera de
 * una transaccion no fija nada, asi que se niega a correr: lanza {@code SinTransaccion}, que el
 * cliente ve como un 422 {@code AP-DOM-01}. Un caso de uso (capa {@code aplicacion}) que llama a
 * {@code conContexto} desde un metodo publico sin {@code @Transactional} —ni en el metodo ni en
 * la clase— falla en cuanto alguien lo invoca, y ninguna prueba con el caso de uso simulado lo
 * ve: es lo que le paso a las lecturas de usuarios, roles y permisos internos.
 *
 * <p>Mecanica, como {@link ClaveIdempotenciaSuelta}: no entiende Spring, mira el texto. Solo
 * revisa la capa {@code aplicacion}: el repositorio recibe el {@code DSLContext} ya preparado y
 * es el caso de uso quien abre la transaccion.
 */
public final class ConContextoSinTransaccion {

    private static final Pattern CLASE_TRANSACCIONAL = Pattern.compile(
            "@Transactional[^\\n]*\\n(?:\\s*@[\\w.]+(?:\\([^)]*\\))?\\s*\\n)*\\s*public\\s+(?:final\\s+|abstract\\s+)?class");

    /** Anotaciones inmediatamente anteriores + firma de un metodo publico (no clase, ni constructor). */
    private static final Pattern METODO_PUBLICO = Pattern.compile(
            "((?:[ \\t]*@[\\w.]+(?:\\((?:[^()]|\\([^()]*\\))*\\))?[ \\t]*\\n)*)"
                    + "[ \\t]*public\\s+(?!(?:abstract\\s+|static\\s+|final\\s+)*(?:class|interface|record|enum)\\b)"
                    + "[\\w<>\\[\\],.? ]+?\\s+(\\w+)\\s*\\((?:[^()]|\\([^()]*\\))*\\)[^{;]*\\{");

    private ConContextoSinTransaccion() {}

    public static List<Hallazgo> revisar(Path raiz) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        for (Path archivo : TamanoDeArchivo.fuentesJava(raiz)) {
            if (!archivo.toString().replace('\\', '/').contains("/aplicacion/")) {
                continue;
            }
            String contenido = leer(archivo);
            if (!contenido.contains("conContexto(") || CLASE_TRANSACCIONAL.matcher(contenido).find()) {
                continue;
            }
            Matcher m = METODO_PUBLICO.matcher(contenido);
            while (m.find()) {
                String anotaciones = m.group(1);
                String cuerpo = cuerpo(contenido, m.end() - 1);
                if (cuerpo.contains("conContexto(") && !anotaciones.contains("@Transactional")) {
                    int linea = 1 + contarSaltos(contenido, m.start(2));
                    hallazgos.add(new Hallazgo(
                            archivo,
                            linea,
                            "metodo publico `" + m.group(2) + "` llama a conContexto sin @Transactional:"
                                    + " SET LOCAL fuera de una transaccion no fija nada y el cliente recibe"
                                    + " 422 AP-DOM-01 (SinTransaccion)"));
                }
            }
        }
        return hallazgos;
    }

    /** Desde la llave de apertura en {@code abre} hasta su cierre, contando llaves. */
    private static String cuerpo(String contenido, int abre) {
        int profundidad = 0;
        for (int i = abre; i < contenido.length(); i++) {
            char c = contenido.charAt(i);
            if (c == '{') {
                profundidad++;
            } else if (c == '}' && --profundidad == 0) {
                return contenido.substring(abre, i + 1);
            }
        }
        return contenido.substring(abre);
    }

    private static int contarSaltos(String contenido, int hasta) {
        int saltos = 0;
        for (int i = 0; i < hasta; i++) {
            if (contenido.charAt(i) == '\n') {
                saltos++;
            }
        }
        return saltos;
    }

    private static String leer(Path archivo) {
        try {
            return Files.readString(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
