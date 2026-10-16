package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.infraestructura.EmisorDeAcceso;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H13.S1.M2 hallazgo 2 · La rotacion de {@code JWT_CLAVE_FIRMA} no desloguea a nadie.
 *
 * <p>Antes el JWKS publicaba UNA clave: al rotar, la vieja desaparecia y los otros catorce servicios
 * dejaban de poder verificar toda sesion viva. Ahora publica la vigente y, durante la ventana, la
 * anterior. La verificacion es la de un servidor de recurso: elegir la clave del JWKS por {@code kid}.
 * El ensayo de mecanica sintetica esta en {@code despliegue/ensayos/rotacion}; esto ejerce el codigo real.
 */
class RotacionDeClavesDeFirmaTest {

    private static RSAKey nuevaClave() throws Exception {
        return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
    }

    /** Como un servidor de recurso: toma del JWKS publicado la clave del {@code kid} del token. */
    private static boolean valida(String token, java.util.Map<String, Object> jwksPublicado) throws Exception {
        SignedJWT jwt = SignedJWT.parse(token);
        JWK clave = JWKSet.parse(jwksPublicado).getKeyByKeyId(jwt.getHeader().getKeyID());
        if (clave == null) {
            return false;
        }
        return jwt.verify(new RSASSAVerifier(clave.toRSAKey()));
    }

    private static String sesion(EmisorDeAcceso emisor) {
        return emisor.emitir(UUID.randomUUID(), "PARTICIPANTE", List.of("BILLETERA_VER"), "BASICO", "d1")
                .token();
    }

    @Test
    @DisplayName("el token de acceso firmado identifica su sesión sin confiar en datos del cliente")
    void incluyeElIdDeSesionEnElToken() throws Exception {
        UUID sesionId = UUID.randomUUID();
        var emisor = new EmisorDeAcceso(nuevaClave().toJSONString(), "");

        var jwt = SignedJWT.parse(
                emisor.emitir(UUID.randomUUID(), "PARTICIPANTE", List.of(), "SIMPLIFICADA", "dispositivo", sesionId)
                        .token());

        assertThat(jwt.getJWTClaimsSet().getStringClaim("sesion_id")).isEqualTo(sesionId.toString());
    }

    @Test
    @DisplayName("durante la ventana el JWKS publica las dos claves y la sesion firmada con la vieja sigue valida")
    void laSesionVivaSobreviveALaRotacion() throws Exception {
        RSAKey vieja = nuevaClave();
        RSAKey nueva = nuevaClave();
        var antes = new EmisorDeAcceso(vieja.toJSONString(), "");
        String sesionViva = sesion(antes);
        assertThat(valida(sesionViva, antes.jwks())).isTrue();

        // Se rota: la nueva firma, la vieja queda como «anterior».
        var despues = new EmisorDeAcceso(nueva.toJSONString(), vieja.toJSONString());
        List<?> publicadas = (List<?>) despues.jwks().get("keys");

        assertThat(publicadas).as("vigente + anterior").hasSize(2);
        assertThat(valida(sesionViva, despues.jwks()))
                .as("lo firmado ANTES de rotar sigue validando")
                .isTrue();
        assertThat(valida(sesion(despues), despues.jwks()))
                .as("lo firmado con la nueva valida desde el primer segundo")
                .isTrue();
    }

    @Test
    @DisplayName("rechaza: la «anterior» NUNCA firma; todo lo emitido despues de rotar lleva el kid de la nueva")
    void laAnteriorSoloSePublica() throws Exception {
        RSAKey vieja = nuevaClave();
        RSAKey nueva = nuevaClave();
        var emisor = new EmisorDeAcceso(nueva.toJSONString(), vieja.toJSONString());

        assertThat(SignedJWT.parse(sesion(emisor)).getHeader().getKeyID()).isEqualTo(nueva.getKeyID());
    }

    @Test
    @DisplayName("rechaza: al retirar la anterior, lo firmado con ella deja de valer (la clave filtrada no sirve)")
    void retirarLaAnteriorInvalidaLoViejo() throws Exception {
        RSAKey vieja = nuevaClave();
        RSAKey nueva = nuevaClave();
        String sesionVieja = sesion(new EmisorDeAcceso(vieja.toJSONString(), ""));

        var sinVentana = new EmisorDeAcceso(nueva.toJSONString(), "");

        assertThat(((List<?>) sinVentana.jwks().get("keys"))).hasSize(1);
        assertThat(valida(sesionVieja, sinVentana.jwks())).isFalse();
        assertThat(valida(sesion(sinVentana), sinVentana.jwks())).isTrue();
    }

    @Test
    @DisplayName("el JWKS publica solo la parte publica de las dos claves: ningun componente privado")
    void nuncaSePublicaLaPrivada() throws Exception {
        var emisor =
                new EmisorDeAcceso(nuevaClave().toJSONString(), nuevaClave().toJSONString());

        String publicado = emisor.jwks().toString();
        for (String privado : new String[] {"\"d\"", "\"p\"", "\"q\"", "\"dp\"", "\"dq\"", "\"qi\""}) {
            assertThat(publicado).doesNotContain(privado);
        }
        assertThat(JWKSet.parse(emisor.jwks()).getKeys()).allMatch(k -> !k.isPrivate());
    }

    @Test
    @DisplayName("rechaza: una «anterior» sin clave vigente configurada no arranca (no hay nada que rotar)")
    void anteriorSinVigente() throws Exception {
        String anterior = nuevaClave().toJSONString();

        assertThatThrownBy(() -> new EmisorDeAcceso("", anterior))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clave-firma-anterior");
    }

    @Test
    @DisplayName("rechaza: la «anterior» con el mismo kid que la vigente no arranca (el verificador elegiria al azar)")
    void mismoKid() throws Exception {
        RSAKey vigente = nuevaClave();
        RSAKey mismoKid = new RSAKeyGenerator(2048).keyID(vigente.getKeyID()).generate();

        assertThatThrownBy(() -> new EmisorDeAcceso(vigente.toJSONString(), mismoKid.toJSONString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kid distinto");
    }

    @Test
    @DisplayName("rechaza: un JWK ilegible no arranca y el mensaje no repite la clave")
    void jwkIlegible() throws Exception {
        String vigente = nuevaClave().toJSONString();

        assertThatThrownBy(() -> new EmisorDeAcceso(vigente, "{esto no es un jwk}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clave-firma-anterior")
                .hasMessageNotContaining("esto no es");
    }

    @Test
    @DisplayName("sin rotacion en curso el JWKS publica una sola clave, como siempre")
    void sinRotacion() throws Exception {
        var emisor = new EmisorDeAcceso(nuevaClave().toJSONString(), "");

        assertThat((List<?>) emisor.jwks().get("keys")).hasSize(1);
    }
}
