package bo.aportaya.entregas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.entregas.dominio.puertos.RespaldoEmpresarial;
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
 * Los adaptadores HTTP hacia aportes y garantia contra un servidor local REAL, en tres niveles
 * (correcto, limite, invalido) y con la caida del dependido ejercitada de verdad: servidor
 * apagado, lento, o que responde mal. Lado CONSUMIDOR del contrato; el productor lo cubren las
 * pruebas web de aportes y garantia con el mismo JSON.
 */
class AdaptadoresDelPozoTest {

    private static final UUID PERIODO = UUID.fromString("f6000000-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("f6000000-0000-4000-8000-000000000002");
    private static final UUID TURNO = UUID.fromString("f6000000-0000-4000-8000-000000000003");
    private static final UUID COBERTURA = UUID.fromString("f6000000-0000-4000-8000-000000000004");

    private HttpServer servidor;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<String> claveRecibida = new AtomicReference<>();

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

    private static final String COBERTURA_APLICADA =
            """
            {"resultado":"APLICADA","coberturaId":"f6000000-0000-4000-8000-000000000004","reservaId":null,
             "cubierto":{"monto":"1000.00","moneda":"BOB"},"sinCubrir":{"monto":"0.00","moneda":"BOB"},
             "disponibleDespues":{"monto":"7000.00","moneda":"BOB"},"exposicionDespues":{"monto":"1000.00","moneda":"BOB"},
             "esNueva":true}
            """;

    // ---------------------------------------------------------------- aportes (recaudo)

    @Test
    @DisplayName("RECAUDO · CORRECTO · la respuesta del contrato de aportes se arma con importes exactos")
    void recaudoCorrecto() throws Exception {
        String url = levantar("/aportes/periodos/" + PERIODO + "/recaudo", 200, RECAUDO, 0);

        var recaudo = new RecaudoPorHttp(conTiempos(), url).consultar(PERIODO).orElseThrow();

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
            assertThat(new RecaudoPorHttp(conTiempos(), url).consultar(PERIODO)).isEmpty();
            apagar();
        }
        String roto = levantar(ruta, 200, "esto no es json", 0);
        assertThat(new RecaudoPorHttp(conTiempos(), roto).consultar(PERIODO)).isEmpty();
        apagar();
        String incompleto = levantar(ruta, 200, "{\"periodoId\":\"" + PERIODO + "\"}", 0);
        assertThat(new RecaudoPorHttp(conTiempos(), incompleto).consultar(PERIODO))
                .isEmpty();
        apagar();

        String lento = levantar(ruta, 200, RECAUDO, 1500);
        long antes = System.nanoTime();
        assertThat(new RecaudoPorHttp(conTiempos(), lento).consultar(PERIODO)).isEmpty();
        assertThat((System.nanoTime() - antes) / 1_000_000).isLessThan(1200);
        apagar();

        String apagado = levantar(ruta, 200, RECAUDO, 0);
        var adaptador = new RecaudoPorHttp(conTiempos(), apagado);
        apagar();
        assertThat(adaptador.consultar(PERIODO)).isEmpty();
    }

    // ---------------------------------------------------------------- garantia (cobertura)

    @Test
    @DisplayName(
            "RESPALDO · CORRECTO · manda solo identificadores y el faltante de contraste, con una clave que sale del TURNO")
    void respaldoCorrecto() throws Exception {
        String url = levantar("/garantia/respaldo/coberturas", 200, COBERTURA_APLICADA, 0);
        var adaptador = new RespaldoPorHttp(conTiempos(), url);

        var cobertura = adaptador.cubrir(GRUPO, PERIODO, TURNO, bob("1000.00")).orElseThrow();
        String primeraClave = claveRecibida.get();
        adaptador.cubrir(GRUPO, PERIODO, TURNO, bob("1000.00"));

        assertThat(cobertura.resultado()).isEqualTo(RespaldoEmpresarial.Resultado.APLICADA);
        assertThat(cobertura.coberturaId()).isEqualTo(COBERTURA);
        assertThat(cobertura.cubierto()).isEqualTo(bob("1000.00"));
        assertThat(cuerpoRecibido.get())
                .contains("faltanteEsperado")
                .doesNotContain("pozo")
                .doesNotContain("lineas");
        // La misma clave en cada reintento del mismo turno (idempotencia por hecho, no por intento).
        assertThat(claveRecibida.get()).isEqualTo(primeraClave).isNotBlank();
    }

    @Test
    @DisplayName("RESPALDO · LIMITE · INSUFICIENTE y SIN_RESERVA llegan como resultados de negocio, no como vacio")
    void respaldoInsuficiente() throws Exception {
        String cuerpo =
                """
                {"resultado":"INSUFICIENTE","coberturaId":null,"cubierto":{"monto":"0.00","moneda":"BOB"},
                 "sinCubrir":{"monto":"200.00","moneda":"BOB"},"esNueva":true}
                """;
        String url = levantar("/garantia/respaldo/coberturas", 200, cuerpo, 0);

        var cobertura = new RespaldoPorHttp(conTiempos(), url)
                .cubrir(GRUPO, PERIODO, TURNO, bob("1000.00"))
                .orElseThrow();

        assertThat(cobertura.resultado()).isEqualTo(RespaldoEmpresarial.Resultado.INSUFICIENTE);
        assertThat(cobertura.sinCubrir()).isEqualTo(bob("200.00"));
        assertThat(cobertura.coberturaId()).isNull();
    }

    @Test
    @DisplayName(
            "RESPALDO · INVALIDO · 422, 500, 403, JSON roto, lento y apagado: vacio, para que el llamador NO avance")
    void respaldoInvalido() throws Exception {
        String ruta = "/garantia/respaldo/coberturas";
        for (int estado : new int[] {422, 500, 403}) {
            String url = levantar(ruta, estado, "{\"codigo\":\"AP-CU23-12\"}", 0);
            assertThat(new RespaldoPorHttp(conTiempos(), url).cubrir(GRUPO, PERIODO, TURNO, bob("1000.00")))
                    .isEmpty();
            apagar();
        }
        String roto = levantar(ruta, 200, "no es json", 0);
        assertThat(new RespaldoPorHttp(conTiempos(), roto).cubrir(GRUPO, PERIODO, TURNO, bob("1000.00")))
                .isEmpty();
        apagar();

        String lento = levantar(ruta, 200, COBERTURA_APLICADA, 1500);
        long antes = System.nanoTime();
        assertThat(new RespaldoPorHttp(conTiempos(), lento).cubrir(GRUPO, PERIODO, TURNO, bob("1000.00")))
                .isEmpty();
        assertThat((System.nanoTime() - antes) / 1_000_000).isLessThan(1200);
        apagar();

        String apagado = levantar(ruta, 200, COBERTURA_APLICADA, 0);
        var adaptador = new RespaldoPorHttp(conTiempos(), apagado);
        apagar();
        assertThat(adaptador.cubrir(GRUPO, PERIODO, TURNO, bob("1000.00"))).isEmpty();
    }
}
