package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.BuscarPorTelefono;
import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario;
import bo.aportaya.identidad.aplicacion.CU02ActualizarFechaExpiracion;
import bo.aportaya.identidad.aplicacion.CU02ConsultarEstadoDeVerificacion;
import bo.aportaya.identidad.aplicacion.CU02GuardarFotoDelExpediente;
import bo.aportaya.identidad.aplicacion.ConfirmarVerificacionCorreo;
import bo.aportaya.identidad.aplicacion.ConsultarNivelKyc;
import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.SolicitarVerificacionCorreo;
import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Verificacion del correo previa al alta (movida desde UsuariosControllerWebTest, sin cambios de aserciones). */
@PruebaWeb(
        value = UsuariosController.class,
        properties = {"aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion"})
class UsuariosVerificacionCorreoWebTest {

    private static final String CLAVE = "dddddddd-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.ConsumirInvitacion consumirInvitacion;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.ValidarTokenDeInvitacion validacionDeInvitacion;

    @MockitoBean
    private CU02GuardarFotoDelExpediente guardarFoto;

    @MockitoBean
    private CU02ConsultarEstadoDeVerificacion estadoDeVerificacion;

    @MockitoBean
    private CU02ActualizarFechaExpiracion actualizarFechaExpiracion;

    @MockitoBean
    private CU01RegistrarUsuario cu01;

    @MockitoBean
    private BuscarPorTelefono busqueda;

    @MockitoBean
    private EmitirTokenDeInvitacion tokens;

    @MockitoBean
    private VerificarTitularidad titularidad;

    @MockitoBean
    private SolicitarVerificacionCorreo solicitarCorreo;

    @MockitoBean
    private ConfirmarVerificacionCorreo confirmarCorreo;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.ActivarRolOrganizador activarRolOrganizador;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.GuardarBorradorOnboarding guardarBorrador;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.ConsultarMiPerfil consultarMiPerfil;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.CU07FotoDePerfil fotoDePerfil;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.CU02IniciarVerificacion iniciarVerificacion;

    @MockitoBean
    private ConsultarNivelKyc nivelesKyc;

    @Test
    @DisplayName("202: solicita un codigo sin devolverlo en la respuesta")
    void solicitarCodigo() throws Exception {
        UUID verificacion = UUID.fromString("dddddddd-0000-4000-8000-000000000003");
        when(solicitarCorreo.ejecutar(any(), any(), any(), any(), any()))
                .thenReturn(new SolicitarVerificacionCorreo.Resultado(
                        verificacion, "pa***@example.com", OffsetDateTime.parse("2026-10-07T20:10:00Z")));

        mvc.perform(post("/usuarios/verificaciones/correo")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"pablo@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.verificacionId").value(verificacion.toString()))
                .andExpect(jsonPath("$.destinoEnmascarado").value("pa***@example.com"))
                .andExpect(jsonPath("$.codigo").doesNotExist());
    }

    @Test
    @DisplayName("200: confirma el codigo mediante el caso de uso")
    void confirmarCodigo() throws Exception {
        UUID verificacion = UUID.fromString("dddddddd-0000-4000-8000-000000000003");
        when(confirmarCorreo.ejecutar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new bo.aportaya.identidad.dominio.ResultadoConfirmacionCorreo(true, false, null));

        mvc.perform(post("/usuarios/verificaciones/correo/{id}/confirmacion", verificacion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"pablo@example.com\",\"codigo\":\"482019\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificada").value(true))
                .andExpect(jsonPath("$.onboardingIncompleto").value(false));

        verify(confirmarCorreo).ejecutar(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("200: confirma codigo con onboarding incompleto y devuelve borrador previo")
    void confirmarCodigoConOnboardingIncompleto() throws Exception {
        UUID verificacion = UUID.fromString("dddddddd-0000-4000-8000-000000000003");
        var borrador = new bo.aportaya.identidad.dominio.BorradorOnboardingDatos(
                "pablo@example.com",
                "+59171234567",
                "Pablo",
                "Arauz",
                java.time.LocalDate.parse("1995-06-15"),
                "CI",
                "1234567",
                "SC",
                null,
                "DEPENDIENTE",
                "Ingeniero",
                "SUELDO",
                "1000",
                "2",
                "DOCUMENTO");

        when(confirmarCorreo.ejecutar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new bo.aportaya.identidad.dominio.ResultadoConfirmacionCorreo(true, true, borrador));

        mvc.perform(post("/usuarios/verificaciones/correo/{id}/confirmacion", verificacion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"pablo@example.com\",\"codigo\":\"482019\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificada").value(true))
                .andExpect(jsonPath("$.onboardingIncompleto").value(true))
                .andExpect(jsonPath("$.borrador.nombres").value("Pablo"))
                .andExpect(jsonPath("$.borrador.pasoPendiente").value("documento"));
    }

    @Test
    @DisplayName("200: guarda borrador de onboarding exitosamente")
    void guardarBorrador() throws Exception {
        UUID verificacion = UUID.fromString("dddddddd-0000-4000-8000-000000000003");
        when(guardarBorrador.ejecutar(any(), any(), any())).thenAnswer(inv -> inv.getArgument(1));

        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/usuarios/verificaciones/correo/{id}/borrador", verificacion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                        {
                          "correo": "pablo@example.com",
                          "telefonoE164": "+59171234567",
                          "nombres": "Pablo",
                          "apellidos": "Arauz",
                          "pasoPendiente": "documento"
                        }
                        """);

        mvc.perform(peticion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Pablo"));

        verify(guardarBorrador).ejecutar(any(), any(), any());
    }

    @Test
    @DisplayName("400: un codigo que no tenga seis digitos no llega al caso de uso")
    void codigoMalFormado() throws Exception {
        mvc.perform(post("/usuarios/verificaciones/correo/{id}/confirmacion", "dddddddd-0000-4000-8000-000000000003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"pablo@example.com\",\"codigo\":\"123\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(confirmarCorreo);
    }
}
