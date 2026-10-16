package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import bo.aportaya.identidad.infraestructura.GmailApiCorreoDeVerificacion;
import java.time.Duration;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Comprueba el intercambio OAuth, el envio Gmail y el MIME HTML que recibe la persona. */
class GmailApiCorreoDeVerificacionTest {

    @Test
    @DisplayName("renueva OAuth y envia por Gmail API el codigo dentro del HTML animado")
    void enviaMensajeRealDeGmail() {
        RestClient.Builder constructor = RestClient.builder();
        MockRestServiceServer servidor =
                MockRestServiceServer.bindTo(constructor).build();
        servidor.expect(once(), requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=refresh_token")))
                .andRespond(withSuccess("{\"access_token\":\"acceso-prueba\"}", MediaType.APPLICATION_JSON));
        servidor.expect(once(), requestTo("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer acceso-prueba"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"raw\":")))
                .andRespond(withSuccess("{\"id\":\"mensaje-1\"}", MediaType.APPLICATION_JSON));

        var gmail =
                new GmailApiCorreoDeVerificacion(constructor, "cliente", "secreto", "refresco", "cuentas@aportaya.bo");
        gmail.enviarCodigo("ana@example.com", "482019", Duration.ofMinutes(10));

        servidor.verify();
    }

    @Test
    @DisplayName("el HTML muestra seis celdas, vigencia y animacion con fallback estatico")
    void plantilla() {
        String html = bo.aportaya.identidad.infraestructura.PlantillaCorreoDeVerificacion.html(
                "482019", Duration.ofMinutes(10));

        assertThat(html)
                .contains(
                        "482019".chars().mapToObj(c -> ">" + (char) c + "</td>").toArray(String[]::new))
                .contains("@keyframes flip", "10 minutos", "Nunca compartas este codigo")
                .doesNotContain("script");
        assertThat(Pattern.compile("class=\\\"digit\\\"").matcher(html).results())
                .hasSize(6);
    }
}
