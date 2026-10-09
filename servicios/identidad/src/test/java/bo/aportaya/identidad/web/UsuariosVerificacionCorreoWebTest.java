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

        mvc.perform(post("/usuarios/verificaciones/correo/{id}/confirmacion", verificacion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"pablo@example.com\",\"codigo\":\"482019\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificada").value(true));

        verify(confirmarCorreo).ejecutar(any(), any(), any(), any(), any(), any());
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
