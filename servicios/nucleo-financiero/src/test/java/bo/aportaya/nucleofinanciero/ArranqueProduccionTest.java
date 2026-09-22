package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.dominio.puertos.SegundoFactor;
import bo.aportaya.nucleofinanciero.infraestructura.SegundoFactorStepUp;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * H2.S1/H2.S3 · con perfil {@code production}, el proceso arranca CON
 * {@link SegundoFactorStepUp} — nunca con un bypass.
 *
 * <p>{@code SegundoFactorLocal} vive detras de {@code @Profile({"local","test"})}
 * (H2.S1.M2) y {@code SegundoFactorStepUp} detras de {@code @Profile("!local & !test")}
 * (H2.S2): en {@code production} el UNICO {@code SegundoFactor} candidato es el real.
 * Antes de que {@code SegundoFactorStepUp} existiera, este mismo archivo probaba lo
 * contrario —que el arranque FALLABA por falta de bean— y ese rojo intencional quedo
 * documentado en el historial de este carril: era la prueba de que la guardia hacia
 * lo que tenia que hacer antes de que el reemplazo apareciera.
 */
class ArranqueProduccionTest {

    @DynamicPropertySource
    static void configuracion(DynamicPropertyRegistry registro) {
        var contenedor = BaseDePrueba.contenedor();
        registro.add("spring.datasource.url", contenedor::getJdbcUrl);
        registro.add("spring.datasource.username", contenedor::getUsername);
        registro.add("spring.datasource.password", contenedor::getPassword);
        registro.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registro.add("aportaya.jwt.jwks-uri", () -> "http://identidad:8080/.well-known/jwks.json");
        registro.add("SEGURIDAD_PIMIENTA", () -> "pimienta-de-prueba");
        registro.add("CUENTA_PUENTE_CUSTODIA", () -> "00000000-0000-0000-0000-0000000000c0");
        registro.add("APORTAYA_CORS_ORIGENES", () -> "https://app.aportaya.bo");
    }

    /** Caso 1 · production, sin nada mas: arranca, y el bean es el real. */
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("production")
    class ConAdaptadorReal {

        @Autowired
        private SegundoFactor segundoFactor;

        @Test
        void arrancaConSegundoFactorStepUp() {
            assertThat(segundoFactor).isInstanceOf(SegundoFactorStepUp.class);
        }
    }

    /**
     * Caso 2 · production con {@code aportaya.mfa.doble-local=true} forzado a mano: NO
     * tiene efecto. No hay bean que lea esa propiedad fuera de local/test —
     * {@code SegundoFactorLocal} ni siquiera existe en el contexto— asi que el
     * adaptador activo sigue siendo el real, nunca un bypass.
     */
    @Nested
    @SpringBootTest(
            webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
            properties = "aportaya.mfa.doble-local=true")
    @ActiveProfiles("production")
    class ConDobleLocalForzado {

        @Autowired
        private SegundoFactor segundoFactor;

        @Test
        void dobleLocalNoTieneEfectoEnProduccion() {
            assertThat(segundoFactor).isInstanceOf(SegundoFactorStepUp.class);
        }
    }
}
