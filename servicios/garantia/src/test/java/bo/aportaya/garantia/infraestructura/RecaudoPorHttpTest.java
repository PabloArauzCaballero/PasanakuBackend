package bo.aportaya.garantia.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * El adaptador HTTP hacia aportes contra un servidor local REAL (no un mock del cliente), en
 * tres niveles: correcto, limite e invalido. Aca se prueba el lado CONSUMIDOR del contrato; el
 * lado productor lo cubre {@code RecaudoWebTest} de aportes con el mismo JSON.
 */
class RecaudoPorHttpTest {

    private static final UUID PERIODO = UUID.fromString("e5000000-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("e5000000-0000-4000-8000-000000000002");
    private static final UUID OBLIGACION = UUID.fromString("e5000000-0000-4000-8000-000000000003");

    private HttpServer servidor;
    private final java.util.concurrent.atomic.AtomicReference<String> autorizacion =
            new java.util.concurrent.atomic.AtomicReference<>();
    private static final bo.aportaya.plataforma.web.clientes.CredencialDeServicio SERVICIO =
            bo.aportaya.plataforma.web.clientes.CredencialDeServicio.fija("token-de-servicio-garantia");

    @AfterEach
    void apagar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private RecaudoPorHttp contra(int estado, String cuerpo, long demoraMs) throws IOException {
        return contra(estado, cuerpo, demoraMs, SERVICIO);
    }

    private RecaudoPorHttp contra(
            int estado,
            String cuerpo,
            long demoraMs,
            bo.aportaya.plataforma.web.clientes.CredencialDeServicio credencial)
            throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/aportes/periodos/" + PERIODO + "/recaudo", intercambio -> {
            autorizacion.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                intercambio.getResponseBody().write(bytes);
            }
            intercambio.close();
        });
        servidor.start();
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(500);
        fabrica.setReadTimeout(400);
        return new RecaudoPorHttp(
                RestClient.builder().requestFactory(fabrica),
                "http://127.0.0.1:" + servidor.getAddress().getPort(),
                credencial);
    }

    private static final String VALIDO =
            """
            {"periodoId":"e5000000-0000-4000-8000-000000000001","grupoId":"e5000000-0000-4000-8000-000000000002",
             "corteEn":"2026-10-08T12:00:00Z",
             "pozo":{"monto":"6000.00","moneda":"BOB"},"confirmado":{"monto":"5000.00","moneda":"BOB"},
             "cubiertoMutual":{"monto":"0.00","moneda":"BOB"},"faltante":{"monto":"1000.00","moneda":"BOB"},
             "pendientes":[{"obligacionId":"e5000000-0000-4000-8000-000000000003","monto":{"monto":"1000.00","moneda":"BOB"}}]}
            """;

    @Test
    @DisplayName(
            "CORRECTO · Dada la respuesta del contrato de aportes · Cuando se consulta · Entonces se arma el recaudo con los importes exactos")
    void correcto() throws Exception {
        var recaudo = contra(200, VALIDO, 0).consultar(PERIODO).orElseThrow();

        assertThat(recaudo.grupoId()).isEqualTo(GRUPO);
        assertThat(recaudo.pozo().toString()).isEqualTo("6000.00");
        assertThat(recaudo.confirmado().toString()).isEqualTo("5000.00");
        assertThat(recaudo.pendientes()).hasSize(1);
        assertThat(recaudo.pendientes().get(0).obligacionId()).isEqualTo(OBLIGACION);
    }

    @Test
    @DisplayName(
            "LIMITE · Dado un pozo completo sin pendientes · Cuando se consulta · Entonces la lista de pendientes llega vacia")
    void sinPendientes() throws Exception {
        String completo = VALIDO.replace("\"monto\":\"5000.00\"", "\"monto\":\"6000.00\"")
                .replaceAll("\"pendientes\":\\[.*\\]\\}", "\"pendientes\":[]}");

        var recaudo = contra(200, completo, 0).consultar(PERIODO).orElseThrow();

        assertThat(recaudo.pendientes()).isEmpty();
    }

    @Test
    @DisplayName(
            "INVALIDO · Dado que aportes responde 500, 404, 403 o JSON roto · Cuando se consulta · Entonces vacio: quien pregunta deniega")
    void respuestasMalas() throws Exception {
        assertThat(contra(500, "{}", 0).consultar(PERIODO)).isEmpty();
        apagar();
        assertThat(contra(404, "{}", 0).consultar(PERIODO)).isEmpty();
        apagar();
        assertThat(contra(403, "{}", 0).consultar(PERIODO)).isEmpty();
        apagar();
        assertThat(contra(200, "no es json", 0).consultar(PERIODO)).isEmpty();
        apagar();
        assertThat(contra(200, "{\"periodoId\":\"" + PERIODO + "\"}", 0).consultar(PERIODO))
                .isEmpty();
    }

    @Test
    @DisplayName(
            "INVALIDO · Dado que aportes tarda mas que el timeout · Cuando se consulta · Entonces vacio y no cuelga el hilo")
    void timeout() throws Exception {
        long antes = System.nanoTime();

        var recaudo = contra(200, VALIDO, 1500).consultar(PERIODO);

        assertThat(recaudo).isEmpty();
        assertThat((System.nanoTime() - antes) / 1_000_000).isLessThan(1200);
    }

    @Test
    @DisplayName("INVALIDO · Dado que aportes esta apagado (nadie escucha) · Cuando se consulta · Entonces vacio")
    void apagado() throws Exception {
        var adaptador = contra(200, VALIDO, 0);
        servidor.stop(0);
        servidor = null;

        assertThat(adaptador.consultar(PERIODO)).isEmpty();
    }

    @Test
    @DisplayName("F1.L1.M07 · la pregunta viaja con el token de la identidad de servicio de garantia")
    void conIdentidadDeServicio() throws Exception {
        assertThat(contra(200, VALIDO, 0).consultar(PERIODO)).isPresent();
        assertThat(autorizacion.get()).isEqualTo("Bearer token-de-servicio-garantia");
    }

    @Test
    @DisplayName("F1.L1.M07 · sin credencial de servicio no sale la peticion: vacio, falla cerrado")
    void sinCredencial() throws Exception {
        var sinCredencial = contra(200, VALIDO, 0, bo.aportaya.plataforma.web.clientes.CredencialDeServicio.ninguna());
        assertThat(sinCredencial.consultar(PERIODO)).isEmpty();
        assertThat(autorizacion.get()).isNull();
    }
}
