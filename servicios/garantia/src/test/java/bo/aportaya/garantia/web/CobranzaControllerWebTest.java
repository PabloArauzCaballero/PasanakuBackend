package bo.aportaya.garantia.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.garantia.aplicacion.CU27RestringirDeudor;
import bo.aportaya.garantia.aplicacion.ConsultarRestriccion;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-27 · la restricción de un deudor, y cómo se sale de ella.
 *
 * <p>Una restricción es una sanción, y el patrón de {@code debido-proceso} exige que
 * <b>se pueda levantar</b> y que el afectado sepa <b>qué tiene que pagar</b> para que se
 * levante. Eso último es lo que fija esta clase: {@code montoQueLaLevanta} viaja
 * siempre, como cadena decimal. Una restricción sin salida escrita es una restricción
 * perpetua de hecho.
 */
@PruebaWeb(CobranzaController.class)
class CobranzaControllerWebTest {

    private static final UUID USUARIO = UUID.fromString("b4000000-0000-4000-8000-000000000001");
    private static final UUID RESTRICCION = UUID.fromString("b4000000-0000-4000-8000-000000000002");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarRestriccion restricciones;

    @MockitoBean
    private CU27RestringirDeudor cu27;

    @Test
    @DisplayName("CU-27 · 200 con la restricción vigente y CUÁNTO hay que pagar para levantarla")
    void restriccionVigenteDiceComoSalir() throws Exception {
        when(restricciones.ejecutar(any(), any()))
                .thenReturn(new ConsultarRestriccion.Restriccion(true, "TOTAL", new BigDecimal("700.00")));

        mvc.perform(get("/cobranza/restricciones/vigentes/{id}", USUARIO)
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vigente").value(true))
                .andExpect(jsonPath("$.nivel").value("TOTAL"))
                // Sin este monto, la restriccion no tiene salida escrita y en los hechos
                // es perpetua.
                .andExpect(jsonPath("$.montoQueLaLevanta").value("700.00"));
    }

    @Test
    @DisplayName("CU-27 · sin restricción vigente, el monto es cero y no nulo")
    void sinRestriccion() throws Exception {
        when(restricciones.ejecutar(any(), any()))
                .thenReturn(new ConsultarRestriccion.Restriccion(false, null, BigDecimal.ZERO));

        mvc.perform(get("/cobranza/restricciones/vigentes/{id}", USUARIO)
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vigente").value(false))
                // Cero y no ausente: el contrato lo marca obligatorio, y un cliente que
                // recibiera null tendria que adivinar si es cero o si no se sabe.
                .andExpect(jsonPath("$.montoQueLaLevanta").value("0.00"));
    }

    @Test
    @DisplayName("CU-27 · levantar una restricción exige un motivo escrito")
    void levantarConMotivo() throws Exception {
        when(cu27.levantar(eq(RESTRICCION), any(), any())).thenReturn(true);

        mvc.perform(post("/cobranza/restricciones/{id}/levantamiento", RESTRICCION)
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"La deuda se regularizo el 2026-03-10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levantada").value(true));

        // El motivo llega entero: es la constancia de por que se levanto una sancion.
        verify(cu27).levantar(eq(RESTRICCION), eq("La deuda se regularizo el 2026-03-10"), any());
    }

    @Test
    @DisplayName("CU-27 · 400: levantar sin motivo — una sanción no se levanta sin constancia")
    void levantarSinMotivo() throws Exception {
        mvc.perform(post("/cobranza/restricciones/{id}/levantamiento", RESTRICCION)
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu27);
    }

    @Test
    @DisplayName("CU-27 · 400: un identificador de usuario que no es un identificador")
    void usuarioQueNoEsUuid() throws Exception {
        mvc.perform(get("/cobranza/restricciones/vigentes/{id}", "el-moroso")
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(restricciones);
    }
}
