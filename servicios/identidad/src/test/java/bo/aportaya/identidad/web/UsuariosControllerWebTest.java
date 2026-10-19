package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
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
import bo.aportaya.identidad.aplicacion.ValidarTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.identidad.dominio.AperturaDeCuenta;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contrato HTTP de {@code /usuarios}: el alta y las tres consultas sensibles.
 *
 * <p>Dos cosas se fijan aca porque no viven en ningun otro nivel.
 *
 * <p><b>El alta nace {@code PENDIENTE_VERIFICACION} y responde {@code 202}, no
 * {@code 201}.</b> El {@code 202} dice lo que realmente paso: el usuario existe y
 * todavia no opera, porque la diligencia vive en {@code cumplimiento} y la billetera
 * en {@code nucleo-financiero}. Un {@code 201} prometeria una cuenta lista.
 *
 * <p><b>Ninguna de estas rutas devuelve datos personales.</b> {@code buscarPorTelefono}
 * contesta si existe y su identificador; {@code verificarTitularidad} contesta si
 * coincide. Devolver el nombre o el documento guardados convertiria estas rutas en un
 * padron de clientes para cualquier servicio comprometido.
 */
@PruebaWeb(
        value = UsuariosController.class,
        properties = {"aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion"})
class UsuariosControllerWebTest {

    private static final UUID USUARIO = UUID.fromString("dddddddd-0000-4000-8000-000000000001");
    private static final UUID CONTRATO = UUID.fromString("dddddddd-0000-4000-8000-000000000002");
    private static final String CLAVE = "dddddddd-0000-4000-8000-0000000000ff";
    private static final CU01RegistrarUsuario.SalidaRegistro ALTA_ACEPTADA =
            new CU01RegistrarUsuario.SalidaRegistro(USUARIO, AperturaDeCuenta.PENDIENTE_VERIFICACION);

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
              "documento": {"tipo": "CI", "numero": "1234567", "lugarExpedicion": "SC", "fechaExpiracion": "2031-03-01"},
              "contrasena": "clave-de-prueba-2026",
              "aceptaContratos": ["dddddddd-0000-4000-8000-000000000002"]
            }
            """;

    @Autowired
    private MockMvc mvc;

    // La subida de la foto del expediente es dependencia de UsuariosController desde
    // que existe el portal de riesgo: sin doblarla, el contexto de esta prueba no levanta.
    @MockitoBean
    private bo.aportaya.identidad.aplicacion.ConsumirInvitacion consumirInvitacion;

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
    private bo.aportaya.identidad.aplicacion.GuardarBorradorOnboarding guardarBorrador;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.ConsultarMiPerfil consultarMiPerfil;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.CU02IniciarVerificacion iniciarVerificacion;

    @MockitoBean
    private ConsultarNivelKyc nivelesKyc;

    private org.springframework.test.web.servlet.ResultActions registrar(String cuerpo) throws Exception {
        return mvc.perform(post("/usuarios")
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Nested
    @DisplayName("POST /usuarios — el alta, que es publica")
    class Alta {

        @Test
        @DisplayName("CU-01 · 202 y PENDIENTE_VERIFICACION: existe y todavia no opera")
        void altaAceptada() throws Exception {
            when(cu01.ejecutar(any(), any())).thenReturn(ALTA_ACEPTADA);

            registrar(ALTA)
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.usuarioId").value(USUARIO.toString()))
                    .andExpect(jsonPath("$.estado").value("PENDIENTE_VERIFICACION"));
        }

        @Test
        @DisplayName("CU-01 · el vencimiento del documento llega al caso de uso, que es quien lo juzga")
        void elVencimientoLlegaAlCasoDeUso() throws Exception {
            var entrada = ArgumentCaptor.forClass(CU01RegistrarUsuario.EntradaRegistro.class);
            when(cu01.ejecutar(entrada.capture(), any())).thenReturn(ALTA_ACEPTADA);

            registrar(ALTA).andExpect(status().isAccepted());

            org.assertj.core.api.Assertions.assertThat(
                            entrada.getValue().documento().fechaExpiracion())
                    .isEqualTo(LocalDate.parse("2031-03-01"));
        }

        @Test
        @DisplayName("CU-01 · el alta entra SIN sesion: es la ruta por la que se llega al sistema")
        void elAltaNoPideSesion() throws Exception {
            when(cu01.ejecutar(any(), any())).thenReturn(ALTA_ACEPTADA);

            // Sin `.with(Sesiones...)`. Si esta prueba diera 401, no habria forma de
            // conseguir un token y por lo tanto no habria forma de usar el sistema.
            registrar(ALTA).andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("CU-01 · la respuesta no devuelve nada de lo que se mando")
        void elAltaNoDevuelveDatosPersonales() throws Exception {
            when(cu01.ejecutar(any(), any())).thenReturn(ALTA_ACEPTADA);

            String cuerpo = registrar(ALTA).andReturn().getResponse().getContentAsString();

            org.assertj.core.api.Assertions.assertThat(cuerpo)
                    .doesNotContain("Pablo")
                    .doesNotContain("Arauz")
                    .doesNotContain("1234567")
                    .doesNotContain("+59171234567")
                    .doesNotContain("1995-06-15");
        }

        @ParameterizedTest(name = "400 · {0}")
        @CsvSource({
            "'telefono fuera del patron', '\"+59171234567\"', '\"71234567\"'",
            "'nombre de una sola letra', '\"Pablo\"', '\"P\"'",
            "'fecha de nacimiento que no es una fecha', '\"1995-06-15\"', '\"15/06/1995\"'",
            "'vencimiento que no es una fecha', '\"2031-03-01\"', '\"01/03/2031\"'",
            "'sin ningun contrato aceptado', '[\"dddddddd-0000-4000-8000-000000000002\"]', '[]'",
        })
        @DisplayName("CU-01 · el contrato rechaza antes de llegar al caso de uso")
        void entradaInvalida(String caso, String original, String reemplazo) throws Exception {
            registrar(ALTA.replace(original, reemplazo)).andExpect(status().isBadRequest());
            verifyNoInteractions(cu01);
        }

        @Test
        @DisplayName("CU-01 · 400 sin Idempotency-Key: dos altas por un timeout serian dos personas")
        void sinClaveNoHayAlta() throws Exception {
            mvc.perform(post("/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ALTA))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu01);
        }
    }

    /** Que el identificador del contrato exista lo decide `cumplimiento`, no esta ruta. */
    @Test
    @DisplayName("CU-01 · el contrato aceptado viaja como identificador, no como texto")
    void elContratoViajaComoIdentificador() throws Exception {
        registrar(ALTA.replace("\"" + CONTRATO + "\"", "\"acepto todo\"")).andExpect(status().isBadRequest());
        verifyNoInteractions(cu01);
    }
}
