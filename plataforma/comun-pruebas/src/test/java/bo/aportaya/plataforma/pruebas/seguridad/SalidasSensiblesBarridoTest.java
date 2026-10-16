package bo.aportaya.plataforma.pruebas.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H13.S1.M3 · Ningun log, URL, evento ni mensaje de error del repositorio entero lleva un
 * dato personal o una credencial, salvo la deuda declarada en
 * {@code seguridad/salidas-sensibles-conocidas.txt}.
 *
 * <p>Barre los quince servicios y las librerias de plataforma de una vez, no cada modulo
 * con su propia regla: un servicio nuevo entra al barrido el dia que se escribe. La lista
 * de deuda no crece sola (un hallazgo nuevo rompe la prueba) y tampoco se pudre (una
 * linea que ya no se encuentra la rompe tambien), asi que bajar la deuda es borrar lineas.
 */
class SalidasSensiblesBarridoTest {

    private final Path raiz = raizDelRepositorio();

    @Test
    @DisplayName("barrido: ninguna salida nueva lleva PII ni credenciales; la deuda conocida esta declarada y vigente")
    void ningunaSalidaNuevaConDatosSensibles() {
        List<SalidasSensibles.Hallazgo> hallazgos = SalidasSensibles.revisar(modulos());
        Set<String> encontrados =
                hallazgos.stream().map(h -> h.clave(raiz)).collect(Collectors.toCollection(TreeSet::new));
        Set<String> conocidos = deudaConocida();

        List<String> nuevos = hallazgos.stream()
                .filter(h -> !conocidos.contains(h.clave(raiz)))
                .map(h -> h.describir(raiz))
                .toList();
        List<String> viejos =
                conocidos.stream().filter(c -> !encontrados.contains(c)).toList();

        assertThat(nuevos)
                .as("salida con dato sensible que no estaba declarada: se loguean ids, no contenidos (90.2.1-2-9)")
                .isEmpty();
        assertThat(viejos)
                .as("lineas de salidas-sensibles-conocidas.txt que ya no existen en el codigo: borrarlas")
                .isEmpty();
    }

    @Test
    @DisplayName("barrido: el barrido recorre de verdad los servicios y la plataforma (no una lista vacia)")
    void recorreLosModulos() {
        assertThat(modulos()).hasSizeGreaterThanOrEqualTo(15 + 6);
    }

    private Set<String> deudaConocida() {
        Path lista =
                raiz.resolve("plataforma/comun-pruebas/src/test/resources/seguridad/salidas-sensibles-conocidas.txt");
        try {
            return Files.readAllLines(lista).stream()
                    .map(String::strip)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .map(l -> l.contains("#") ? l.substring(0, l.indexOf('#')).strip() : l)
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<Path> modulos() {
        List<Path> modulos = new ArrayList<>();
        for (String carpeta : List.of("servicios", "plataforma")) {
            try (Stream<Path> hijos = Files.list(raiz.resolve(carpeta))) {
                hijos.filter(Files::isDirectory).sorted().forEach(modulos::add);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return modulos;
    }

    private static Path raizDelRepositorio() {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null && !Files.isDirectory(actual.resolve("servicios"))) {
            actual = actual.getParent();
        }
        return actual;
    }
}
