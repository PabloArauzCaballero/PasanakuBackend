package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.CU04Autenticar;
import bo.aportaya.identidad.aplicacion.CU04StepUp;
import bo.aportaya.identidad.aplicacion.EmitirAcceso;
import bo.aportaya.identidad.aplicacion.RenovarSesion;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;

/**
 * {@code POST /sesiones/desafios} y su verificacion: el step-up que exige una operacion sensible (B37).
 *
 * <p>Vive aparte de {@link SesionesControllerWebTest} por tamano (el barrido corta en 300 lineas): el ingreso y el
 * step-up son dos contratos distintos del mismo controlador.
 */
@PruebaWeb(
        value = SesionesController.class,
        properties = {
            "aportaya.acceso.intentos-maximos=5",
            "aportaya.acceso.duracion-bloqueo=PT15M",
            "aportaya.acceso.vigencia-sesion=PT15M",
        })
@org.springframework.context.annotation.Import(CookieDeRefresco.class)
class SesionesStepUpWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU04Autenticar cu04;

    @MockitoBean
    private EmitirAcceso emitirAcceso;

    @MockitoBean
    private RenovarSesion renovarSesion;

    @MockitoBean
    private CU04StepUp cu04StepUp;

    @Test
    @DisplayName("B37 · 201: con sesion se abre un desafio de step-up")
    void abreUnDesafio() throws Exception {
        var desafio = UUID.fromString("cccccccc-0000-4000-8000-000000000010");
        when(cu04StepUp.abrir(any(), any()))
                .thenReturn(new CU04StepUp.Desafio(desafio, Instant.parse("2026-04-01T12:05:00Z")));

        mvc.perform(post("/sesiones/desafios")
                        .with(bo.aportaya.plataforma.pruebas.web.Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proposito\":\"RETIRO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.desafioId").value(desafio.toString()));
    }

    @Test
    @DisplayName("B37 · 401: sin sesion no hay desafio ni evidencia")
    void sinSesionNoHayStepUp() throws Exception {
        mvc.perform(post("/sesiones/desafios")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proposito\":\"RETIRO\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/sesiones/desafios/{id}/verificacion", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"factor\":{\"tipo\":\"OTP\",\"valor\":\"123456\"}}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cu04StepUp);
    }

    @Test
    @DisplayName("B37 · 200: con el factor correcto sale la evidencia")
    void entregaLaEvidencia() throws Exception {
        var desafio = UUID.randomUUID();
        var jti = UUID.randomUUID();
        when(cu04StepUp.verificar(any(), any(), any()))
                .thenReturn(
                        new CU04StepUp.Evidencia("evidencia.firmada.jwt", Instant.parse("2026-04-01T12:05:00Z"), jti));

        mvc.perform(post("/sesiones/desafios/{id}/verificacion", desafio)
                        .with(bo.aportaya.plataforma.pruebas.web.Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"factor\":{\"tipo\":\"OTP\",\"valor\":\"123456\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidencia").value("evidencia.firmada.jwt"))
                .andExpect(jsonPath("$.jti").value(jti.toString()));
    }

    @Test
    @DisplayName("B37 · 400: un proposito que el contrato no enumera")
    void propositoInvalido() throws Exception {
        mvc.perform(post("/sesiones/desafios")
                        .with(bo.aportaya.plataforma.pruebas.web.Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proposito\":\"VACIAR_LA_CUENTA\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu04StepUp);
    }
}
