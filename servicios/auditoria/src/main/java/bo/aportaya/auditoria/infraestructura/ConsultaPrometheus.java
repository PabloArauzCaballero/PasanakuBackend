package bo.aportaya.auditoria.infraestructura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tableros · OPERACION: lee Prometheus (que recibe las metricas por OTLP desde el Collector) con su API de consulta
 * instantanea ({@code GET /api/v1/query}). Timeout explicito (regla 98.2.1) y degradacion DECLARADA: si Prometheus no
 * responde, tarda o contesta un error, el resultado es {@code Optional.empty()} —«fuente no disponible»—, nunca una
 * lista vacia que se leeria como «no hubo trafico».
 */
@Component
public class ConsultaPrometheus {

    private static final Logger LOG = LoggerFactory.getLogger(ConsultaPrometheus.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Una serie del resultado: sus etiquetas y su valor (exacto, como lo escribe Prometheus). */
    public record Muestra(Map<String, String> etiquetas, BigDecimal valor) {}

    private final HttpClient http;
    private final String base;
    private final Duration plazo;

    public ConsultaPrometheus(
            @Value("${aportaya.prometheus.url}") String base,
            @Value("${aportaya.prometheus.timeout:PT3S}") Duration plazo) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        this.plazo = plazo;
        this.http = HttpClient.newBuilder().connectTimeout(plazo).build();
    }

    public Optional<List<Muestra>> consultar(String promql, Instant al) {
        if (base.isBlank()) {
            return Optional.empty();
        }
        URI uri = URI.create(base + "/api/v1/query?query=" + URLEncoder.encode(promql, StandardCharsets.UTF_8)
                + "&time=" + al.getEpochSecond());
        try {
            HttpResponse<String> r = http.send(
                    HttpRequest.newBuilder(uri).timeout(plazo).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) {
                LOG.warn("prometheus respondio {}: el indicador sale sin fuente", r.statusCode());
                return Optional.empty();
            }
            JsonNode cuerpo = JSON.readTree(r.body());
            if (!"success".equals(cuerpo.path("status").asText())) {
                return Optional.empty();
            }
            List<Muestra> muestras = new ArrayList<>();
            for (JsonNode serie : cuerpo.path("data").path("result")) {
                String texto = serie.path("value").path(1).asText("");
                // NaN e Inf salen de dividir por cero en PromQL: no son un valor, son la ausencia de uno.
                if (texto.isEmpty() || texto.contains("NaN") || texto.contains("Inf")) {
                    continue;
                }
                Map<String, String> etiquetas = new HashMap<>();
                serie.path("metric")
                        .properties()
                        .forEach(e -> etiquetas.put(e.getKey(), e.getValue().asText()));
                muestras.add(new Muestra(Map.copyOf(etiquetas), new BigDecimal(texto)));
            }
            return Optional.of(List.copyOf(muestras));
        } catch (java.io.IOException | NumberFormatException fallo) {
            LOG.warn("prometheus no disponible: {}", fallo.getClass().getSimpleName());
            return Optional.empty();
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
