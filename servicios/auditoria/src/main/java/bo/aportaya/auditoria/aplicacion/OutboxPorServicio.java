package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.dominio.OutboxDeServicio;
import bo.aportaya.auditoria.infraestructura.ConsultaPrometheus;
import bo.aportaya.auditoria.infraestructura.ConsultaPrometheus.Muestra;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sistemas › Outbox: lo que cada relevo publica en Prometheus, leido con {@link ConsultaPrometheus}. auditoria no lee
 * el esquema de otro servicio (regla 98.1): mira las metricas que el propio servicio expone de su outbox.
 *
 * <p>Los nombres de las metricas son configurables, como {@code aportaya.prometheus.metrica-http}: dependen de como el
 * Collector traduce los nombres de OTLP. Si Prometheus no responde a CUALQUIERA de las consultas, el resultado es
 * {@code fuenteDisponible = false}: una lista armada con la mitad de las series se leeria como «sin pendientes».
 */
@Service
public class OutboxPorServicio {

    public record Salida(Instant consultadoEn, boolean fuenteDisponible, List<OutboxDeServicio> servicios) {}

    private final ConsultaPrometheus prometheus;
    private final Reloj reloj;
    private final String pendientes;
    private final String fallidos;
    private final String edad;
    private final String publicaciones;

    public OutboxPorServicio(
            ConsultaPrometheus prometheus,
            Reloj reloj,
            @Value("${aportaya.prometheus.metrica-outbox-pendientes:aportaya_outbox_pendientes}") String pendientes,
            @Value("${aportaya.prometheus.metrica-outbox-fallidos:aportaya_outbox_fallidos}") String fallidos,
            @Value("${aportaya.prometheus.metrica-outbox-edad:aportaya_outbox_edad_mas_viejo_segundos}") String edad,
            @Value("${aportaya.prometheus.metrica-outbox-publicaciones:aportaya_outbox_publicaciones_total}")
                    String publicaciones) {
        this.prometheus = prometheus;
        this.reloj = reloj;
        this.pendientes = pendientes;
        this.fallidos = fallidos;
        this.edad = edad;
        this.publicaciones = publicaciones;
    }

    public Salida consultar() {
        Instant ahora = reloj.ahora();
        var p = prometheus.consultar("sum by (service_name) (" + pendientes + ")", ahora);
        var f = prometheus.consultar("sum by (service_name) (" + fallidos + ")", ahora);
        var e = prometheus.consultar("max by (service_name) (" + edad + ")", ahora);
        var pub =
                prometheus.consultar("sum by (service_name, resultado) (increase(" + publicaciones + "[24h]))", ahora);
        if (p.isEmpty() || f.isEmpty() || e.isEmpty() || pub.isEmpty()) {
            return new Salida(ahora, false, List.of());
        }
        Map<String, Long> porPendientes = porServicio(p.get(), null);
        Map<String, Long> porFallidos = porServicio(f.get(), null);
        Map<String, Long> porEdad = porServicio(e.get(), null);
        Map<String, Long> ok = porServicio(pub.get(), "ok");
        Map<String, Long> error = porServicio(pub.get(), "error");
        var nombres = new TreeSet<String>();
        List.of(porPendientes, porFallidos, porEdad, ok, error).forEach(m -> nombres.addAll(m.keySet()));
        List<OutboxDeServicio> servicios = new ArrayList<>();
        for (String s : nombres) {
            servicios.add(new OutboxDeServicio(
                    s,
                    Optional.ofNullable(porPendientes.get(s)),
                    Optional.ofNullable(porFallidos.get(s)),
                    Optional.ofNullable(porEdad.get(s)),
                    Optional.ofNullable(ok.get(s)),
                    Optional.ofNullable(error.get(s))));
        }
        return new Salida(ahora, true, List.copyOf(servicios));
    }

    /** Una serie por servicio, redondeada al entero (increase extrapola). Sin {@code service_name} no se atribuye. */
    private static Map<String, Long> porServicio(List<Muestra> muestras, String resultado) {
        Map<String, Long> m = new TreeMap<>();
        for (Muestra x : muestras) {
            String servicio = x.etiquetas().get("service_name");
            if (servicio == null
                    || (resultado != null && !resultado.equals(x.etiquetas().get("resultado")))) {
                continue;
            }
            long valor = x.valor()
                    .max(BigDecimal.ZERO)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
            m.merge(servicio, valor, Long::sum);
        }
        return m;
    }
}
