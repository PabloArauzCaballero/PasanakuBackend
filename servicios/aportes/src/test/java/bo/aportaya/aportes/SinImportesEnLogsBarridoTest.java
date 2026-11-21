package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regla 90.2.1: un log lleva identificadores, nunca importes. Un monto en el log es un dato
 * financiero atribuible a una persona que queda en cada copia del log, sin bitacora de lectura.
 *
 * <p>Se barre el texto de cada llamada a la bitacora (hasta su {@code ;}) buscando un marcador
 * «monto={}», «saldo={}», «importe={}», «neto={}» o «bruto={}».
 */
class SinImportesEnLogsBarridoTest {

    private static final Pattern LLAMADA = Pattern.compile(
            "\\b(BITACORA|LOG|log|logger)\\s*\\.\\s*(trace|debug|info|warn|error)\\s*\\([^;]*;", Pattern.DOTALL);
    private static final Pattern IMPORTE =
            Pattern.compile("(?i)\\b\\w*(monto|saldo|importe|neto|bruto)\\w*\\s*[=:]\\s*\\{\\}");

    @Test
    @DisplayName("sin-importes-en-logs: ningun log de este servicio escribe un importe (regla 90.2.1)")
    void ningunLogEscribeUnImporte() throws IOException {
        assertThat(infracciones(Path.of("src/main/java"))).isEmpty();
    }

    @Test
    @DisplayName("el detector reconoce un importe en el log y deja pasar los identificadores")
    void elDetectorFunciona() {
        assertThat(enTexto("BITACORA.info(\"Orden creada: id={}, monto={}\", id, monto);"))
                .isNotEmpty();
        assertThat(enTexto("log.warn(\n \"x: saldoDisponible = {}\",\n s);")).isNotEmpty();
        assertThat(enTexto("BITACORA.info(\"Orden creada: id={}, cuenta={}\", id, cuenta);"))
                .isEmpty();
    }

    static List<String> infracciones(Path raiz) throws IOException {
        List<String> hallazgos = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(raiz)) {
            for (Path archivo :
                    archivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                enTexto(Files.readString(archivo)).forEach(l -> hallazgos.add(archivo + ": " + l));
            }
        }
        return hallazgos;
    }

    static List<String> enTexto(String fuente) {
        List<String> hallazgos = new ArrayList<>();
        var llamadas = LLAMADA.matcher(fuente);
        while (llamadas.find()) {
            if (IMPORTE.matcher(llamadas.group()).find()) {
                hallazgos.add(llamadas.group().replaceAll("\\s+", " "));
            }
        }
        return hallazgos;
    }
}
