package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.grupos.aplicacion.CU60Sortear.Compromiso;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F3.L2.M01 · Productor de {@code grupos.turnos_asignados} (pacto de {@code entregas}, PR #490): al revelar el
 * sorteo, en la misma transacción, un evento con todos los turnos, su periodo, su cupo y el titular del cupo. Sin
 * revelación válida no sale nada. Datos sintéticos.
 */
class TurnosAsignadosRepositorioTest extends BaseDeCU60 {

    private static final String TIPO = "grupos.turnos_asignados";

    private int eventos(UUID grupo) {
        return dsl.fetchOne(
                        "SELECT count(*)::int FROM grupos.evento_dominio WHERE agregado_id = ? AND tipo = ?",
                        grupo,
                        TIPO)
                .get(0, Integer.class);
    }

    private Compromiso comprometido(UUID grupo) {
        return transaccion.execute(
                e -> sortear.comprometer(grupo, List.of("entropia-sintetica"), Optional.empty(), contexto()));
    }

    @Test
    @DisplayName(
            "Correcto · al revelar sale un evento con los 6 turnos, cada uno con su periodo, cupo y titular, versión 1")
    void alRevelar() {
        UUID grupo = fixtura.grupoConformado(6);
        fixtura.cuposOcupados(grupo, 6);
        List<UUID> periodos = fixtura.periodos(grupo, 6, new BigDecimal("3000.00"));
        Compromiso c = comprometido(grupo);

        transaccion.execute(e -> sortear.revelar(
                c.sorteoId(),
                c.semilla(),
                List.of("entropia-sintetica"),
                periodos,
                new BigDecimal("500.00"),
                Optional.empty(),
                contexto()));

        assertThat(eventos(grupo)).isEqualTo(1);
        // Cada turno del evento coincide con la fila de grupos.turno y con el titular de su cupo.
        int coincidentes = dsl.fetchOne(
                        """
                        SELECT count(*)::int
                          FROM grupos.evento_dominio e, jsonb_array_elements(e.payload -> 'turnos') t
                          JOIN grupos.turno tu ON tu.id = (t ->> 'turnoId')::uuid
                          JOIN grupos.cupo cu ON cu.id = tu.cupo_id
                         WHERE e.agregado_id = ? AND e.tipo = ?
                           AND (t ->> 'periodoId')::uuid = tu.periodo_id
                           AND (t ->> 'cupoId')::uuid = tu.cupo_id
                           AND (t ->> 'titularParticipanteId')::uuid = cu.participante_id
                           AND (t ->> 'numero')::int = tu.orden_asignado
                           AND (t ->> 'version')::int = 1
                        """,
                        grupo,
                        TIPO)
                .get(0, Integer.class);
        assertThat(coincidentes).isEqualTo(6);
    }

    @Test
    @DisplayName("Límite · comprometer no publica turnos: todavía no existen")
    void alComprometer() {
        UUID grupo = fixtura.grupoConformado(6);
        fixtura.cuposOcupados(grupo, 6);
        comprometido(grupo);

        assertThat(eventos(grupo)).isZero();
    }

    @Test
    @DisplayName("Inválido · una semilla que no verifica no crea turnos ni publica el evento")
    void semillaQueNoVerifica() {
        UUID grupo = fixtura.grupoConformado(6);
        fixtura.cuposOcupados(grupo, 6);
        List<UUID> periodos = fixtura.periodos(grupo, 6, new BigDecimal("3000.00"));
        Compromiso c = comprometido(grupo);

        transaccion.execute(e -> sortear.revelar(
                c.sorteoId(),
                "otra-semilla",
                List.of("entropia-sintetica"),
                periodos,
                new BigDecimal("500.00"),
                Optional.empty(),
                contexto()));

        assertThat(eventos(grupo)).isZero();
    }
}
