package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento.EventoDeRiesgo;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-54 · la cola de eventos de riesgo operativo del backoffice (carril A · H9). */
class CU54ColaTest extends EscenarioDeColas {

    private UUID evento(OffsetDateTime detectado, String estado, String bruta, String recuperada) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO cumplimiento.evento_riesgo_operativo
                    (id, codigo, registrado_por, categoria_evento, factor_riesgo, linea_negocio, descripcion,
                     fecha_ocurrencia, fecha_deteccion, perdida_bruta, recuperacion, moneda, estado)
                VALUES (?, ?, ?, 'FALLAS_SISTEMAS', 'TECNOLOGIA_INFORMACION', 'PAGOS', 'descripcion sintetica',
                        ?::timestamptz, ?::timestamptz, ?, ?, 'BOB', ?)
                """,
                id,
                "ERO-" + id.toString().substring(0, 8),
                fixtura.usuario(),
                detectado.minusHours(2),
                detectado,
                new java.math.BigDecimal(bruta),
                new java.math.BigDecimal(recuperada),
                estado);
        return id;
    }

    @Test
    @DisplayName(
            "Dados un evento REGISTRADO y uno CERRADO · Cuando Riesgos lista los eventos filtrando por REGISTRADO · Entonces ve solo el registrado con su pérdida bruta, recuperación y pérdida neta en su moneda")
    void filtraConPerdidas() {
        var base = OffsetDateTime.of(2026, 6, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        UUID abierto = evento(base, "REGISTRADO", "1000.00", "250.00");
        UUID cerrado = evento(base.plusHours(1), "CERRADO", "10.00", "0.00");
        var ctx = operador();

        var filas = todas(
                (PaginaPedida<OffsetDateTime> p) ->
                        colas().eventos(List.of("REGISTRADO"), p.antesDe(), p.antesDeId(), p.limite(), ctx),
                3);

        assertThat(propias(filas, EventoDeRiesgo::eventoId, List.of(abierto, cerrado)))
                .containsExactly(abierto);
        assertThat(filas)
                .filteredOn(e -> e.eventoId().equals(abierto))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.perdidaBruta()).isEqualByComparingTo("1000.00");
                    assertThat(e.recuperacion()).isEqualByComparingTo("250.00");
                    assertThat(e.perdidaNeta()).isEqualByComparingTo("750.00");
                    assertThat(e.moneda()).isEqualTo("BOB");
                });
    }

    @Test
    @DisplayName("rechaza un estado desconocido con AP-CU54-04")
    void rechazaFiltroInvalido() {
        var ctx = operador();
        rechazaCon(
                "AP-CU54-04",
                () -> transaccion.execute(
                        t -> colas().eventos(List.of("OLVIDADO"), Optional.empty(), Optional.empty(), 10, ctx)));
    }
}
