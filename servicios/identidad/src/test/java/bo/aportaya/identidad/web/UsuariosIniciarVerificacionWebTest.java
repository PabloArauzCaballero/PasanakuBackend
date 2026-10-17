package bo.aportaya.identidad.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import bo.aportaya.identidad.aplicacion.ConsumirInvitacion;
import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.SolicitarVerificacionCorreo;
import bo.aportaya.identidad.aplicacion.ValidarTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * {@code POST /usuarios/{usuarioId}/verificacion} (2026-10-17): quien abrio su cuenta sin documento lo
 * envia despues. Exige la sesion del PROPIO titular ({@code TitularDelExpediente.exigir(usuarioId, null)}):
 * ni el UUID de la ruta ni el token de alta alcanzan. Lo declara {@code PoliticaDeAcceso.AMBITO}; aca se prueba.
 */
@PruebaWeb(
        value = UsuariosController.class,
        properties = {"aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion"})
class UsuariosIniciarVerificacionWebTest {

    private static final String PIMIENTA = "pimienta-de-prueba-no-es-la-de-produccion";
    private static final UUID TITULAR = Sesiones.USUARIO;
    private static final UUID OTRA = Sesiones.OTRO_USUARIO;
    private static final UUID NO_EXISTE = UUID.fromString("99999999-0000-4000-8000-000000000009");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Reloj reloj;

    @MockitoBean
    private ConsumirInvitacion consumirInvitacion;

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
    private ValidarTokenDeInvitacion validacionDeInvitacion;

    @MockitoBean
    private VerificarTitularidad titularidad;

    @MockitoBean
    private SolicitarVerificacionCorreo solicitarCorreo;

    @MockitoBean
    private ConfirmarVerificacionCorreo confirmarCorreo;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.ActivarRolOrganizador activarRolOrganizador;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.ConsultarMiPerfil consultarMiPerfil;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.CU02IniciarVerificacion iniciarVerificacion;

    @MockitoBean
    private ConsultarNivelKyc nivelesKyc;

    private TokenDeAlta emisor() {
        return new TokenDeAlta(PIMIENTA, reloj, Duration.ofMinutes(30));
    }

    private static final String DOCUMENTO =
            "{\"tipo\":\"CI\",\"numero\":\"1234567\",\"lugarExpedicion\":\"SC\",\"fechaExpiracion\":\"2031-01-15\"}";

    private ResultActions enviar(MockHttpServletRequestBuilder peticion) throws Exception {
        return mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON).content(DOCUMENTO));
    }

    @Test
    @DisplayName("rechaza: sin sesion no se abre ninguna verificacion (401), aunque se conozca el UUID")
    void sinSesionNoAbre() throws Exception {
        enviar(post("/usuarios/{id}/verificacion", TITULAR)).andExpect(status().isUnauthorized());
        verifyNoInteractions(iniciarVerificacion);
    }

    @Test
    @DisplayName("rechaza: el token de alta del propio titular no alcanza (401: exige sesion)")
    void tokenDeAltaNoAlcanza() throws Exception {
        String token = emisor().emitir(TITULAR).token();
        enviar(post("/usuarios/{id}/verificacion", TITULAR).header("Token-De-Alta", token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(iniciarVerificacion);
    }

    @Test
    @DisplayName("rechaza: la sesion de otra persona no abre la verificacion ajena, exista o no (403)")
    void otraSesionNoAbre() throws Exception {
        enviar(post("/usuarios/{id}/verificacion", NO_EXISTE).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        enviar(post("/usuarios/{id}/verificacion", OTRA).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(iniciarVerificacion);
    }

    @Test
    @DisplayName("correcto: el titular con su sesion abre su verificacion y recibe su id, en revision y sin fotos")
    void elTitularAbreLaSuya() throws Exception {
        UUID verificacion = UUID.fromString("abababab-0000-4000-8000-000000000001");
        when(iniciarVerificacion.ejecutar(eq(TITULAR), any(), any(), any())).thenReturn(verificacion);

        enviar(post("/usuarios/{id}/verificacion", TITULAR).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.verificacionId").value(verificacion.toString()))
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));
        verify(iniciarVerificacion).ejecutar(eq(TITULAR), any(), any(), any());
    }
}
