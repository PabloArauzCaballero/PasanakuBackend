package bo.aportaya.cumplimiento.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.cumplimiento.aplicacion.CU46VerificarAlcance;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-46 · si la plataforma tiene licencia para prestar este servicio.
 *
 * <p>Lo que se fija aca: que la respuesta diga <b>por qué vía</b> está habilitado —
 * licencia plena o entorno de prueba regulado— y, cuando es entorno de prueba, <b>con
 * qué límites</b>. Un «sí» sin la vía deja al llamador operando sin saber que está
 * dentro de un sandbox con tope de usuarios y de monto, que es la forma más rápida de
 * pasarse de lo que el regulador autorizó.
 */
@PruebaWeb(LicenciaController.class)
class LicenciaControllerWebTest {

    private static final UUID USUARIO = UUID.fromString("c0000000-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU46VerificarAlcance cu46;

    private org.springframework.test.web.servlet.ResultActions pedir(String servicio) throws Exception {
        return mvc.perform(get("/licencia/alcance")
                .param("servicio", servicio)
                .param("usuarioId", USUARIO.toString())
                .with(Sesiones.como("SOPORTE")));
    }

    @Test
    @DisplayName("CU-46 · habilitado por licencia plena: sin límites de sandbox")
    void habilitadoPorLicencia() throws Exception {
        when(cu46.ejecutar(any(), any()))
                .thenReturn(new CU46VerificarAlcance.SalidaAlcance(true, "LICENCIA", null, "licencia vigente"));

        pedir("BILLETERA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(true))
                .andExpect(jsonPath("$.via").value("LICENCIA"))
                .andExpect(jsonPath("$.limitesSandbox").doesNotExist());
    }

    @Test
    @DisplayName("CU-46 · habilitado por entorno de prueba: la respuesta trae los topes")
    void habilitadoPorSandboxTraeSusLimites() throws Exception {
        when(cu46.ejecutar(any(), any()))
                .thenReturn(new CU46VerificarAlcance.SalidaAlcance(
                        true,
                        "SANDBOX",
                        new CU46VerificarAlcance.LimitesSandbox(500, "1000.00"),
                        "entorno de prueba regulado"));

        pedir("BILLETERA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(true))
                .andExpect(jsonPath("$.via").value("SANDBOX"))
                // Un «si» sin los topes deja al llamador operando fuera de lo
                // autorizado sin enterarse.
                .andExpect(jsonPath("$.limitesSandbox.usuarios").value(500))
                .andExpect(jsonPath("$.limitesSandbox.montoOperacion").value("1000.00"));
    }

    @Test
    @DisplayName("CU-46 · no habilitado: dice que no y por qué, y no inventa límites")
    void noHabilitado() throws Exception {
        when(cu46.ejecutar(any(), any()))
                .thenReturn(new CU46VerificarAlcance.SalidaAlcance(
                        false, "NINGUNA", null, "el servicio no esta en el alcance de la licencia"));

        pedir("CREDITO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(false))
                .andExpect(jsonPath("$.motivo").exists())
                .andExpect(jsonPath("$.limitesSandbox").doesNotExist());
    }

    @Test
    @DisplayName("CU-46 · el servicio consultado llega tal cual al caso de uso")
    void loQueLlegaAlCasoDeUso() throws Exception {
        when(cu46.ejecutar(any(), any()))
                .thenReturn(new CU46VerificarAlcance.SalidaAlcance(true, "LICENCIA", null, ""));

        pedir("BILLETERA").andExpect(status().isOk());

        var capturada = ArgumentCaptor.forClass(CU46VerificarAlcance.EntradaAlcance.class);
        verify(cu46).ejecutar(capturada.capture(), any());
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().servicio())
                .isEqualTo("BILLETERA");
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().usuarioId())
                .isEqualTo(Optional.of(USUARIO));
    }

    @Test
    @DisplayName("CU-46 · 400: sin el servicio a verificar, que el contrato exige")
    void faltaElServicio() throws Exception {
        mvc.perform(get("/licencia/alcance").with(Sesiones.como("SOPORTE"))).andExpect(status().isBadRequest());
        verifyNoInteractions(cu46);
    }
}
