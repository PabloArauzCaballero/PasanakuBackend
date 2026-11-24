package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** FLUJO-QR-001: el modo de recarga de prueba no arranca en produccion, y lo dice. */
class GuardiaDeRecargaDePruebaTest {

    private static final String MODO = "aportaya.recarga.qr.modo";

    private ApplicationContextRunner en(String perfil) {
        return new ApplicationContextRunner()
                .withUserConfiguration(GuardiaDeRecargaDePrueba.class)
                .withInitializer(c -> c.getEnvironment().setActiveProfiles(perfil));
    }

    private static String causaRaiz(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return String.valueOf(t.getMessage());
    }

    @Test
    @DisplayName("inválido: rechaza arrancar en produccion con modo=prueba y nombra la propiedad")
    void rechazaPruebaEnProduccion() {
        en("production").withPropertyValues(MODO + "=prueba").run(c -> {
            assertThat(c).hasFailed();
            assertThat(causaRaiz(c.getStartupFailure())).contains("aportaya.recarga.qr.modo=prueba");
        });
    }

    @Test
    @DisplayName("correcto: arranca en produccion con el proveedor real")
    void arrancaConProveedor() {
        en("production").withPropertyValues(MODO + "=proveedor").run(c -> assertThat(c)
                .hasNotFailed());
    }

    @Test
    @DisplayName("límite: arranca en produccion sin la propiedad (por omision es proveedor)")
    void arrancaSinPropiedad() {
        en("production").run(c -> assertThat(c).hasNotFailed());
    }

    @Test
    @DisplayName("límite: en TEST (perfil local) el modo prueba sigue disponible")
    void arrancaEnLocalConPrueba() {
        en("local").withPropertyValues(MODO + "=prueba").run(c -> assertThat(c).hasNotFailed());
    }
}
