package bo.aportaya.aportes.infraestructura.pasarela;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.dominio.PasarelaDePagos;
import bo.aportaya.aportes.dominio.ProveedorDeSecretoWebhook;
import bo.aportaya.aportes.infraestructura.pasarela.real.PasarelaRealNoImplementada;
import bo.aportaya.aportes.infraestructura.pasarela.simulada.GuardiaDePasarelaSimulada;
import bo.aportaya.aportes.infraestructura.pasarela.simulada.InterpreteDeWebhookJson;
import bo.aportaya.aportes.infraestructura.pasarela.simulada.PasarelaSimulada;
import bo.aportaya.plataforma.dominio.Reloj;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * La seleccion por configuracion y el fail-closed (regla 90.3.4): el modo simulado solo arranca en local/test.
 * Sin base ni red: solo los beans de la pasarela, con {@link ApplicationContextRunner}.
 */
class PasarelaConfiguracionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(
                    ProveedorDeSecretoWebhook.class,
                    () -> codigo -> Optional.of("secreto-sintetico-de-la-prueba-0001-xx"))
            .withBean(Reloj.class, Reloj::delSistema)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(
                    PasarelaSimulada.class,
                    InterpreteDeWebhookJson.class,
                    GuardiaDePasarelaSimulada.class,
                    PasarelaRealNoImplementada.class)
            .withPropertyValues("aportaya.aportes.pasarela.proveedor-codigo=QR_INTEROP");

    @Test
    @DisplayName("correcto: perfil local o test con modo simulada (o sin modo) levanta y expone el puerto")
    void localYTestLevantan() {
        runner.withPropertyValues("spring.profiles.active=local", "aportaya.aportes.pasarela.modo=simulada")
                .run(ctx -> assertThat(ctx).hasNotFailed().hasSingleBean(PasarelaDePagos.class));
        runner.withPropertyValues("spring.profiles.active=test")
                .run(ctx -> assertThat(ctx).hasNotFailed().hasSingleBean(PasarelaDePagos.class));
    }

    @Test
    @DisplayName(
            "invalido: production, staging o sin perfil con modo simulada NO arrancan y el mensaje nombra la propiedad")
    void fueraDeLocalNoArranca() {
        for (String perfil : new String[] {"production", "staging"}) {
            runner.withPropertyValues("spring.profiles.active=" + perfil, "aportaya.aportes.pasarela.modo=simulada")
                    .run(ctx -> assertThat(ctx)
                            .hasFailed()
                            .getFailure()
                            .hasStackTraceContaining("aportaya.aportes.pasarela.modo=simulada"));
        }
        runner.run(ctx -> assertThat(ctx).hasFailed()); // sin perfil y sin modo: el defecto es simulada => falla
    }

    @Test
    @DisplayName("limite: modo=real no cae en el doble: sin adaptador real el arranque se niega diciendo por que")
    void modoRealSinAdaptadorFalla() {
        runner.withPropertyValues("spring.profiles.active=production", "aportaya.aportes.pasarela.modo=real")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().hasStackTraceContaining("no hay adaptador real"));
    }
}
