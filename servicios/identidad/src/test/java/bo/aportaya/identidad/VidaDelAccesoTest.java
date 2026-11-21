package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.infraestructura.EmisorDeAcceso;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * F1.S3.M1 · El token de acceso de una cuenta suspendida deja de servir en 15 minutos como maximo.
 *
 * <p>Al suspender se revocan las sesiones y el refresco se niega; lo unico que sigue vivo es el
 * token de acceso ya emitido. Esta prueba lo decodifica como un servidor de recurso (Nimbus con
 * la clave publica) y le aplica el {@link JwtTimestampValidator} <b>por omision</b> de Spring, el
 * mismo que arma {@code NimbusJwtDecoder.withJwkSetUri(..).build()} en {@code comun-web}, con su
 * tolerancia de reloj. Solo se fija el reloj.
 */
class VidaDelAccesoTest {

    private static final Duration TOPE = Duration.ofMinutes(15);

    private Jwt decodificado(RSAKey clave) throws Exception {
        String token = new EmisorDeAcceso(clave.toJSONString(), "")
                .emitir(UUID.randomUUID(), "PARTICIPANTE", List.of("BILLETERA_VER"), "BASICO", "d1", UUID.randomUUID())
                .token();
        NimbusJwtDecoder decodificador =
                NimbusJwtDecoder.withPublicKey(clave.toRSAPublicKey()).build();
        decodificador.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return decodificador.decode(token);
    }

    private static boolean aceptadoEn(Jwt jwt, Instant cuando) {
        var validador = new JwtTimestampValidator();
        validador.setClock(Clock.fixed(cuando, ZoneOffset.UTC));
        return !validador.validate(jwt).hasErrors();
    }

    @Test
    @DisplayName("un servidor de recurso con la tolerancia por omision rechaza el token pasados 15 minutos")
    void noSirveMasDeQuinceMinutos() throws Exception {
        Jwt jwt = decodificado(new RSAKeyGenerator(2048).keyID("k1").generate());
        Instant emitido = jwt.getIssuedAt();

        assertThat(aceptadoEn(jwt, emitido.plus(Duration.ofMinutes(13)))).isTrue();
        assertThat(aceptadoEn(jwt, emitido.plus(TOPE).plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("la vida del token mas la tolerancia del decodificador no supera el tope")
    void vidaMasToleranciaNoSuperaElTope() throws Exception {
        Jwt jwt = decodificado(new RSAKeyGenerator(2048).keyID("k2").generate());

        Duration vida = Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt());

        assertThat(vida.plus(EmisorDeAcceso.TOLERANCIA_DEL_DECODIFICADOR)).isLessThanOrEqualTo(TOPE);
        assertThat(EmisorDeAcceso.TOPE_TRAS_REVOCAR).isEqualTo(TOPE);
    }
}
