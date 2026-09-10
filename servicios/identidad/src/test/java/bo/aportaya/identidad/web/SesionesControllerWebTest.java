package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.CU04Autenticar;
import bo.aportaya.identidad.aplicacion.EmitirAcceso;
import bo.aportaya.identidad.dominio.ResultadoDeAutenticacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code POST /sesiones} — la puerta por la que se entra al sistema.
 *
 * <p>Es el unico endpoint autenticado del producto que <b>tiene</b> que ser publico, y
 * eso lo vuelve el mas expuesto: le pega cualquiera, y lo que responde le llega a
 * cualquiera. De ahi las tres cosas que se fijan aca y en ningun otro lado.
 *
 * <p><b>Uno.</b> Un token solo sale con la sesion abierta. Emitirlo cuando todavia
 * falta el segundo factor seria una sesion completa conseguida con media credencial.
 *
 * <p><b>Dos.</b> La respuesta de un ingreso fallido no distingue entre «ese telefono no
 * existe» y «esa credencial no es». Distinguirlas convierte el ingreso en un
 * comprobador de padron.
 *
 * <p><b>Tres.</b> La credencial no vuelve nunca, ni siquiera dentro de un mensaje de
 * error.
 */
@PruebaWeb(
        value = SesionesController.class,
        properties = {
            "aportaya.acceso.intentos-maximos=5",
            "aportaya.acceso.duracion-bloqueo=PT15M",
            "aportaya.acceso.vigencia-sesion=PT15M",
        })
class SesionesControllerWebTest {

    private static final UUID USUARIO = UUID.fromString("cccccccc-0000-4000-8000-000000000001");
    private static final UUID SESION = UUID.fromString("cccccccc-0000-4000-8000-000000000002");
    private static final String CREDENCIAL = "una-credencial-secretisima";

    private static final String CUERPO =
            """
            {
              "telefonoE164": "+59171234567",
              "credencial": "%s",
              "huellaDispositivo": "huella-de-prueba",
              "plataforma": "ANDROID"
            }
            """
                    .formatted(CREDENCIAL);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU04Autenticar cu04;

    @MockitoBean
    private EmitirAcceso emitirAcceso;

    private static ResultadoDeAutenticacion abierta() {
        return ResultadoDeAutenticacion.sesionAbierta(
                USUARIO, SESION, OffsetDateTime.of(2026, 4, 1, 12, 0, 0, 0, ZoneOffset.UTC), true);
    }

    @Test
    @DisplayName("CU-04 · 200 con la sesion abierta: sale el token y sale el identificador de sesion")
    void ingresoExitoso() throws Exception {
        when(cu04.ejecutar(any(), any())).thenReturn(abierta());
        when(emitirAcceso.ejecutar(any(), any(), any(), any()))
                .thenReturn(new EmitirAcceso.Acceso("un.token.firmado", Instant.parse("2026-04-01T12:00:00Z")));

        mvc.perform(post("/sesiones").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiereFactorAdicional").value(false))
                .andExpect(jsonPath("$.sesionId").value(SESION.toString()))
                .andExpect(jsonPath("$.tokenAcceso").value("un.token.firmado"));
    }

    @Test
    @DisplayName("CU-04 · si falta el segundo factor NO se emite token")
    void sinSegundoFactorNoHayToken() throws Exception {
        when(cu04.ejecutar(any(), any())).thenReturn(ResultadoDeAutenticacion.faltaSegundoFactor(false));

        mvc.perform(post("/sesiones").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU04-03"));

        // Lo importante no es el estado: es que el emisor no se toco. Un token emitido
        // aca seria una sesion completa con media credencial.
        verify(emitirAcceso, never()).ejecutar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("CU-04 · la credencial no vuelve NUNCA, ni en el error")
    void laCredencialNoVuelve() throws Exception {
        when(cu04.ejecutar(any(), any()))
                .thenReturn(
                        ResultadoDeAutenticacion.rechazado(CodigoError.de(4, 1), "No pudimos verificar esos datos."));

        String cuerpo = mvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isUnprocessableEntity())
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain(CREDENCIAL)
                .doesNotContain("credencial")
                .doesNotContain("huella-de-prueba");
    }

    @Test
    @DisplayName("CU-04 · el rechazo no dice si el telefono existe")
    void elRechazoNoEsUnComprobadorDePadron() throws Exception {
        when(cu04.ejecutar(any(), any()))
                .thenReturn(
                        ResultadoDeAutenticacion.rechazado(CodigoError.de(4, 1), "No pudimos verificar esos datos."));

        String cuerpo = mvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Si el mensaje cambiara segun exista o no el numero, el endpoint publico se
        // volveria una forma de averiguar quien tiene cuenta.
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("no existe")
                .doesNotContain("no encontrado")
                .doesNotContain("+59171234567");
    }

    @ParameterizedTest(name = "telefono invalido: «{0}»")
    @ValueSource(strings = {"", "71234567", "+5917123456", "+591712345678", "+1555123456", "no-es-un-numero"})
    @DisplayName("CU-04 · 400: el telefono que no cumple el patron del contrato muere antes del caso de uso")
    void telefonoFueraDelPatron(String telefono) throws Exception {
        String cuerpo = CUERPO.replace("+59171234567", telefono);

        mvc.perform(post("/sesiones").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());

        // Sin esto, un patron declarado en el YAML y no aplicado por el codigo generado
        // pasaria inadvertido: la prueba diria «400» porque el caso de uso rechazo, y no
        // porque el contrato lo hizo.
        verifyNoInteractions(cu04);
    }

    @Test
    @DisplayName("CU-04 · 400: falta la huella del dispositivo, que el contrato exige")
    void faltaLaHuella() throws Exception {
        String cuerpo =
                """
                {"telefonoE164":"+59171234567","credencial":"%s","plataforma":"ANDROID"}
                """
                        .formatted(CREDENCIAL);

        mvc.perform(post("/sesiones").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu04);
    }

    @Test
    @DisplayName("CU-04 · 400: JSON corrupto en el endpoint mas expuesto no es un 500")
    void jsonCorrupto() throws Exception {
        mvc.perform(post("/sesiones").contentType(MediaType.APPLICATION_JSON).content("{ roto"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu04);
    }
}
