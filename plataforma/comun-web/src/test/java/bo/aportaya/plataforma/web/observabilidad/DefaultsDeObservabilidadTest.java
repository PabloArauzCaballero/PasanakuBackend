package bo.aportaya.plataforma.web.observabilidad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class DefaultsDeObservabilidadTest {

    private static StandardEnvironment entorno(Map<String, Object> propias) {
        var e = new StandardEnvironment();
        e.getPropertySources().addFirst(new MapPropertySource("propias", propias));
        new DefaultsDeObservabilidad().postProcessEnvironment(e, new SpringApplication());
        return e;
    }

    @Test
    @DisplayName(
            "correcto: sin configurar nada, trazas apagadas, muestreo 0.1, Collector por nombre e histograma encendido")
    void porOmision() {
        var e = entorno(Map.of());

        assertThat(e.getProperty("management.otlp.tracing.export.enabled")).isEqualTo("false");
        assertThat(e.getProperty("management.tracing.sampling.probability")).isEqualTo("0.1");
        assertThat(e.getProperty("management.otlp.tracing.endpoint")).isEqualTo("http://otel-collector:4318/v1/traces");
        assertThat(e.getProperty("management.metrics.distribution.percentiles-histogram.http.server.requests"))
                .isEqualTo("true");
    }

    @Test
    @DisplayName("límite: el entorno las enciende con sus variables, sin tocar ningún application.yml")
    void porEntorno() {
        var e = entorno(Map.of(
                "TRAZAS_HABILITADAS", "true", "TRAZAS_MUESTREO", "1.0", "OTEL_TRAZAS_URL", "http://c:4318/v1/traces"));

        assertThat(e.getProperty("management.otlp.tracing.export.enabled")).isEqualTo("true");
        assertThat(e.getProperty("management.tracing.sampling.probability")).isEqualTo("1.0");
        assertThat(e.getProperty("management.otlp.tracing.endpoint")).isEqualTo("http://c:4318/v1/traces");
    }

    @Test
    @DisplayName("límite: un servicio que fija la propiedad gana: los defaults van con la precedencia más baja")
    void elServicioGana() {
        var e = entorno(Map.of("management.tracing.sampling.probability", "0.5"));

        assertThat(e.getProperty("management.tracing.sampling.probability")).isEqualTo("0.5");
        assertThat(e.getPropertySources().precedenceOf(e.getPropertySources().get(DefaultsDeObservabilidad.FUENTE)))
                .isEqualTo(e.getPropertySources().size() - 1);
    }
}
