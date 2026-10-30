package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.FondosDelComprador;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * El saldo del comprador en {@code nucleo-financiero}, por su contrato HTTP. Falla CERRADO: cualquier cosa que no sea
 * una respuesta entendible es {@code NO_DISPONIBLE} y la compra queda detenida y reanudable, sin dar el titulo.
 *
 * <p>Quien firma cada llamada (carril G, defecto hallado al cerrar H9.S1.M5):
 *
 * <ul>
 *   <li><b>retener</b> y <b>liberar</b> van con el token de la PERSONA que compra: el libro solo deja apartar saldo a
 *       su titular (o a un rol con mandato). Antes de retener se le pregunta su billetera
 *       ({@code GET /billetera/cuentas/propia}, con ese mismo token) y se comprueba que el token es del comprador que
 *       pide la saga: no se retiene bajo una identidad ajena. Sin token (un trabajo programado) no sale ninguna
 *       peticion util: el libro responde 401 y la compra espera a la proxima persona que la reanude.
 *   <li><b>pagarAlVendedor</b> va con la credencial de SERVICIO ({@code SERVICIO_ENTREGAS}, emitida por identidad y
 *       renovada sola): nucleo rechaza a cualquier sesion de usuario final, aunque tenga {@code BILLETERA_OPERAR}. El
 *       cuerpo va VACIO: el beneficiario y el reparto no los dicta este servicio, el libro los deriva de la oferta
 *       que la retencion nombra (preguntandole a {@code entregas}). Los importes que recibe el metodo son lo que
 *       aca se espera; si el libro no los reproduce, rechaza (AP-CU13-06) y no se liquida.
 * </ul>
 *
 * <p>Las claves de idempotencia de la saga son textos ({@code cesion:<id>:retener}); el contrato pide un UUID, asi que
 * se deriva uno determinista de la clave: repetir el paso tras una caida manda la MISMA clave y el libro responde lo
 * mismo, sin retener ni pagar dos veces. Tiempo maximo y circuit breaker ({@link ClienteConCircuito}).
 */
@Component
public class FondosPorHttp implements FondosDelComprador {

