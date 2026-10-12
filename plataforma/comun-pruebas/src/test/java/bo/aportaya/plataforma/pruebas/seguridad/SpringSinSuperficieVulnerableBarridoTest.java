package bo.aportaya.plataforma.pruebas.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H17 · Mantiene VERDADERA la excepcion de OSV de Spring Framework 6.2.19 (sin parche en 6.2.x; el fix es 7.0.x).
 *
 * <p>Las tres advisories exigen un mecanismo concreto. Esta prueba falla si el codigo empieza a usarlo mientras
 * la migracion no ocurra, porque entonces la excepcion deja de decir la verdad:
 *
 * <ul>
 *   <li>CVE-2026-47890 (GHSA-j9f9-w8pj-32f8): SSE con fragmentos de vistas. Exige SSE y renderizado de vistas.
 *   <li>CVE-2026-47884 (GHSA-pc63-qcmh-9cmg): {@code XsltView} con un mapeo {@code /**} que renderiza vistas.
 *   <li>CVE-2026-47892 (GHSA-9qf2-26p9-2q2q): endpoints funcionales de WebFlux desplegados bajo
 *       {@code DispatcherServlet}. El gateway los usa (una sonda) pero corre sobre Netty, sin servlet.
 * </ul>
 *
 * <p>Se borra junto con la excepcion de {@code .github/osv-scanner.toml} al migrar a Spring Boot 4.
 */
class SpringSinSuperficieVulnerableBarridoTest {

    private static final List<String> MECANISMOS_PROHIBIDOS = List.of(
            "XsltView", "FragmentsRendering", "SseEmitter", "TEXT_EVENT_STREAM", "text/event-stream", "ViewResolver");

    @Test
    @DisplayName("barrido: ningun codigo de produccion usa SSE, vistas ni XSLT (superficie de CVE-2026-47890 y -47884)")
    void sinSseNiVistas() {
        List<String> usos = new ArrayList<>();
        for (Path fuente : fuentesDeProduccion(raiz())) {
            String texto = leer(fuente);
            for (String mecanismo : MECANISMOS_PROHIBIDOS) {
                if (texto.contains(mecanismo)) {
                    usos.add(mecanismo + " en " + raiz().relativize(fuente));
                }
            }
        }

        assertThat(usos)
                .as(
                        "usa un mecanismo del que depende una vulnerabilidad de Spring 6.2 sin parche: migrar o revisar la excepcion de OSV")
                .isEmpty();
    }

    @Test
    @DisplayName("barrido: el gateway no corre sobre servlet (CVE-2026-47892 exige DispatcherServlet)")
    void gatewaySinServlet() {
        Path gateway = raiz().resolve("plataforma/gateway");
        List<String> usos = new ArrayList<>();
        for (Path fuente : fuentesDeProduccion(gateway)) {
            String texto = leer(fuente);
            if (texto.contains("jakarta.servlet") || texto.contains("@EnableWebMvc")) {
                usos.add(raiz().relativize(fuente).toString());
            }
        }
        String build = leer(gateway.resolve("build.gradle.kts"));
        if (build.contains("starter-web\"") || build.contains("spring.boot.web")) {
            usos.add("plataforma/gateway/build.gradle.kts declara el starter servlet");
        }

        assertThat(usos)
                .as("el gateway dejo de ser solo reactivo: la excepcion de GHSA-9qf2-26p9-2q2q ya no aplica")
                .isEmpty();
    }

    @Test
    @DisplayName("barrido: los servicios servlet no declaran endpoints funcionales (RouterFunction)")
    void serviciosSinEndpointsFuncionales() {
        List<String> usos = new ArrayList<>();
        for (Path fuente : fuentesDeProduccion(raiz().resolve("servicios"))) {
            if (leer(fuente).contains("RouterFunction")) {
                usos.add(raiz().relativize(fuente).toString());
            }
        }

        assertThat(usos)
                .as("endpoint funcional en un servicio que corre bajo DispatcherServlet (GHSA-9qf2-26p9-2q2q)")
                .isEmpty();
    }

    private static List<Path> fuentesDeProduccion(Path base) {
        List<Path> archivos = new ArrayList<>();
        try (Stream<Path> arbol = Files.walk(base)) {
            arbol.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().replace('\\', '/').contains("/src/main/java/"))
                    .sorted()
                    .forEach(archivos::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return archivos;
    }

    private static String leer(Path archivo) {
        try {
            return Files.readString(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path raiz() {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null && !Files.isDirectory(actual.resolve("servicios"))) {
            actual = actual.getParent();
        }
        return actual;
    }
}
