package bo.aportaya.notificaciones.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.notificaciones.aplicacion.CU82ProcesarRespuesta;
import bo.aportaya.notificaciones.aplicacion.ConsultarMensajeria;
import bo.aportaya.notificaciones.aplicacion.ConsultarSupresion;
import bo.aportaya.notificaciones.aplicacion.SolicitarAviso;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
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
 * Backoffice carril A · A6 · {@code GET /notificaciones/plantillas} y {@code GET /notificaciones/envios}:
 * la auditoria operativa de la mensajeria. Permiso (AUDITORIA_LEER), respuesta sin destinatario ni
 * contenido, y rechazos con codigo. El recorrido real vive en {@code CU80PlantillasTest} y
 * {@code CU83EnviosTest}.
 */
@PruebaWeb(NotificacionesController.class)
class MensajeriaAuditoriaWebTest {

    private static final UUID ENVIO = UUID.fromString("a6000000-0000-4000-8000-000000000001");
    private static final UUID VERSION = UUID.fromString("a6000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime EN = OffsetDateTime.of(2026, 10, 16, 15, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SolicitarAviso avisos;

    @MockitoBean
    private ConsultarSupresion supresiones;

    @MockitoBean
    private CU82ProcesarRespuesta cu82;

    @MockitoBean
    private ConsultarMensajeria mensajeria;

    @MockitoBean
    private bo.aportaya.notificaciones.aplicacion.GestionarAvisosInternos gestionarAvisosInternos;

    private static ConsultarMensajeria.Pagina<ConsultarMensajeria.Envio> unFallido(boolean llena) {
        var e = new ConsultarMensajeria.Envio(
                ENVIO,
                UUID.fromString("a6000000-0000-4000-8000-000000000003"),
                UUID.fromString("a6000000-0000-4000-8000-000000000004"),
                "PROV-A",
                VERSION,
                "WHATSAPP",
                "FALLIDO",
                1,
                3,
                3,
                new BigDecimal("0.0450"),
                "BOB",
                Optional.of("PROVEEDOR_CAIDO"),
                EN,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new ConsultarMensajeria.Acuse("FALLIDO", EN, EN, Optional.of("131026"))));
        return new ConsultarMensajeria.Pagina<>(
                List.of(e), llena ? Optional.of(EN) : Optional.empty(), llena ? Optional.of(ENVIO) : Optional.empty());
    }

    private static ConsultarMensajeria.Pagina<ConsultarMensajeria.Plantilla> unaPlantilla() {
        var p = new ConsultarMensajeria.Plantilla(
                UUID.fromString("a6000000-0000-4000-8000-000000000005"),
                "PL-RECORDATORIO",
                "aporte.recordatorio",
                "WHATSAPP",
                "UTILITY",
                "APROBADA",
                true,
                VERSION,
                1,
                "es",
                EN,
                Optional.empty());
        return new ConsultarMensajeria.Pagina<>(List.of(p), Optional.empty(), Optional.empty());
    }

    @Test
    @DisplayName(
            "200: AUDITORIA_LEER ve los envios con estado, costo de cuatro decimales y ultimo acuse, sin destinatario")
    void auditoriaVeEnvios() throws Exception {
        when(mensajeria.envios(any(), any(), any(), eq(1), any())).thenReturn(unFallido(true));

        String cuerpo = mvc.perform(get("/notificaciones/envios")
                        .param("limite", "1")
                        .with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].envioId").value(ENVIO.toString()))
                .andExpect(jsonPath("$.items[0].estado").value("FALLIDO"))
                .andExpect(jsonPath("$.items[0].proveedor").value("PROV-A"))
                .andExpect(jsonPath("$.items[0].costo").value("0.0450"))
                .andExpect(jsonPath("$.items[0].moneda").value("BOB"))
                .andExpect(jsonPath("$.items[0].ultimoAcuse.tipoEvento").value("FALLIDO"))
                .andExpect(jsonPath("$.proximoAntesDeId").value(ENVIO.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        // Respuesta minima (90.2): ni a quien, ni que se le dijo, ni con que id lo busca el proveedor.
        assertThat(cuerpo)
                .doesNotContain("destinatario")
                .doesNotContain("contenido")
                .doesNotContain("idMensajeProveedor")
                .doesNotContain("payload")
                .doesNotContain("claveIdempotencia");
    }

    @Test
    @DisplayName("200: AUDITORIA_LEER ve las plantillas con su version y vigencia, sin cuerpo ni asunto")
    void auditoriaVePlantillas() throws Exception {
        when(mensajeria.plantillas(any(), any(), any(), eq(25), any())).thenReturn(unaPlantilla());

        String cuerpo = mvc.perform(
                        get("/notificaciones/plantillas").with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].codigo").value("PL-RECORDATORIO"))
                .andExpect(jsonPath("$.items[0].evento").value("aporte.recordatorio"))
                .andExpect(jsonPath("$.items[0].estadoAprobacion").value("APROBADA"))
                .andExpect(jsonPath("$.items[0].versionId").value(VERSION.toString()))
                .andExpect(jsonPath("$.proximoAntesDeId").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(cuerpo).doesNotContain("cuerpo").doesNotContain("asunto");
        verify(mensajeria).plantillas(any(), eq(Optional.empty()), eq(Optional.empty()), eq(25), any());
    }

    @Test
    @DisplayName("403: quien no audita no ve ni envios ni plantillas, aunque opere grupos")
    void sinPermiso() throws Exception {
        var sesion = Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR");
        mvc.perform(get("/notificaciones/envios").with(sesion))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        mvc.perform(get("/notificaciones/plantillas").with(sesion))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        verifyNoInteractions(mensajeria);
    }

    @Test
    @DisplayName("401: sin token no hay auditoria de mensajeria")
    void sinToken() throws Exception {
        mvc.perform(get("/notificaciones/envios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/notificaciones/plantillas")).andExpect(status().isUnauthorized());
        verifyNoInteractions(mensajeria);
    }

    @Test
    @DisplayName("422: un estado desconocido sale con AP-CU83-06 en envios y AP-CU80-05 en plantillas")
    void estadoDesconocido() throws Exception {
        when(mensajeria.envios(any(), any(), any(), any(Integer.class), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(83, 6), "Filtro de listado invalido."));
        when(mensajeria.plantillas(any(), any(), any(), any(Integer.class), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(80, 5), "Filtro de listado invalido."));
        var sesion = Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER");

        mvc.perform(get("/notificaciones/envios").param("estados", "PERDIDO").with(sesion))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU83-06"));
        mvc.perform(get("/notificaciones/plantillas")
                        .param("estados", "VIGENTE")
                        .with(sesion))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU80-05"));
    }

    @Test
    @DisplayName("400: limite fuera de 1..100 o cursor que no es UUID lo corta el contrato")
    void entradaInvalida() throws Exception {
        var sesion = Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER");
        mvc.perform(get("/notificaciones/envios").param("limite", "0").with(sesion))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/notificaciones/plantillas").param("antesDeId", "x").with(sesion))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(mensajeria);
    }
}
