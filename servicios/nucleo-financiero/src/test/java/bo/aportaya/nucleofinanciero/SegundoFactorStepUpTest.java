package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.infraestructura.ConsumoDeEvidenciaMfa;
import bo.aportaya.nucleofinanciero.infraestructura.EmisorDeEvidenciaDePrueba;
import bo.aportaya.nucleofinanciero.infraestructura.EvidenciaMfaConsumidaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.SegundoFactorStepUp;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * H2.S2.M3/M4 · {@code SegundoFactorStepUp} contra el doble de identidad, en los tres
 * niveles del contrato (§3 del encargo).
 *
 * <p>Los 7 casos del DoD: sin evidencia, firma ajena, vencida, otro {@code sub}, otro
 * {@code proposito}, {@code jti} ya consumido, valida.
 */
class SegundoFactorStepUpTest extends BaseDeBilletera {

    private EmisorDeEvidenciaDePrueba emisor;
    private SegundoFactorStepUp segundoFactor;

    @BeforeEach
    void armar() {
        emisor = new EmisorDeEvidenciaDePrueba();
        var decodificador =
                NimbusJwtDecoder.withJwkSetUri(emisor.jwksUri().toString()).build();
        var consumo =
                new ConsumoDeEvidenciaMfa(new Datos(dsl), new EvidenciaMfaConsumidaRepositorio(), Reloj.delSistema());
        segundoFactor = new SegundoFactorStepUp(decodificador, consumo);
    }

    @AfterEach
    void cerrar() {
        emisor.close();
        fixtura.limpiarBilleteras();
    }

    private boolean verificar(UUID usuarioId, String evidencia) {
        return transaccion.execute(t -> segundoFactor.verificado(usuarioId, evidencia));
    }

    @Test
    @DisplayName("sin evidencia: rechaza")
    void sinEvidencia() {
        assertThat(verificar(fixtura.usuario(), null)).isFalse();
        assertThat(verificar(fixtura.usuario(), "")).isFalse();
        assertThat(verificar(fixtura.usuario(), "   ")).isFalse();
    }

    @Test
    @DisplayName("firmada con otra clave: rechaza")
    void firmaAjena() {
        UUID usuario = fixtura.usuario();
        assertThat(verificar(usuario, emisor.firmadaConOtraClave(usuario))).isFalse();
    }

    @Test
    @DisplayName("vencida (exp = ahora - 1s): rechaza")
    void vencida() {
        UUID usuario = fixtura.usuario();
        assertThat(verificar(usuario, emisor.expiroHaceUnSegundo(usuario))).isFalse();
    }

    @Test
    @DisplayName("otro sub: rechaza — evidencia valida de OTRO usuario no autoriza este retiro")
    void otroSub() {
        UUID usuario = fixtura.usuario();
        assertThat(verificar(usuario, emisor.otroSujeto())).isFalse();
    }

    @Test
    @DisplayName("otro proposito (ADMIN en vez de RETIRO): rechaza")
    void otroProposito() {
        UUID usuario = fixtura.usuario();
        assertThat(verificar(usuario, emisor.otroProposito(usuario))).isFalse();
    }

    @Test
    @DisplayName("aud ajena: rechaza")
    void otraAudiencia() {
        UUID usuario = fixtura.usuario();
        assertThat(verificar(usuario, emisor.otraAudiencia(usuario))).isFalse();
    }

    @Test
    @DisplayName("sin acr=mfa: rechaza")
    void sinAcrMfa() {
        UUID usuario = fixtura.usuario();
        assertThat(verificar(usuario, emisor.sinAcrMfa(usuario))).isFalse();
    }

    @Test
    @DisplayName("jti ya consumido: la primera pasa, la segunda con el MISMO jti se rechaza")
    void jtiYaConsumido() {
        UUID usuario = fixtura.usuario();
        UUID jti = UUID.randomUUID();
        String evidencia = emisor.conJti(usuario, jti);

        assertThat(verificar(usuario, evidencia)).isTrue();
        assertThat(verificar(usuario, evidencia)).isFalse();
        assertThat(contar("SELECT count(*)::int FROM nucleo_financiero.evidencia_mfa_consumida WHERE jti = ?", jti))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("valida: todos los claims correctos, jti nuevo → acepta")
    void valida() {
        UUID usuario = fixtura.usuario();
        assertThat(verificar(usuario, emisor.correcto(usuario))).isTrue();
    }
}
