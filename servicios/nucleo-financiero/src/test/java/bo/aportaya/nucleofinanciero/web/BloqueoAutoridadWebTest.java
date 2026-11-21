package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * F1.L1.M10 (ANF-SEG-006) · CU-17 con permisos propios de la accion y segregacion: registrar exige
 * BLOQUEO_AUTORIDAD_REGISTRAR y levantar BLOQUEO_AUTORIDAD_LEVANTAR, cada uno desde el token de una persona.
 */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
@org.springframework.context.annotation.Import(FondeoDeLaBilletera.class)
@MockitoBean(types = bo.aportaya.nucleofinanciero.aplicacion.CU13LiberarRetencionDelMercado.class)
class BloqueoAutoridadWebTest extends BaseDeOperacionesWeb {

    private static final UUID BLOQUEO = UUID.fromString("eeeeeeee-0000-4000-8000-0000000000b1");

    private org.springframework.test.web.servlet.ResultActions levantar(
            org.springframework.test.web.servlet.request.RequestPostProcessor quien) throws Exception {
        return mvc.perform(post("/billetera/bloqueos/{id}/levantamiento", BLOQUEO)
                .with(quien)
                .header("Idempotency-Key", CLAVE));
    }

    @Test
    @DisplayName("invalido: DATOS_SENSIBLES_LEER ya no alcanza para registrar un bloqueo (403)")
    void registrarConElPermisoViejo() throws Exception {
        mvc.perform(post("/billetera/bloqueos")
                        .with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "DATOS_SENSIBLES_LEER"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu17);
    }

    @Test
    @DisplayName("correcto: otra persona con BLOQUEO_AUTORIDAD_LEVANTAR levanta el bloqueo (200 LEVANTADO)")
    void otraPersonaLevanta() throws Exception {
        when(cu17.levantar(eq(BLOQUEO), any()))
                .thenReturn(new CU17BloquearPorAutoridad.SalidaLevantamiento(BLOQUEO, "LEVANTADO"));
        levantar(Sesiones.enSesion(
                        Sesiones.OTRO_USUARIO, UUID.randomUUID(), "OFICIAL_CUMPLIMIENTO", "BLOQUEO_AUTORIDAD_LEVANTAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloqueoId").value(BLOQUEO.toString()))
                .andExpect(jsonPath("$.estado").value("LEVANTADO"));
        verify(cu17).levantar(eq(BLOQUEO), any());
    }

    @Test
    @DisplayName("invalido: quien registro el bloqueo no lo levanta (422 AP-CU17-04)")
    void mismaPersonaNoLevanta() throws Exception {
        when(cu17.levantar(eq(BLOQUEO), any()))
                .thenThrow(new ErrorDeNegocio(
                        CodigoError.de(17, 4),
                        "Quien registro el bloqueo no puede levantarlo: lo levanta otra persona."));
        levantar(Sesiones.enSesion(
                        Sesiones.USUARIO, UUID.randomUUID(), "OFICIAL_CUMPLIMIENTO", "BLOQUEO_AUTORIDAD_LEVANTAR"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU17-04"));
    }

    @Test
    @DisplayName("invalido: solo BLOQUEO_AUTORIDAD_REGISTRAR o DATOS_SENSIBLES_LEER no levanta (403)")
    void sinPermisoDeLevantar() throws Exception {
        levantar(Sesiones.enSesion(
                        Sesiones.OTRO_USUARIO,
                        UUID.randomUUID(),
                        "ANALISTA_CUMPLIMIENTO",
                        "BLOQUEO_AUTORIDAD_REGISTRAR",
                        "DATOS_SENSIBLES_LEER"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu17);
    }

    @Test
    @DisplayName("invalido: sin token no se levanta (401)")
    void sinToken() throws Exception {
        mvc.perform(post("/billetera/bloqueos/{id}/levantamiento", BLOQUEO).header("Idempotency-Key", CLAVE))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cu17);
    }

    @Test
    @DisplayName("invalido: un token sin sesion (servicio) no levanta (401)")
    void tokenSinSesion() throws Exception {
        levantar(Sesiones.de(Sesiones.OTRO_USUARIO, "OFICIAL_CUMPLIMIENTO", "BLOQUEO_AUTORIDAD_LEVANTAR"))
                .andExpect(status().isUnauthorized());
        verify(cu17, never()).levantar(any(), any());
    }

    @Test
    @DisplayName("limite: levantar un bloqueo que ya no esta vigente responde 422 AP-CU17-05")
    void yaLevantado() throws Exception {
        when(cu17.levantar(eq(BLOQUEO), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(17, 5), "Ese bloqueo no esta vigente."));
        levantar(Sesiones.enSesion(
                        Sesiones.OTRO_USUARIO, UUID.randomUUID(), "OFICIAL_CUMPLIMIENTO", "BLOQUEO_AUTORIDAD_LEVANTAR"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU17-05"));
    }
}
