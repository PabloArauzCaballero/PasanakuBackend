package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Le pregunta el precio a {@code tarifas}, por su contrato.
 *
 * <p>Cotizar **deja la cotizacion escrita**, y eso es deliberado: R-TAR-02 dice que lo
 * que se cotiza es lo que se cobra, asi que la operacion que se autoriza despues queda
 * atada a un precio que alguien puede volver a mirar.
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
 *
 * <p>El {@code referenciaTipo} que viaja es uno de los que el contrato de {@code tarifas}
 * admite ({@code ENTREGA_FONDO, ORDEN_RECARGA, ORDEN_RETIRO, PAGO, PERIODO,
 * TRANSACCION_BILLETERA}). Mandar un valor fuera de esa lista hace que {@code tarifas} rechace
 * la cotizacion, y quien pregunta rechazaba entonces TODA recarga y TODO retiro.
 */
@Component
public class CotizadorPorHttp implements CotizadorDeComision {

    private static final String SIN_CONCEPTO = "AP-CU30-01";

    private final RestClient rest;
    private final String codigoTarifario;

    public CotizadorPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.tarifas}") String urlDeTarifas,
            @Value("${aportaya.tarifas.codigo-tarifario}") String codigoTarifario,
            @Value("${aportaya.tarifas.timeout:PT3S}") Duration timeout) {
        // Toda llamada saliente tiene tiempo maximo: sin el, un tarifas lento cuelga el hilo de quien opera.
        var fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build());
        fabrica.setReadTimeout(timeout);
        this.rest = constructor.requestFactory(fabrica).baseUrl(urlDeTarifas).build();
        this.codigoTarifario = codigoTarifario;
    }

    // El fallback vive SOLO en @Retry, no en @CircuitBreaker: si los dos declaran
    // fallbackMethod, el aspecto mas interno de los dos resuelve la excepcion ANTES
    // de que el otro llegue a verla — con @CircuitBreaker como el mas interno (el
    // orden real, no el textual, se confirmo corriendo esto: sin este ajuste
    // reintentaba una sola vez, no tres), su propio fallback devolvia
    // Optional.empty() al primer 503 y @Retry nunca se enteraba de que algo habia
    // fallado. Un solo fallback, en el aspecto mas EXTERNO, evita la ambiguedad.
    //
    // Sin catch generico de RuntimeException: un timeout o un error de conexion se
    // propaga tal cual, para que @Retry lo reintente y @CircuitBreaker lo cuente.
    @Override
    @Retry(name = "cotizador", fallbackMethod = "costoDeFallback")
    @CircuitBreaker(name = "cotizador")
    public Optional<Dinero> costoDe(
            String hechoGenerador, UUID referenciaId, Dinero montoBase, String claveIdempotencia) {
        var moneda = montoBase.moneda();
        return Optional.of(pedir(hechoGenerador, referenciaId, montoBase, claveIdempotencia)
                .map(r -> Dinero.de(new BigDecimal(r.montoTotal().monto()), moneda))
                .orElseGet(() -> Dinero.cero(moneda)));
    }

    /**
     * La cotizacion completa para mostrarla antes de operar. A diferencia de {@code costoDe}
     * (que alimenta la reserva del retiro y vive bajo {@code @Retry}/{@code @CircuitBreaker}), esta
     * consulta no deja que una falla de transporte escape: «no se pudo saber» es vacio y quien
     * pregunta rechaza. Una respuesta incompleta (sin comision o impuesto) tambien es vacio: no se
     * le muestra a nadie un desglose inventado.
     */
    @Override
    public Optional<Cotizacion> cotizar(
            String hechoGenerador, UUID referenciaId, Dinero montoBase, String claveIdempotencia) {
        try {
            return Optional.of(pedir(hechoGenerador, referenciaId, montoBase, claveIdempotencia)
                    .map(r -> aCotizacion(r, montoBase))
                    .orElseGet(() -> gratuita(montoBase)));
        } catch (RuntimeException noSePudoSaber) {
            return Optional.empty();
        }
    }

    /**
     * Le pregunta a {@code tarifas}. Vacio significa que el tarifario dice que no hay concepto para
     * el hecho (la operacion es gratuita: una RESPUESTA de negocio, no una falla). Cualquier otra
     * respuesta de error, un cuerpo ausente o una falla de transporte se deja propagar.
     */
    private Optional<Respuesta> pedir(
            String hechoGenerador, UUID referenciaId, Dinero montoBase, String claveIdempotencia) {
        Map<String, Object> cuerpo = Map.of(
                "codigoTarifario",
                codigoTarifario,
                "hechoGenerador",
                hechoGenerador,
                // El contrato de tarifas solo admite ENTREGA_FONDO, ORDEN_RECARGA, ORDEN_RETIRO, PAGO,
                // PERIODO y TRANSACCION_BILLETERA; con "OPERACION" respondia 400 y ningun retiro cotizaba.
                "referenciaTipo",
                referenciaTipoDe(hechoGenerador),
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
                    .body(Respuesta.class);
            if (salida == null) {
                throw new IllegalStateException("tarifas respondio sin cuerpo");
            }
            return Optional.of(salida);
        } catch (RestClientResponseException respondio) {
            if (esGratuita(respondio)) {
                return Optional.empty();
            }
            throw respondio;
        }
    }

    private static Cotizacion gratuita(Dinero montoBase) {
        var cero = Dinero.cero(montoBase.moneda());
        return new Cotizacion(Optional.empty(), montoBase, cero, cero, cero, Optional.empty(), true);
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

    @Override
    public boolean aceptar(UUID cotizacionId) {
        try {
            var salida = rest.post()
                    .uri("/comisiones/cotizaciones/{id}/aceptacion", cotizacionId)
                    .headers(ClienteDeServicio::propagarElToken)
                    .retrieve()
                    .body(Aceptacion.class);
            return salida != null && salida.aceptada();
        } catch (RuntimeException sinAceptar) {
            // Vencida, inexistente o sin respuesta: la persona no acepto un precio vigente.
            return false;
        }
    }

    /** Cada hecho generador se cotiza contra el tipo de referencia del contrato que le corresponde. */
    static String referenciaTipoDe(String hechoGenerador) {
        return switch (hechoGenerador) {
            case "RECARGA" -> "ORDEN_RECARGA";
            case "RETIRO_ACREDITADO" -> "ORDEN_RETIRO";
            default -> "TRANSACCION_BILLETERA";
        };
    }

    private static Cotizacion aCotizacion(Respuesta r, Dinero base) {
        var moneda = base.moneda();
        return new Cotizacion(
                Optional.of(r.cotizacionId()),
                base,
                Dinero.de(new BigDecimal(r.montoComision().monto()), moneda),
                Dinero.de(new BigDecimal(r.montoImpuesto().monto()), moneda),
                Dinero.de(new BigDecimal(r.montoTotal().monto()), moneda),
                Optional.ofNullable(r.validaHasta()),
                false);
    }

    /** «No hay concepto para ese hecho» es un precio, no un error de transporte. */
    private boolean esGratuita(RestClientResponseException respondio) {
        String cuerpo = respondio.getResponseBodyAsString();
        return cuerpo.contains(SIN_CONCEPTO) && cuerpo.contains("\"gratuita\"");
    }

    private record Respuesta(
            UUID cotizacionId,
            Importe montoComision,
            Importe montoImpuesto,
            Importe montoTotal,
            OffsetDateTime validaHasta) {
        private record Importe(String monto, String moneda) {}
    }

    private record Aceptacion(boolean aceptada) {}
}
