package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-69 · El organizador inicia el grupo al 75 % (2026-10-30). Sobre PostgreSQL real; datos sinteticos.
 *
 * <p>Con 10 cupos el minimo es ceil(0,75 × 10) = 8. Iniciar achica el grupo a sus cupos OCUPADO, deja el renglon en el
 * historial, arma el calendario y es idempotente por clave.
 */
class CU69IniciarGrupoTest extends BaseDeInicio {

    @Test
    @DisplayName(
            "Dado un grupo con 7 de 10 cupos ocupados · "
                    + "Cuando el organizador lo inicia · "
                    + "Entonces recibe AP-CU69-16 con los ocupados, el minimo requerido y los cupos totales, y el grupo no cambia")
    void sieteDeDiezNoAlcanza() {
        UUID grupo = grupoAbierto(10, del1Al(7), Set.of());

        assertThatThrownBy(() -> iniciar(grupo, UUID.randomUUID(), organizador))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> {
                    assertThat(codigo(e)).isEqualTo("AP-CU69-16");
                    assertThat(((ErrorDeNegocio) e).detalle())
                            .containsEntry("ocupados", 7)
                            .containsEntry("minimoRequerido", 8)
                            .containsEntry("cuposTotales", 10);
                });
        assertThat(grupo(grupo))
                .containsEntry("estado", "ABIERTO_A_INSCRIPCION")
                .containsEntry("cupos_totales", (short) 10);
        assertThat(contar("SELECT count(*) FROM grupos.historial_estado_grupo WHERE grupo_id = ?", grupo))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dado un grupo con 8 de 10 cupos ocupados · "
                    + "Cuando el organizador lo inicia · "
                    + "Entonces queda CONFORMADO con 8 cupos y 8 periodos, un renglon en el historial y los dos libres fuera del ciclo")
    void ochoDeDiezInicia() {
        UUID grupo = grupoAbierto(10, del1Al(8), Set.of());

        var r = iniciar(grupo, UUID.randomUUID(), organizador);

        assertThat(r.estado()).isEqualTo("CONFORMADO");
        assertThat(r.cuposTotales()).isEqualTo(8);
        assertThat(r.numPeriodos()).isEqualTo(8);
        assertThat(grupo(grupo))
                .containsEntry("estado", "CONFORMADO")
                .containsEntry("cupos_totales", (short) 8)
                .containsEntry("num_periodos", (short) 8)
                .containsEntry("cupos_ocupados", (short) 8);
        assertThat(contar(
                        """
                        SELECT count(*) FROM grupos.historial_estado_grupo
                         WHERE grupo_id = ? AND estado_anterior = 'ABIERTO_A_INSCRIPCION'
                           AND estado_nuevo = 'CONFORMADO' AND ejecutado_por = ?
                        """,
                        grupo,
                        organizador))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM grupos.periodo WHERE grupo_id = ?", grupo))
                .isEqualTo(8);
        // Los dos LIBRE quedaron fuera del ciclo, por encima de cupos_totales: el sorteo ya puede correr.
        assertThat(contar(
                        "SELECT count(*) FROM grupos.cupo WHERE grupo_id = ? AND estado = 'LIBRE' AND numero > 8",
                        grupo))
                .isEqualTo(2);
        assertThat(sorteos.estaConformado(dsl, grupo)).isTrue();
    }

    @Test
    @DisplayName("Dado un grupo de 10 cupos con el 3 y el 10 libres · "
            + "Cuando el organizador lo inicia · "
            + "Entonces los ocupados quedan numerados del 1 al 8 en su orden y el organizador sigue en el cupo 1")
    void renumeraSinHuecos() {
        // Libres intercalados: el 3 y el 10.
        UUID grupo = grupoAbierto(10, Set.of(1, 2, 4, 5, 6, 7, 8, 9), Set.of());
        List<UUID> antes = participantesEnOrden(grupo);

        iniciar(grupo, UUID.randomUUID(), organizador);

        assertThat(dsl.fetch(
                                "SELECT numero FROM grupos.cupo WHERE grupo_id = ? AND estado = 'OCUPADO' ORDER BY numero",
                                grupo)
                        .getValues(0, Integer.class))
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(participantesEnOrden(grupo)).isEqualTo(antes);
        assertThat(dsl.fetchValue(
                        """
                        SELECT p.usuario_id FROM grupos.cupo c JOIN grupos.participante p ON p.id = c.participante_id
                         WHERE c.grupo_id = ? AND c.numero = 1
                        """,
                        grupo))
                .isEqualTo(organizador);
    }

    private List<UUID> participantesEnOrden(UUID grupo) {
        return dsl.fetch(
                        "SELECT participante_id FROM grupos.cupo WHERE grupo_id = ? AND estado = 'OCUPADO' ORDER BY numero",
                        grupo)
                .getValues(0, UUID.class);
    }

    @Test
    @DisplayName(
            "Dado un grupo iniciado con una clave · "
                    + "Cuando el organizador repite el inicio con la misma clave y despues con otra · "
                    + "Entonces la misma clave devuelve lo mismo sin repetir historial ni periodos y la otra responde AP-CU69-17")
    void idempotente() {
        UUID grupo = grupoAbierto(10, del1Al(9), Set.of());
        UUID clave = UUID.randomUUID();

        var primera = iniciar(grupo, clave, organizador);
        var repetida = iniciar(grupo, clave, organizador);

        assertThat(repetida).isEqualTo(primera);
        assertThat(contar("SELECT count(*) FROM grupos.historial_estado_grupo WHERE grupo_id = ?", grupo))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM grupos.periodo WHERE grupo_id = ?", grupo))
                .isEqualTo(9);
        assertThatThrownBy(() -> iniciar(grupo, UUID.randomUUID(), organizador))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU69-17"));
    }

    @Test
    @DisplayName("Dado un grupo abierto con 8 de 10 cupos ocupados · "
            + "Cuando lo intenta iniciar un integrante que no organiza o el organizador de otro grupo, o se inicia un grupo que no existe · "
            + "Entonces cada intento recibe AP-CU69-07 y el grupo sigue ABIERTO_A_INSCRIPCION")
    void soloElOrganizadorDeEseGrupo() {
        UUID grupo = grupoAbierto(10, del1Al(8), Set.of());
        UUID integrante = (UUID) dsl.fetchValue(
                """
                SELECT p.usuario_id FROM grupos.participante p
                 WHERE p.grupo_id = ? AND NOT p.es_organizador LIMIT 1
                """,
                grupo);
        // Quien organiza OTRO grupo (y tiene GRUPO_ADMINISTRAR) no inicia este.
        UUID otroGrupo = grupoAbierto(4, del1Al(3), Set.of());
        UUID organizadorDelOtro = fixtura.usuario();
        dsl.execute(
                "UPDATE grupos.participante SET usuario_id = ? WHERE grupo_id = ? AND es_organizador",
                organizadorDelOtro,
                otroGrupo);

        for (UUID quien : List.of(integrante, organizadorDelOtro)) {
            assertThatThrownBy(() -> iniciar(grupo, UUID.randomUUID(), quien))
                    .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU69-07"));
        }
        assertThatThrownBy(() -> iniciar(UUID.randomUUID(), UUID.randomUUID(), organizador))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU69-07"));
        assertThat(grupo(grupo)).containsEntry("estado", "ABIERTO_A_INSCRIPCION");
    }

    @Test
    @DisplayName("Dado un grupo con alguien aceptado sin firmar y otro grupo en BORRADOR · "
            + "Cuando el organizador intenta iniciarlos · "
            + "Entonces el primero recibe AP-CU69-19 con cuantos reservados hay y el BORRADOR recibe AP-CU69-12")
    void reservadosYBorrador() {
        UUID conReservado = grupoAbierto(10, del1Al(8), Set.of(9));
        assertThatThrownBy(() -> iniciar(conReservado, UUID.randomUUID(), organizador))
                .satisfies(e -> {
                    assertThat(codigo(e)).isEqualTo("AP-CU69-19");
                    assertThat(((ErrorDeNegocio) e).detalle()).containsEntry("reservados", 1);
                });

        UUID borrador = grupoAbierto(4, del1Al(4), Set.of());
        dsl.execute("UPDATE grupos.grupo SET estado = 'BORRADOR' WHERE id = ?", borrador);
        assertThatThrownBy(() -> iniciar(borrador, UUID.randomUUID(), organizador))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU69-12"));
    }
}
