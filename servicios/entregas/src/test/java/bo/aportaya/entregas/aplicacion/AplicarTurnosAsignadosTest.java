package bo.aportaya.entregas.aplicacion;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.entregas.aplicacion.AplicarTurnosAsignados.TurnosAsignados;
import bo.aportaya.entregas.infraestructura.TurnoDelGrupoRepositorio.Turno;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F3.L2.M01 · lo que la copia de turnos rechaza ANTES de tocar la base. La aplicacion por version, el duplicado y la
 * persistencia estan en {@code CU133EntregaAutomaticaTest} (integracion, CI).
 */
class AplicarTurnosAsignadosTest {

    private final AplicarTurnosAsignados aplicar = new AplicarTurnosAsignados(null, null, null);

    private static TurnosAsignados evento(UUID grupoDelEvento, UUID grupoDelTurno, int version) {
        return new TurnosAsignados(
                UUID.randomUUID(),
                UUID.randomUUID(),
                grupoDelEvento,
                List.of(new Turno(
                        UUID.randomUUID(),
                        grupoDelTurno,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        version)));
    }

    @Test
    @DisplayName("invalido: un turno de otro grupo rechaza el evento entero")
    void turnoDeOtroGrupo() {
        assertThatThrownBy(() -> aplicar.procesar(evento(UUID.randomUUID(), UUID.randomUUID(), 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("grupo");
    }

    @Test
    @DisplayName("limite: la version 0 no es del pacto (empieza en 1) y se rechaza")
    void versionCero() {
        UUID grupo = UUID.randomUUID();
        assertThatThrownBy(() -> aplicar.procesar(evento(grupo, grupo, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version");
    }
}
