package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Un mapeo de cuentas logicas sintetico no puede asentar dinero de produccion: el arranque se niega. */
class GuardiaDeCuentasLogicasTest {

    private static final String CONSUMO = "aportaya.contabilidad.consumo-de-asientos.habilitado";
    private static final String APTO = "aportaya.contabilidad.cuentas-logicas.apto-produccion";

    private ApplicationContextRunner en(String perfil) {
        return new ApplicationContextRunner()
                .withUserConfiguration(GuardiaDeCuentasLogicas.class)
                .withInitializer(c -> c.getEnvironment().setActiveProfiles(perfil));
    }

    private static String causaRaiz(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return String.valueOf(t.getMessage());
    }

    @Test
    @DisplayName("rechaza arrancar en produccion con el consumo encendido y el mapeo sin declarar apto para produccion")
    void rechazaProduccionConMapeoSintetico() {
        en("production").withPropertyValues(CONSUMO + "=true", APTO + "=false").run(c -> {
            assertThat(c).hasFailed();
            assertThat(causaRaiz(c.getStartupFailure())).contains("CUENTAS_LOGICAS_APTO_PRODUCCION");
        });
    }

    @Test
    @DisplayName(
            "rechaza arrancar en produccion con el consumo encendido y la marca de apto ausente (por omision es false)")
    void rechazaProduccionSinLaMarca() {
        en("production").withPropertyValues(CONSUMO + "=true").run(c -> assertThat(c)
                .hasFailed());
    }

    @Test
    @DisplayName("arranca en produccion cuando contabilidad entrego el mapeo y alguien lo declaro apto")
    void arrancaConMapeoDefinitivo() {
        en("production").withPropertyValues(CONSUMO + "=true", APTO + "=true").run(c -> assertThat(c)
                .hasNotFailed());
    }

    @Test
    @DisplayName("arranca en TEST (perfil local) con el mapeo sintetico y el consumo encendido")
    void arrancaEnTestConMapeoSintetico() {
        en("local").withPropertyValues(CONSUMO + "=true", APTO + "=false").run(c -> assertThat(c)
                .hasNotFailed());
    }

    @Test
    @DisplayName("arranca en produccion con el consumo apagado, aunque el mapeo no sea apto")
    void arrancaConElConsumoApagado() {
        en("production").withPropertyValues(CONSUMO + "=false", APTO + "=false").run(c -> assertThat(c)
                .hasNotFailed());
    }
}
