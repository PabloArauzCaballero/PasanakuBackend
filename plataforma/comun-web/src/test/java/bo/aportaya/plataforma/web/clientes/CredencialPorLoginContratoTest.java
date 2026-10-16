package bo.aportaya.plataforma.web.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato del CONSUMIDOR de identidad ({@code POST /sesiones}, CU-04) en tres niveles. El servidor es un doble
 * declarado (HTTP real del JDK) con la forma publicada en {@code identidad.yaml}: la prueba contra el servidor real
 * de identidad no existe porque identidad no valida un segundo factor real fuera del perfil local.
 */
class CredencialPorLoginContratoTest {

    private static final String TELEFONO = "+59170000000";
    private static final String CLAVE = "clave-de-prueba-de-esta-clase";
    private static final Instant T0 = Instant.parse("2026-10-09T12:00:00Z");

    private record Pedido(String metodo, String ruta, String cuerpo) {}

    private HttpServer identidad;
    private final List<Pedido> recibidos = new CopyOnWriteArrayList<>();
    private volatile int estado = 200;
    private volatile String respuesta;
    private volatile long demoraMs;
    private final Reloj reloj = new Reloj(T0);

    /** Un reloj que se mueve cuando la prueba lo dice. */
    private static final class Reloj extends Clock {
        private volatile Instant ahora;

        Reloj(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    /** Un JWT sin firma valida: solo el {@code exp} importa a quien lo recibe; lo verifica el servicio destino. */
    private static String jwt(Instant vence) {
        var b64 = Base64.getUrlEncoder().withoutPadding();
        String cabecera = b64.encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8));
        String carga = b64.encodeToString(
                ("{\"sub\":\"servicio\",\"exp\":" + vence.getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8));
        return cabecera + "." + carga + ".firma";
    }

    private String sesionAbierta(Instant vence) {
        return "{\"sesionId\":\"11111111-1111-4111-8111-111111111111\",\"requiereFactorAdicional\":false,"
                + "\"dispositivoConfiable\":false,\"tokenAcceso\":\"" + jwt(vence) + "\"}";
    }

    @BeforeEach
    void levantar() throws IOException {
        identidad = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        identidad.createContext("/", this::atender);
        identidad.start();
        respuesta = sesionAbierta(T0.plus(Duration.ofMinutes(15)));
    }

    @AfterEach
    void bajar() {
        identidad.stop(0);
    }

