package bo.aportaya.auditoria.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.CU58DescargarExportacion;
import bo.aportaya.auditoria.aplicacion.CU58EjecutarReporte;
import bo.aportaya.auditoria.aplicacion.ConsultarHistorialDeReportes;
import bo.aportaya.auditoria.aplicacion.ConsultarHistorialDeReportes.Definicion;
import bo.aportaya.auditoria.aplicacion.ConsultarHistorialDeReportes.Ejecucion;
import bo.aportaya.auditoria.aplicacion.ConsultarHistorialDeReportes.Pagina;
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
 * Backoffice carril A · A6 · {@code GET /reportes} y {@code GET /reportes/ejecuciones}: permiso, respuesta
 * minima (sin consulta base, parametros ni mensaje de error) y rechazos con codigo. El recorrido real vive en
 * {@code CU58HistorialTest}.
 */
@PruebaWeb(ReportesController.class)
class ReportesHistorialWebTest {
    private static final UUID ID = UUID.fromString("a5800000-0000-4000-8000-000000000001");
    private static final OffsetDateTime EN = OffsetDateTime.of(2026, 10, 16, 12, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU58EjecutarReporte cu58;

    @MockitoBean
    private CU58DescargarExportacion descargas;

    @MockitoBean
    private ConsultarHistorialDeReportes historial;

    @Test
    @DisplayName("200: el catalogo trae el permiso de cada reporte y NO la consulta base")
    void catalogo() throws Exception {
        when(historial.definiciones(any(), any(), anyInt(), any()))
                .thenReturn(new Pagina<>(
                        List.of(new Definicion(ID, "CARTERA_EN_MORA", "Cartera", "Mora por grupo", "REPORTE_X", true)),
                        Optional.of("Cartera"),
                        Optional.of(ID)));

        String cuerpo = mvc.perform(
                        get("/reportes").param("limite", "1").with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].permisoRequerido").value("REPORTE_X"))
                .andExpect(jsonPath("$.items[0].contieneDatosSensibles").value(true))
                .andExpect(jsonPath("$.proximoDespuesDe").value("Cartera"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("consulta")
                .doesNotContain("SELECT");
    }

    @Test
    @DisplayName("200: el historial trae quien pidio, estado y hash, sin parametros ni mensaje de error")
    void historial() throws Exception {
        when(historial.ejecuciones(any(), any(), any(), anyInt(), any()))
                .thenReturn(new Pagina<>(
                        List.of(new Ejecucion(
                                ID, ID, ID, "COMPLETADA", 3, 12, Optional.of("a".repeat(64)), EN, Optional.of(EN))),
                        Optional.empty(),
                        Optional.empty()));

        String cuerpo = mvc.perform(get("/reportes/ejecuciones").with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.items[0].hashResultado").value("a".repeat(64)))
                .andExpect(jsonPath("$.proximoAntesDeId").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("parametros")
                .doesNotContain("mensajeError");
    }

    @Test
    @DisplayName("403: sin AUDITORIA_LEER no hay catalogo ni historial")
    void sinPermiso() throws Exception {
        mvc.perform(get("/reportes").with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        mvc.perform(get("/reportes/ejecuciones").with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(historial);
    }

    @Test
    @DisplayName("401: sin token no hay catalogo")
    void sinToken() throws Exception {
        mvc.perform(get("/reportes")).andExpect(status().isUnauthorized());
        verifyNoInteractions(historial);
    }

    @Test
    @DisplayName("422: un estado desconocido sale con AP-CU58-07, nunca como 500")
    void estadoDesconocido() throws Exception {
        when(historial.ejecuciones(any(), any(), any(), anyInt(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(58, 7), "Filtro de listado invalido."));

        mvc.perform(get("/reportes/ejecuciones")
                        .param("estados", "PERDIDA")
                        .with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU58-07"));
    }

    @Test
    @DisplayName("400: limite fuera de 1..100 lo corta el contrato")
    void limite() throws Exception {
        mvc.perform(get("/reportes").param("limite", "101").with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(historial);
    }
}
