package bo.aportaya.auditoria.infraestructura;

import bo.aportaya.auditoria.dominio.SaludDeServicio;
import bo.aportaya.auditoria.dominio.SaludDeServicio.Estado;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * operacion/estado · pregunta el readiness de cada servicio por la red interna ({@code /actuator/health/readiness},
 * abierto sin sesion en todos: {@code ConfiguracionDeSeguridad}). Las consultas salen en paralelo y cada una tiene
 * timeout explicito (regla 98.2.1): un servicio colgado no demora a los demas ni a la pantalla mas que el plazo.
 *
 * <p>Que contesto es lo unico que se mira: 200 es ARRIBA, cualquier otro estado HTTP es DEGRADADO (el servicio vive
 * pero se declara fuera de servicio, tipicamente 503 cuando su base o Kafka no estan), y no contestar es CAIDO.
 * Del cuerpo no se lee nada: no se copia el detalle de dependencias del servicio a otra respuesta.
 */
@Component
public class SondaDeSalud {

    private static final Logger LOG = LoggerFactory.getLogger(SondaDeSalud.class);

    private final HttpClient http;
    private final List<String> servicios;
    private final String plantilla;
    private final Duration plazo;

    public SondaDeSalud(
            @Value("${aportaya.salud.servicios:}") String servicios,
            @Value("${aportaya.salud.plantilla:http://%s:8080/actuator/health/readiness}") String plantilla,
            @Value("${aportaya.salud.timeout:PT2S}") Duration plazo) {
        this.servicios = Arrays.stream(servicios.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        this.plantilla = plantilla;
        this.plazo = plazo;
        this.http = HttpClient.newBuilder().connectTimeout(plazo).build();
    }

    public List<String> servicios() {
        return servicios;
    }

    /** Una consulta por servicio, todas a la vez; vuelve en el orden de la configuracion. */
    public List<SaludDeServicio> consultar() {
        List<CompletableFuture<SaludDeServicio>> enCurso =
                servicios.stream().map(this::preguntar).toList();
        return enCurso.stream().map(CompletableFuture::join).toList();
    }

    private CompletableFuture<SaludDeServicio> preguntar(String servicio) {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(String.format(plantilla, servicio)))
                .timeout(plazo)
                .GET()
                .build();
        long inicio = System.nanoTime();
        return http.sendAsync(pedido, HttpResponse.BodyHandlers.discarding())
                .orTimeout(plazo.toMillis(), TimeUnit.MILLISECONDS)
                .handle((respuesta, fallo) -> {
                    if (fallo != null) {
                        // El nombre del servicio y la clase del fallo: nada del pedido ni de la respuesta.
                        LOG.warn(
                                "salud de {}: sin respuesta ({})",
                                servicio,
                                causa(fallo).getClass().getSimpleName());
                        return new SaludDeServicio(servicio, Estado.CAIDO, Optional.empty());
                    }
                    long ms = Duration.ofNanos(System.nanoTime() - inicio).toMillis();
                    Estado estado = respuesta.statusCode() == 200 ? Estado.ARRIBA : Estado.DEGRADADO;
                    return new SaludDeServicio(servicio, estado, Optional.of(ms));
                });
    }

    private static Throwable causa(Throwable fallo) {
        return fallo.getCause() != null ? fallo.getCause() : fallo;
    }
}
