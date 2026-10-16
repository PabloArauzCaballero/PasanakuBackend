package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.nucleofinanciero.aplicacion.CU51EjecutarCierreDiario.SalidaCierre;
import bo.aportaya.nucleofinanciero.aplicacion.CierreDiarioDesdeContabilidad;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarTesoreria;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Backoffice carril A · H8 · {@code POST /contabilidad/cierres-diarios}: rol, forma, 201/200 segun si el dia
 * ya estaba cerrado, y que los rechazos lleguen con su codigo. El recorrido real vive en
 * {@code CU51CierreDesdeContabilidadTest}.
 */
@PruebaWeb(ContabilidadController.class)
class CierreDiarioWebTest {
    private static final LocalDate DIA = LocalDate.of(2026, 3, 10);
    private static final UUID CIERRE = UUID.fromString("eeeeeeee-0000-4000-8000-000000000061");
    private static final String CLAVE = "eeeeeeee-0000-4000-8000-0000000000bb";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarTesoreria tesoreria;

    @MockitoBean
    private CierreDiarioDesdeContabilidad cierres;

    private ResultActions cerrar(String rol, String... permisos) throws Exception {
        return mvc.perform(post("/contabilidad/cierres-diarios")
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fecha\":\"2026-03-10\"}")
                .with(Sesiones.como(rol, permisos)));
    }

    @Test
    @DisplayName("201: CONTABILIDAD cierra el dia y recibe si cuadro")
    void cierra() throws Exception {
        when(cierres.cerrar(eq(DIA), any())).thenReturn(new SalidaCierre(CIERRE, DIA, true, 0, false, null));

        cerrar("CONTABILIDAD")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cierreId").value(CIERRE.toString()))
                .andExpect(jsonPath("$.cuadrado").value(true))
                .andExpect(jsonPath("$.yaExistia").value(false));
    }

    @Test
    @DisplayName("200: el dia ya cerrado devuelve ese cierre, no otro")
    void yaCerrado() throws Exception {
        when(cierres.cerrar(eq(DIA), any())).thenReturn(new SalidaCierre(CIERRE, DIA, true, 0, true, null));

        cerrar("CONTABILIDAD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yaExistia").value(true));
    }

    @Test
    @DisplayName("403: AUDITORIA_LEER lee los cierres pero no cierra")
    void auditoriaNoCierra() throws Exception {
        cerrar("AUDITOR", "AUDITORIA_LEER").andExpect(status().isForbidden());
        verifyNoInteractions(cierres);
    }

    @Test
    @DisplayName("401: sin token no se cierra nada")
    void sinToken() throws Exception {
        mvc.perform(post("/contabilidad/cierres-diarios")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-03-10\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cierres);
    }

    @Test
    @DisplayName("422: aportes sin respuesta sale con AP-CU51-08, nunca como 500")
    void sinRecaudo() throws Exception {
        when(cierres.cerrar(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(51, 8), "aportes no informo el recaudo."));

        cerrar("CONTABILIDAD")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU51-08"));
    }

    @Test
    @DisplayName("400: un campo de mas (las cifras del cliente) lo rechaza el contrato antes del caso de uso")
    void cifrasDelCliente() throws Exception {
        mvc.perform(post("/contabilidad/cierres-diarios")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-03-10\",\"totalRecaudado\":\"1.00\"}")
                        .with(Sesiones.como("CONTABILIDAD")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cierres);
    }
}
