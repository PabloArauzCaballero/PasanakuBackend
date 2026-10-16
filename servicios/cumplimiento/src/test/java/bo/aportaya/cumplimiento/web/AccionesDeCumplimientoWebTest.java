package bo.aportaya.cumplimiento.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.cumplimiento.aplicacion.AccionesDeCumplimiento;
import bo.aportaya.cumplimiento.aplicacion.AccionesDeCumplimiento.Resultado;
import bo.aportaya.cumplimiento.aplicacion.CU02ElevarDiligencia;
import bo.aportaya.cumplimiento.aplicacion.CU03DeclararPep;
import bo.aportaya.cumplimiento.aplicacion.CU05AceptarContrato;
import bo.aportaya.cumplimiento.aplicacion.CU05ConsultarContratosVigentes;
import bo.aportaya.cumplimiento.aplicacion.CU43RemitirReportes.SalidaReporte;
import bo.aportaya.cumplimiento.aplicacion.CU54RegistrarRiesgoOperativo;
import bo.aportaya.cumplimiento.aplicacion.CU55GestionarIncidente;
import bo.aportaya.cumplimiento.aplicacion.CU56EjecutarPruebaDeContinuidad.SalidaPrueba;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejasLft;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Backoffice carril A · H9 · acciones de CU-43, CU-45 y CU-56 por HTTP: rol, 201/200 segun reintento, que
 * ningun actor viaje en el cuerpo (un campo de mas es 400) y rechazos con codigo. El recorrido real vive en
 * {@code CU43/CU45/CU56AccionesTest}.
 */
@PruebaWeb({UifController.class, CumplimientoController.class})
@Import(AccionesDeCumplimientoWeb.class)
class AccionesDeCumplimientoWebTest {
    private static final UUID ID = UUID.fromString("c9000000-0000-4000-8000-000000000043");
    private static final String CLAVE = "c9000000-0000-4000-8000-0000000000aa";
    private static final String ENVIO =
            """
            {"periodo":"2026-05","catalogoCodigo":"PCC01","canal":"PORTAL_WEB","numeroConstancia":"C-1",
             "aceptadoPorElOrganismo":true,"reintentos":0}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AccionesDeCumplimiento acciones;

    @MockitoBean
    private TableroDeCumplimientoWeb tablero;

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

    private ResultActions pedir(String ruta, String cuerpo, String rol, String... permisos) throws Exception {
        return mvc.perform(post(ruta)
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)
                .with(Sesiones.como(rol, permisos)));
    }

    private static SalidaReporte reporte() {
        return new SalidaReporte(ID, 0, true, null, "f".repeat(64), "C-1", false);
    }

    @Test
    @DisplayName("201: CUMPLIMIENTO_REPORTAR genera el reporte del periodo")
    void genera() throws Exception {
        when(acciones.generarReporte(eq("2026-05"), eq("PCC01"), eq("PCC-01"), any()))
                .thenReturn(new Resultado<>(reporte(), true));

        pedir(
                        "/uif/reportes",
                        "{\"periodo\":\"2026-05\",\"catalogoCodigo\":\"PCC01\",\"formulario\":\"PCC-01\"}",
                        "OFICIAL_CUMPLIMIENTO",
                        "CUMPLIMIENTO_REPORTAR")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reporteId").value(ID.toString()))
                .andExpect(jsonPath("$.reporteEnCero").value(true));
    }

    @Test
    @DisplayName("200: el envio repetido por el mismo aprobador devuelve el que ya hay")
    void envioRepetido() throws Exception {
        when(acciones.aprobarYEnviar(any(), any(), any(), any(), anyBoolean(), anyInt(), any()))
                .thenReturn(new Resultado<>(reporte(), false));

        pedir("/uif/reportes/envios", ENVIO, "OFICIAL_CUMPLIMIENTO", "CUMPLIMIENTO_REPORTAR")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("400: un aprobadoPor en el cuerpo se rechaza: el aprobador es la sesion")
    void aprobadorEnElCuerpo() throws Exception {
        pedir(
                        "/uif/reportes/envios",
                        ENVIO.replace("\"reintentos\":0", "\"reintentos\":0,\"aprobadoPor\":\"" + ID + "\""),
                        "OFICIAL_CUMPLIMIENTO",
                        "CUMPLIMIENTO_REPORTAR")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(acciones);
    }

    @Test
    @DisplayName("422: la autoaprobacion sale con AP-CU43-02, nunca como 500")
    void autoaprobacion() throws Exception {
        when(acciones.aprobarYEnviar(any(), any(), any(), any(), anyBoolean(), anyInt(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(43, 2), "Quien aprueba no puede ser quien genero."));

        pedir("/uif/reportes/envios", ENVIO, "OFICIAL_CUMPLIMIENTO", "CUMPLIMIENTO_REPORTAR")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU43-02"));
    }

    @Test
    @DisplayName("201: el oficial responde un requerimiento solo con la URL de la respuesta")
    void responde() throws Exception {
        when(acciones.responderRequerimiento(eq(ID), eq("https://oficios.bo/r/1"), any()))
                .thenReturn(new Resultado<>(ID, true));

        pedir(
                        "/uif/requerimientos/" + ID + "/respuesta",
                        "{\"respuestaUrl\":\"https://oficios.bo/r/1\"}",
                        "OFICIAL_CUMPLIMIENTO")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requerimientoId").value(ID.toString()));
        verify(acciones).responderRequerimiento(eq(ID), eq("https://oficios.bo/r/1"), any());
    }

    @Test
    @DisplayName("400: un plazo en el cuerpo de la respuesta se rechaza: el plazo es el registrado")
    void plazoEnElCuerpo() throws Exception {
        pedir(
                        "/uif/requerimientos/" + ID + "/respuesta",
                        "{\"respuestaUrl\":\"https://oficios.bo/r/1\",\"plazoRespuesta\":\"2099-01-01T00:00:00Z\"}",
                        "OFICIAL_CUMPLIMIENTO")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(acciones);
    }

    @Test
    @DisplayName("201: Riesgos registra una prueba de continuidad")
    void registraPrueba() throws Exception {
        when(acciones.registrarPrueba(any(), any()))
                .thenReturn(new Resultado<>(new SalidaPrueba(ID, 95, 10, "PARCIAL", UUID.randomUUID()), true));

        pedir(
                        "/cumplimiento/continuidad/pruebas",
                        """
                        {"planContinuidadId":"%s","tipo":"PARCIAL","fecha":"2026-07-01","ejecutadaPor":"%s",
                         "rtoObtenido":95,"rpoObtenido":10,"impactoEnClientes":false}"""
                                .formatted(ID, ID),
                        "RESPONSABLE_RIESGOS")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("PARCIAL"));
    }

    @Test
    @DisplayName("403: AUDITORIA_LEER lee las colas pero no ejecuta ninguna accion")
    void auditoriaNoActua() throws Exception {
        pedir("/uif/reportes/envios", ENVIO, "AUDITOR", "AUDITORIA_LEER").andExpect(status().isForbidden());
        pedir("/uif/requerimientos/" + ID + "/respuesta", "{\"respuestaUrl\":\"x\"}", "AUDITOR", "AUDITORIA_LEER")
                .andExpect(status().isForbidden());
        verifyNoInteractions(acciones);
    }

    @Test
    @DisplayName("401: sin token no hay accion")
    void sinToken() throws Exception {
        mvc.perform(post("/uif/reportes/envios")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENVIO))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(acciones);
    }
}
