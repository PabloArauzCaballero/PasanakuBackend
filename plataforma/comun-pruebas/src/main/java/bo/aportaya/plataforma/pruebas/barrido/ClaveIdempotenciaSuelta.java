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
 * Regla {@code clave-idempotencia-suelta} — ADR-046.
 *
 * <p>La identidad de una reserva de idempotencia es {@code (usuario_id, operacion,
 * clave_idempotencia)}, las tres juntas (H1.S1.M3). Un {@code .where(...)} que arranca
 * filtrando por {@code clave_idempotencia} y no encadena ningun {@code .and(...)} en la
 * misma sentencia es exactamente el defecto que {@code ADR-046} corrigio: dos usuarios —
 * u dos operaciones— con la misma clave se pisan.
 *
 * <p>La regla es deliberadamente mecanica, como {@link SinUmbralLiteral}: no entiende
 * jOOQ, solo busca el patron textual y el {@code .and(} que tendria que seguirlo antes
 * del {@code ;} de la sentencia. Un {@code .and()} presente no prueba que las columnas
 * sean las correctas — eso lo prueba {@code IdempotenciaRepositorioTest} — pero su
 * AUSENCIA prueba con certeza que la columna via sola, que es el caso que importa
 * detectar aca: nadie escribe la version correcta por accidente y la incorrecta a
 * proposito.
 */
public final class ClaveIdempotenciaSuelta {

    private static final Pattern WHERE_CLAVE =
            Pattern.compile("\\.where\\(\\s*DSL\\.field\\(\\s*\"clave_idempotencia\"");

    private ClaveIdempotenciaSuelta() {}

    public static List<Hallazgo> revisar(Path raiz) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        for (Path archivo : TamanoDeArchivo.fuentesJava(raiz)) {
            String contenido = leer(archivo);
            Matcher m = WHERE_CLAVE.matcher(contenido);
            while (m.find()) {
                int finDeSentencia = finDeSentencia(contenido, m.end());
                String sentencia = contenido.substring(m.start(), finDeSentencia);
                if (!sentencia.contains(".and(")) {
                    int linea = 1 + contarSaltos(contenido, m.start());
                    hallazgos.add(new Hallazgo(
                            archivo,
                            linea,
                            "clave_idempotencia en .where(...) sin .and(...) en la misma sentencia:"
                                    + " la identidad de una reserva es (usuario_id, operacion,"
                                    + " clave_idempotencia), nunca la clave sola (ADR-046)"));
                }
            }
        }
        return hallazgos;
    }

    /** El primer {@code ;} despues del punto de arranque, o el final del archivo. */
    private static int finDeSentencia(String contenido, int desde) {
        int fin = contenido.indexOf(';', desde);
        return fin == -1 ? contenido.length() : fin;
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
