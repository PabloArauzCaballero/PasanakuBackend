package bo.aportaya.grupos.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Carril P · emitir y validar el enlace de una invitacion contra {@code identidad}.
 *
 * <p>H2 (hallado contra TEST): identidad rechazaba el canal ENLACE con 422 AP-CU69-05 y {@code grupos} lo convertia en un
 * 500 «fallo no previsto». Un 422 de identidad es una decision de negocio y llega con su codigo. H4: validar un enlace no
 * traga la falla en silencio, y el rastro es la clase y el estado HTTP, nunca el telefono ni el secreto.
 *
 * <p>Tres niveles contra un {@code identidad} de ensayo: correcto, limite (422 con codigo; 422 sin codigo legible) e
 * invalido (400, 403, 500, red caida).
 */
class HechosEmisionDeInvitacionTest {

    private static final UUID CLAVE = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID TOKEN = UUID.randomUUID();
    private static final String TELEFONO = "+59171234567";
    private static final String SECRETO = "ab".repeat(32);

    private HttpServer servidor;
    private final ListAppender<ILoggingEvent> bitacora = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(TokensDeInvitacionPorHttp.class);

    @BeforeEach
    void escuchar() {
        bitacora.start();
        logger.addAppender(bitacora);
    }

    @AfterEach
    void apagar() {
        logger.detachAppender(bitacora);
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private HechosPorHttp contra(int estado, String respuesta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/", intercambio -> {
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                intercambio.getResponseBody().write(bytes);
            }
            intercambio.close();
        });
        servidor.start();
        return conectado("http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    private HechosPorHttp conectado(String url) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(300);
        fabrica.setReadTimeout(300);
        return new HechosPorHttp(RestClient.builder().requestFactory(fabrica), url, url, url, url, url, url, url, url);
    }

    @Test
    @DisplayName("correcto: el enlace emitido llega con su identificador, su secreto y su vencimiento")
    void emiteElEnlace() throws Exception {
        var hechos = contra(
                201, "{\"tokenId\":\"" + TOKEN + "\",\"token\":\"" + SECRETO + "\",\"expiraEn\":\"2031-01-01T00:00:00Z\"}");

        var emitido = hechos.tokenDeInvitacion(CLAVE, GRUPO, "ENLACE", TELEFONO);

        assertThat(emitido.tokenId()).isEqualTo(TOKEN);
        assertThat(emitido.token()).isEqualTo(SECRETO);
    }

    @Test
    @DisplayName("limite: un 422 de identidad (canal no permitido) llega como ESE rechazo de negocio, no como un 500")
    void elRechazoDeIdentidadLlegaConSuCodigo() throws Exception {
        var hechos = contra(
                422,
                "{\"codigo\":\"AP-CU69-05\",\"mensaje\":\"No se pudo emitir esa invitacion.\",\"detalle\":{},\"trazaId\":\"x\"}");

        assertThatThrownBy(() -> hechos.tokenDeInvitacion(CLAVE, GRUPO, "ENLACE", TELEFONO))
                .isInstanceOfSatisfying(ErrorDeNegocio.class, e -> {
                    assertThat(e.codigo().valor()).isEqualTo("AP-CU69-05");
                    assertThat(e.getMessage()).contains("No se pudo emitir");
                });
    }

    @Test
    @DisplayName("limite: el tope diario de identidad (AP-CU69-08) tambien llega con su codigo y su mensaje")
    void elTopeDiarioLlegaConSuCodigo() throws Exception {
        var hechos = contra(
                422, "{\"codigo\":\"AP-CU69-08\",\"mensaje\":\"Alcanzaste el limite diario de invitaciones. Proba manana.\"}");

        assertThatThrownBy(() -> hechos.tokenDeInvitacion(CLAVE, GRUPO, "SMS", TELEFONO))
                .isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(e.codigo().valor())
                        .isEqualTo("AP-CU69-08"));
    }

    @Test
    @DisplayName("invalido: un 422 con cuerpo ilegible o sin codigo valido NO se inventa como regla de negocio")
    void unCuerpoIlegibleConservaLaFalla() throws Exception {
        for (String cuerpo : new String[] {"no es json", "{\"mensaje\":\"sin codigo\"}", "{\"codigo\":\"XX-1\"}"}) {
            var hechos = contra(422, cuerpo);

            assertThatThrownBy(() -> hechos.tokenDeInvitacion(CLAVE, GRUPO, "ENLACE", TELEFONO))
                    .isInstanceOf(HttpClientErrorException.UnprocessableEntity.class);
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("invalido: un 400, un 403 o un 500 de identidad son fallas de integracion, no reglas de negocio")
    void lasFallasDeIntegracionNoSeTraducen() throws Exception {
        for (int estado : new int[] {400, 403, 500}) {
            var hechos = contra(estado, "{\"codigo\":\"AP-CU69-05\",\"mensaje\":\"x\"}");

            assertThatThrownBy(() -> hechos.tokenDeInvitacion(CLAVE, GRUPO, "ENLACE", TELEFONO))
                    .isInstanceOf(RestClientResponseException.class)
                    .isNotInstanceOf(ErrorDeNegocio.class);
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("el rastro de un enlace que no se pudo validar lleva la clase o el estado HTTP, nunca el telefono ni el secreto")
    void validarDejaRastroSeguro() throws Exception {
        var conRespuesta = contra(500, "{\"detalle\":\"" + TELEFONO + SECRETO + "\"}");
        assertThat(conRespuesta.enlaceDeInvitacionValido(TOKEN, SECRETO, TELEFONO, "NINGUNO"))
                .isFalse();
        servidor.stop(0);
        var sinRed = conectado("http://127.0.0.1:1");
        assertThat(sinRed.enlaceDeInvitacionValido(TOKEN, SECRETO, TELEFONO, "NINGUNO"))
                .isFalse();

        var mensajes = bitacora.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(mensajes).hasSize(2);
        assertThat(mensajes.get(0)).contains("HTTP 500");
        assertThat(mensajes.get(1)).containsAnyOf("ResourceAccessException", "Exception");
        assertThat(String.join("|", mensajes)).doesNotContain(TELEFONO).doesNotContain(SECRETO);
    }

    @Test
    @DisplayName("un enlace valido no deja rastro de falla")
    void validarCorrectoNoLoguea() throws Exception {
        var hechos = contra(200, "{\"valido\":true}");

        assertThat(hechos.enlaceDeInvitacionValido(TOKEN, SECRETO, TELEFONO, "NINGUNO"))
                .isTrue();
        assertThat(bitacora.list).isEmpty();
    }
}
