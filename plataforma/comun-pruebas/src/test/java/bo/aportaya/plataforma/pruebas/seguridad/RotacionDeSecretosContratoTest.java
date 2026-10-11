package bo.aportaya.plataforma.pruebas.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H13.S1.M3 · Todo secreto que el despliegue inyecta tiene su fila en el inventario de
 * rotacion. Un secreto sin procedimiento no se puede rotar el dia que se filtra.
 *
 * <p>La lista sale de {@code scripts/generar_compose.py} —la unica fuente de lo que llega al
 * entorno— y no de una copia: un secreto nuevo en el compose rompe esta prueba hasta que
 * alguien decide como se rota.
 */
class RotacionDeSecretosContratoTest {

    private static final Pattern VARIABLE = Pattern.compile("\\$\\{([A-Z0-9_]+)\\}");
    private static final Pattern ES_SECRETO =
            Pattern.compile("(?i)(CLAVE|SECRET|PIMIENTA|TOKEN|API_KEY|FIRMA|PASSWORD|CREDENCIAL)");

    private final Path raiz = raiz();

    @Test
    @DisplayName("rechaza: todo secreto que el compose desplegado inyecta figura en el inventario de rotacion")
    void todoSecretoTieneFila() throws IOException {
        String compose = Files.readString(raiz.resolve("scripts/generar_compose.py"));
        int desde = compose.indexOf("COMUNES_DESPLEGADO = {");
        String bloque = compose.substring(desde);
        int entorno = bloque.indexOf("DE_ENTORNO = {");
        String comunes = bloque.substring(0, bloque.indexOf("\n}"));
        String deEntorno = bloque.substring(entorno);
        deEntorno = deEntorno.substring(0, deEntorno.indexOf("\n}"));

        Set<String> secretos = new TreeSet<>();
        Matcher m = VARIABLE.matcher(comunes + "\n" + deEntorno);
        while (m.find()) {
            if (ES_SECRETO.matcher(m.group(1)).find()) {
                secretos.add(m.group(1));
            }
        }
        assertThat(secretos).as("el compose inyecta secretos que inventariar").hasSizeGreaterThanOrEqualTo(8);

        String documento = Files.readString(raiz.resolve("docs/operacion/rotacion-de-secretos-ensayada.md"));
        assertThat(secretos.stream().filter(s -> !documento.contains("`" + s + "`")))
                .as("secretos sin fila en docs/operacion/rotacion-de-secretos-ensayada.md")
                .isEmpty();
    }

    @Test
    @DisplayName("el inventario declara la clase de rotacion y el costo de cada secreto, y el ensayo esta junto")
    void elInventarioDiceComoSeRota() throws IOException {
        String documento = Files.readString(raiz.resolve("docs/operacion/rotacion-de-secretos-ensayada.md"));

        assertThat(documento).contains("| Secreto |", "Costo de rotarlo", "## Acciones seguras", "## Validación");
        assertThat(Files.isRegularFile(raiz.resolve("despliegue/ensayos/rotacion/ensayo_rotacion.py")))
                .as("el ensayo que respalda el documento")
                .isTrue();
    }

    private static Path raiz() {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null && !Files.isDirectory(actual.resolve("servicios"))) {
            actual = actual.getParent();
        }
        return actual;
    }
}
