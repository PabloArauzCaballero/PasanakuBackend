package bo.aportaya.identidad.infraestructura;

import bo.aportaya.identidad.dominio.puertos.CorreoDeVerificacion;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/** Envio real mediante OAuth 2 y {@code users.messages.send} de Gmail API. */
@Component
@ConditionalOnProperty(name = "aportaya.correo.proveedor", havingValue = "gmail_api")
public class GmailApiCorreoDeVerificacion implements CorreoDeVerificacion {

    private final RestClient rest;
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String remitente;

    public GmailApiCorreoDeVerificacion(
            RestClient.Builder constructor,
            @Value("${aportaya.correo.gmail.client-id}") String clientId,
            @Value("${aportaya.correo.gmail.client-secret}") String clientSecret,
            @Value("${aportaya.correo.gmail.refresh-token}") String refreshToken,
            @Value("${aportaya.correo.gmail.from-email}") String remitente) {
        this.rest = constructor.build();
        this.clientId = exigir(clientId, "GMAIL_CLIENT_ID");
        this.clientSecret = exigir(clientSecret, "GMAIL_CLIENT_SECRET");
        this.refreshToken = exigir(refreshToken, "GMAIL_REFRESH_TOKEN");
        this.remitente = exigir(remitente, "GMAIL_FROM_EMAIL");
    }

    @Override
    public void enviarCodigo(String destino, String codigo, Duration vigencia) {
        var formulario = new LinkedMultiValueMap<String, String>();
        formulario.add("client_id", clientId);
        formulario.add("client_secret", clientSecret);
        formulario.add("refresh_token", refreshToken);
        formulario.add("grant_type", "refresh_token");

        @SuppressWarnings("unchecked")
        Map<String, Object> token = rest.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(Map.class);
        String acceso = token == null ? null : String.valueOf(token.get("access_token"));
        if (acceso == null || acceso.isBlank() || "null".equals(acceso)) {
            throw new IllegalStateException("Gmail no devolvio un access_token");
        }

        String asunto = Base64.getEncoder()
                .encodeToString(PlantillaCorreoDeVerificacion.asunto().getBytes(StandardCharsets.UTF_8));
        String cuerpo = Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(
                        PlantillaCorreoDeVerificacion.html(codigo, vigencia).getBytes(StandardCharsets.UTF_8));
        String mime = "From: AportaYa <" + remitente + ">\r\n"
                + "To: " + destino + "\r\n"
                + "Subject: =?UTF-8?B?" + asunto + "?=\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n\r\n"
                + cuerpo;
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(mime.getBytes(StandardCharsets.UTF_8));

        rest.post()
                .uri("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
                .headers(h -> h.setBearerAuth(acceso))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("raw", raw))
                .retrieve()
                .toBodilessEntity();
    }

    private static String exigir(String valor, String variable) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(variable + " es obligatoria cuando el proveedor es gmail_api");
        }
        return valor;
    }
}
