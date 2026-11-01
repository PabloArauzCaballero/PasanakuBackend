package bo.aportaya.transparencia.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.transparencia.aplicacion.CU70RegistrarEventoReputacion;
import bo.aportaya.transparencia.aplicacion.CU71RecalcularPuntaje;
import bo.aportaya.transparencia.aplicacion.CU72SellarBloque;
import bo.aportaya.transparencia.aplicacion.CU74EvaluarInsignias;
import bo.aportaya.transparencia.aplicacion.CU75EmitirCertificado;
import bo.aportaya.transparencia.aplicacion.CU76PublicarResena;
import bo.aportaya.transparencia.aplicacion.CU97EvaluarRiesgo;
import bo.aportaya.transparencia.aplicacion.ConsultarEtapasDePersonas;
import bo.aportaya.transparencia.aplicacion.ConsultarPerfilEnGrupo;
import bo.aportaya.transparencia.aplicacion.ConsultarPuntaje;
import bo.aportaya.transparencia.aplicacion.ListarEtapas;
import bo.aportaya.transparencia.dominio.EtapasDeReputacion;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** El contrato HTTP del catalogo de etapas y de la etapa del puntaje propio (2026-10-30). */
@PruebaWeb(ReputacionController.class)
@MockitoBean(
        types = {
            CU70RegistrarEventoReputacion.class,
            CU71RecalcularPuntaje.class,
            CU72SellarBloque.class,
            CU74EvaluarInsignias.class,
            CU75EmitirCertificado.class,
            CU76PublicarResena.class,
            CU97EvaluarRiesgo.class,
            ConsultarPerfilEnGrupo.class,
            ConsultarEtapasDePersonas.class
        })
@DisplayName("Etapas (2026-10-30) — catálogo y etapa del puntaje")
class EtapasWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarPuntaje puntajes;

    @MockitoBean
    private ListarEtapas etapas;

    private static final EtapasDeReputacion.Etapa INICIAL = new EtapasDeReputacion.Etapa(
            "INICIAL",
            "Etapa 1 · Inicial",
            1,
            new BigDecimal("500.00"),
            new BigDecimal("550.00"),
            new EtapasDeReputacion.Desbloqueo(6, new BigDecimal("200.00"), "BOB", "PROPUESTO"));
    private static final EtapasDeReputacion.Etapa EN_MARCHA = new EtapasDeReputacion.Etapa(
            "EN_MARCHA", "Etapa 2 · En marcha", 2, new BigDecimal("550.00"), new BigDecimal("600.00"), null);
    private static final EtapasDeReputacion.Etapa PILAR = new EtapasDeReputacion.Etapa(
            "PILAR", "Etapa 10 · Pilar", 10, new BigDecimal("980"), new BigDecimal("1000"), null);

    @Test
    @DisplayName("GET /reputacion/etapas · 200: tramos y aporte como cadenas con dos decimales, desbloqueo con estado")
    void catalogo() throws Exception {
        when(etapas.ejecutar(any())).thenReturn(List.of(INICIAL, PILAR));

        mvc.perform(get("/reputacion/etapas").with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("INICIAL"))
                .andExpect(jsonPath("$[0].orden").value(1))
                .andExpect(jsonPath("$[0].desde").value("500.00"))
                .andExpect(jsonPath("$[0].hasta").value("550.00"))
                .andExpect(jsonPath("$[0].desbloqueo.cuposMax").value(6))
                .andExpect(jsonPath("$[0].desbloqueo.aporteMax").value("200.00"))
                .andExpect(jsonPath("$[0].desbloqueo.moneda").value("BOB"))
                .andExpect(jsonPath("$[0].desbloqueo.estado").value("PROPUESTO"))
                // 980 de la base sale "980.00": el patron Decimal del contrato exige dos decimales.
                .andExpect(jsonPath("$[1].desde").value("980.00"))
                .andExpect(jsonPath("$[1].hasta").value("1000.00"))
                .andExpect(jsonPath("$[1].desbloqueo").doesNotExist());
    }

    @Test
    @DisplayName("GET /reputacion/etapas · 200 con lista vacía si no hay modelo vigente")
    void sinModelo() throws Exception {
        when(etapas.ejecutar(any())).thenReturn(List.of());

        mvc.perform(get("/reputacion/etapas").with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /reputacion/etapas · 401 sin sesión, sin llegar al caso")
    void sinSesion() throws Exception {
        mvc.perform(get("/reputacion/etapas")).andExpect(status().isUnauthorized());
        verifyNoInteractions(etapas);
    }

    @Test
    @DisplayName("puntaje · 549,99 en Etapa 1 · Inicial: progreso como cadena de cuatro decimales y la siguiente")
    void etapaDelPuntaje() throws Exception {
        when(puntajes.ejecutar(any(), any()))
                .thenReturn(new ConsultarPuntaje.Puntaje(
                        true,
                        new BigDecimal("549.99"),
                        "BASICO",
                        new EtapasDeReputacion.Ubicacion(INICIAL, EN_MARCHA, new BigDecimal("0.9998"))));

        mvc.perform(get("/reputacion/{id}/puntaje", Sesiones.USUARIO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa.codigo").value("INICIAL"))
                .andExpect(jsonPath("$.etapa.nombre").value("Etapa 1 · Inicial"))
                .andExpect(jsonPath("$.etapa.desde").value("500.00"))
                .andExpect(jsonPath("$.etapa.hasta").value("550.00"))
                .andExpect(jsonPath("$.etapa.progreso").value("0.9998"))
                .andExpect(jsonPath("$.etapa.siguiente.codigo").value("EN_MARCHA"))
                .andExpect(jsonPath("$.etapa.siguiente.desde").value("550.00"));
    }

    @Test
    @DisplayName("puntaje · en la última etapa no viaja siguiente y el máximo llena la barra")
    void ultimaEtapa() throws Exception {
        when(puntajes.ejecutar(any(), any()))
                .thenReturn(new ConsultarPuntaje.Puntaje(
                        true,
                        new BigDecimal("1000.00"),
                        "REFERENTE",
                        new EtapasDeReputacion.Ubicacion(PILAR, null, new BigDecimal("1.0000"))));

        mvc.perform(get("/reputacion/{id}/puntaje", Sesiones.USUARIO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa.codigo").value("PILAR"))
                .andExpect(jsonPath("$.etapa.progreso").value("1.0000"))
                .andExpect(jsonPath("$.etapa.siguiente").doesNotExist());
    }

    @Test
    @DisplayName("puntaje · sin catálogo de etapas la respuesta omite la etapa en vez de inventarla")
    void sinEtapa() throws Exception {
        when(puntajes.ejecutar(any(), any()))
                .thenReturn(new ConsultarPuntaje.Puntaje(false, BigDecimal.ZERO, "SIN_HISTORIAL"));

        mvc.perform(get("/reputacion/{id}/puntaje", Sesiones.USUARIO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").doesNotExist());
    }
}
