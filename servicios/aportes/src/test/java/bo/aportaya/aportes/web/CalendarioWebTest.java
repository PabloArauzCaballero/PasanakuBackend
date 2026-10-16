package bo.aportaya.aportes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.ConsultarEstadoDelParticipante;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDelPeriodo;
import bo.aportaya.aportes.aplicacion.GenerarObligacionesDelPeriodo;
import bo.aportaya.aportes.dominio.VencimientoDeObligacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** El contrato HTTP de la generacion del calendario de obligaciones de un periodo. */
@PruebaWeb(AportesController.class)
class CalendarioWebTest {

    private static final UUID PERIODO = UUID.fromString("d5000000-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("d5000000-0000-4000-8000-000000000009");
    private static final String CLAVE = "d5000000-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU21CobrarAporte cu21;

    @MockitoBean
    private ConsultarEstadoDelParticipante estados;

    @MockitoBean
    private ConsultarRecaudoDelPeriodo recaudos;

    @MockitoBean
    private GenerarObligacionesDelPeriodo generar;

    @MockitoBean
    private bo.aportaya.aportes.aplicacion.HechosDeGrupos grupos;

    private static GenerarObligacionesDelPeriodo.Salida elCalendario() {
        var linea = new GenerarObligacionesDelPeriodo.Linea(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                Dinero.de("500.00", Moneda.BOB),
                "PENDIENTE",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 7),
                true);
        return new GenerarObligacionesDelPeriodo.Salida(
                PERIODO,
                GRUPO,
                1,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 4),
                true,
                List.of(new VencimientoDeObligacion.DiaSalteado(LocalDate.of(2026, 5, 1), "feriado nacional")),
                1,
                List.of(linea));
    }

    @Test
    @DisplayName(
            "POST /obligaciones · 200 con titular, turno, monto, estado y las DOS fechas (contractual y de liquidacion)")
    void devuelveElCalendario() throws Exception {
        when(generar.generar(any(), any())).thenReturn(elCalendario());

        mvc.perform(post("/aportes/periodos/{id}/obligaciones", PERIODO)
                        .header("Idempotency-Key", CLAVE)
                        .with(Sesiones.como("OPERADOR", "ADMIN_PLATAFORMA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaContractual").value("2026-05-01"))
                .andExpect(jsonPath("$.fechaDeLiquidacion").value("2026-05-04"))
                .andExpect(jsonPath("$.corridaPorNoHabil").value(true))
                .andExpect(jsonPath("$.creadas").value(1))
                .andExpect(jsonPath("$.diasSalteados[0].motivo").value("feriado nacional"))
                .andExpect(jsonPath("$.obligaciones[0].monto.monto").value("500.00"))
                .andExpect(jsonPath("$.obligaciones[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.obligaciones[0].turno").value(1))
                .andExpect(jsonPath("$.obligaciones[0].esNueva").value(true));
        verify(generar).generar(any(), any());
    }

    @Test
    @DisplayName("POST /obligaciones · 422 AP-CU21-09 cuando no hay calendario de feriados del anio")
    void sinCalendario() throws Exception {
        when(generar.generar(any(), any()))
                .thenThrow(
                        new ErrorDeNegocio(CodigoError.de(21, 9), "No hay calendario de feriados cargado para 2031."));

        mvc.perform(post("/aportes/periodos/{id}/obligaciones", PERIODO)
                        .header("Idempotency-Key", CLAVE)
                        .with(Sesiones.como("OPERADOR", "ADMIN_PLATAFORMA")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU21-09"));
    }

    @Test
    @DisplayName(
            "POST /obligaciones · 400 sin Idempotency-Key, 401 sin sesion, 403 sin permiso y 400 con un id que no es un id")
    void guardias() throws Exception {
        mvc.perform(post("/aportes/periodos/{id}/obligaciones", PERIODO)
                        .with(Sesiones.como("OPERADOR", "ADMIN_PLATAFORMA")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/aportes/periodos/{id}/obligaciones", PERIODO).header("Idempotency-Key", CLAVE))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/aportes/periodos/{id}/obligaciones", PERIODO)
                        .header("Idempotency-Key", CLAVE)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/aportes/periodos/{id}/obligaciones", "no-es-un-id")
                        .header("Idempotency-Key", CLAVE)
                        .with(Sesiones.como("OPERADOR", "ADMIN_PLATAFORMA")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(generar);
    }

    @Test
    @DisplayName("POST /obligaciones · el cuerpo no puede afirmar nada: lo que se escribe lo afirma grupos")
    void elCuerpoNoSeAcepta() throws Exception {
        when(generar.generar(any(), any())).thenReturn(elCalendario());

        // Un cuerpo con titular e importe inventados no cambia el resultado: el caso de uso solo recibe el id del
        // periodo.
        mvc.perform(post("/aportes/periodos/{id}/obligaciones", PERIODO)
                        .header("Idempotency-Key", CLAVE)
                        .contentType("application/json")
                        .content("{\"monto\":\"1.00\",\"titular\":\"" + UUID.randomUUID() + "\"}")
                        .with(Sesiones.como("OPERADOR", "ADMIN_PLATAFORMA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligaciones[0].monto.monto").value("500.00"));
        verify(generar).generar(org.mockito.ArgumentMatchers.eq(PERIODO), any());
    }

    // El controlador tambien responde el recaudo diario (CU-51, carril A · H8): sin este doble no carga.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private bo.aportaya.aportes.aplicacion.ConsultarRecaudoDiario recaudosDiarios;
}
