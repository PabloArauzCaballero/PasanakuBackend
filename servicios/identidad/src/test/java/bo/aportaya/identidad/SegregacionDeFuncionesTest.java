package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.dominio.SegregacionDeFunciones;
import bo.aportaya.identidad.dominio.SegregacionDeFunciones.Par;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * R-SEG-04 con los codigos del catalogo REAL. La version anterior comparaba {@code ENTREGAS_AUTORIZAR} y nunca se
 * disparaba, porque la semilla dice {@code ENTREGA_AUTORIZAR}: por eso esta prueba lee la semilla en vez de repetir los
 * codigos de memoria.
 */
class SegregacionDeFuncionesTest {

    private static final Path SEMILLA = Path.of("seeders", "minimos", "10-roles-y-permisos.json");

    @ParameterizedTest(name = "P-3 · {0} con {1} es incompatible, en cualquier orden")
    @CsvSource({
        "ENTREGA_AUTORIZAR, ENTREGA_EJECUTAR",
        "BLOQUEO_AUTORIDAD_REGISTRAR, BLOQUEO_AUTORIDAD_LEVANTAR",
        "PUBLICIDAD_MODERAR, PUBLICIDAD_ANUNCIANTES",
        "PUBLICIDAD_MODERAR, PUBLICIDAD_APROBAR_CAMPANA"
    })
    void cadaParDecididoSeDetecta(String uno, String otro) {
        assertThat(SegregacionDeFunciones.viola(Set.of(uno, otro, "AUDITORIA_LEER")))
                .isTrue();
        assertThat(SegregacionDeFunciones.viola(Set.of(otro, uno))).isTrue();
        assertThat(SegregacionDeFunciones.conflicto(Set.of(uno, otro))).contains(uno + " con " + otro);
    }

    @ParameterizedTest(name = "P-3 · {0} sin su par no es conflicto")
    @CsvSource({
        "ENTREGA_AUTORIZAR",
        "ENTREGA_EJECUTAR",
        "BLOQUEO_AUTORIDAD_REGISTRAR",
        "BLOQUEO_AUTORIDAD_LEVANTAR",
        "PUBLICIDAD_MODERAR"
    })
    void unLadoSoloNoEsConflicto(String permiso) {
        assertThat(SegregacionDeFunciones.conflicto(Set.of(permiso, "AUDITORIA_LEER")))
                .isEmpty();
    }

    @Test
    @DisplayName("los pares de autorizar y ejecutar distintos no se cruzan: autorizar una entrega y levantar un bloqueo"
            + " no es conflicto")
    void paresDistintosNoSeCruzan() {
        assertThat(SegregacionDeFunciones.viola(Set.of("ENTREGA_AUTORIZAR", "BLOQUEO_AUTORIDAD_LEVANTAR")))
                .isFalse();
    }

    @Test
    @DisplayName("cada codigo de la tabla de pares existe en el catalogo sembrado")
    void losCodigosSonDelCatalogo() throws IOException {
        Set<String> catalogo = permisosDelCatalogo();

        for (Par par : SegregacionDeFunciones.pares()) {
            assertThat(catalogo).as("par %s", par).contains(par.uno(), par.otro());
        }
    }

    @Test
    @DisplayName("ningun rol sembrado rompe la segregacion por si solo: si no, nadie podria recibirlo")
    void ningunRolSembradoLaRompe() throws IOException {
        Map<String, Set<String>> roles = permisosPorRol();

        assertThat(roles).containsKeys("TESORERIA", "OFICIAL_CUMPLIMIENTO", "SUPERVISOR_CUMPLIMIENTO");
        roles.forEach((rol, permisos) -> assertThat(SegregacionDeFunciones.conflicto(permisos))
                .as("rol %s", rol)
                .isEmpty());
    }

    @Test
    @DisplayName("P-3 · quien registra un bloqueo por autoridad (oficial) no lo levanta: lo levanta el supervisor")
    void elBloqueoLoLevantaOtroRol() throws IOException {
        Map<String, Set<String>> roles = permisosPorRol();

        assertThat(roles.get("OFICIAL_CUMPLIMIENTO"))
                .contains("BLOQUEO_AUTORIDAD_REGISTRAR")
                .doesNotContain("BLOQUEO_AUTORIDAD_LEVANTAR");
        assertThat(roles.get("SUPERVISOR_CUMPLIMIENTO"))
                .contains("BLOQUEO_AUTORIDAD_LEVANTAR")
                .doesNotContain("BLOQUEO_AUTORIDAD_REGISTRAR");
    }

    private static Set<String> permisosDelCatalogo() throws IOException {
        Set<String> codigos = new HashSet<>();
        for (JsonNode fila : bloque("permiso").get("filas")) {
            codigos.add(fila.get("codigo").asText());
        }
        return codigos;
    }

    private static Map<String, Set<String>> permisosPorRol() throws IOException {
        Map<String, Set<String>> roles = new TreeMap<>();
        for (JsonNode fila : bloque("rol").get("filas")) {
            roles.put(fila.get("codigo").asText(), new TreeSet<>());
        }
        for (JsonNode fila : bloque("rol_permiso").get("filas")) {
            roles.get(fila.get("rol_id").get("codigo").asText())
                    .add(fila.get("permiso_id").get("codigo").asText());
        }
        return roles;
    }

    private static JsonNode bloque(String tabla) throws IOException {
        JsonNode semilla = new ObjectMapper().readTree(Files.readString(raiz().resolve(SEMILLA)));
        for (JsonNode bloque : semilla.get("bloques")) {
            if (tabla.equals(bloque.path("tabla").asText())) {
                return bloque;
            }
        }
        throw new IllegalStateException("La semilla no tiene el bloque " + tabla);
    }

    /** Sube desde el directorio del modulo hasta el que tiene {@code seeders/}. */
    private static Path raiz() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isDirectory(p.resolve("seeders"))) {
            p = p.getParent();
        }
        if (p == null) {
            throw new IllegalStateException("No se encontro la raiz del repositorio (seeders/)");
        }
        return p;
    }
}
