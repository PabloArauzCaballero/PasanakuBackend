package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento.Requerimiento;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-45 · la cola de requerimientos de autoridad del backoffice (carril A · H9). */
class CU45ColaTest extends EscenarioDeColas {

    private UUID oficio(OffsetDateTime recibido, String estado) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO cumplimiento.requerimiento_autoridad
                    (id, autoridad, numero_oficio, fecha_recepcion, plazo_respuesta, alcance, documento_url,
                     hash_documento, estado)
                VALUES (?, 'ASFI', ?, ?::timestamptz, ?::timestamptz, 'alcance sintetico de prueba',
                        'https://oficios.bo/x', ?, ?)
                """,
                id,
                "OF-" + id.toString().substring(0, 8),
                recibido,
                recibido.plusDays(5),
                "b".repeat(64),
                estado);
        return borrarAlTerminar("requerimiento_autoridad", id);
    }

    @Test
    @DisplayName(
            "Dados oficios RECIBIDO y RESPONDIDO · Cuando el oficial lista la cola sin filtro · Entonces ve los dos, del recibido más reciente al más antiguo, sin el alcance ni el documento")
    void listaSinFiltro() {
        var base = OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        UUID recibido = oficio(base.plusHours(1), "RECIBIDO");
        UUID respondido = oficio(base, "RESPONDIDO");
        var ctx = operador();

        var filas = todas(
                (PaginaPedida<OffsetDateTime> p) ->
                        colas().requerimientos(List.of(), p.antesDe(), p.antesDeId(), p.limite(), ctx),
                2);

        assertThat(propias(filas, Requerimiento::requerimientoId, List.of(recibido, respondido)))
                .containsExactly(recibido, respondido);
        assertThat(filas)
                .filteredOn(r -> r.requerimientoId().equals(recibido))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.autoridad()).isEqualTo("ASFI");
                    assertThat(r.conBloqueoDeSaldo()).isFalse();
                    assertThat(r.plazoRespuesta()).isEqualTo(base.plusHours(1).plusDays(5));
                });
    }

    @Test
    @DisplayName("rechaza un estado desconocido o un cursor a medias con AP-CU45-05")
    void rechazaFiltroInvalido() {
        var ctx = operador();
        rechazaCon(
                "AP-CU45-05",
                () -> transaccion.execute(
                        t -> colas().requerimientos(List.of("PERDIDO"), Optional.empty(), Optional.empty(), 10, ctx)));
        rechazaCon(
                "AP-CU45-05",
                () -> transaccion.execute(t ->
                        colas().requerimientos(List.of(), Optional.empty(), Optional.of(UUID.randomUUID()), 10, ctx)));
    }
}