    private void atender(HttpExchange e) throws IOException {
        recibidos.add(new Pedido(
                e.getRequestMethod(),
                e.getRequestURI().getPath(),
                new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
        try {
            Thread.sleep(demoraMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().add("Content-Type", "application/json");
        e.sendResponseHeaders(estado, bytes.length);
        e.getResponseBody().write(bytes);
        e.close();
    }

    private CredencialPorLogin credencial(java.util.function.Function<Instant, String> factor) {
        return new CredencialPorLogin(
                URI.create("http://127.0.0.1:" + identidad.getAddress().getPort()),
                Duration.ofSeconds(1),
                "pruebas",
                TELEFONO,
                CLAVE,
                factor,
                reloj);
    }

    @Test
    @DisplayName("correcto · entra por POST /sesiones con telefono, clave y factor TOTP, y devuelve el token de acceso")
    void ingresaYDevuelveElToken() {
        var c = credencial(CredencialPorLogin.fijo("123456"));

        var token = c.vigente();

        assertThat(token).isPresent();
        assertThat(token.get()).isEqualTo(jwt(T0.plus(Duration.ofMinutes(15))));
        assertThat(recibidos).hasSize(1);
        assertThat(recibidos.get(0).metodo()).isEqualTo("POST");
        assertThat(recibidos.get(0).ruta()).isEqualTo("/sesiones");
        assertThat(recibidos.get(0).cuerpo())
                .contains("\"telefonoE164\":\"" + TELEFONO + "\"")
                .contains("\"tipo\":\"TOTP\"")
                .contains("\"valor\":\"123456\"")
                .contains("\"huellaDispositivo\"");
    }

    @Test
    @DisplayName("correcto · el factor es el TOTP del instante (RFC 6238), no un valor guardado")
    void elFactorEsElTotpDelInstante() {
        // Semilla de los vectores de RFC 6238 ("12345678901234567890" en Base32).
        var c = credencial(CredencialPorLogin.totp("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"));

        c.vigente();

        String esperado = CodigoTotp.de("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", T0);
        assertThat(recibidos.get(0).cuerpo()).contains("\"valor\":\"" + esperado + "\"");
    }

    @Test
    @DisplayName(
            "limite · mientras el token no esta por vencer se reutiliza; falta el margen y se renueva una sola vez")
    void seRenuevaAntesDeVencer() {
        var c = credencial(CredencialPorLogin.fijo("000000"));
        c.vigente();
        reloj.avanzar(Duration.ofMinutes(10));
        c.vigente();
        c.vigente();
        assertThat(recibidos).as("a los 10 min todavia vale").hasSize(1);

        respuesta = sesionAbierta(T0.plus(Duration.ofMinutes(30)));
        reloj.avanzar(Duration.ofMinutes(4).plusSeconds(10)); // faltan menos de 90 s para el exp de los 15 min
        var renovado = c.vigente();
        c.vigente();

        assertThat(recibidos).as("renovo una sola vez").hasSize(2);
        assertThat(renovado.orElseThrow()).isEqualTo(jwt(T0.plus(Duration.ofMinutes(30))));
    }

    @Test
    @DisplayName(
            "rechaza · con credencial rechazada (422) no hay token y NO se vuelve a intentar antes de la espera: cada fallo cuenta para el bloqueo de la cuenta")
    void rechazadaNoReintentaEnBucle() {
        estado = 422;
        respuesta = "{\"codigo\":\"AP-CU04-01\",\"mensaje\":\"El telefono o la credencial no coinciden.\"}";
        var c = credencial(CredencialPorLogin.fijo("000000"));

        assertThat(c.vigente()).isEmpty();
        assertThat(c.vigente()).isEmpty();
        assertThat(c.vigente()).isEmpty();
        assertThat(recibidos).as("un solo intento dentro de la espera").hasSize(1);

        reloj.avanzar(CredencialPorLogin.ESPERA_TRAS_FALLO.plusSeconds(1));
        estado = 200;
        respuesta = sesionAbierta(reloj.instant().plus(Duration.ofMinutes(15)));
        assertThat(c.vigente()).as("pasada la espera vuelve a intentar y entra").isPresent();
        assertThat(recibidos).hasSize(2);
    }

    @Test
    @DisplayName("rechaza · si identidad pide un factor adicional (sin tokenAcceso) no hay credencial")
    void sinTokenNoHayCredencial() {
        respuesta = "{\"requiereFactorAdicional\":true,\"dispositivoConfiable\":false}";
        assertThat(credencial(CredencialPorLogin.fijo("000000")).vigente()).isEmpty();
    }

    @Test
    @DisplayName("rechaza · identidad caida (5xx) o que responde con basura: vacio, sin excepcion")
    void identidadCaida() {
        estado = 503;
        respuesta = "no es json";
        assertThat(credencial(CredencialPorLogin.fijo("000000")).vigente()).isEmpty();

        estado = 200;
        respuesta = "no es json";
        reloj.avanzar(Duration.ofMinutes(1));
        assertThat(credencial(CredencialPorLogin.fijo("000000")).vigente()).isEmpty();
    }

    @Test
    @DisplayName("rechaza · identidad que no contesta a tiempo: vacio (timeout explicito), no cuelga")
    void identidadLenta() {
        demoraMs = 2500;
        long antes = System.nanoTime();
        assertThat(credencial(CredencialPorLogin.fijo("000000")).vigente()).isEmpty();
        assertThat(Duration.ofNanos(System.nanoTime() - antes)).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    @DisplayName(
            "limite · un token sin exp o ilegible vence a los 5 minutos por omision, no queda vigente para siempre")
    void tokenSinExp() {
        assertThat(CredencialPorLogin.vencimientoDe("a.b.c", T0)).isEqualTo(T0.plus(Duration.ofMinutes(5)));
        assertThat(CredencialPorLogin.vencimientoDe("sin-puntos", T0)).isEqualTo(T0.plus(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName(
            "rechaza · una cuenta de servicio configurada a medias no arranca (no degrada en silencio a sin credencial)")
    void configuracionAMedias() {
        var uri = URI.create("http://identidad:8080");
        assertThatThrownBy(() -> CredencialDeServicioConfigurada.de(
                        "pruebas", "PRUEBAS", uri, Duration.ofSeconds(1), "", TELEFONO, "", "", ""))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CredencialDeServicioConfigurada.de(
                        "pruebas", "PRUEBAS", uri, Duration.ofSeconds(1), "", TELEFONO, CLAVE, "", ""))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CredencialDeServicioConfigurada.de(
                        "pruebas", "PRUEBAS", uri, Duration.ofSeconds(1), "", "", "", "SEMILLA", ""))
                .isInstanceOf(IllegalStateException.class);
        assertThat(CredencialDeServicioConfigurada.de(
                                "pruebas", "PRUEBAS", uri, Duration.ofSeconds(1), "", "", "", "", "")
                        .vigente())
                .as("sin nada configurado: vacio")
                .isEmpty();
        assertThat(CredencialDeServicioConfigurada.de(
                                "pruebas", "PRUEBAS", uri, Duration.ofSeconds(1), " token-emitido ", "", "", "", "")
                        .vigente())
                .contains("token-emitido");
        assertThat(CredencialDeServicioConfigurada.de(
                        "pruebas", "PRUEBAS", uri, Duration.ofSeconds(1), "", TELEFONO, CLAVE, "", "000000"))
                .isInstanceOf(CredencialPorLogin.class);
    }
}
