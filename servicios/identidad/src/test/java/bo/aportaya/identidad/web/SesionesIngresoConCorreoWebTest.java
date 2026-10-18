package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.CU04Autenticar;
import bo.aportaya.identidad.aplicacion.CU04StepUp;
import bo.aportaya.identidad.aplicacion.EmitirAcceso;
import bo.aportaya.identidad.aplicacion.EntradaAutenticacion;
import bo.aportaya.identidad.aplicacion.RenovarSesion;
import bo.aportaya.identidad.dominio.ResultadoDeAutenticacion;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code POST /sesiones} con correo: exactamente uno de {@code telefonoE164} o
 * {@code correo}; ninguno o los dos es una entrada mal formada (400), nunca algo que
 * llegue al caso de uso.
 */
@PruebaWeb(
        value = SesionesController.class,
        properties = {
            "aportaya.acceso.intentos-maximos=5",
            "aportaya.acceso.duracion-bloqueo=PT15M",
            "aportaya.acceso.vigencia-sesion=PT8H",
        })
@org.springframework.context.annotation.Import(CookieDeRefresco.class)
class SesionesIngresoConCorreoWebTest {

    private static final UUID USUARIO = UUID.fromString("cccccccc-0000-4000-8000-000000000001");
    private static final UUID SESION = UUID.fromString("cccccccc-0000-4000-8000-000000000002");
    private static final String CREDENCIAL = "una-credencial-secretisima";

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

    private static ResultadoDeAutenticacion abierta() {
        return ResultadoDeAutenticacion.sesionAbierta(
                USUARIO, SESION, OffsetDateTime.of(2026, 4, 1, 12, 0, 0, 0, ZoneOffset.UTC), true, false);
    }

    @Test
    @DisplayName("CU-04 · con correo en vez de telefono, el correo llega tal cual al caso de uso")
    void ingresoConCorreo() throws Exception {
        when(cu04.ejecutar(any(), any())).thenReturn(abierta());
        when(emitirAcceso.ejecutar(any(), any(), any(), any(), any()))
                .thenReturn(new EmitirAcceso.Acceso("un.token.firmado", Instant.parse("2026-04-01T12:00:00Z")));
        when(renovarSesion.emitir(any(), any(), any(), any(), any()))
                .thenReturn(new RenovarSesion.Emitido(
                        "b".repeat(64), OffsetDateTime.now(ZoneOffset.UTC).plusHours(8)));
        String cuerpo =
                """
                {"correo":"Ana.Prueba@Example.com","credencial":"%s","huellaDispositivo":"h","plataforma":"ANDROID"}
                """
                        .formatted(CREDENCIAL);

        mvc.perform(post("/sesiones").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk());

        ArgumentCaptor<EntradaAutenticacion> entrada = ArgumentCaptor.forClass(EntradaAutenticacion.class);
        verify(cu04).ejecutar(entrada.capture(), any());
        org.assertj.core.api.Assertions.assertThat(entrada.getValue().identificador())
                .isEqualTo("Ana.Prueba@Example.com");
        org.assertj.core.api.Assertions.assertThat(entrada.getValue().esCorreo())
                .isTrue();
    }

    @Test
    @DisplayName("CU-04 · sin telefono ni correo, o con los dos, es 400 AP-VAL-02 y no llega al caso de uso")
    void identificadorAusenteODobleEs400() throws Exception {
        String ninguno = """
                {"credencial":"%s","huellaDispositivo":"h"}
                """
                .formatted(CREDENCIAL);
        String ambos =
                """
                {"telefonoE164":"+59171234567","correo":"a@example.com","credencial":"%s","huellaDispositivo":"h"}
                """
                        .formatted(CREDENCIAL);

        for (String cuerpo : new String[] {ninguno, ambos}) {
            mvc.perform(post("/sesiones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        }
        verify(cu04, org.mockito.Mockito.never()).ejecutar(any(), any());
    }
}
