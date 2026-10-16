package bo.aportaya.tarifas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.tarifas.aplicacion.CU34PublicarTarifario;
import bo.aportaya.tarifas.aplicacion.CU35CerrarLiquidacion;
import bo.aportaya.tarifas.aplicacion.CU36ResolverPrecio;
import bo.aportaya.tarifas.aplicacion.ConsultarTarifarioVigente;
import bo.aportaya.tarifas.aplicacion.ConsultarTarifarios;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Backoffice carril A · A6 · {@code GET /tarifas/tarifarios}: el historial de tarifarios. Permiso
 * (TARIFARIO_PUBLICAR o AUDITORIA_LEER), forma de la respuesta y rechazos con codigo. El recorrido
 * real vive en {@code CU34TarifariosTest}.
 */
@PruebaWeb(TarifasController.class)
class TarifariosListadoWebTest {

    private static final UUID NUEVO = UUID.fromString("c6000000-0000-4000-8000-000000000001");
    private static final UUID ANTERIOR = UUID.fromString("c6000000-0000-4000-8000-000000000002");
    private static final UUID CAMBIO = UUID.fromString("c6000000-0000-4000-8000-000000000003");
    private static final OffsetDateTime DESDE = OffsetDateTime.of(2026, 11, 15, 0, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarTarifarioVigente vigentes;

    @MockitoBean
    private ConsultarTarifarios historial;

    @MockitoBean
    private CU34PublicarTarifario cu34;

    @MockitoBean
    private CU35CerrarLiquidacion cu35;

    @MockitoBean
    private CU36ResolverPrecio cu36;

    private static ConsultarTarifarios.Pagina unaEnPreaviso(boolean llena) {
        var t = new ConsultarTarifarios.Tarifario(
                NUEVO,
                "TAR-BASE",
                2,
                "Version nueva",
                "EN_PREAVISO",
                "BOB",
                DESDE,
                Optional.empty(),
                30,
                Optional.of(DESDE.minusDays(30)),
                Optional.of(ANTERIOR),
                Optional.empty(),
                Optional.of("ACTA-2026-07"),
                Optional.of(new ConsultarTarifarios.Cambio(
                        CAMBIO,
                        "INCREMENTO",
                        true,
                        30,
                        Optional.of(DESDE.minusDays(30)),
                        Optional.of("CORREO"),
                        340,
                        false)),
                Optional.of(new ConsultarTarifarios.Simulacion(new BigDecimal("1200.5"), 340, DESDE.minusDays(31))));
        return new ConsultarTarifarios.Pagina(
                List.of(t),
                llena ? Optional.of(DESDE) : Optional.empty(),
                llena ? Optional.of(NUEVO) : Optional.empty());
    }

    @Test
    @DisplayName(
            "200: TARIFARIO_PUBLICAR ve la version con su preaviso, su cambio, la simulacion sin moneda y el cursor")
    void plataformaVe() throws Exception {
        when(historial.listar(any(), any(), any(), any(), eq(1), any())).thenReturn(unaEnPreaviso(true));

        mvc.perform(get("/tarifas/tarifarios")
                        .param("codigo", "TAR-BASE")
                        .param("limite", "1")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_PUBLICAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].tarifarioId").value(NUEVO.toString()))
                .andExpect(jsonPath("$.items[0].estado").value("EN_PREAVISO"))
                .andExpect(jsonPath("$.items[0].monedaBase").value("BOB"))
                .andExpect(jsonPath("$.items[0].diasPreaviso").value(30))
                .andExpect(jsonPath("$.items[0].tarifarioAnteriorId").value(ANTERIOR.toString()))
                .andExpect(jsonPath("$.items[0].cambio.tipoCambio").value("INCREMENTO"))
                .andExpect(jsonPath("$.items[0].cambio.requierePreaviso").value(true))
                .andExpect(
                        jsonPath("$.items[0].simulacion.deltaIngresoEstimado").value("1200.50"))
                .andExpect(jsonPath("$.items[0].simulacion.moneda").doesNotExist())
                .andExpect(jsonPath("$.proximoAntesDeId").value(NUEVO.toString()));
        verify(historial)
                .listar(eq(Optional.of("TAR-BASE")), any(), eq(Optional.empty()), eq(Optional.empty()), eq(1), any());
    }

    @Test
    @DisplayName("200: AUDITORIA_LEER tambien lo lee; sin parametros pasa codigo vacio y el default 25")
    void auditoriaLee() throws Exception {
        when(historial.listar(any(), any(), any(), any(), eq(25), any())).thenReturn(unaEnPreaviso(false));

        mvc.perform(get("/tarifas/tarifarios").with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximoAntesDeId").doesNotExist());
        verify(historial)
                .listar(eq(Optional.empty()), any(), eq(Optional.empty()), eq(Optional.empty()), eq(25), any());
    }

    @Test
    @DisplayName("403: quien no publica tarifarios ni audita no ve el historial")
    void sinPermiso() throws Exception {
        mvc.perform(get("/tarifas/tarifarios").with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        verifyNoInteractions(historial);
    }

    @Test
    @DisplayName("401: sin token no hay historial")
    void sinToken() throws Exception {
        mvc.perform(get("/tarifas/tarifarios")).andExpect(status().isUnauthorized());
        verifyNoInteractions(historial);
    }

    @Test
    @DisplayName("422: un estado desconocido sale con AP-CU34-05, nunca como 500")
    void estadoDesconocido() throws Exception {
        when(historial.listar(any(), any(), any(), any(), any(Integer.class), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(34, 5), "Filtro de listado invalido."));

        mvc.perform(get("/tarifas/tarifarios")
                        .param("estados", "PUBLICADO")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_PUBLICAR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU34-05"));
    }

    @Test
    @DisplayName("400: limite fuera de 1..100, cursor que no es UUID o codigo de mas de 30 lo corta el contrato")
    void entradaInvalida() throws Exception {
        var sesion = Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_PUBLICAR");
        mvc.perform(get("/tarifas/tarifarios").param("limite", "101").with(sesion))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/tarifas/tarifarios").param("antesDeId", "x").with(sesion))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/tarifas/tarifarios").param("codigo", "C".repeat(31)).with(sesion))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(historial);
    }
}
