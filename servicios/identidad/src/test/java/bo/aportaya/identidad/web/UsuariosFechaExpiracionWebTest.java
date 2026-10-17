package bo.aportaya.identidad.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * {@code PUT /usuarios/{usuarioId}/documento/fecha-expiracion} es {@code @Publico} (quien todavia no
 * tiene rol no tiene permiso que declarar), asi que su control es otro: SOLO la sesion del propio titular
 * ({@code TitularDelExpediente.exigir(usuarioId, null)}). A diferencia de subir fotos, el token de alta NO
 * alcanza: la fecha se completa ya con sesion. Lo declara {@code PoliticaDeAcceso}; aca se prueba.
 */
@PruebaWeb(
        value = UsuariosController.class,
        properties = {"aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion"})
class UsuariosFechaExpiracionWebTest {

    private static final String PIMIENTA = "pimienta-de-prueba-no-es-la-de-produccion";
    private static final UUID TITULAR = Sesiones.USUARIO;
    private static final UUID OTRA = Sesiones.OTRO_USUARIO;
    private static final UUID NO_EXISTE = UUID.fromString("99999999-0000-4000-8000-000000000009");
    private static final String CLAVE = "dddddddd-0000-4000-8000-0000000000ff";
    private static final byte[] FOTO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    private static final String ALTA =
            """
            {
              "telefonoE164": "+59171234567",
              "nombres": "Pablo",
              "apellidos": "Arauz",
              "fechaNacimiento": "1995-06-15",
              "correo": "pablo@example.com",
              "canalVerificacion": "CORREO",
              "verificacionCorreoId": "dddddddd-0000-4000-8000-000000000003",
              "documento": {"tipo": "CI", "numero": "1234567", "lugarExpedicion": "SC"},
              "contrasena": "clave-de-prueba-2026",
              "aceptaContratos": ["dddddddd-0000-4000-8000-000000000002"]
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Reloj reloj;

    @Autowired
    private ObjectMapper json;

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
    private ConsultarNivelKyc nivelesKyc;

    private TokenDeAlta emisor() {
        return new TokenDeAlta(PIMIENTA, reloj, Duration.ofMinutes(30));
    }

    private ResultActions completarFecha(MockHttpServletRequestBuilder peticion) throws Exception {
        return mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaExpiracionDocumento\":\"2030-01-15\"}"));
    }

    @Test
    @DisplayName("rechaza: sin sesion no se escribe la fecha del documento (401), aunque se conozca el UUID")
    void sinSesionNoEscribe() throws Exception {
        // 401 como dice el contrato: sin sesion no hay a quien atribuir la escritura.
        completarFecha(put("/usuarios/{id}/documento/fecha-expiracion", TITULAR))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(actualizarFechaExpiracion);
    }

    @Test
    @DisplayName("rechaza: el token de alta del propio titular no alcanza para cambiar la fecha (401: exige sesion)")
    void tokenDeAltaNoAlcanza() throws Exception {
        String token = emisor().emitir(TITULAR).token();
        completarFecha(put("/usuarios/{id}/documento/fecha-expiracion", TITULAR).header("Token-De-Alta", token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(actualizarFechaExpiracion);
    }

    @Test
    @DisplayName("rechaza: la sesion de otra persona no cambia la fecha ajena, y la respuesta no distingue si existe")
    void otraSesionNoEscribe() throws Exception {
        completarFecha(put("/usuarios/{id}/documento/fecha-expiracion", NO_EXISTE)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        verifyNoInteractions(actualizarFechaExpiracion);
    }
}
