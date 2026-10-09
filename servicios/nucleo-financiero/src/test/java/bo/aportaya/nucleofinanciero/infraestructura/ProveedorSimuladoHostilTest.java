package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor;
import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor.Tipo;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El contrato del proveedor en sus tres niveles (regla 65): lo correcto, los bordes y lo
 * hostil. Lo hostil se clasifica: una respuesta que no se puede creer es una
 * <b>discrepancia</b> (se registra); una que no llega es <b>desconocido</b> (se consulta).
 */
class ProveedorSimuladoHostilTest {
    private static final String SECRETO = "firma-ficticia-solo-prueba-0000000000000001";
    private static final String API = "api-ficticia-solo-prueba-000000000000000001";
    private static final ObjectMapper JSON = new ObjectMapper();
    private final UUID referencia = UUID.randomUUID();
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private final AtomicInteger estadoHttp = new AtomicInteger(200);
    private final AtomicInteger consultas = new AtomicInteger();
    private HttpServer server;
    private RecargasProveedorSimulado recargas;
    private RetirosProveedorSimulado retiros;

    @BeforeEach
    void iniciar() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/operaciones", exchange -> {
            consultas.incrementAndGet();
            byte[] raw = cuerpo.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(estadoHttp.get(), raw.length);
            try (var stream = exchange.getResponseBody()) {
                stream.write(raw);
            }
        });
        server.start();
        URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        recargas = new RecargasProveedorSimulado("simulado", "simulado", url, API, SECRETO, Duration.ofSeconds(2));
        retiros = new RetirosProveedorSimulado("simulado", "simulado", url, API, SECRETO, Duration.ofSeconds(2));
        cuerpo.set(firmar(payload("RECARGA")));
    }

    @AfterEach
    void cerrar() {
        server.stop(0);
    }

    private Map<String, Object> payload(String tipo) {
        Map<String, Object> data = new HashMap<>();
        data.put("referencia", referencia.toString());
        data.put("transaccionProveedor", UUID.randomUUID().toString());
        data.put("tipo", tipo);
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
        return sobre(Base64.getEncoder().encodeToString(raw), HexFormat.of().formatHex(mac.doFinal(raw)));
    }

    private String sobre(String payloadBase64, String firma) throws Exception {
        return JSON.writeValueAsString(Map.of("payload", payloadBase64, "firma", firma));
    }

    private Tipo tipoDeLaDiscrepancia() {
        try {
            recargas.consultar(referencia);
        } catch (DiscrepanciaDelProveedor d) {
            assertThat(d.huella()).hasSize(64);
            return d.tipo();
        }
        throw new AssertionError("debio lanzar una discrepancia");
    }

    @Test
    @DisplayName("correcto: el recibo firmado se acepta y los retiros leen su propio tipo")
    void reciboCorrecto() throws Exception {
        assertThat(recargas.consultar(referencia).estado()).isEqualTo("CONFIRMADO");
        cuerpo.set(firmar(payload("RETIRO")));
        assertThat(retiros.consultar(referencia)).isPresent();
    }

    @Test
    @DisplayName("hostil: la firma alterada, no hexadecimal o ausente es FIRMA_INVALIDA")
    void firmasInvalidas() throws Exception {
        String payload = Base64.getEncoder().encodeToString(JSON.writeValueAsBytes(payload("RECARGA")));
        for (String firma : new String[] {"00".repeat(32), "no-es-hexadecimal", "", "abc"}) {
            cuerpo.set(sobre(payload, firma));
            assertThat(tipoDeLaDiscrepancia()).as("firma «%s»", firma).isEqualTo(Tipo.FIRMA_INVALIDA);
        }
    }

    @Test
    @DisplayName("hostil: lo firmado pero ajeno o malformado es REFERENCIA_DISTINTA o RESPUESTA_INVALIDA")
    void contenidoQueNoSePuedeCreer() throws Exception {
        var ajena = payload("RECARGA");
        ajena.put("referencia", UUID.randomUUID().toString());
        cuerpo.set(firmar(ajena));
        assertThat(tipoDeLaDiscrepancia()).isEqualTo(Tipo.REFERENCIA_DISTINTA);

        var vencido = payload("RECARGA");
        vencido.put("emitidoEn", Instant.now().minus(Duration.ofHours(1)).toString());
        cuerpo.set(firmar(vencido));
        assertThat(tipoDeLaDiscrepancia()).as("recibo vencido").isEqualTo(Tipo.RESPUESTA_INVALIDA);

        cuerpo.set(firmar(payload("RETIRO")));
        assertThat(tipoDeLaDiscrepancia()).as("tipo de otra operacion").isEqualTo(Tipo.RESPUESTA_INVALIDA);

        for (Object monto : new Object[] {500, "500", "-1.00", "1e3", "01.00", ""}) {
            var malo = payload("RECARGA");
            malo.put("monto", monto);
            cuerpo.set(firmar(malo));
            assertThat(tipoDeLaDiscrepancia()).as("monto %s", monto).isEqualTo(Tipo.RESPUESTA_INVALIDA);
        }

        var moneda = payload("RECARGA");
        moneda.put("moneda", "XXX");
        cuerpo.set(firmar(moneda));
        assertThat(tipoDeLaDiscrepancia()).as("moneda desconocida").isEqualTo(Tipo.RESPUESTA_INVALIDA);

        var estado = payload("RECARGA");
        estado.put("estado", "LIQUIDADO");
        cuerpo.set(firmar(estado));
        assertThat(tipoDeLaDiscrepancia()).as("estado fuera del contrato").isEqualTo(Tipo.RESPUESTA_INVALIDA);

        var noSimulado = payload("RECARGA");
        noSimulado.put("simulado", false);
        cuerpo.set(firmar(noSimulado));
        assertThat(tipoDeLaDiscrepancia()).isEqualTo(Tipo.RESPUESTA_INVALIDA);
    }

    @Test
    @DisplayName("hostil: el proveedor contradiciendose a si mismo es ESTADO_CONTRADICTORIO")
    void estadoYLiquidacionQueSeContradicen() throws Exception {
        var sinFecha = payload("RECARGA");
        sinFecha.put("liquidadaEn", null);
        cuerpo.set(firmar(sinFecha));
        assertThat(tipoDeLaDiscrepancia()).as("CONFIRMADO sin liquidar").isEqualTo(Tipo.ESTADO_CONTRADICTORIO);

        for (String estado : new String[] {"PENDIENTE", "RECHAZADO"}) {
            var conFecha = payload("RECARGA");
            conFecha.put("estado", estado);
            cuerpo.set(firmar(conFecha));
            assertThat(tipoDeLaDiscrepancia()).as("%s liquidado", estado).isEqualTo(Tipo.ESTADO_CONTRADICTORIO);
        }
    }

    @Test
    @DisplayName("hostil: cuerpos que ni siquiera son un sobre son RESPUESTA_INVALIDA, nunca una excepcion cruda")
    void sobresMalformados() throws Exception {
        String base64Roto = sobre("%%%no-es-base64%%%", "00");
        for (String basura : new String[] {"no es json", "[]", "\"texto\"", "{}", base64Roto, "null"}) {
            cuerpo.set(basura);
            Tipo tipo = tipoDeLaDiscrepancia();
            assertThat(tipo).as("cuerpo «%s»", basura).isIn(Tipo.RESPUESTA_INVALIDA, Tipo.FIRMA_INVALIDA);
        }
    }

    @Test
    @DisplayName("limite: la misma respuesta hostil produce siempre la misma huella")
    void laHuellaEsEstable() throws Exception {
        cuerpo.set(sobre("AAAA", "00".repeat(32)));
        String una = huella();
        String otra = huella();
        assertThat(una).isEqualTo(otra);
        cuerpo.set(sobre("BBBB", "00".repeat(32)));
        assertThat(huella()).isNotEqualTo(una);
    }

    private String huella() {
        try {
            recargas.consultar(referencia);
        } catch (DiscrepanciaDelProveedor d) {
            return d.huella();
        }
        throw new AssertionError("debio lanzar una discrepancia");
    }

    @Test
    @DisplayName("desconocido: 5xx, cuerpo desmesurado o servidor caido no son discrepancia sino resultado desconocido")
    void loQueNoLlegaNoEsEvidencia() throws Exception {
        estadoHttp.set(503);
        assertThatThrownBy(() -> recargas.consultar(referencia))
                .isInstanceOf(ErrorDeNegocio.class)
                .isNotInstanceOf(DiscrepanciaDelProveedor.class);

        estadoHttp.set(200);
        cuerpo.set("x".repeat(20_000));
        assertThatThrownBy(() -> recargas.consultar(referencia))
                .isInstanceOf(ErrorDeNegocio.class)
                .isNotInstanceOf(DiscrepanciaDelProveedor.class);

        server.stop(0);
        assertThatThrownBy(() -> recargas.consultar(referencia))
                .isInstanceOf(ErrorDeNegocio.class)
                .isNotInstanceOf(DiscrepanciaDelProveedor.class);
        assertThatThrownBy(() -> retiros.instruir(referencia, Dinero.de("10.00", Moneda.BOB)))
                .isInstanceOf(ErrorDeNegocio.class)
                .isNotInstanceOf(DiscrepanciaDelProveedor.class);
    }

    @Test
    @DisplayName("un 404 significa «no conozco esa referencia»: los retiros lo ven vacio, las recargas no confirman")
    void referenciaDesconocidaPorElProveedor() {
        estadoHttp.set(404);
        cuerpo.set("{\"codigo\":\"OPERACION_NO_ENCONTRADA\"}");
        assertThat(retiros.consultar(referencia)).isEmpty();
        assertThatThrownBy(() -> recargas.consultar(referencia)).isInstanceOf(ErrorDeNegocio.class);
    }
}
