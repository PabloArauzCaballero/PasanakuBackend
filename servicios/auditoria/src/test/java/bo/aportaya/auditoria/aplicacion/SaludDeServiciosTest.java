package bo.aportaya.auditoria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.auditoria.dominio.SaludDeServicio;
import bo.aportaya.auditoria.infraestructura.SondaDeSalud;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** operacion/estado · la ultima consulta se reusa 10 s: abrir la pantalla seguido no multiplica las llamadas. */
class SaludDeServiciosTest {

    private Instant ahora = Instant.parse("2026-10-17T15:00:00Z");
    private final Reloj reloj = () -> ahora;

    @Test
    @DisplayName("salud · dentro de los 10 s devuelve la misma consulta; al vencer, pregunta de nuevo")
    void vigencia() {
        // Una sonda sin servicios que cuenta cuantas veces le preguntaron (sin red y sin mock).
        int[] preguntas = {0};
        SondaDeSalud sonda = new SondaDeSalud("", "http://%s", Duration.ofSeconds(1)) {
            @Override
            public List<SaludDeServicio> consultar() {
                preguntas[0]++;
                return List.of();
            }
        };
        SaludDeServicios salud = new SaludDeServicios(sonda, reloj);

        Instant primera = salud.consultar().consultadoEn();
        ahora = ahora.plusSeconds(9);
        assertThat(salud.consultar().consultadoEn()).isEqualTo(primera);
        assertThat(preguntas[0]).isEqualTo(1);

        ahora = ahora.plusSeconds(1);
        assertThat(salud.consultar().consultadoEn()).isEqualTo(ahora);
        assertThat(preguntas[0]).isEqualTo(2);
    }
}