    private static final Logger BITACORA = LoggerFactory.getLogger(FondosPorHttp.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SALDO_INSUFICIENTE = "AP-CU13-01";

    private final ClienteConCircuito nucleo;
    private final CredencialDeServicio servicio;

    @Autowired
    public FondosPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.nucleo-financiero}") String urlDeNucleo,
            ConfiguracionDeCircuito circuito,
            CredencialDeServicio servicio) {
        this.nucleo = new ClienteConCircuito(constructor, urlDeNucleo, "nucleo-financiero", circuito);
        this.servicio = servicio;
    }

    @Override
    public Referencia retener(UUID compradorUsuarioId, UUID ofertaId, Dinero monto, String clave) {
        if (!esLaSesionDe(compradorUsuarioId)) {
            return noDisponible();
        }
        try {
            CuentaDeLaSesion cuenta = nucleo.ejecutar(rest -> rest.get()
                    .uri(uri -> uri.path("/billetera/cuentas/propia")
                            .queryParam("moneda", monto.moneda().name())
                            .build())
                    .headers(ClienteDeServicio::propagarElToken)
                    .retrieve()
                    .body(CuentaDeLaSesion.class));
            if (cuenta == null || cuenta.cuentaId() == null) {
                return noDisponible();
            }
            var retencion = nucleo.ejecutar(rest -> rest.post()
                    .uri("/billetera/retenciones")
                    .headers(ClienteDeServicio::propagarElToken)
                    .header("Idempotency-Key", llaveDe(clave).toString())
                    .body(Map.of(
                            "cuentaBilleteraId", cuenta.cuentaId().toString(),
                            "monto", importe(monto),
                            "motivo", "COMPRA_EN_MERCADO",
                            "referenciaTipo", "OFERTA_MERCADO",
                            "referenciaId", ofertaId.toString()))
                    .retrieve()
                    .body(Retencion.class));
            return retencion == null || retencion.retencionId() == null
                    ? noDisponible()
                    : new Referencia(Resultado.OK, retencion.retencionId());
        } catch (RestClientResponseException rechazo) {
            return esSaldoInsuficiente(rechazo)
                    ? new Referencia(Resultado.SALDO_INSUFICIENTE, null)
                    : registrar("retener", rechazo);
        } catch (RuntimeException noSePudo) {
            return registrar("retener", noSePudo);
        }
    }

    @Override
    public Referencia pagarAlVendedor(
            UUID retencionRef, UUID vendedorUsuarioId, Dinero paraElVendedor, Dinero cargos, String clave) {
        Optional<String> token = servicio.vigente();
        if (token.isEmpty()) {
            // Sin credencial de servicio no sale ninguna peticion que mueva plata (falla cerrado).
            BITACORA.warn("pagarAlVendedor sin credencial de servicio: no se paga");
            return noDisponible();
        }
        try {
            var pago = nucleo.ejecutar(rest -> rest.post()
                    .uri("/billetera/retenciones/{id}/pago-a-tercero", retencionRef)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.get())
                    .header("Idempotency-Key", llaveDe(clave).toString())
                    .retrieve()
                    .body(Ejecucion.class));
            return pago == null || pago.transaccionId() == null
                    ? noDisponible()
                    : new Referencia(Resultado.OK, pago.transaccionId());
        } catch (RuntimeException noSePudo) {
            return registrar("pagarAlVendedor", noSePudo);
        }
    }

    @Override
    public Resultado liberar(UUID retencionRef, String clave) {
        try {
            var cierre = nucleo.ejecutar(rest -> rest.post()
                    .uri("/billetera/retenciones/{id}/cierre", retencionRef)
                    .headers(ClienteDeServicio::propagarElToken)
                    .header("Idempotency-Key", llaveDe(clave).toString())
                    .body(Map.of("desenlace", "LIBERADA"))
                    .retrieve()
                    .body(Cierre.class));
            // Solo cuenta lo que el libro DIJO: la retencion pedida, cerrada como LIBERADA.
            return cierre != null && retencionRef.equals(cierre.retencionId()) && "LIBERADA".equals(cierre.estado())
                    ? Resultado.OK
                    : registrar("liberar", new IllegalStateException("respuesta ilegible"))
                            .resultado();
        } catch (RestClientResponseException rechazo) {
            // Reintento tras una caida: ya estaba liberada. Es el resultado que se queria; EJECUTADA no lo es.
            return yaLiberada(rechazo)
                    ? Resultado.OK
                    : registrar("liberar", rechazo).resultado();
        } catch (RuntimeException noSePudo) {
            return registrar("liberar", noSePudo).resultado();
        }
    }

    @Override
    public Resultado devolver(UUID retencionRef, String clave) {
        Optional<String> token = servicio.vigente();
        if (token.isEmpty()) {
            BITACORA.warn("devolver sin credencial de servicio: queda para reintentar");
            return Resultado.NO_DISPONIBLE;
        }
        try {
            var cierre = nucleo.ejecutar(rest -> rest.post()
                    .uri("/billetera/retenciones/{id}/liberacion-de-mercado", retencionRef)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.get())
                    .header("Idempotency-Key", llaveDe(clave).toString())
                    .retrieve()
                    .body(Cierre.class));
            return cierre != null && retencionRef.equals(cierre.retencionId()) && "LIBERADA".equals(cierre.estado())
                    ? Resultado.OK
                    : registrar("devolver", new IllegalStateException("respuesta ilegible"))
                            .resultado();
        } catch (RestClientResponseException rechazo) {
            return yaLiberada(rechazo)
                    ? Resultado.OK
                    : registrar("devolver", rechazo).resultado();
        } catch (RuntimeException noSePudo) {
            return registrar("devolver", noSePudo).resultado();
        }
    }

    /** 422 AP-CU13-03 con el estado ya LIBERADA en el detalle (nunca por el texto del mensaje). */
    private static boolean yaLiberada(RestClientResponseException rechazo) {
        if (rechazo.getStatusCode().value() != 422) {
            return false;
        }
        try {
            JsonNode cuerpo = JSON.readTree(rechazo.getResponseBodyAsString());
            return "AP-CU13-03".equals(cuerpo.path("codigo").asText())
                    && "LIBERADA".equals(cuerpo.path("detalle").path("estado").asText());
        } catch (java.io.IOException | RuntimeException ilegible) {
            return false;
        }
    }

    /** El token de la peticion en curso es del comprador que pide la saga; si no, no se retiene. */
    private static boolean esLaSesionDe(UUID usuario) {
        return ClienteDeServicio.usuarioDeLaSesion()
                .filter(usuario.toString()::equals)
                .isPresent();
    }

    /** UUID determinista de una clave de la saga: el contrato del libro pide un UUID y repetir el paso manda el mismo. */
    static UUID llaveDe(String clave) {
        return UUID.nameUUIDFromBytes(clave.getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, String> importe(Dinero monto) {
        return Map.of("monto", monto.toString(), "moneda", monto.moneda().name());
    }

    /** 422 del libro con el codigo del saldo y lo que SI hay (el mismo codigo sirve para "no es tu billetera"). */
    private static boolean esSaldoInsuficiente(RestClientResponseException rechazo) {
        if (rechazo.getStatusCode().value() != 422) {
            return false;
        }
        try {
            JsonNode cuerpo = JSON.readTree(rechazo.getResponseBodyAsString());
            return SALDO_INSUFICIENTE.equals(cuerpo.path("codigo").asText())
                    && cuerpo.path("detalle").has("disponible");
        } catch (java.io.IOException | RuntimeException ilegible) {
            return false;
        }
    }

    private static Referencia noDisponible() {
        return new Referencia(Resultado.NO_DISPONIBLE, null);
    }

    /** Solo la operacion y la clase de la falla: ni el cuerpo, ni el token, ni los importes (90.2). */
    private static Referencia registrar(String operacion, Exception falla) {
        String que = falla instanceof RestClientResponseException r
                ? "HTTP " + r.getStatusCode().value()
                : falla instanceof ResourceAccessException
                        ? "sin respuesta"
                        : falla.getClass().getSimpleName();
        BITACORA.warn("nucleo-financiero no confirmo {}: {}", operacion, que);
        return noDisponible();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CuentaDeLaSesion(UUID cuentaId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Retencion(UUID retencionId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Ejecucion(UUID transaccionId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Cierre(UUID retencionId, String estado) {}
}
