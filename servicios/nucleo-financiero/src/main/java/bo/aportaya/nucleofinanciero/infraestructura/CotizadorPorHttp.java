package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Le pregunta el precio a {@code tarifas}, por su contrato.
 *
 * <p>Cotizar **deja la cotizacion escrita**, y eso es deliberado: R-TAR-02 dice que lo
 * que se cotiza es lo que se cobra, asi que el retiro que se autoriza despues queda
 * atado a un precio que alguien puede volver a mirar.
 *
 * <p>Cuando el tarifario no tiene concepto para el hecho, {@code tarifas} responde
 * {@code AP-CU30-01} con {@code gratuita=true}. Eso <b>no</b> es una falla: es el
 * catalogo diciendo que la operacion no cobra. Cualquier otra respuesta —incluida la
 * ausencia de respuesta— se traduce a vacio, y quien pregunta rechaza.
 *
 * <p><b>H4.S2.M4 — cliente resiliente.</b> Los tiempos de conexion/lectura ya estan
 * acotados globalmente (`ConfiguracionDeClientes`, `comun-web`); lo que faltaba era
 * QUE HACER cuando ese tiempo se agota mas de una vez seguida: reintentar un par de
 * veces con espera creciente (`@Retry`), y despues de varios fallos SEGUIDOS dejar de
 * insistir por un rato (`@CircuitBreaker`) — un `tarifas` caido no puede convertirse en
 * cientos de retiros colgados esperando cada uno su propio timeout de 1s+lectura. La
 * excepcion de transporte (todo lo que NO es la respuesta de negocio "gratuita") ahora
 * se deja propagar hasta las anotaciones en vez de tragarse adentro del método — sin
 * eso, resilience4j nunca ve una falla y el circuito nunca se abre. El resultado hacia
 * quien llama no cambia: {@link #costoDeFallback} devuelve el mismo {@code Optional
 * .empty()} que el catch original.
 */
@Component
public class CotizadorPorHttp implements CotizadorDeComision {

    private static final String SIN_CONCEPTO = "AP-CU30-01";

    private final RestClient rest;
    private final String codigoTarifario;

    public CotizadorPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.tarifas}") String urlDeTarifas,
            @Value("${aportaya.tarifas.codigo-tarifario}") String codigoTarifario) {
        this.rest = constructor.baseUrl(urlDeTarifas).build();
        this.codigoTarifario = codigoTarifario;
    }

    // El fallback vive SOLO en @Retry, no en @CircuitBreaker: si los dos declaran
    // fallbackMethod, el aspecto mas interno de los dos resuelve la excepcion ANTES
    // de que el otro llegue a verla — con @CircuitBreaker como el mas interno (el
    // orden real, no el textual, se confirmo corriendo esto: sin este ajuste
    // reintentaba una sola vez, no tres), su propio fallback devolvia
    // Optional.empty() al primer 503 y @Retry nunca se enteraba de que algo habia
    // fallado. Un solo fallback, en el aspecto mas EXTERNO, evita la ambiguedad.
    @Override
    @Retry(name = "cotizador", fallbackMethod = "costoDeFallback")
    @CircuitBreaker(name = "cotizador")
    public Optional<Dinero> costoDe(
            String hechoGenerador, UUID referenciaId, Dinero montoBase, String claveIdempotencia) {

        Map<String, Object> cuerpo = Map.of(
                "codigoTarifario",
                codigoTarifario,
                "hechoGenerador",
                hechoGenerador,
                "referenciaTipo",
                "OPERACION",
                "referenciaId",
                referenciaId.toString(),
                "montoBase",
                Map.of(
                        "monto", montoBase.monto().toPlainString(),
                        "moneda", montoBase.moneda().name()));
        try {
            var salida = rest.post()
                    .uri("/comisiones/cotizaciones")
                    .header("Idempotency-Key", claveIdempotencia)
                    .headers(ClienteDeServicio::propagarElToken)
                    .body(cuerpo)
                    .retrieve()
                    .body(Cotizacion.class);
            return Optional.ofNullable(salida)
                    .map(c -> Dinero.de(new BigDecimal(c.montoTotal().monto()), montoBase.moneda()));
        } catch (RestClientResponseException respondio) {
            // "Gratuita" es una RESPUESTA de negocio, no una falla de transporte: se
            // resuelve aca mismo, nunca llega a @Retry/@CircuitBreaker. Cualquier otro
            // codigo (4xx/5xx real) SI se deja propagar — es la unica forma de que
            // resilience4j vea que algo esta fallando.
            if (esGratuita(respondio)) {
                return Optional.of(Dinero.cero(montoBase.moneda()));
            }
            throw respondio;
        }
        // Sin catch generico de RuntimeException: un timeout o un error de conexion se
        // propaga tal cual, para que @Retry lo reintente y @CircuitBreaker lo cuente.
    }

    /**
     * Lo que {@code costoDe} devuelve cuando {@code tarifas} no contesta ni despues de
     * reintentar, o cuando el circuito esta abierto (H4.S2.M4). Mismo resultado externo
     * que el catch original: quien pregunta ve "no se pudo cotizar" y rechaza — nunca
     * un retiro sale sin costo conocido.
     */
    @SuppressWarnings("unused")
    private Optional<Dinero> costoDeFallback(
            String hechoGenerador, UUID referenciaId, Dinero montoBase, String claveIdempotencia, Throwable falla) {
        return Optional.empty();
    }

    /** «No hay concepto para ese hecho» es un precio, no un error de transporte. */
    private boolean esGratuita(RestClientResponseException respondio) {
        String cuerpo = respondio.getResponseBodyAsString();
        return cuerpo.contains(SIN_CONCEPTO) && cuerpo.contains("\"gratuita\"");
    }

    private record Cotizacion(Importe montoTotal) {
        private record Importe(String monto, String moneda) {}
    }
}
