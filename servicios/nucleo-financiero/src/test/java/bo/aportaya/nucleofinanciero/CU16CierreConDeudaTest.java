package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU16CerrarBilletera.EntradaCierre;
import bo.aportaya.nucleofinanciero.infraestructura.clientes.HechosPorHttp;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

/**
 * Carril Q · CU-16 contra el cliente REAL de {@code aportes}/{@code grupos} ({@code HechosPorHttp}) y la base real: con una
 * deuda abierta la billetera NO se cierra (AP-CU16-01). Antes el cliente preguntaba a {@code aportes} con el usuarioId y
 * siempre recibia «sin obligaciones»: se cerraba la billetera de quien debia. Los dos servicios remotos son dobles de
 * contrato (rutas y campos de su OpenAPI); NO esta cubierto el proceso real de cada uno.
 */
class CU16CierreConDeudaTest extends BaseDeBilletera {

    private HttpServer server;
    private final AtomicReference<String> estadoDeAportes = new AtomicReference<>();
    private final AtomicInteger codigoDeAportes = new AtomicInteger(200);
    private HechosPorHttp hechos;
    private UUID participante;

    @BeforeEach
    void iniciar() throws IOException {
        participante = UUID.randomUUID();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String ruta = exchange.getRequestURI().getPath();
            boolean deGrupos = ruta.equals("/grupos/participaciones");
            String cuerpo = deGrupos
                    ? "[{\"grupoId\":\"%s\",\"participanteId\":\"%s\",\"estado\":\"ACTIVO\"}]"
                            .formatted(UUID.randomUUID(), participante)
                    : estadoDeAportes.get();
            byte[] raw = cuerpo.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(deGrupos ? 200 : codigoDeAportes.get(), raw.length);
            try (var salida = exchange.getResponseBody()) {
                salida.write(raw);
            }
        });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        hechos = new HechosPorHttp(RestClient.builder(), url, url);
    }

    @AfterEach
    void cerrar() {
        SecurityContextHolder.clearContext();
        server.stop(0);
        fixtura.limpiarBilleteras();
    }

    private record Caso(UUID usuario, UUID cuenta) {}

    private Caso titular() {
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("t")
                        .header("alg", "none")
                        .subject(usuario.toString())
                        .build()));
        return new Caso(usuario, cuenta);
    }

    /** Lo que hace {@code MovimientosDeLaBilletera.cerrar}: pregunta fuera de la transaccion y despues solicita. */
    private void solicitarElCierre(Caso c) {
        var ctx = contextoDe(c.usuario());
        boolean debe = hechos.tieneObligacionesAbiertas(c.usuario());
        boolean enGrupo = false;
        transaccion.execute(
                t -> cierreCU.solicitar(new EntradaCierre(c.cuenta(), "Ya no la uso", "RETIRO", debe, enGrupo), ctx));
    }

    private static String pagos(int abiertas, String deuda) {
        return "{\"alDia\":%s,\"totalAportado\":\"0.00\",\"deudaVigente\":\"%s\",\"porAportar\":\"0.00\",\"obligacionesAbiertas\":%d,\"moneda\":\"BOB\"}"
                .formatted(abiertas == 0, deuda, abiertas);
    }

    @Test
    @DisplayName("rechaza el cierre con una deuda abierta: aportes informa una obligacion en mora de la participacion de la"
            + " persona · AP-CU16-01 y la solicitud no se crea")
    void rechazaElCierreConDeudaAbierta() {
        var c = titular();
        estadoDeAportes.set(pagos(1, "500.00"));

        assertThatThrownBy(() -> solicitarElCierre(c))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(((ErrorDeNegocio) x).codigo().valor()).isEqualTo("AP-CU16-01"));
        assertThat(contar("SELECT count(*) FROM nucleo_financiero.solicitud_cierre_billetera WHERE cuenta_billetera_id = ?", c.cuenta()))
                .isZero();
    }

    @Test
    @DisplayName("rechaza el cierre si aportes no contesta: no saber es «debe» (cerrar le pasaria la deuda a los otros del"
            + " grupo) · AP-CU16-01")
    void rechazaElCierreSiAportesEstaCaido() {
        var c = titular();
        estadoDeAportes.set(pagos(0, "0.00"));
        codigoDeAportes.set(500);

        assertThatThrownBy(() -> solicitarElCierre(c))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(((ErrorDeNegocio) x).codigo().valor()).isEqualTo("AP-CU16-01"));
    }

    @Test
    @DisplayName("Dado un titular al dia en todas sus participaciones · Cuando solicita el cierre · Entonces la solicitud se"
            + " crea (la puerta de aportes se abre solo con el hecho, no por omision)")
    void sinDeudaSeSolicita() {
        var c = titular();
        estadoDeAportes.set(pagos(0, "0.00"));

        solicitarElCierre(c);

        assertThat(contar("SELECT count(*) FROM nucleo_financiero.solicitud_cierre_billetera WHERE cuenta_billetera_id = ?", c.cuenta()))
                .isEqualTo(1);
    }
}
