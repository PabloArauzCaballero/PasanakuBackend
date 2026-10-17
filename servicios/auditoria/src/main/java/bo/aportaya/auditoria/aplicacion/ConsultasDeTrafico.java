package bo.aportaya.auditoria.aplicacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Las consultas PromQL del trafico HTTP, en un solo lugar. El nombre de la metrica es configurable porque depende de
 * como el Collector traduce la de Micrometer ({@code http.server.requests}, segundos) al formato de Prometheus; por
 * omision, {@code http_server_requests_seconds}. Se excluye el actuator: son sondas, no trafico de personas.
 */
@Component
public class ConsultasDeTrafico {

    private final String metrica;

    public ConsultasDeTrafico(
            @Value("${aportaya.prometheus.metrica-http:http_server_requests_seconds}") String metrica) {
        this.metrica = metrica;
    }

    private String filtro(String estados) {
        String base = "uri!~\"/actuator.*\"";
        return estados == null ? "{" + base + "}" : "{" + base + ",status=~\"" + estados + "\"}";
    }

    public String solicitudes(String rango, String estados) {
        return "sum(increase(" + metrica + "_count" + filtro(estados) + "[" + rango + "]))";
    }

    public String percentilMs(String cuantil, String rango) {
        return "histogram_quantile(" + cuantil + ", sum by (le) (increase(" + metrica + "_bucket" + filtro(null) + "["
                + rango + "]))) * 1000";
    }

    public String solicitudesPorRuta(String rango, String estados) {
        return "sum by (service_name, method, uri) (increase(" + metrica + "_count" + filtro(estados) + "[" + rango
                + "]))";
    }

    public String p95PorRutaMs(String rango) {
        return "histogram_quantile(0.95, sum by (service_name, method, uri, le) (increase(" + metrica + "_bucket"
                + filtro(null) + "[" + rango + "]))) * 1000";
    }
}
