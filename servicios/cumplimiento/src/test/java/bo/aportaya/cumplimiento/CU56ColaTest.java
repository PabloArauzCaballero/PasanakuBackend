package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento.Prueba;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-56 · la cola de pruebas de continuidad del backoffice (carril A · H9). */
class CU56ColaTest extends EscenarioDeColas {

    private UUID prueba(UUID plan, LocalDate fecha, String resultado) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO cumplimiento.prueba_continuidad
                    (id, plan_continuidad_id, ejecutada_por, tipo, fecha, rto_obtenido_minutos,
                     rpo_obtenido_minutos, resultado)
                VALUES (?, ?, ?, 'PARCIAL', ?, 95, 10, ?)
                """,
                id,
                plan,
                fixtura.usuario(),
                fecha,
                resultado);
        return borrarAlTerminar("prueba_continuidad", id);
    }

    @Test
    @DisplayName(
            "Dadas dos pruebas de continuidad en fechas distintas · Cuando Riesgos lista las pruebas · Entonces las ve de la más reciente a la más antigua con su RTO, RPO y resultado")
    void listaPorFecha() {
        UUID plan = gobiernoFixtura.planDeContinuidad(
                "COLA-" + UUID.randomUUID(), 120, 30, 6, LocalDate.now().plusMonths(6));
        UUID vieja = prueba(plan, LocalDate.of(2026, 1, 15), "FALLIDA");
        UUID nueva = prueba(plan, LocalDate.of(2026, 4, 15), "PARCIAL");
        var ctx = operador();

        var filas =
                todas((PaginaPedida<LocalDate> p) -> colas().pruebas(p.antesDe(), p.antesDeId(), p.limite(), ctx), 2);

        assertThat(propias(filas, Prueba::pruebaId, List.of(vieja, nueva))).containsExactly(nueva, vieja);
        assertThat(filas)
                .filteredOn(p -> p.pruebaId().equals(nueva))
                .singleElement()
                .satisfies(p -> {
                    assertThat(p.rtoMinutos()).isEqualTo(95);
                    assertThat(p.rpoMinutos()).isEqualTo(10);
                    assertThat(p.actaComiteId()).isEmpty();
                });
    }

    @Test
    @DisplayName("rechaza un cursor a medias en la cola de pruebas con AP-CU56-04")
    void rechazaCursorAMedias() {
        var ctx = operador();
        rechazaCon(
                "AP-CU56-04",
                () -> transaccion.execute(
                        t -> colas().pruebas(Optional.of(LocalDate.now()), Optional.empty(), 10, ctx)));
    }
}
