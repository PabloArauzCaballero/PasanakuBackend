package bo.aportaya.plataforma.web.observabilidad;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Tableros (D4) · la observabilidad igual en los quince procesos, en UN lugar y con la precedencia MAS BAJA: un
 * servicio o el entorno la pisan si hace falta, pero nadie la tiene que copiar en su application.yml.
 *
 * <ul>
 *   <li>Trazas por OpenTelemetry (bridge de Micrometer + exportador OTLP) hacia el Collector. APAGADAS por omision:
 *       las enciende el entorno ({@code TRAZAS_HABILITADAS=true}), asi un servicio sin Collector no queda reintentando
 *       contra la nada. Muestreo 10 % por omision ({@code TRAZAS_MUESTREO}).
 *   <li>Histograma de {@code http.server.requests}: sin cubetas, Prometheus no puede calcular p95 ni p99.
 * </ul>
 *
 * Nombres de propiedad verificados en el apendice de Spring Boot 3.5 (application-properties): no se inventa ninguno.
 */
public class DefaultsDeObservabilidad implements EnvironmentPostProcessor {

    static final String FUENTE = "aportaya-observabilidad-por-omision";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment entorno, SpringApplication aplicacion) {
        entorno.getPropertySources()
                .addLast(new MapPropertySource(
                        FUENTE,
                        Map.of(
                                "management.otlp.tracing.export.enabled", "${TRAZAS_HABILITADAS:false}",
                                "management.otlp.tracing.endpoint",
                                        "${OTEL_TRAZAS_URL:http://otel-collector:4318/v1/traces}",
                                "management.tracing.sampling.probability", "${TRAZAS_MUESTREO:0.1}",
                                "management.metrics.distribution.percentiles-histogram.http.server.requests", "true")));
    }
}
