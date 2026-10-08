package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.BuscarPorTelefono;
import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario;
import bo.aportaya.identidad.aplicacion.CU02ConsultarEstadoDeVerificacion;
import bo.aportaya.identidad.aplicacion.CU02GuardarFotoDelExpediente;
import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.ValidarTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.identidad.dominio.ExpedienteDeIdentidad;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code GET /usuarios/{id}/verificacion}: publica (quien recien se registro todavia no
 * tiene sesion) y sin datos personales. Aparte de {@link UsuariosControllerWebTest} por el
 * limite de 300 lineas del barrido; el controlador es el mismo.
 */
@PruebaWeb(
        value = UsuariosController.class,
        properties = {"aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion"})
@DisplayName("GET /usuarios/{id}/verificacion — publica, sin datos personales")
class EstadoDeVerificacionWebTest {

    private static final UUID USUARIO = UUID.fromString("dddddddd-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU02GuardarFotoDelExpediente guardarFoto;

    @MockitoBean
    private CU02ConsultarEstadoDeVerificacion estadoDeVerificacion;

    @MockitoBean
    private CU01RegistrarUsuario cu01;

    @MockitoBean
    private BuscarPorTelefono busqueda;

    @MockitoBean
    private EmitirTokenDeInvitacion tokens;

    @MockitoBean
    private ValidarTokenDeInvitacion validacionDeInvitacion;

    @MockitoBean
    private VerificarTitularidad titularidad;

    @Test
    @DisplayName("CU-02 · 200 SIN sesion: quien recien se registro todavia no puede abrir una")
    void contestaSinSesion() throws Exception {
        when(estadoDeVerificacion.ejecutar(eq(USUARIO), any()))
                .thenReturn(new ExpedienteDeIdentidad(
                        USUARIO,
                        USUARIO,
                        "Pablo Arauz",
                        "CI SC",
                        "EN_REVISION",
                        OffsetDateTime.now(),
                        null,
                        null,
                        List.of("ANVERSO", "REVERSO")));

        // Sin `.with(Sesiones...)`, igual que el alta: todavia no hay token que presentar.
        mvc.perform(get("/usuarios/{id}/verificacion", USUARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificacionId").value(USUARIO.toString()))
                .andExpect(jsonPath("$.estado").value("EN_REVISION"))
                .andExpect(jsonPath("$.fotos", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    @DisplayName("CU-02 · la respuesta no trae nombre ni documento, solo lo minimo")
    void laRespuestaNoTraeDatosPersonales() throws Exception {
        when(estadoDeVerificacion.ejecutar(eq(USUARIO), any()))
                .thenReturn(new ExpedienteDeIdentidad(
                        USUARIO,
                        USUARIO,
                        "Pablo Arauz",
                        "CI SC",
                        "APROBADA",
                        OffsetDateTime.now(),
                        OffsetDateTime.now(),
                        null,
                        List.of("ANVERSO", "REVERSO", "SELFIE", "PERFIL_IZQUIERDO", "PERFIL_DERECHO")));

        String cuerpo = mvc.perform(get("/usuarios/{id}/verificacion", USUARIO))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("nombreCompleto")
                .doesNotContain("Pablo")
                .doesNotContain("documento")
                .doesNotContain("CI SC");
    }

    @Test
    @DisplayName("CU-02 · 422 cuando todavia no existe un expediente para ese usuario")
    void sinExpedienteEs422() throws Exception {
        when(estadoDeVerificacion.ejecutar(eq(USUARIO), any()))
                .thenThrow(new ErrorDeDominio("Todavia no hay un expediente abierto para esa persona"));

        mvc.perform(get("/usuarios/{id}/verificacion", USUARIO)).andExpect(status().isUnprocessableEntity());
    }
}
