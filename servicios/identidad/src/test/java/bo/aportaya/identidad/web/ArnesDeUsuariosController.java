package bo.aportaya.identidad.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import bo.aportaya.identidad.aplicacion.BuscarPorTelefono;
import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario;
import bo.aportaya.identidad.aplicacion.CU02ActualizarFechaExpiracion;
import bo.aportaya.identidad.aplicacion.CU02ConsultarEstadoDeVerificacion;
import bo.aportaya.identidad.aplicacion.CU02GuardarFotoDelExpediente;
import bo.aportaya.identidad.aplicacion.ConfirmarVerificacionCorreo;
import bo.aportaya.identidad.aplicacion.ConsultarNivelKyc;
import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.SolicitarVerificacionCorreo;
import bo.aportaya.identidad.aplicacion.ValidarTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.identidad.dominio.AperturaDeCuenta;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lo que comparten las pruebas web de {@code UsuariosController}: el contexto MVC
 * necesita TODOS los colaboradores del controlador doblados, y repetirlos en cada
 * clase seria la forma de que se desincronicen.
 */
abstract class ArnesDeUsuariosController {

    static final UUID USUARIO = UUID.fromString("dddddddd-0000-4000-8000-000000000001");
    static final UUID CONTRATO = UUID.fromString("dddddddd-0000-4000-8000-000000000002");
    static final String CLAVE = "dddddddd-0000-4000-8000-0000000000ff";
    static final CU01RegistrarUsuario.SalidaRegistro ALTA_ACEPTADA =
            new CU01RegistrarUsuario.SalidaRegistro(USUARIO, AperturaDeCuenta.PENDIENTE_VERIFICACION);

    static final String ALTA =
            """
            {
              "telefonoE164": "+59171234567",
              "nombres": "Pablo",
              "apellidos": "Arauz",
              "fechaNacimiento": "1995-06-15",
              "correo": "pablo@example.com",
              "canalVerificacion": "CORREO",
              "verificacionCorreoId": "dddddddd-0000-4000-8000-000000000003",
              "documento": {"tipo": "CI", "numero": "1234567", "lugarExpedicion": "SC", "fechaExpiracion": "2031-03-01"},
              "contrasena": "clave-de-prueba-2026",
              "aceptaContratos": ["dddddddd-0000-4000-8000-000000000002"]
            }
            """;

    @Autowired
    MockMvc mvc;

    // La subida de la foto del expediente es dependencia de UsuariosController desde
    // que existe el portal de riesgo: sin doblarla, el contexto de esta prueba no levanta.
    @MockitoBean
    bo.aportaya.identidad.aplicacion.ConsumirInvitacion consumirInvitacion;

    @MockitoBean
    CU02GuardarFotoDelExpediente guardarFoto;

    @MockitoBean
    CU02ConsultarEstadoDeVerificacion estadoDeVerificacion;

    @MockitoBean
    CU02ActualizarFechaExpiracion actualizarFechaExpiracion;

    @MockitoBean
    CU01RegistrarUsuario cu01;

    @MockitoBean
    BuscarPorTelefono busqueda;

    @MockitoBean
    EmitirTokenDeInvitacion tokens;

    @MockitoBean
    ValidarTokenDeInvitacion validacionDeInvitacion;

    @MockitoBean
    VerificarTitularidad titularidad;

    @MockitoBean
    SolicitarVerificacionCorreo solicitarCorreo;

    @MockitoBean
    ConfirmarVerificacionCorreo confirmarCorreo;

    @MockitoBean
    ConsultarNivelKyc nivelesKyc;

    org.springframework.test.web.servlet.ResultActions registrar(String cuerpo) throws Exception {
        return mvc.perform(post("/usuarios")
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }
}
