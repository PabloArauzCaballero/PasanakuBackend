package bo.aportaya.transparencia.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import bo.aportaya.transparencia.dominio.PerfilDeReputacionEnGrupo;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Perfil del integrante (2026-10-21): la reputacion de alguien, solo para quien comparte su grupo. */
@PruebaWeb(ReputacionController.class)
@MockitoBean(
        types = {
            ConsultarPuntaje.class, CU70RegistrarEventoReputacion.class, CU71RecalcularPuntaje.class,
            CU72SellarBloque.class, CU74EvaluarInsignias.class, CU75EmitirCertificado.class,
            CU76PublicarResena.class, CU97EvaluarRiesgo.class, ListarEtapas.class,
            ConsultarEtapasDePersonas.class
        })
class PerfilEnGrupoWebTest {

    private static final UUID USUARIO = UUID.fromString("e2000000-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("e2000000-0000-4000-8000-000000000002");
    private static final String RUTA = "/reputacion/{u}/perfil-en-grupo/{g}";
    private static final OffsetDateTime CUANDO = OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarPerfilEnGrupo perfiles;

    @Test
    @DisplayName("perfil · 200 con historial: cifras como cadenas o enteros, insignias vigentes, sin montos")
    void conHistorial() throws Exception {
        when(perfiles.ejecutar(eq(GRUPO), eq(USUARIO), any()))
                .thenReturn(new PerfilDeReputacionEnGrupo(
                        Optional.of(new PerfilDeReputacionEnGrupo.Factores(
                                new BigDecimal("812.50"), "CONFIABLE", new BigDecimal("96.40"), 3, 1, 0, 14)),
                        List.of(new PerfilDeReputacionEnGrupo.Insignia(
                                "PAGADOR_PUNTUAL", "Pagador puntual", "Pago a tiempo", CUANDO))));
        mvc.perform(get(RUTA, USUARIO, GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneHistorial").value(true))
                .andExpect(jsonPath("$.puntaje").value("812.50"))
                .andExpect(jsonPath("$.nivelDeConfianza").value("CONFIABLE"))
                .andExpect(jsonPath("$.indicePuntualidad").value("96.40"))
                .andExpect(jsonPath("$.gruposCompletados").value(3))
                .andExpect(jsonPath("$.gruposAbandonados").value(1))
                .andExpect(jsonPath("$.incumplimientosAbiertos").value(0))
                .andExpect(jsonPath("$.antiguedadMeses").value(14))
                .andExpect(jsonPath("$.insignias[0].codigo").value("PAGADOR_PUNTUAL"))
                .andExpect(jsonPath("$.montoTotalAportado").doesNotExist());
    }

    @Test
    @DisplayName("perfil ajeno · 200 sin el campo puntaje cuando el caso lo quito (solo lo ve su dueño)")
    void ajenoSinPuntaje() throws Exception {
        when(perfiles.ejecutar(eq(GRUPO), eq(USUARIO), any()))
                .thenReturn(new PerfilDeReputacionEnGrupo(
                        Optional.of(new PerfilDeReputacionEnGrupo.Factores(
                                        new BigDecimal("420.00"),
                                        "EN_OBSERVACION",
                                        new BigDecimal("71.00"),
                                        1,
                                        0,
                                        2,
                                        5))
                                .map(PerfilDeReputacionEnGrupo.Factores::sinPuntaje),
                        List.of()));
        String cuerpo = mvc.perform(get(RUTA, USUARIO, GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneHistorial").value(true))
                .andExpect(jsonPath("$.puntaje").doesNotExist())
                .andExpect(jsonPath("$.indicePuntualidad").value("71.00"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(cuerpo).doesNotContain("420");
    }

    @Test
    @DisplayName("perfil · 200 sin historial: SIN_HISTORIAL y las cifras ausentes, nunca en cero")
    void sinHistorial() throws Exception {
        when(perfiles.ejecutar(eq(GRUPO), eq(USUARIO), any()))
                .thenReturn(new PerfilDeReputacionEnGrupo(Optional.empty(), List.of()));
        mvc.perform(get(RUTA, USUARIO, GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneHistorial").value(false))
                .andExpect(jsonPath("$.nivelDeConfianza").value("SIN_HISTORIAL"))
                .andExpect(jsonPath("$.puntaje").doesNotExist())
                .andExpect(jsonPath("$.gruposCompletados").doesNotExist())
                .andExpect(jsonPath("$.insignias.length()").value(0));
    }

    @Test
    @DisplayName("perfil · 403: quien no comparte el grupo (o no se pudo comprobar) no ve nada")
    void noComparten() throws Exception {
        when(perfiles.ejecutar(eq(GRUPO), eq(USUARIO), any())).thenThrow(new AccessDeniedException("no comparte"));
        mvc.perform(get(RUTA, USUARIO, GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.puntaje").doesNotExist());
    }

    @Test
    @DisplayName("perfil · 401 sin sesion, 403 sin el permiso de participante, 400 con un id que no es UUID")
    void sinSesionNiPermiso() throws Exception {
        mvc.perform(get(RUTA, USUARIO, GRUPO)).andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA, USUARIO, GRUPO).with(Sesiones.como("OTRO_PERMISO")))
                .andExpect(status().isForbidden());
        mvc.perform(get(RUTA, "no-es-uuid", GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(perfiles);
    }
}
