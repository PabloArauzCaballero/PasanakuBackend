package bo.aportaya.garantia.infraestructura;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Los parametros del circuit breaker de los clientes hacia otros servicios. SINTETICOS: ninguno sale de
 * medir trafico real; los fija el dueno del servicio y se leen de {@code application.yml}
 * ({@code aportaya.resiliencia.circuito.*}), no del codigo (invariante 10).
 *
 * @param ventana llamadas que se miran (y minimo para juzgar)
 * @param umbralDeFallos porcentaje de fallos que abre el circuito
 * @param esperaAbierto cuanto se deja de llamar antes de probar de nuevo
 * @param llamadasDePrueba llamadas permitidas en semiabierto para confirmar la recuperacion
 */
@Component
public record ConfiguracionDeCircuito(
        @Value("${aportaya.resiliencia.circuito.ventana:10}") int ventana,
        @Value("${aportaya.resiliencia.circuito.umbral-de-fallos:50}") int umbralDeFallos,
        @Value("${aportaya.resiliencia.circuito.espera-abierto:PT30S}") Duration esperaAbierto,
        @Value("${aportaya.resiliencia.circuito.llamadas-de-prueba:3}") int llamadasDePrueba) {

    /** Los valores del plan de resiliencia (evidencia/carril-S2/DEUDA-circuit-breaker-clientes-http.md). */
    public static ConfiguracionDeCircuito porOmision() {
        return new ConfiguracionDeCircuito(10, 50, Duration.ofSeconds(30), 3);
    }
}
