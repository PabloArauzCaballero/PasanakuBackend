package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.infraestructura.EvidenciaDeStepUpJwt;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * La evidencia step-up de los actos de gobierno (GU-11) en sus tres niveles (regla 65): la valida, los bordes
 * (vence ya, reuso) y lo que se rechaza (firma, sujeto, proposito, audiencia, acr, jti). La firma la hace el
 * {@link JwtDecoder} real en produccion; aca se lo reemplaza por un decodificador que devuelve el JWT armado,
 * para probar lo que este adaptador decide DESPUES de la firma.
 */
class EvidenciaDeStepUpJwtTest {

    private static final UUID ACTOR = UUID.fromString("66666666-7777-4888-8999-aaaaaaaaaaaa");
    private static final String AUDIENCIA = "aportaya-nucleo-financiero";

    @Test
    @DisplayName("correcto: la evidencia ADMIN del propio actor, vigente, se acepta una sola vez")
    void unaSolaVez() {
        var adaptador = adaptador(UnaryOperator.identity());

        assertThat(adaptador.consumir(ACTOR, "evidencia")).isTrue();
        assertThat(adaptador.consumir(ACTOR, "evidencia")).isFalse();
    }

    @Test
    @DisplayName("limite: vencida un segundo antes se rechaza aunque la firma valga")
    void vencida() {
        assertThat(adaptador(b -> b.expiresAt(Instant.now().minusSeconds(1))).consumir(ACTOR, "evidencia"))
                .isFalse();
    }

    @Test
    @DisplayName("invalido: otro sujeto, otro proposito, otra audiencia, sin acr mfa o jti no UUID se rechazan")
    void invalidas() {
        List<UnaryOperator<Jwt.Builder>> malas = List.of(
                b -> b.subject(UUID.randomUUID().toString()),
                b -> b.claim("proposito", "RETIRO"),
                b -> b.audience(List.of("aportaya")),
                b -> b.claim("acr", "pwd"),
                b -> b.claim("iss", "otro-emisor"),
                b -> b.jti("no-es-un-uuid"));
        for (UnaryOperator<Jwt.Builder> mala : malas) {
            assertThat(adaptador(mala).consumir(ACTOR, "evidencia")).isFalse();
        }
    }

    @Test
    @DisplayName("invalido: sin evidencia o con firma rota no hay step-up")
    void sinEvidenciaOFirmaRota() {
        var adaptador = adaptador(UnaryOperator.identity());
        assertThat(adaptador.consumir(ACTOR, null)).isFalse();
        assertThat(adaptador.consumir(ACTOR, " ")).isFalse();

        JwtDecoder firmaRota = token -> {
            throw new BadJwtException("firma invalida");
        };
        assertThat(new EvidenciaDeStepUpJwt(firmaRota, AUDIENCIA).consumir(ACTOR, "evidencia"))
                .isFalse();
    }

    private static EvidenciaDeStepUpJwt adaptador(UnaryOperator<Jwt.Builder> ajuste) {
        String jti = UUID.randomUUID().toString();
        JwtDecoder decodificador = token -> ajuste.apply(Jwt.withTokenValue(token)
                        .header("alg", "RS256")
                        .claim("iss", "aportaya-identidad")
                        .audience(List.of(AUDIENCIA))
                        .subject(ACTOR.toString())
                        .claim("proposito", "ADMIN")
                        .claim("acr", "mfa")
                        .jti(jti)
                        .issuedAt(Instant.now().minusSeconds(5))
                        .expiresAt(Instant.now().plusSeconds(240)))
                .build();
        return new EvidenciaDeStepUpJwt(decodificador, AUDIENCIA);
    }
}
