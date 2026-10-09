package bo.aportaya.aportes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.ConsultarEstadoDelParticipante;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDelPeriodo;
import bo.aportaya.aportes.dominio.RecaudoDelPeriodo;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** El contrato HTTP de la consulta del recaudo del periodo. */
@PruebaWeb(AportesController.class)
class RecaudoWebTest {

    private static final UUID PERIODO = UUID.fromString("d4000000-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("d4000000-0000-4000-8000-000000000009");
    private static final UUID OBLIGACION = UUID.fromString("d4000000-0000-4000-8000-000000000002");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU21CobrarAporte cu21;

    @MockitoBean
    private ConsultarEstadoDelParticipante estados;

    @MockitoBean
    private ConsultarRecaudoDelPeriodo recaudos;

    @MockitoBean
    private bo.aportaya.aportes.aplicacion.HechosDeGrupos grupos;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName("GET /recaudo · 200 con los importes como cadena decimal y los pendientes por obligacion")
    void recaudo() throws Exception {
        when(recaudos.ejecutar(any(), any()))
                .thenReturn(new ConsultarRecaudoDelPeriodo.Salida(
                        PERIODO,
                        GRUPO,
                        OffsetDateTime.parse("2026-10-08T12:00:00Z"),
                        new RecaudoDelPeriodo.Resultado(
                                bob("6000.00"),
                                bob("5000.00"),
                                bob("0.00"),
                                bob("1000.00"),
                                List.of(new RecaudoDelPeriodo.Pendiente(OBLIGACION, bob("1000.00"))))));

        mvc.perform(get("/aportes/periodos/{id}/recaudo", PERIODO).with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupoId").value(GRUPO.toString()))
                .andExpect(jsonPath("$.pozo.monto").value("6000.00"))
                .andExpect(jsonPath("$.faltante.monto").value("1000.00"))
                .andExpect(jsonPath("$.pendientes[0].obligacionId").value(OBLIGACION.toString()))
                .andExpect(jsonPath("$.pendientes[0].monto.monto").value("1000.00"));
    }

    @Test
    @DisplayName("GET /recaudo · 403 sin el permiso de ejecutar la entrega y 401 sin sesion; no llega al caso de uso")
    void sinPermiso() throws Exception {
        mvc.perform(get("/aportes/periodos/{id}/recaudo", PERIODO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/aportes/periodos/{id}/recaudo", PERIODO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(recaudos);
    }

    @Test
    @DisplayName(
            "GET /recaudo · 422 con el codigo del contrato cuando el periodo no tiene obligaciones; 400 con un id mal formado")
    void errores() throws Exception {
        when(recaudos.ejecutar(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(21, 5), "Ese periodo no tiene obligaciones."));

        mvc.perform(get("/aportes/periodos/{id}/recaudo", PERIODO).with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU21-05"));
        mvc.perform(get("/aportes/periodos/no-es-un-uuid/recaudo").with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR")))
                .andExpect(status().isBadRequest());
    }
}
