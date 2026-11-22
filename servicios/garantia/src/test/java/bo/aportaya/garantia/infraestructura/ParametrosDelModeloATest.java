package bo.aportaya.garantia.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * F3.L2.M14 · Con el modelo A encendido, garantia no arranca sin sus parametros financieros, y el error nombra el que
 * falta. Apagado (por omision), arranca como hoy. Los valores de prueba vienen del perfil SINTETICO
 * {@code application-modelo-a-sintetico.yml}: no son una propuesta.
 */
class ParametrosDelModeloATest {

    private static final String RAIZ = ParametrosDelModeloA.RAIZ;

    private static ApplicationContextRunner contexto() {
        return new ApplicationContextRunner().withUserConfiguration(ParametrosDelModeloA.class);
    }

    /** El perfil sintetico completo, cargado como lo haria Spring Boot al arrancar con ese perfil. */
    private static ApplicationContextRunner conPerfilSintetico() {
        return contexto().withInitializer(c -> {
            c.getEnvironment().setActiveProfiles("modelo-a-sintetico");
            ConfigDataEnvironmentPostProcessor.applyTo(c.getEnvironment());
        });
    }

    private static String causaRaiz(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return String.valueOf(t.getMessage());
    }

    @Test
    @DisplayName("con el modelo A apagado (por omision) garantia arranca sin parametros, como hoy")
    void apagadoArrancaSinParametros() {
        contexto().run(c -> {
            assertThat(c).hasNotFailed();
            assertThat(c).doesNotHaveBean(ParametrosDelModeloA.class);
        });
    }

    @Test
    @DisplayName("con el modelo A encendido y todos los parametros, arranca y los lee en decimal exacto")
    void encendidoCompletoArranca() {
        conPerfilSintetico().run(c -> {
            assertThat(c).hasNotFailed();
            var p = c.getBean(ParametrosDelModeloA.class);
            assertThat(p.porcentajeDelFondoPorNivel()).containsEntry("NIVEL-INICIAL", new BigDecimal("2.00"));
            assertThat(p.diasDeGracia()).isEqualTo(3);
            assertThat(p.tasaDeRecargo()).isEqualByComparingTo("1.25");
            assertThat(p.umbralDeSolvencia().moneda()).isEqualTo("BOB");
        });
    }

    @Test
    @DisplayName("rechaza arrancar sin los dias de gracia y el error nombra la clave")
    void sinDiasDeGracia() {
        conPerfilSintetico().withPropertyValues(RAIZ + "dias-de-gracia=").run(c -> {
            assertThat(c).hasFailed();
            assertThat(causaRaiz(c.getStartupFailure())).contains(RAIZ + "dias-de-gracia");
        });
    }

    @Test
    @DisplayName("rechaza arrancar encendido y sin ningun parametro: los nombra a todos, sin inventar ninguno")
    void encendidoSinNada() {
        contexto().withPropertyValues("aportaya.modelo-a.habilitado=true").run(c -> {
            assertThat(c).hasFailed();
            assertThat(causaRaiz(c.getStartupFailure()))
                    .contains(RAIZ + "porcentaje-del-fondo-por-nivel")
                    .contains(RAIZ + "tasa-de-recargo")
                    .contains(RAIZ + "tope-del-recargo.valor")
                    .contains(RAIZ + "umbral-de-solvencia.unidad")
                    .contains(RAIZ + "umbral-de-disolucion-automatica.valor");
        });
    }

    @Test
    @DisplayName("rechaza un porcentaje negativo, uno que no es decimal y dias de gracia con decimales")
    void rechazaValoresInvalidos() {
        conPerfilSintetico()
                .withPropertyValues(
                        RAIZ + "tasa-de-recargo=-1",
                        RAIZ + "porcentaje-del-fondo-por-nivel.NIVEL-ALTO=dos",
                        RAIZ + "dias-de-gracia=2.5")
                .run(c -> {
                    assertThat(c).hasFailed();
                    assertThat(causaRaiz(c.getStartupFailure()))
                            .contains(RAIZ + "tasa-de-recargo (no puede ser negativo)")
                            .contains(RAIZ + "porcentaje-del-fondo-por-nivel.NIVEL-ALTO (no es un decimal: «dos»)")
                            .contains(RAIZ + "dias-de-gracia (son dias enteros)");
                });
    }

    @Test
    @DisplayName("rechaza un umbral en MONTO sin su moneda ISO 4217 (un importe sin moneda esta prohibido)")
    void montoSinMoneda() {
        conPerfilSintetico()
                .withPropertyValues(RAIZ + "umbral-de-solvencia.moneda=")
                .run(c -> {
                    assertThat(c).hasFailed();
                    assertThat(causaRaiz(c.getStartupFailure())).contains(RAIZ + "umbral-de-solvencia.moneda");
                });
    }
}
