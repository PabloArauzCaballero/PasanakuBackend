package bo.aportaya.cumplimiento.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.cumplimiento.aplicacion.CU02ElevarDiligencia;
import bo.aportaya.cumplimiento.aplicacion.CU03DeclararPep;
import bo.aportaya.cumplimiento.aplicacion.CU05AceptarContrato;
import bo.aportaya.cumplimiento.aplicacion.CU05ConsultarContratosVigentes;
import bo.aportaya.cumplimiento.aplicacion.CU54RegistrarRiesgoOperativo;
import bo.aportaya.cumplimiento.aplicacion.CU55GestionarIncidente;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejasLft;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
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
 * Backoffice carril A · H5 · el contrato HTTP de las bandejas LFT ({@code /cumplimiento/alertas} y
 * {@code /cumplimiento/casos}).
 *
 * <p>Dos permisos distintos y ninguno abre el otro; la fila no lleva a la persona (deber de reserva).
 * Lo que hace la consulta contra la base vive en {@code CU44BandejaTest}.
 */
@PruebaWeb(CumplimientoController.class)
class LftBandejaWebTest {

    private static final UUID ALERTA = UUID.fromString("c1000000-0000-4000-8000-0000000000a1");
    private static final UUID CASO = UUID.fromString("c1000000-0000-4000-8000-0000000000a2");
    private static final UUID ANALISTA = UUID.fromString("c1000000-0000-4000-8000-0000000000a3");
    private static final OffsetDateTime CUANDO = OffsetDateTime.of(2026, 10, 1, 14, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU02ElevarDiligencia cu02;

    @MockitoBean
    private CU03DeclararPep cu03;

    @MockitoBean
    private CU05AceptarContrato cu05;

    @MockitoBean
    private CU05ConsultarContratosVigentes contratosVigentes;

    @MockitoBean
    private CU54RegistrarRiesgoOperativo cu54;

    @MockitoBean
    private CU55GestionarIncidente cu55;

    @MockitoBean
    private ConsultarBandejasLft bandejas;

    @MockitoBean
    private bo.aportaya.cumplimiento.aplicacion.CU44InvestigarYReportar cu44;

    @MockitoBean
    private bo.aportaya.cumplimiento.aplicacion.CU44DecidirComoRevisor decisionLft;

    @Test
    @DisplayName("200: con CUMPLIMIENTO_ALERTAS la cola trae regla, severidad y estado, sin persona ni monto")
    void alertas() throws Exception {
        when(bandejas.alertas(any(), any(), any(), any(), eq(25), any()))
                .thenReturn(new ConsultarBandejasLft.PaginaDeAlertas(
                        List.of(new ConsultarBandejasLft.Alerta(
                                ALERTA,
                                "RM-FRACCIONAMIENTO",
                                "Fraccionamiento",
                                "ALTA",
                                "ABIERTA",
                                CUANDO,
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty())),
                        Optional.empty(),
                        Optional.empty()));

        String cuerpo = mvc.perform(get("/cumplimiento/alertas")
                        .with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "CUMPLIMIENTO_ALERTAS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].alertaId").value(ALERTA.toString()))
                .andExpect(jsonPath("$.items[0].severidad").value("ALTA"))
                .andExpect(jsonPath("$.items[0].reglaCodigo").value("RM-FRACCIONAMIENTO"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(cuerpo).doesNotContain("usuarioId").doesNotContain("monto").doesNotContain("detalle");
    }

    @Test
    @DisplayName("200: los filtros de estado y severidad llegan enteros al caso de uso")
    void filtros() throws Exception {
        when(bandejas.alertas(any(), any(), any(), any(), any(Integer.class), any()))
                .thenReturn(new ConsultarBandejasLft.PaginaDeAlertas(List.of(), Optional.empty(), Optional.empty()));

        mvc.perform(get("/cumplimiento/alertas")
                        .param("estados", "ESCALADA")
                        .param("severidades", "CRITICA,ALTA")
                        .with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "CUMPLIMIENTO_ALERTAS")))
                .andExpect(status().isOk());
        verify(bandejas).alertas(eq(List.of("ESCALADA")), eq(List.of("CRITICA", "ALTA")), any(), any(), eq(25), any());
    }

    @Test
    @DisplayName("200: con CUMPLIMIENTO_CASOS la cola de casos trae plazo y vencido, sin persona ni resumen")
    void casos() throws Exception {
        when(bandejas.casos(any(), any(), any(), eq(25), any()))
                .thenReturn(new ConsultarBandejasLft.PaginaDeCasos(
                        List.of(new ConsultarBandejasLft.Caso(
                                CASO,
                                "LFT-2026-0001",
                                "ALERTA",
                                "ABIERTO",
                                "ALTA",
                                ANALISTA,
                                Optional.empty(),
                                CUANDO,
                                CUANDO.plusDays(30),
                                false,
                                Optional.empty(),
                                Optional.empty())),
                        Optional.empty(),
                        Optional.empty()));

        String cuerpo = mvc.perform(
                        get("/cumplimiento/casos").with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "CUMPLIMIENTO_CASOS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].casoId").value(CASO.toString()))
                .andExpect(jsonPath("$.items[0].vencido").value(false))
                .andExpect(jsonPath("$.items[0].analistaId").value(ANALISTA.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(cuerpo).doesNotContain("usuarioId").doesNotContain("resumen").doesNotContain("hallazgos");
    }

    @Test
    @DisplayName("403: el permiso de alertas no abre la cola de casos, ni al reves")
    void permisosSeparados() throws Exception {
        mvc.perform(get("/cumplimiento/casos").with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "CUMPLIMIENTO_ALERTAS")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/cumplimiento/alertas").with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "CUMPLIMIENTO_CASOS")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/cumplimiento/alertas").with(Sesiones.como("SOPORTE", "RECLAMO_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(bandejas);
    }

    @Test
    @DisplayName("422: un filtro invalido sale con AP-CU44-05; 400 si el limite pasa de 100; 401 sin sesion")
    void errores() throws Exception {
        when(bandejas.casos(any(), any(), any(), any(Integer.class), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(44, 5), "Filtro de bandeja invalido."));

        mvc.perform(get("/cumplimiento/casos")
                        .param("estados", "ARCHIVADO")
                        .with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "CUMPLIMIENTO_CASOS")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU44-05"));
        mvc.perform(get("/cumplimiento/alertas")
                        .param("limite", "101")
                        .with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "CUMPLIMIENTO_ALERTAS")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/cumplimiento/alertas")).andExpect(status().isUnauthorized());
    }
}
