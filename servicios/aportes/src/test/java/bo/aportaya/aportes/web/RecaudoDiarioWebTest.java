package bo.aportaya.aportes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.ConsultarEstadoDelParticipante;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDelPeriodo;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDiario;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** El contrato HTTP del recaudo diario que pide nucleo-financiero para cerrar el dia (CU-51 · carril A · H8). */
@PruebaWeb(AportesController.class)
class RecaudoDiarioWebTest {

    private static final LocalDate DIA = LocalDate.of(2026, 3, 10);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU21CobrarAporte cu21;

    @MockitoBean
    private ConsultarEstadoDelParticipante estados;

    @MockitoBean
    private bo.aportaya.aportes.aplicacion.GenerarObligacionesDelPeriodo generarObligaciones;

    @MockitoBean
    private ConsultarRecaudoDelPeriodo recaudos;

    @MockitoBean
    private bo.aportaya.aportes.aplicacion.HechosDeGrupos grupos;

    @MockitoBean
    private ConsultarRecaudoDiario recaudosDiarios;

    private static ConsultarRecaudoDiario.Salida unDia() {
        return new ConsultarRecaudoDiario.Salida(
                DIA,
                "America/La_Paz",
                List.of(new ConsultarRecaudoDiario.PorMoneda(
                        "BOB", new BigDecimal("800.00"), new BigDecimal("500.00"), new BigDecimal("20.00"), 2)),
                1);
    }

    @Test
    @DisplayName("200: CONTABILIDAD recibe los totales por moneda como cadena al centavo y la zona del corte")
    void contabilidadLee() throws Exception {
        when(recaudosDiarios.ejecutar(eq(DIA), any())).thenReturn(unDia());

        mvc.perform(get("/aportes/recaudo-diario").param("fecha", "2026-03-10").with(Sesiones.como("CONTABILIDAD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zonaHoraria").value("America/La_Paz"))
                .andExpect(jsonPath("$.excepcionesAbiertas").value(1))
                .andExpect(jsonPath("$.porMoneda[0].moneda").value("BOB"))
                .andExpect(jsonPath("$.porMoneda[0].totalRecaudado").value("800.00"))
                .andExpect(jsonPath("$.porMoneda[0].totalConciliado").value("500.00"))
                .andExpect(jsonPath("$.porMoneda[0].totalExcepciones").value("20.00"))
                .andExpect(jsonPath("$.porMoneda[0].cantidadPagos").value(2));
    }

    @Test
    @DisplayName("200: AUDITORIA_LEER tambien lo lee")
    void auditoriaLee() throws Exception {
        when(recaudosDiarios.ejecutar(eq(DIA), any())).thenReturn(unDia());

        mvc.perform(get("/aportes/recaudo-diario")
                        .param("fecha", "2026-03-10")
                        .with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("403: quien cobra aportes (ENTREGA_EJECUTAR) no ve los totales del dia")
    void sinPermiso() throws Exception {
        mvc.perform(get("/aportes/recaudo-diario")
                        .param("fecha", "2026-03-10")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recaudosDiarios);
    }

    @Test
    @DisplayName("401: sin token no hay recaudo")
    void sinToken() throws Exception {
        mvc.perform(get("/aportes/recaudo-diario").param("fecha", "2026-03-10")).andExpect(status().isUnauthorized());
        verifyNoInteractions(recaudosDiarios);
    }

    @Test
    @DisplayName("400: sin fecha, o con una fecha mal formada, lo corta el contrato")
    void sinFecha() throws Exception {
        mvc.perform(get("/aportes/recaudo-diario").with(Sesiones.como("CONTABILIDAD")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/aportes/recaudo-diario").param("fecha", "10/03/2026").with(Sesiones.como("CONTABILIDAD")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(recaudosDiarios);
    }
}
