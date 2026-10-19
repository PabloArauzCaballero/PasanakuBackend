package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento.Incidente;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-55 · la cola de incidentes de seguridad del backoffice (carril A · H9). */
class CU55ColaTest extends EscenarioDeColas {

    private UUID incidente(OffsetDateTime detectado, String estado) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO cumplimiento.incidente_seguridad
                    (id, codigo, tipo, severidad, datos_personales_afectados, usuarios_afectados, detectado_en,
                     plazo_reporte, estado)
                VALUES (?, ?, 'PHISHING', 'ALTA', true, 12, ?::timestamptz, ?::timestamptz, ?)
                """,
                id,
                "INC-" + id.toString().substring(0, 8),
                detectado,
                detectado.plusHours(24),
                estado);
        return borrarAlTerminar("incidente_seguridad", id);
    }

    @Test
    @DisplayName(
            "Dados dos incidentes DETECTADO · Cuando Seguridad recorre la cola de a uno · Entonces ve cada uno una sola vez, del más reciente al más antiguo, con su plazo de reporte")
    void recorreConCursor() {
        var base = OffsetDateTime.of(2026, 7, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        UUID primero = incidente(base, "DETECTADO");
        UUID segundo = incidente(base.plusMinutes(30), "DETECTADO");
        var ctx = operador();

        var filas = todas(
                (PaginaPedida<OffsetDateTime> p) ->
                        colas().incidentes(List.of("DETECTADO"), p.antesDe(), p.antesDeId(), p.limite(), ctx),
                1);

        assertThat(propias(filas, Incidente::incidenteId, List.of(primero, segundo)))
                .containsExactly(segundo, primero);
        assertThat(filas)
                .filteredOn(i -> i.incidenteId().equals(segundo))
                .singleElement()
                .satisfies(i -> {
                    assertThat(i.plazoReporte()).isEqualTo(base.plusMinutes(30).plusHours(24));
                    assertThat(i.datosPersonalesAfectados()).isTrue();
                    assertThat(i.reportadoAlOrganismoEn()).isEmpty();
                });
    }

    @Test
    @DisplayName("rechaza un cursor a medias en la cola de incidentes con AP-CU55-04")
    void rechazaCursorAMedias() {
        var ctx = operador();
        rechazaCon(
                "AP-CU55-04",
                () -> transaccion.execute(t ->
                        colas().incidentes(List.of(), Optional.of(OffsetDateTime.now()), Optional.empty(), 10, ctx)));
    }
}
