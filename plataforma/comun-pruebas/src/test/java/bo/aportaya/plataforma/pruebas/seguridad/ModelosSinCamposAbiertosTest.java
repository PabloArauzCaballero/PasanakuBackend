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
 * H13.S1.M2 hallazgo 7 · Los modelos que genera el OpenAPI no absorben campos de mas.
 *
 * <p>El generador {@code spring} 7.14 emitia {@code @JsonAnySetter} en 31 modelos de 11 servicios aunque
 * el contrato dijera {@code additionalProperties: false}: cualquier campo no declarado se guardaba en
 * un mapa en vez de rechazarse (mass assignment, regla 96.2.2). Se corrigio con plantillas propias
 * ({@code buildSrc/plantillas-openapi/spring}) que no emiten ese mapa.
 *
 * <p>Esta prueba cierra la puerta por las dos puntas: (1) las plantillas no vuelven a traer el mapa
 * (se lee la fuente, sin compilar nada), y (2) si hay modelos generados en {@code build/}, ninguno lo
 * tiene — la comprobacion sobre lo REALMENTE generado, que es lo que importa, se ejerce en cuanto se
 * compila un servicio; sin compilacion previa se declara que no hubo nada que mirar.
 */
class ModelosSinCamposAbiertosTest {

    private static final Path RAIZ = raiz();

    @Test
    @DisplayName("rechaza: las plantillas del generador no emiten el mapa de propiedades adicionales")
    void lasPlantillasNoLoEmiten() throws IOException {
        Path carpeta = RAIZ.resolve("buildSrc/plantillas-openapi/spring");
        assertThat(carpeta).as("las plantillas propias existen").isDirectory();
        for (String archivo : List.of("pojo.mustache", "model.mustache")) {
            String texto = Files.readString(carpeta.resolve(archivo));
            assertThat(texto)
                    .as("%s no puede volver a traer additionalProperties (JsonAnySetter)", archivo)
                    .doesNotContain("additionalPropertiesType")
                    .doesNotContain("JsonAnySetter")
                    .doesNotContain("{{>additionalProperties}}");
        }
    }

    @Test
    @DisplayName("rechaza: la tarea del generador usa las plantillas propias (si no, la correccion no aplica)")
    void elGeneradorLasUsa() throws IOException {
        String gradle = Files.readString(RAIZ.resolve("buildSrc/src/main/kotlin/aportaya.openapi.gradle.kts"));
        assertThat(gradle).contains("templateDir.set(").contains("plantillas-openapi/spring");
    }

    @Test
    @DisplayName("rechaza: ningun modelo ya generado en build/ declara @JsonAnySetter")
    void ningunModeloGeneradoLoTiene() throws IOException {
        List<String> con = new ArrayList<>();
        int revisados = 0;
        try (Stream<Path> servicios = Files.list(RAIZ.resolve("servicios"))) {
            for (Path servicio : (Iterable<Path>) servicios::iterator) {
                Path modelos = servicio.resolve("build/generated/openapi/src/main/java");
                if (!Files.isDirectory(modelos)) {
                    continue;
                }
                try (Stream<Path> fuentes = Files.walk(modelos)) {
                    for (Path java :
                            (Iterable<Path>) fuentes.filter(p -> p.toString().endsWith(".java"))::iterator) {
                        revisados++;
                        if (Files.readString(java).contains("@JsonAnySetter")) {
                            con.add(RAIZ.relativize(java).toString().replace('\\', '/'));
                        }
                    }
                }
            }
        }
        assertThat(con)
                .as("modelos generados con @JsonAnySetter (de %d revisados)", revisados)
                .isEmpty();
    }

    private static Path raiz() {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null && !Files.isDirectory(actual.resolve("servicios"))) {
            actual = actual.getParent();
        }
        if (actual == null) {
            throw new UncheckedIOException(new IOException("no encuentro la raiz del repositorio"));
        }
        return actual;
    }
}
