package bo.aportaya.plataforma.web.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;

/** GU-15 · el token de una sesion cerrada no entra aunque su firma y su {@code exp} esten bien. */
class ValidadorDeRevocacionTest {

    private static final Instant AHORA = Instant.parse("2026-11-24T15:05:00Z");

    private final ListaDeRevocaciones lista = new ListaDeRevocaciones();
    private final ValidadorDeRevocacion validador = new ValidadorDeRevocacion(lista, Reloj.fijo(AHORA));

    private static Jwt token(String sesionId) {
        var constructor = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .claim("rol", "ADMIN_PLATAFORMA")
                .issuedAt(AHORA.minusSeconds(60))
                .expiresAt(AHORA.plusSeconds(600));
        if (sesionId != null) {
            constructor.claim("sesion_id", sesionId);
        }
        return constructor.build();
    }

    private void revocar(UUID sesion) {
        lista.registrar(
                new RevocacionDeSesion(sesion, UUID.randomUUID(), AHORA.minusSeconds(1), AHORA.plusSeconds(900)));
    }

    @Test
    @DisplayName("correcto: con la lista al dia, el token de una sesion revocada se rechaza con invalid_token")
    void revocadoRechazado() {
        UUID sesion = UUID.randomUUID();
        revocar(sesion);
        lista.marcarAlDia();

        var resultado = validador.validate(token(sesion.toString()));

        assertThat(resultado.hasErrors()).isTrue();
        assertThat(resultado.getErrors()).extracting(OAuth2Error::getErrorCode).containsExactly("invalid_token");
    }

    @Test
    @DisplayName("correcto: con la lista al dia, el token de una sesion que nadie revoco se acepta")
    void noRevocadoAceptado() {
        revocar(UUID.randomUUID());
        lista.marcarAlDia();

        assertThat(validador.validate(token(UUID.randomUUID().toString())).hasErrors())
                .isFalse();
    }

    @Test
    @DisplayName("limite: una revocacion ya vencida no rechaza (su token ya no puede estar vivo)")
    void revocacionVencida() {
        UUID sesion = UUID.randomUUID();
        lista.registrar(
                new RevocacionDeSesion(sesion, UUID.randomUUID(), AHORA.minusSeconds(2000), AHORA.minusSeconds(1)));
        lista.marcarAlDia();

        assertThat(validador.validate(token(sesion.toString())).hasErrors()).isFalse();
    }

    @Test
    @DisplayName("limite: un token sin sesion_id no es revocable por este camino y se acepta")
    void sinSesion() {
        assertThat(validador.validate(token(null)).hasErrors()).isFalse();
        assertThat(validador.validate(token(" ")).hasErrors()).isFalse();
    }

    @Test
    @DisplayName("arranque en frio: sin la lista rehidratada no entra ningun token con sesion, y la sonda dice DOWN")
    void arranqueEnFrio() {
        var resultado = validador.validate(token(UUID.randomUUID().toString()));

        assertThat(resultado.hasErrors()).isTrue();
        assertThat(new SaludDeRevocaciones(lista).health().getStatus()).isEqualTo(Status.DOWN);

        lista.marcarAlDia();
        assertThat(new SaludDeRevocaciones(lista).health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("invalido: un sesion_id que no es UUID se rechaza")
    void sesionMalformada() {
        lista.marcarAlDia();

        assertThat(validador.validate(token("no-es-uuid")).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("con el lector apagado por configuracion la lista nace al dia: el servicio no rechaza todo")
    void lectorApagado() {
        var apagada = new ConfiguracionDeRevocacion().listaDeRevocaciones(false);
        var encendida = new ConfiguracionDeRevocacion().listaDeRevocaciones(true);

        assertThat(apagada.estaAlDia()).isTrue();
        assertThat(encendida.estaAlDia()).isFalse();
    }
}
