package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecargasProveedorSimuladoTest {
    private static final String SECRETO = "firma-ficticia-solo-prueba-0000000000000001";
    private static final String API = "api-ficticia-solo-prueba-000000000000000001";
    private static final ObjectMapper JSON = new ObjectMapper();
    private final UUID referencia = UUID.randomUUID();
    private final AtomicReference<String> respuesta = new AtomicReference<>();
    private HttpServer server;
    private RecargasProveedorSimulado cliente;

    @BeforeEach
    void iniciar() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/operaciones/", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer " + API);
            byte[] raw = respuesta.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, raw.length);
            try (var stream = exchange.getResponseBody()) {
                stream.write(raw);
            }
        });
        server.start();
        cliente = new RecargasProveedorSimulado("simulado", "simulado", url(), API, SECRETO, Duration.ofSeconds(2));
        respuesta.set(firmar(payload()));
    }

    @AfterEach
    void cerrar() {
        server.stop(0);
    }

    private URI url() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private Map<String, Object> payload() {
        Map<String, Object> data = new HashMap<>();
        data.put("referencia", referencia.toString());
        data.put("transaccionProveedor", UUID.randomUUID().toString());
        data.put("tipo", "RECARGA");
        data.put("monto", "500.00");
        data.put("moneda", "BOB");
        data.put("estado", "CONFIRMADO");
        data.put("simulado", true);
        data.put("emitidoEn", Instant.now().toString());
        data.put("liquidadaEn", Instant.now().toString());
        return data;
    }

    private String firmar(Map<String, Object> data) throws Exception {
        byte[] raw = JSON.writeValueAsBytes(data);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return JSON.writeValueAsString(Map.of(
                "payload",
                Base64.getEncoder().encodeToString(raw),
                "firma",
                HexFormat.of().formatHex(mac.doFinal(raw))));
    }

    @Test
    void confirmaSoloReciboFirmadoParaLaReferencia() {
        var recibo = cliente.consultar(referencia);
        assertThat(recibo.referencia()).isEqualTo(referencia);
        assertThat(recibo.monto().toString()).isEqualTo("500.00");
        assertThat(recibo.estado()).isEqualTo("CONFIRMADO");
    }

    @Test
    void rechazaFirmaAlterada() throws Exception {
        var sobre = JSON.readTree(respuesta.get());
        respuesta.set(
                JSON.writeValueAsString(Map.of("payload", sobre.get("payload").asText(), "firma", "00".repeat(32))));
        assertThatThrownBy(() -> cliente.consultar(referencia)).isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    void rechazaOtraReferenciaAunqueLaFirmaSeaValida() {
        assertThatThrownBy(() -> cliente.consultar(UUID.randomUUID())).isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    void rechazaReciboVencido() throws Exception {
        var data = payload();
        data.put("emitidoEn", Instant.now().minus(Duration.ofHours(1)).toString());
        respuesta.set(firmar(data));
        assertThatThrownBy(() -> cliente.consultar(referencia)).isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    void noConfundeEstadoPendienteConConfirmado() throws Exception {
        var data = payload();
        data.put("estado", "PENDIENTE");
        data.put("liquidadaEn", null);
        respuesta.set(firmar(data));
        assertThat(cliente.consultar(referencia).estado()).isEqualTo("PENDIENTE");
    }

    @Test
    void exigeMontoDecimalTextual() throws Exception {
        var data = payload();
        data.put("monto", 500);
        respuesta.set(firmar(data));
        assertThatThrownBy(() -> cliente.consultar(referencia)).isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    void deshabilitadoNoConsultaNiAcredita() {
        var apagado =
                new RecargasProveedorSimulado("deshabilitado", "produccion", url(), "", "", Duration.ofSeconds(1));
        assertThatThrownBy(() -> apagado.consultar(referencia)).isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    void noArrancaSimuladoEnProduccion() {
        assertThatThrownBy(() -> new RecargasProveedorSimulado(
                        "simulado", "produccion", url(), API, SECRETO, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noArrancaConHostRemoto() {
        assertThatThrownBy(() -> new RecargasProveedorSimulado(
                        "simulado", "simulado", URI.create("https://example.com"), API, SECRETO, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void caidaDelProveedorNoConfirma() {
        server.stop(0);
        assertThatThrownBy(() -> cliente.consultar(referencia)).isInstanceOf(ErrorDeNegocio.class);
    }
}
