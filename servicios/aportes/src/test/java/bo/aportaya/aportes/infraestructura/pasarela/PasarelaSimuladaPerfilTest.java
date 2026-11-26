package bo.aportaya.aportes.infraestructura.pasarela;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import bo.aportaya.aportes.aplicacion.CU100EmitirAvisoDePagoSimulado;
import bo.aportaya.aportes.web.PasarelaSimuladaController;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Fail-closed del endpoint que dispara el aviso simulado: el controlador y el caso de uso solo EXISTEN en {@code local}/
 * {@code test} con la pasarela simulada. Sin bean no hay ruta mapeada: 404. Sin base ni red.
 */
class PasarelaSimuladaPerfilTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(CU100EmitirAvisoDePagoSimulado.class, () -> mock(CU100EmitirAvisoDePagoSimulado.class))
            .withBean(SesionDeLaPeticion.class, () -> mock(SesionDeLaPeticion.class))
            .withUserConfiguration(PasarelaSimuladaController.class);

    @Test
    @DisplayName("correcto: local y test con modo simulada (o sin modo) registran el controlador")
    void existeEnLocalYTest() {
        runner.withPropertyValues("spring.profiles.active=local", "aportaya.aportes.pasarela.modo=simulada")
                .run(ctx -> assertThat(ctx).hasSingleBean(PasarelaSimuladaController.class));
        runner.withPropertyValues("spring.profiles.active=test")
                .run(ctx -> assertThat(ctx).hasSingleBean(PasarelaSimuladaController.class));
    }

    @Test
    @DisplayName("invalido: production, staging o sin perfil NO registran el controlador (la ruta no existe: 404)")
    void noExisteFueraDeLocalNiTest() {
        for (String perfil : new String[] {"production", "staging"}) {
            runner.withPropertyValues("spring.profiles.active=" + perfil)
                    .run(ctx -> assertThat(ctx).doesNotHaveBean(PasarelaSimuladaController.class));
        }
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(PasarelaSimuladaController.class));
    }

    @Test
    @DisplayName(
            "limite: en local pero con modo=real tampoco existe (el doble no se expone si la pasarela no es el doble)")
    void noExisteConModoReal() {
        runner.withPropertyValues("spring.profiles.active=local", "aportaya.aportes.pasarela.modo=real")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(PasarelaSimuladaController.class));
    }
}
