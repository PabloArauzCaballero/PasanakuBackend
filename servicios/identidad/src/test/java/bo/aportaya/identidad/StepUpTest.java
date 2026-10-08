package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bo.aportaya.identidad.aplicacion.CU04StepUp;
import bo.aportaya.identidad.dominio.puertos.DesafioDeFactor;
import bo.aportaya.identidad.infraestructura.AccesoRepositorio;
import bo.aportaya.identidad.infraestructura.EmisorDeAcceso;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** B37 · la evidencia step-up que `nucleo-financiero` exige para un retiro (contrato step-up-jwt.md). */
@org.junit.jupiter.api.Timeout(60) // generar una clave RSA-2048 agotaba los 5 s del corredor en una maquina cargada
class StepUpTest {
    private static EmisorDeAcceso emisorCompartido;
    private static final String CODIGO = "123456";

    private final UUID usuario = UUID.randomUUID();
    private EmisorDeAcceso emisor;

    @org.junit.jupiter.api.BeforeAll
    static void clave() {
        emisorCompartido = new EmisorDeAcceso("");
    }

    private DesafioDeFactor factor;
    private CU04StepUp stepUp;

    @BeforeEach
    @org.junit.jupiter.api.Timeout(60) // primer uso de Mockito en final class: la inicializacion agota 5 s
    @SuppressWarnings("unchecked")
    void armar() {
        emisor = emisorCompartido;
        factor = mock(DesafioDeFactor.class);
        when(factor.emitir(eq(usuario), any())).thenReturn(UUID.randomUUID());
        when(factor.validar(eq(usuario), any(), eq(CODIGO))).thenReturn(true);
        var datos = mock(Datos.class);
        when(datos.conContexto(any(), any()))
                .thenAnswer(i -> ((Function<Object, Object>) i.getArgument(1)).apply(null));
        var accesos = mock(AccesoRepositorio.class);
        when(accesos.factorActivo(any(), eq(usuario))).thenReturn(Optional.of("TOTP"));
        stepUp = new CU04StepUp(datos, accesos, factor, emisor, Duration.ofMinutes(5), "aportaya-nucleo-financiero");
    }

    private ContextoSesion como(UUID u) {
        return ContextoSesion.de(u, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName(
            "la evidencia lleva los claims del contrato, vence en <= 5 min y la firma verifica con el JWKS publico")
    void evidenciaCorrecta() throws Exception {
        var d = stepUp.abrir("RETIRO", como(usuario));
        var e = stepUp.verificar(d.id(), CODIGO, como(usuario));

        SignedJWT jwt = SignedJWT.parse(e.jwt());
        RSAKey publica = (RSAKey) JWKSet.parse(emisor.jwks()).getKeys().get(0);
        assertThat(jwt.verify(new RSASSAVerifier(publica))).isTrue();
        var c = jwt.getJWTClaimsSet();
        assertThat(c.getIssuer()).isEqualTo("aportaya-identidad");
        assertThat(c.getAudience()).containsExactly("aportaya-nucleo-financiero");
        assertThat(c.getSubject()).isEqualTo(usuario.toString());
        assertThat(c.getStringClaim("proposito")).isEqualTo("RETIRO");
        assertThat(c.getStringClaim("acr")).isEqualTo("mfa");
        assertThat(c.getStringListClaim("amr")).containsExactly("totp");
        assertThat(c.getStringClaim("desafio_id")).isEqualTo(d.id().toString());
        assertThat(UUID.fromString(c.getJWTID())).isEqualTo(e.jti());
        assertThat(Duration.between(
                        c.getIssueTime().toInstant(), c.getExpirationTime().toInstant()))
                .isLessThanOrEqualTo(Duration.ofMinutes(5));
        assertThat(c.getExpirationTime().toInstant()).isAfter(Instant.now());
        assertThat(jwt.getHeader().getKeyID()).isEqualTo(publica.getKeyID());
    }

    @Test
    @DisplayName("un solo uso: el segundo intento sobre el mismo desafio falla")
    void unSoloUso() {
        var d = stepUp.abrir("RETIRO", como(usuario));
        stepUp.verificar(d.id(), CODIGO, como(usuario));
        assertThatThrownBy(() -> stepUp.verificar(d.id(), CODIGO, como(usuario)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya se uso");
    }

    @Test
    @DisplayName("un codigo errado no entrega evidencia, y igual consume el desafio")
    void codigoErrado() {
        var d = stepUp.abrir("RETIRO", como(usuario));
        assertThatThrownBy(() -> stepUp.verificar(d.id(), "000000", como(usuario)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no sirve");
        assertThatThrownBy(() -> stepUp.verificar(d.id(), CODIGO, como(usuario)))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName("el desafio de otro usuario no sirve, ni uno inexistente")
    void ajenoOInexistente() {
        var d = stepUp.abrir("RETIRO", como(usuario));
        assertThatThrownBy(() -> stepUp.verificar(d.id(), CODIGO, como(UUID.randomUUID())))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> stepUp.verificar(UUID.randomUUID(), CODIGO, como(usuario)))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName("un proposito fuera del contrato se rechaza, y sin factor enrolado no se abre desafio")
    void propositoYFactor() {
        assertThatThrownBy(() -> stepUp.abrir("TRANSFERIR_TODO", como(usuario))).isInstanceOf(ErrorDeNegocio.class);
        UUID sinFactor = UUID.randomUUID();
        assertThatThrownBy(() -> stepUp.abrir("RETIRO", como(sinFactor))).isInstanceOf(ErrorDeNegocio.class);
    }
}
