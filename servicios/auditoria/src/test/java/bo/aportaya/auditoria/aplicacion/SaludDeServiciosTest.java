package bo.aportaya.auditoria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.auditoria.dominio.DisponibilidadDelServicio;
import bo.aportaya.auditoria.dominio.SaludDeServicio;
import bo.aportaya.auditoria.infraestructura.SondaDeSalud;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        SaludDeServicios salud = new SaludDeServicios(sonda, sinMedicion(), reloj, new BigDecimal("99.5"));

        Instant primera = salud.consultar().consultadoEn();
        ahora = ahora.plusSeconds(9);
        assertThat(salud.consultar().consultadoEn()).isEqualTo(primera);
        assertThat(preguntas[0]).isEqualTo(1);

        ahora = ahora.plusSeconds(1);
        assertThat(salud.consultar().consultadoEn()).isEqualTo(ahora);
        assertThat(preguntas[0]).isEqualTo(2);
    }

    /** Un registro sin base: lo medido lo devuelve fijo (sin red, sin base y sin mock). */
    private static RegistroDeDisponibilidad medido(Map<String, DisponibilidadDelServicio> fijo) {
        return new RegistroDeDisponibilidad(null, null) {
            @Override
            public Map<String, DisponibilidadDelServicio> enLaVentana(Instant ahora) {
                return fijo;
            }
        };
    }

    private static RegistroDeDisponibilidad sinMedicion() {
        return medido(Map.of());
    }

    @Test
    @DisplayName("sistemas/servicios · la salida lleva lo medido en la ventana y el objetivo configurado")
    void llevaLoMedido() {
        SondaDeSalud sonda = new SondaDeSalud("", "http://%s", Duration.ofSeconds(1)) {
            @Override
            public List<SaludDeServicio> consultar() {
                return List.of(new SaludDeServicio("identidad", SaludDeServicio.Estado.ARRIBA, Optional.of(5L)));
            }
        };
        var identidad = new DisponibilidadDelServicio("identidad", 100, 99, Optional.of(ahora));
        var salida = new SaludDeServicios(sonda, medido(Map.of("identidad", identidad)), reloj, new BigDecimal("99.9"))
                .consultar();

        assertThat(salida.disponibilidad()).containsEntry("identidad", identidad);
        assertThat(salida.objetivoSlo()).isEqualByComparingTo("99.9");
    }

    @Test
    @DisplayName("configuracion invalida · un SLO de 100 % o de 0 no arranca")
    void objetivoInvalido() {
        SondaDeSalud sonda = new SondaDeSalud("", "http://%s", Duration.ofSeconds(1));
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new SaludDeServicios(sonda, sinMedicion(), reloj, new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new SaludDeServicios(sonda, sinMedicion(), reloj, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
