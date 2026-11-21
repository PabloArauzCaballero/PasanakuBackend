package bo.aportaya.entregas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * El adaptador HTTP hacia aportes contra un servidor local REAL, en tres niveles (correcto, limite,
 * invalido) y con la caida del dependido ejercitada de verdad: servidor apagado, lento, o que
 * responde mal. Lado CONSUMIDOR del contrato; el productor lo cubren las pruebas web de aportes con
 * el mismo JSON. El adaptador hacia el respaldo de garantia se retiro con el respaldo (F2.L2.M01).
 */
class AdaptadoresDelPozoTest {

    /** F1.L1.M07: el recaudo se pregunta con la identidad de servicio de entregas. */
    private static final bo.aportaya.plataforma.web.clientes.CredencialDeServicio SERVICIO =
            bo.aportaya.plataforma.web.clientes.CredencialDeServicio.fija("token-de-servicio-entregas");

    private static final UUID PERIODO = UUID.fromString("f6000000-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("f6000000-0000-4000-8000-000000000002");

    private HttpServer servidor;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<String> claveRecibida = new AtomicReference<>();
    private final AtomicReference<String> autorizacionRecibida = new AtomicReference<>();

    @AfterEach
    void apagar() {
        if (servidor != null) {
            servidor.stop(0);
            servidor = null;
        }
    }

    private String levantar(String ruta, int estado, String cuerpo, long demoraMs) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext(ruta, intercambio -> {
            cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            claveRecibida.set(intercambio.getRequestHeaders().getFirst("Idempotency-Key"));
            autorizacionRecibida.set(intercambio.getRequestHeaders().getFirst("Authorization"));
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
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private static RestClient.Builder conTiempos() {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(500);
        fabrica.setReadTimeout(400);
        return RestClient.builder().requestFactory(fabrica);
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static final String RECAUDO =
            """
            {"periodoId":"f6000000-0000-4000-8000-000000000001","grupoId":"f6000000-0000-4000-8000-000000000002",
             "corteEn":"2026-10-08T12:00:00Z",
             "pozo":{"monto":"6000.00","moneda":"BOB"},"confirmado":{"monto":"5000.00","moneda":"BOB"},
             "cubiertoMutual":{"monto":"0.00","moneda":"BOB"},"faltante":{"monto":"1000.00","moneda":"BOB"},
             "pendientes":[]}
            """;

    // ---------------------------------------------------------------- aportes (recaudo)

    @Test
    @DisplayName("RECAUDO · F1.L1.M07 · la pregunta viaja con el token de la identidad de servicio de entregas")
    void recaudoConIdentidadDeServicio() throws Exception {
        String url = levantar("/aportes/periodos/" + PERIODO + "/recaudo", 200, RECAUDO, 0);

        assertThat(new RecaudoPorHttp(conTiempos(), url, SERVICIO).consultar(PERIODO))
                .isPresent();
        assertThat(autorizacionRecibida.get()).isEqualTo("Bearer token-de-servicio-entregas");
    }

    @Test
    @DisplayName("RECAUDO · F1.L1.M07 · sin credencial de servicio no sale la peticion: vacio y la entrega no avanza")
    void recaudoSinCredencial() throws Exception {
        String url = levantar("/aportes/periodos/" + PERIODO + "/recaudo", 200, RECAUDO, 0);

        var sinCredencial = new RecaudoPorHttp(
                conTiempos(), url, bo.aportaya.plataforma.web.clientes.CredencialDeServicio.ninguna());
        assertThat(sinCredencial.consultar(PERIODO)).isEmpty();
        assertThat(autorizacionRecibida.get()).isNull();
    }

    @Test
    @DisplayName("RECAUDO · CORRECTO · la respuesta del contrato de aportes se arma con importes exactos")
    void recaudoCorrecto() throws Exception {
        String url = levantar("/aportes/periodos/" + PERIODO + "/recaudo", 200, RECAUDO, 0);

        var recaudo = new RecaudoPorHttp(conTiempos(), url, SERVICIO)
                .consultar(PERIODO)
                .orElseThrow();

        assertThat(recaudo.grupoId()).isEqualTo(GRUPO);
        assertThat(recaudo.pozo()).isEqualTo(bob("6000.00"));
        assertThat(recaudo.faltante()).isEqualTo(bob("1000.00"));
    }

    @Test
    @DisplayName("RECAUDO · INVALIDO · 500, 404, 403, JSON roto, campos faltantes, lento y apagado: todo es vacio")
    void recaudoInvalido() throws Exception {
        String ruta = "/aportes/periodos/" + PERIODO + "/recaudo";
        for (int estado : new int[] {500, 404, 403}) {
            String url = levantar(ruta, estado, "{}", 0);
            assertThat(new RecaudoPorHttp(conTiempos(), url, SERVICIO).consultar(PERIODO))
                    .isEmpty();
            apagar();
        }
        String roto = levantar(ruta, 200, "esto no es json", 0);
        assertThat(new RecaudoPorHttp(conTiempos(), roto, SERVICIO).consultar(PERIODO))
                .isEmpty();
        apagar();
        String incompleto = levantar(ruta, 200, "{\"periodoId\":\"" + PERIODO + "\"}", 0);
        assertThat(new RecaudoPorHttp(conTiempos(), incompleto, SERVICIO).consultar(PERIODO))
                .isEmpty();
        apagar();

        String lento = levantar(ruta, 200, RECAUDO, 1500);
        long antes = System.nanoTime();
        assertThat(new RecaudoPorHttp(conTiempos(), lento, SERVICIO).consultar(PERIODO))
                .isEmpty();
        assertThat((System.nanoTime() - antes) / 1_000_000).isLessThan(1200);
        apagar();

        String apagado = levantar(ruta, 200, RECAUDO, 0);
        var adaptador = new RecaudoPorHttp(conTiempos(), apagado, SERVICIO);
        apagar();
        assertThat(adaptador.consultar(PERIODO)).isEmpty();
    }

    // ---------------------------------------------------------------- garantia (cobertura)
}
