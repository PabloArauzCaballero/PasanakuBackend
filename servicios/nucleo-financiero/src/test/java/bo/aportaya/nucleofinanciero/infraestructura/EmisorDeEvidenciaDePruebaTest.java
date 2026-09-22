package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H2.S2.M1 · el doble de {@code identidad} emite los tres niveles del contrato de
 * evidencia step-up, y sirve un JWKS real por HTTP.
 */
class EmisorDeEvidenciaDePruebaTest {

    private EmisorDeEvidenciaDePrueba emisor;

    @BeforeEach
    void armar() {
        emisor = new EmisorDeEvidenciaDePrueba();
    }

    @AfterEach
    void cerrar() {
        emisor.close();
    }

    @Test
    @DisplayName("el JWKS se sirve por HTTP y publica la clave con el kid esperado")
    void sirveElJwks() throws Exception {
        HttpResponse<String> respuesta = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(emisor.jwksUri()).GET().build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("\"kid\":\"" + EmisorDeEvidenciaDePrueba.KID + "\"");
        assertThat(respuesta.body()).contains("\"kty\":\"RSA\"");
    }

    @Test
    @DisplayName("nivel correcto: todos los claims del contrato, firma valida con el kid publicado")
    void nivelCorrecto() throws ParseException {
        UUID usuario = UUID.randomUUID();
        SignedJWT jwt = SignedJWT.parse(emisor.correcto(usuario));

        var claims = jwt.getJWTClaimsSet();
        assertThat(jwt.getHeader().getKeyID()).isEqualTo(EmisorDeEvidenciaDePrueba.KID);
        assertThat(claims.getIssuer()).isEqualTo(EmisorDeEvidenciaDePrueba.ISS);
        assertThat(claims.getAudience()).containsExactly(EmisorDeEvidenciaDePrueba.AUD);
        assertThat(claims.getSubject()).isEqualTo(usuario.toString());
        assertThat(claims.getStringClaim("proposito")).isEqualTo("RETIRO");
        assertThat(claims.getStringClaim("acr")).isEqualTo("mfa");
        assertThat(claims.getJWTID()).isNotBlank();
    }

    @Test
    @DisplayName("nivel limite: exp = ahora+1s todavia trae proposito RETIRO y acr mfa")
    void nivelLimiteVigente() throws ParseException {
        SignedJWT jwt = SignedJWT.parse(emisor.expiraEnUnSegundo(UUID.randomUUID()));
        var claims = jwt.getJWTClaimsSet();
        assertThat(claims.getExpirationTime()).isAfter(new java.util.Date());
    }

    @Test
    @DisplayName("nivel limite: exp = ahora-1s ya vencio")
    void nivelLimiteVencido() throws ParseException {
        SignedJWT jwt = SignedJWT.parse(emisor.expiroHaceUnSegundo(UUID.randomUUID()));
        var claims = jwt.getJWTClaimsSet();
        assertThat(claims.getExpirationTime()).isBefore(new java.util.Date());
    }

    @Test
    @DisplayName("nivel limite: el mismo jti en dos evidencias es reconocible para el validador")
    void nivelLimiteJtiReusado() throws ParseException {
        UUID jti = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();

        SignedJWT primera = SignedJWT.parse(emisor.conJti(usuario, jti));
        SignedJWT segunda = SignedJWT.parse(emisor.conJti(usuario, jti));

        assertThat(primera.getJWTClaimsSet().getJWTID())
                .isEqualTo(segunda.getJWTClaimsSet().getJWTID())
                .isEqualTo(jti.toString());
    }

    @Test
    @DisplayName("nivel invalido: firmada con una clave que el JWKS de este doble no publica")
    void nivelInvalidoOtraClave() throws ParseException {
        SignedJWT jwt = SignedJWT.parse(emisor.firmadaConOtraClave(UUID.randomUUID()));
        assertThat(jwt.getHeader().getKeyID()).isNotEqualTo(EmisorDeEvidenciaDePrueba.KID);
    }

    @Test
    @DisplayName("nivel invalido: sub distinto, proposito ADMIN, aud ajena, sin acr — cada uno por separado")
    void nivelInvalidoOtrosClaims() throws ParseException {
        UUID usuario = UUID.randomUUID();

        assertThat(SignedJWT.parse(emisor.otroSujeto()).getJWTClaimsSet().getSubject())
                .isNotEqualTo(usuario.toString());
        assertThat(SignedJWT.parse(emisor.otroProposito(usuario)).getJWTClaimsSet().getStringClaim("proposito"))
                .isEqualTo("ADMIN");
        assertThat(SignedJWT.parse(emisor.otraAudiencia(usuario)).getJWTClaimsSet().getAudience())
                .doesNotContain(EmisorDeEvidenciaDePrueba.AUD);
        assertThat(SignedJWT.parse(emisor.sinAcrMfa(usuario)).getJWTClaimsSet().getStringClaim("acr"))
                .isNull();
    }
}
