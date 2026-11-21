package bo.aportaya.grupos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F1.L3.M06 · los estados del dominio son exactamente los del {@code CHECK} que genera el modelo.
 *
 * <p>Se lee el DDL generado ({@code sql/10_tablas}), que sale del {@code .puml}: si alguien agrega o renombra un
 * estado en el modelo y no en el enum —o al revés—, esta prueba falla antes de que una consulta cuente mal.
 */
class EstadosContraElModeloTest {

    private static Set<String> estadosDelCheck(String tabla, String restriccion) throws IOException {
        Path raiz = Path.of("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("sql/10_tablas"))) {
            raiz = raiz.getParent();
        }
        assertThat(raiz).as("raiz del repositorio con sql/10_tablas").isNotNull();
        String ddl = Files.readString(raiz.resolve("sql/10_tablas/02_grupos_turnos/" + tabla + ".sql"));
        var m = Pattern.compile(restriccion + " CHECK \\(estado IN \\(([^)]*)\\)\\)")
                .matcher(ddl);
        assertThat(m.find()).as(restriccion + " en " + tabla + ".sql").isTrue();
        return Arrays.stream(m.group(1).split(","))
                .map(v -> v.trim().replace("'", ""))
                .collect(Collectors.toSet());
    }

    private static Set<String> nombres(Enum<?>[] valores) {
        return Arrays.stream(valores).map(Enum::name).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("EstadoDeGrupo coincide con ck_grupo_estado, y sus terminales existen en la base")
    void estadoDeGrupoEsElDelModelo() throws IOException {
        var delModelo = estadosDelCheck("grupo", "ck_grupo_estado");
        assertThat(nombres(EstadoDeGrupo.values())).isEqualTo(delModelo);
        assertThat(EstadoDeGrupo.TERMINALES)
                .extracting(Enum::name)
                .containsExactlyInAnyOrder("CANCELADO", "DISUELTO_ANTICIPADAMENTE", "FINALIZADO");
    }

    @Test
    @DisplayName("EstadoDeParticipante coincide con ck_participante_estado, y EN_MORA sigue vinculado")
    void estadoDeParticipanteEsElDelModelo() throws IOException {
        var delModelo = estadosDelCheck("participante", "ck_participante_estado");
        assertThat(nombres(EstadoDeParticipante.values())).isEqualTo(delModelo);
        assertThat(EstadoDeParticipante.VINCULADOS)
                .contains(EstadoDeParticipante.EN_MORA, EstadoDeParticipante.ACTIVO)
                .doesNotContain(EstadoDeParticipante.RETIRADO, EstadoDeParticipante.REEMPLAZADO);
    }
}
