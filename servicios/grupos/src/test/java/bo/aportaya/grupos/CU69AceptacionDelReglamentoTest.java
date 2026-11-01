package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-69 · Reglamento al achicar (decision del dueño 2026-10-30): cada integrante ACTIVO acepta la version nueva por la
 * firma del reglamento, una sola vez, y el sorteo espera a que la acepten todos. Partida de
 * {@link CU69ReglamentoAlIniciarTest} (limite de 300 lineas). Sobre PostgreSQL real con el rol de base del servicio
 * ({@code SET LOCAL ROLE svc_grupos}); datos sinteticos.
 */
class CU69AceptacionDelReglamentoTest extends BaseDeReglamentoAlIniciar {

    @Test
    @DisplayName("Dado un grupo recien iniciado con la version 2 del reglamento vigente · "
            + "Cuando un integrante la acepta dos veces · "
            + "Entonces queda UNA aceptacion, el cupo no cambia y el detalle ya no se la pide")
    void aceptarDosVecesEsIdempotente() {
        UUID grupo = grupoConVersion1();
        iniciarConElRolDelServicio(grupo, UUID.randomUUID());
        UUID juan = usuarioDelCupo(grupo, 2);
        String hash = hashVigente(grupo);
        var cupoAntes = dsl.fetchOne(
                        "SELECT id, estado, numero FROM grupos.cupo WHERE grupo_id = ? AND numero = 2", grupo)
                .intoMap();

        var primera = aceptarConElRolDelServicio(grupo, hash, juan);
        var segunda = aceptarConElRolDelServicio(grupo, hash, juan);

        assertThat(segunda.participanteId()).isEqualTo(primera.participanteId());
        assertThat(primera.grupoConformado()).isFalse();
        assertThat(primera.estadoDelGrupo()).isEqualTo("CONFORMADO");
        assertThat(contar(
                        """
                        SELECT count(*) FROM grupos.aceptacion_reglamento a
                          JOIN grupos.reglamento_grupo r ON r.id = a.reglamento_id
                         WHERE r.grupo_id = ? AND r.version = 2 AND a.participante_id = ?
                        """,
                        grupo,
                        primera.participanteId()))
                .isEqualTo(1);
        assertThat(dsl.fetchOne("SELECT id, estado, numero FROM grupos.cupo WHERE grupo_id = ? AND numero = 2", grupo)
                        .intoMap())
                .isEqualTo(cupoAntes);
        assertThat(detalleDe(grupo, juan).reglamentoPendienteDeAceptar()).isFalse();
    }

    @Test
    @DisplayName("Dado un grupo recien iniciado con la version 2 del reglamento vigente · "
            + "Cuando un integrante acepta con el hash de la version 1 · "
            + "Entonces recibe AP-CU69-05 y no queda ninguna aceptacion de la version 2")
    void hashQueNoEsElVigente() {
        UUID grupo = grupoConVersion1();
        iniciarConElRolDelServicio(grupo, UUID.randomUUID());
        UUID juan = usuarioDelCupo(grupo, 2);

        assertThatThrownBy(() -> aceptarConElRolDelServicio(grupo, sha256(TEXTO_V1), juan))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU69-05"));
        assertThat(aceptacionesDeLaVersion(grupo, 2)).isZero();
    }

    @Test
    @DisplayName("Dado un grupo recien iniciado con 8 integrantes y la version 2 del reglamento vigente · "
            + "Cuando la aceptan 7 y despues el octavo · "
            + "Entonces con 7 el organizador todavia no puede sortear y con los 8 si")
    void conTodosAceptadosSePuedeSortear() {
        UUID grupo = grupoConVersion1();
        iniciarConElRolDelServicio(grupo, UUID.randomUUID());
        String hash = hashVigente(grupo);

        for (int numero = 1; numero <= 7; numero++) {
            aceptarConElRolDelServicio(grupo, hash, usuarioDelCupo(grupo, numero));
        }
        assertThat(detalleDe(grupo, organizador).puedeSortear()).as("falta uno").isFalse();
        aceptarConElRolDelServicio(grupo, hash, usuarioDelCupo(grupo, 8));

        assertThat(aceptacionesDeLaVersion(grupo, 2)).isEqualTo(8);
        assertThat(detalleDe(grupo, organizador).puedeSortear()).isTrue();
    }

    @Test
    @DisplayName("Dado un grupo recien iniciado donde todos tienen la version 1 y solo 7 de 8 aceptaron la version 2 · "
            + "Cuando el organizador compromete el sorteo · "
            + "Entonces el servidor responde AP-CU60-05 y no hay sorteo · "
            + "Y con los 8 aceptados el sorteo se compromete")
    void elServidorExigeLaVersionVigenteParaSortear() {
        UUID grupo = grupoConVersion1();
        iniciarConElRolDelServicio(grupo, UUID.randomUUID());
        String hash = hashVigente(grupo);
        for (int numero = 1; numero <= 7; numero++) {
            aceptarConElRolDelServicio(grupo, hash, usuarioDelCupo(grupo, numero));
        }

        assertThatThrownBy(() -> comprometerConElRolDelServicio(grupo))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU60-05"));
        assertThat(contar("SELECT count(*) FROM grupos.sorteo_turnos WHERE grupo_id = ?", grupo))
                .isZero();

        aceptarConElRolDelServicio(grupo, hash, usuarioDelCupo(grupo, 8));
        comprometerConElRolDelServicio(grupo);
        assertThat(contar("SELECT count(*) FROM grupos.sorteo_turnos WHERE grupo_id = ?", grupo))
                .isEqualTo(1);
    }
}
