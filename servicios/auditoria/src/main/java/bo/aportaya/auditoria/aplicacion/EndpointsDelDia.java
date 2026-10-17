package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.infraestructura.ConsultaPrometheus;
import bo.aportaya.auditoria.infraestructura.ConsultaPrometheus.Muestra;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Tableros · OPERACION: los endpoints tocados HOY (desde la medianoche de La Paz hasta ahora), con cuantas solicitudes,
 * cuantas fallaron y su p95. Es una vista en vivo, no un indicador del catalogo. Lista acotada a los {@code limite} mas
 * usados, con orden determinista (solicitudes desc, despues servicio, metodo y ruta). La ruta es la PLANTILLA
 * ({@code /grupos/{id}}): nunca la URL con identificadores.
 */
@Service
public class EndpointsDelDia {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");
    public static final int LIMITE_MAXIMO = 100;

    public record Endpoint(
            String servicio,
            String metodo,
            String ruta,
            long solicitudes,
            long errores4xx,
            long errores5xx,
            Optional<BigDecimal> p95Ms) {}

    /** `fuenteDisponible = false` es «no sabemos», distinto de una lista vacia («nadie llamo»). */
    public record Salida(Instant desde, Instant hasta, boolean fuenteDisponible, List<Endpoint> endpoints) {}

    private final ConsultaPrometheus prometheus;
    private final ConsultasDeTrafico consultas;
    private final Reloj reloj;

    public EndpointsDelDia(ConsultaPrometheus prometheus, ConsultasDeTrafico consultas, Reloj reloj) {
        this.prometheus = prometheus;
        this.consultas = consultas;
        this.reloj = reloj;
    }

    public Salida consultar(int limite) {
        Instant ahora = reloj.ahora();
        Instant desde = reloj.hoy().atStartOfDay(LA_PAZ).toInstant();
        long segundos = Math.max(60, Duration.between(desde, ahora).toSeconds());
        String rango = segundos + "s";

        var total = prometheus.consultar(consultas.solicitudesPorRuta(rango, null), ahora);
        if (total.isEmpty()) {
            return new Salida(desde, ahora, false, List.of());
        }
        var de4xx = porClave(prometheus.consultar(consultas.solicitudesPorRuta(rango, "4.."), ahora));
        var de5xx = porClave(prometheus.consultar(consultas.solicitudesPorRuta(rango, "5.."), ahora));
        var p95 = porClave(prometheus.consultar(consultas.p95PorRutaMs(rango), ahora));

        List<Endpoint> lista = total.get().stream()
                .map(m -> {
                    String clave = clave(m);
                    return new Endpoint(
                            m.etiquetas().getOrDefault("service_name", "desconocido"),
                            m.etiquetas().getOrDefault("method", "?"),
                            m.etiquetas().getOrDefault("uri", "?"),
                            entero(m.valor()),
                            entero(de4xx.getOrDefault(clave, BigDecimal.ZERO)),
                            entero(de5xx.getOrDefault(clave, BigDecimal.ZERO)),
                            Optional.ofNullable(p95.get(clave)).map(v -> v.setScale(2, RoundingMode.HALF_EVEN)));
                })
                .filter(e -> e.solicitudes() > 0)
                .sorted(Comparator.comparingLong(Endpoint::solicitudes)
                        .reversed()
                        .thenComparing(Endpoint::servicio)
                        .thenComparing(Endpoint::metodo)
                        .thenComparing(Endpoint::ruta))
                .limit(Math.min(Math.max(1, limite), LIMITE_MAXIMO))
                .toList();
        return new Salida(desde, ahora, true, lista);
    }

    private static Map<String, BigDecimal> porClave(Optional<List<Muestra>> muestras) {
        Map<String, BigDecimal> mapa = new HashMap<>();
        muestras.orElse(List.of()).forEach(m -> mapa.merge(clave(m), m.valor(), BigDecimal::add));
        return mapa;
    }

    private static String clave(Muestra m) {
        return m.etiquetas().getOrDefault("service_name", "") + "|"
                + m.etiquetas().getOrDefault("method", "") + "|" + m.etiquetas().getOrDefault("uri", "");
    }

    private static long entero(BigDecimal v) {
        return v.setScale(0, RoundingMode.HALF_EVEN).longValue();
    }
}
