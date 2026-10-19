package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento.Acta;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-94 · la cola de actas de comite del backoffice (carril A · H9). */
class CU94ColaTest extends EscenarioDeColas {

    @Test
    @DisplayName(
            "Dada un acta de comité registrada · Cuando el oficial lista las actas · Entonces la ve con su número, quórum y hash, sin asistentes ni decisiones")
    void listaActa() {
        UUID acta = gobiernoFixtura.actaMinima();
        var ctx = operador();

        var filas = todas((PaginaPedida<LocalDate> p) -> colas().actas(p.antesDe(), p.antesDeId(), p.limite(), ctx), 5);

        assertThat(filas)
                .filteredOn(a -> a.actaId().equals(acta))
                .singleElement()
                .satisfies(a -> {
                    assertThat(a.cumpleQuorum()).isTrue();
                    assertThat(a.hash()).hasSize(64);
                    assertThat(a.fecha()).isEqualTo(LocalDate.now());
                });
        assertThat(propias(filas, Acta::actaId, List.of(acta))).containsExactly(acta);
    }

    @Test
    @DisplayName("rechaza un cursor a medias en la cola de actas con AP-CU94-07")
    void rechazaCursorAMedias() {
        var ctx = operador();
        rechazaCon(
                "AP-CU94-07",
                () -> transaccion.execute(
                        t -> colas().actas(Optional.empty(), Optional.of(UUID.randomUUID()), 10, ctx)));
    }
}
