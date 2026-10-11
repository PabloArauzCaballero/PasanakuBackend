package bo.aportaya.plataforma.web.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** La identidad que un adaptador compara antes de actuar en nombre de alguien: sale del token, no de un cuerpo. */
class UsuarioDeLaSesionTest {

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("correcto: con un JWT en la sesion devuelve su sujeto")
    void conJwt() {
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "none")
                .subject("11111111-1111-4111-8111-111111111111")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThat(ClienteDeServicio.usuarioDeLaSesion()).contains("11111111-1111-4111-8111-111111111111");
    }

    @Test
    @DisplayName("limite: sin sesion (un trabajo programado) o con una autenticacion que no es JWT no hay usuario")
    void sinJwt() {
        assertThat(ClienteDeServicio.usuarioDeLaSesion()).isEmpty();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("x", "y"));
        assertThat(ClienteDeServicio.usuarioDeLaSesion()).isEmpty();
    }
}
