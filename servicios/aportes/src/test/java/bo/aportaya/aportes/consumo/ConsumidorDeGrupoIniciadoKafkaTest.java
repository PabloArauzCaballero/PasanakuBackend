package bo.aportaya.aportes.consumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El sobre de {@code grupos.grupo_iniciado} en los tres niveles: completo, al limite (sin cupos) e ilegible. */
class ConsumidorDeGrupoIniciadoKafkaTest {

    private static final UUID EVENTO = UUID.randomUUID();
    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID CUPO = UUID.randomUUID();
    private static final UUID PARTICIPANTE = UUID.randomUUID();

    private static final String CUERPO =
            """
            {"grupoId":"%s","moneda":"BOB","cuposTotales":1,"iniciadoEn":"2026-10-30T15:00:00Z",
             "periodo":{"periodoId":"%s","numero":1,"fechaInicio":"2026-11-01","fechaLimitePago":"2026-11-30",
                        "fechaFinGracia":"2026-12-03","fechaEntregaPrevista":"2026-12-04"},
             "cupos":[{"cupoId":"%s","numero":1,"participanteId":"%s","montoAporte":"500.00"}]}
            """
                    .formatted(GRUPO, PERIODO, CUPO, PARTICIPANTE);

    private static ConsumerRecord<String, String> registro(String cuerpo, boolean conCabeceras) {
        var r = new ConsumerRecord<>("aportaya.grupos.grupo_iniciado", 0, 0L, GRUPO.toString(), cuerpo);
        if (conCabeceras) {
            r.headers().add("event_id", EVENTO.toString().getBytes(StandardCharsets.UTF_8));
            r.headers().add("correlation_id", CORRELACION.toString().getBytes(StandardCharsets.UTF_8));
        }
        return r;
    }

    @Test
    @DisplayName("Un sobre completo se lee entero: cabeceras, periodo 1, fechas y el aporte de cada cupo con su moneda")
    void completo() {
        var inicio = ConsumidorDeGrupoIniciadoKafka.leer(registro(CUERPO, true));

        assertThat(inicio.eventoId()).isEqualTo(EVENTO);
        assertThat(inicio.correlationId()).isEqualTo(CORRELACION);
        assertThat(inicio.grupoId()).isEqualTo(GRUPO);
        assertThat(inicio.periodoId()).isEqualTo(PERIODO);
        assertThat(inicio.numeroDePeriodo()).isEqualTo(1);
        assertThat(inicio.fechaLimitePago()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(inicio.fechaFinGracia()).isEqualTo(LocalDate.of(2026, 12, 3));
        assertThat(inicio.cupos()).hasSize(1);
        assertThat(inicio.cupos().getFirst().cupoId()).isEqualTo(CUPO);
        assertThat(inicio.cupos().getFirst().participanteId()).isEqualTo(PARTICIPANTE);
        assertThat(inicio.cupos().getFirst().montoAporte().monto()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(inicio.cupos().getFirst().montoAporte().moneda()).isEqualTo(Moneda.BOB);
    }

    @Test
    @DisplayName("Sin cabeceras de sobre se rechaza: un evento sin event_id no se puede deduplicar")
    void sinCabeceras() {
        assertThatThrownBy(() -> ConsumidorDeGrupoIniciadoKafka.leer(registro(CUERPO, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("event_id");
    }

    @Test
    @DisplayName(
            "Un cuerpo sin periodo, con una fecha que no es ISO o con un importe que no es cadena de dos decimales se rechaza")
    void ilegible() {
        assertThatThrownBy(() -> ConsumidorDeGrupoIniciadoKafka.leer(
                        registro("{\"grupoId\":\"" + GRUPO + "\",\"moneda\":\"BOB\",\"cupos\":[]}", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periodo");
        assertThatThrownBy(() ->
                        ConsumidorDeGrupoIniciadoKafka.leer(registro(CUERPO.replace("2026-11-30", "30/11/2026"), true)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeGrupoIniciadoKafka.leer(registro("no es json", true)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
