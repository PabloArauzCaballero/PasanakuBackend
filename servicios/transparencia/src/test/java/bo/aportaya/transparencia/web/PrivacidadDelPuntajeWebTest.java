package bo.aportaya.transparencia.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Privacidad del puntaje y lectura de etapas (2026-10-30, regla 90.2.8): el puntaje exacto es solo
 * de su dueño, el catalogo lo leen las puertas declaradas y la etapa de otras personas solo la pide
 * la identidad de servicio de grupos.
 */
@PruebaWeb(ReputacionController.class)
@MockitoBean(
        types = {
            CU70RegistrarEventoReputacion.class, CU71RecalcularPuntaje.class, CU72SellarBloque.class,
            CU74EvaluarInsignias.class, CU75EmitirCertificado.class, CU76PublicarResena.class,
            CU97EvaluarRiesgo.class, ConsultarPerfilEnGrupo.class
        })
@DisplayName("Privacidad del puntaje y lectura de etapas (2026-10-30, regla 90.2.8)")
class PrivacidadDelPuntajeWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarPuntaje puntajes;

    @MockitoBean
    private ListarEtapas etapas;

    @MockitoBean
    private ConsultarEtapasDePersonas etapasDePersonas;

    private static final EtapasDeReputacion.Etapa INICIAL = new EtapasDeReputacion.Etapa(
            "INICIAL",
            "Etapa 1 · Inicial",
            1,
            new BigDecimal("500.00"),
            new BigDecimal("550.00"),
            new EtapasDeReputacion.Desbloqueo(6, new BigDecimal("200.00"), "BOB", "PROPUESTO"));
    private static final EtapasDeReputacion.Etapa EN_MARCHA = new EtapasDeReputacion.Etapa(
            "EN_MARCHA", "Etapa 2 · En marcha", 2, new BigDecimal("550.00"), new BigDecimal("600.00"), null);

    @Test
    @DisplayName("puntaje · 403 sobre el puntaje de OTRA persona, sin llegar al caso, aunque sea PARTICIPANTE")
    void puntajeAjeno() throws Exception {
        mvc.perform(get("/reputacion/{id}/puntaje", Sesiones.OTRO_USUARIO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.puntaje").doesNotExist());
        verifyNoInteractions(puntajes);
    }

    @Test
    @DisplayName("puntaje · 403 también para la identidad de servicio de grupos: ya no lee el puntaje de nadie")
    void puntajeAjenoDesdeElServicio() throws Exception {
        mvc.perform(get("/reputacion/{id}/puntaje", Sesiones.OTRO_USUARIO).with(Sesiones.como("SERVICIO_GRUPOS")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(puntajes);
    }

    @Test
    @DisplayName("etapas · backoffice: SOPORTE, SOLICITUD_INGRESO_VER/RESOLVER y AUDITORIA_LEER leen el catálogo")
    void catalogoParaBackoffice() throws Exception {
        when(etapas.ejecutar(any())).thenReturn(List.of(INICIAL));

        mvc.perform(get("/reputacion/etapas").with(Sesiones.como("SOPORTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("INICIAL"))
                .andExpect(jsonPath("$[0].nombre").value("Etapa 1 · Inicial"));
        mvc.perform(get("/reputacion/etapas").with(Sesiones.como("OPERATIONS_ANALYST", "SOLICITUD_INGRESO_VER")))
                .andExpect(status().isOk());
        mvc.perform(get("/reputacion/etapas").with(Sesiones.como("OPERATIONS_MANAGER", "SOLICITUD_INGRESO_RESOLVER")))
                .andExpect(status().isOk());
        mvc.perform(get("/reputacion/etapas").with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk());
        mvc.perform(get("/reputacion/etapas").with(Sesiones.como("SERVICIO_GRUPOS")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("etapas · 403 para un rol de backoffice sin ninguna de esas puertas, sin llegar al caso")
    void catalogoSinPuerta() throws Exception {
        mvc.perform(get("/reputacion/etapas").with(Sesiones.como("COMERCIAL", "TABLERO_COMERCIAL_VER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/reputacion/etapas").with(Sesiones.sinPermisos())).andExpect(status().isForbidden());
        verifyNoInteractions(etapas);
    }

    @Test
    @DisplayName("de-personas · SERVICIO_GRUPOS: etapa sin puntaje, sin progreso y sin hasta; el orden de la petición")
    void etapasParaElServicio() throws Exception {
        UUID ana = UUID.fromString("e1000000-0000-4000-8000-0000000000a1");
        UUID beto = UUID.fromString("e1000000-0000-4000-8000-0000000000b2");
        when(etapasDePersonas.ejecutar(any(), any()))
                .thenReturn(List.of(
                        new ConsultarEtapasDePersonas.EtapaDePersona(beto, true, EN_MARCHA),
                        new ConsultarEtapasDePersonas.EtapaDePersona(ana, false, INICIAL)));

        mvc.perform(get("/reputacion/etapas/de-personas")
                        .param("usuarioId", beto.toString(), ana.toString())
                        .with(Sesiones.como("SERVICIO_GRUPOS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].usuarioId").value(beto.toString()))
                .andExpect(jsonPath("$[0].tieneHistorial").value(true))
                .andExpect(jsonPath("$[0].etapa.codigo").value("EN_MARCHA"))
                .andExpect(jsonPath("$[0].etapa.nombre").value("Etapa 2 · En marcha"))
                .andExpect(jsonPath("$[0].etapa.orden").value(2))
                .andExpect(jsonPath("$[0].etapa.desde").value("550.00"))
                .andExpect(jsonPath("$[0].etapa.hasta").doesNotExist())
                .andExpect(jsonPath("$[0].etapa.progreso").doesNotExist())
                .andExpect(jsonPath("$[0].puntaje").doesNotExist())
                .andExpect(jsonPath("$[1].tieneHistorial").value(false))
                .andExpect(jsonPath("$[1].etapa.codigo").value("INICIAL"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> pedidos = ArgumentCaptor.forClass(List.class);
        verify(etapasDePersonas).ejecutar(pedidos.capture(), any());
        org.assertj.core.api.Assertions.assertThat(pedidos.getValue()).containsExactly(beto, ana);
    }

    @Test
    @DisplayName("de-personas · sin catálogo de etapas la persona viaja sin etapa (grupos falla cerrado)")
    void sinCatalogo() throws Exception {
        UUID ana = UUID.fromString("e1000000-0000-4000-8000-0000000000a1");
        when(etapasDePersonas.ejecutar(any(), any()))
                .thenReturn(List.of(new ConsultarEtapasDePersonas.EtapaDePersona(ana, true, null)));

        mvc.perform(get("/reputacion/etapas/de-personas")
                        .param("usuarioId", ana.toString())
                        .with(Sesiones.como("SERVICIO_GRUPOS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].etapa").doesNotExist());
    }

    @Test
    @DisplayName(
            "de-personas · 403 a una sesión de persona (PARTICIPANTE, ORGANIZADOR) y a backoffice, sin llegar al caso")
    void soloElServicio() throws Exception {
        String id = Sesiones.OTRO_USUARIO.toString();
        mvc.perform(get("/reputacion/etapas/de-personas").param("usuarioId", id).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/reputacion/etapas/de-personas").param("usuarioId", id).with(Sesiones.como("ORGANIZADOR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/reputacion/etapas/de-personas").param("usuarioId", id).with(Sesiones.como("SOPORTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/reputacion/etapas/de-personas")
                        .param("usuarioId", id)
                        .with(Sesiones.como("SERVICIO_ENTREGAS")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(etapasDePersonas);
    }

    @Test
    @DisplayName("de-personas · 401 sin sesión")
    void sinSesion() throws Exception {
        mvc.perform(get("/reputacion/etapas/de-personas").param("usuarioId", Sesiones.OTRO_USUARIO.toString()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(etapasDePersonas);
    }

    @Test
    @DisplayName("de-personas · 400: sin usuarioId, con uno que no es UUID, y con 61 (el contrato admite 1..60)")
    void tamanoYFormato() throws Exception {
        mvc.perform(get("/reputacion/etapas/de-personas").with(Sesiones.como("SERVICIO_GRUPOS")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/reputacion/etapas/de-personas")
                        .param("usuarioId", "el-vecino")
                        .with(Sesiones.como("SERVICIO_GRUPOS")))
                .andExpect(status().isBadRequest());
        String[] sesentaYUno = java.util.stream.IntStream.range(0, 61)
                .mapToObj(i -> UUID.randomUUID().toString())
                .toArray(String[]::new);
        mvc.perform(get("/reputacion/etapas/de-personas")
                        .param("usuarioId", sesentaYUno)
                        .with(Sesiones.como("SERVICIO_GRUPOS")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(etapasDePersonas);
    }

    @Test
    @DisplayName("de-personas · 200 con 60, el borde de arriba del contrato")
    void sesenta() throws Exception {
        when(etapasDePersonas.ejecutar(any(), any())).thenReturn(List.of());
        String[] sesenta = java.util.stream.IntStream.range(0, 60)
                .mapToObj(i -> UUID.randomUUID().toString())
                .toArray(String[]::new);
        mvc.perform(get("/reputacion/etapas/de-personas")
                        .param("usuarioId", sesenta)
                        .with(Sesiones.como("SERVICIO_GRUPOS")))
                .andExpect(status().isOk());
    }
}
