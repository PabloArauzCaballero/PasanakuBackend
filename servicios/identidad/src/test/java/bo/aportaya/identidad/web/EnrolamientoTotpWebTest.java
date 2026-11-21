package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.EmitirAcceso;
import bo.aportaya.identidad.aplicacion.EnrolarTotp;
import bo.aportaya.identidad.aplicacion.RenovarSesion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * F1.S3.M2 · el contrato HTTP del enrolamiento TOTP: el titular sale del token, el cuerpo no
 * puede nombrar a nadie, la semilla no se cachea, y sin token no hay nada.
 */
// SesionDeLaPeticion es real (ContextoWebDePrueba): el contexto sale del JWT de cada peticion.
@PruebaWeb(value = SesionController.class)
@Import(CookieDeRefresco.class)
class EnrolamientoTotpWebTest {

    private static final UUID TITULAR = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID OTRO = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID FACTOR = UUID.fromString("f1000000-0000-4000-8000-0000000000f1");
    // Semilla SINTETICA (la de los vectores de RFC 6238), no un secreto real.
    private static final String SEMILLA = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RenovarSesion renovarSesion;

    @MockitoBean
    private EmitirAcceso emitirAcceso;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.CerrarSesion cerrarSesion;

    @MockitoBean
    private EnrolarTotp enrolarTotp;

    private static RequestPostProcessor token(UUID usuario, String rol, String... permisos) {
        return jwt().jwt(t -> t.subject(usuario.toString())
                .claim("rol", rol)
                .claim("permisos", java.util.List.of(permisos))
                .claim("sesion_id", UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("correcto: iniciar devuelve 201, la semilla y Cache-Control no-store; el titular es el del token")
    void iniciarConElTitularDelToken() throws Exception {
        when(enrolarTotp.iniciar(any()))
                .thenReturn(new EnrolarTotp.Inicio(FACTOR, SEMILLA, "otpauth://totp/AportaYa:SINT?secret=" + SEMILLA));

        mvc.perform(post("/sesion/factores/totp").with(token(TITULAR, "PARTICIPANTE")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.factorId").value(FACTOR.toString()))
                .andExpect(jsonPath("$.semilla").value(SEMILLA));

        verify(enrolarTotp).iniciar(argThat((ContextoSesion ctx) -> TITULAR.equals(ctx.usuarioId())));
    }

    @Test
    @DisplayName("correcto: un operador de backoffice tambien enrola el suyo")
    void operadorEnrola() throws Exception {
        when(enrolarTotp.iniciar(any())).thenReturn(new EnrolarTotp.Inicio(FACTOR, SEMILLA, "otpauth://totp/x"));
        mvc.perform(post("/sesion/factores/totp").with(token(OTRO, "BACKOFFICE")))
                .andExpect(status().isCreated());
        verify(enrolarTotp).iniciar(argThat((ContextoSesion ctx) -> OTRO.equals(ctx.usuarioId())));
    }

    @Test
    @DisplayName("invalido: sin token, 401 y no se toca el caso de uso")
    void sinToken() throws Exception {
        mvc.perform(post("/sesion/factores/totp")).andExpect(status().isUnauthorized());
        mvc.perform(post("/sesion/factores/totp/{id}/confirmacion", FACTOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(enrolarTotp);
    }

    @Test
    @DisplayName("invalido: un rol sin permiso, 403")
    void rolSinPermiso() throws Exception {
        mvc.perform(post("/sesion/factores/totp").with(token(TITULAR, "ROL_QUE_NO_EXISTE_EN_EL_CATALOGO")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(enrolarTotp);
    }

    @Test
    @DisplayName("invalido: el cuerpo de la confirmacion no puede nombrar a otro usuario (400 AP-VAL-02)")
    void cuerpoConUsuarioRechazado() throws Exception {
        mvc.perform(post("/sesion/factores/totp/{id}/confirmacion", FACTOR)
                        .with(token(TITULAR, "PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"123456\",\"usuarioId\":\"" + OTRO + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        verifyNoInteractions(enrolarTotp);
    }

    @Test
    @DisplayName("limite: un codigo que no son seis digitos no llega al caso de uso (400)")
    void codigoMalFormado() throws Exception {
        for (String codigo : new String[] {"12345", "1234567", "12345a", ""}) {
            mvc.perform(post("/sesion/factores/totp/{id}/confirmacion", FACTOR)
                            .with(token(TITULAR, "PARTICIPANTE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"codigo\":\"" + codigo + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(enrolarTotp);
    }

    @Test
    @DisplayName("invalido: codigo que no coincide, 422 AP-CU04-04")
    void codigoQueNoCoincide() throws Exception {
        when(enrolarTotp.confirmar(eq(FACTOR), eq("000000"), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(4, 4), "Ese codigo no coincide."));
        mvc.perform(post("/sesion/factores/totp/{id}/confirmacion", FACTOR)
                        .with(token(TITULAR, "PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"000000\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU04-04"));
    }

    @Test
    @DisplayName("correcto: confirmar con el titular del token devuelve el factor confirmado")
    void confirmar() throws Exception {
        OffsetDateTime cuando = OffsetDateTime.of(2026, 11, 21, 12, 0, 0, 0, ZoneOffset.UTC);
        when(enrolarTotp.confirmar(eq(FACTOR), eq("287082"), any()))
                .thenReturn(new EnrolarTotp.Confirmacion(FACTOR, cuando));
        mvc.perform(post("/sesion/factores/totp/{id}/confirmacion", FACTOR)
                        .with(token(TITULAR, "PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"287082\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factorId").value(FACTOR.toString()));
        verify(enrolarTotp)
                .confirmar(eq(FACTOR), eq("287082"), argThat((ContextoSesion ctx) -> TITULAR.equals(ctx.usuarioId())));
    }
}
