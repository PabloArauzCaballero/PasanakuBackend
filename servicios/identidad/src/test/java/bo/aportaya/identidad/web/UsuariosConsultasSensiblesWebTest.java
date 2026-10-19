package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contrato HTTP de {@code /usuarios}: las consultas sensibles (separadas de
 * {@code UsuariosControllerWebTest} por el limite de 300 lineas; mismo montaje, mismas pruebas).
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
class UsuariosConsultasSensiblesWebTest {

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
    @DisplayName("Las consultas sensibles contestan si, o no — nunca el dato")
    class ConsultasSensibles {

        @Test
        @DisplayName("CU-69 · buscar por telefono devuelve existe + id, y nada mas")
        void buscarPorTelefono() throws Exception {
            when(busqueda.ejecutar(any(), any())).thenReturn(Optional.of(USUARIO));

            String cuerpo = mvc.perform(get("/usuarios/por-telefono")
                            .param("telefono", "+59171234567")
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.existe").value(true))
                    .andExpect(jsonPath("$.usuarioId").value(USUARIO.toString()))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Dos campos y ningun dato personal: ni nombre, ni documento, ni el telefono
            // que se pregunto.
            org.assertj.core.api.Assertions.assertThat(cuerpo)
                    .doesNotContain("nombre")
                    .doesNotContain("documento")
                    .doesNotContain("+59171234567");
        }

        @Test
        @DisplayName("CU-69 · si no existe, contesta que no y no inventa un identificador")
        void telefonoQueNoExiste() throws Exception {
            when(busqueda.ejecutar(any(), any())).thenReturn(Optional.empty());

            mvc.perform(get("/usuarios/por-telefono")
                            .param("telefono", "+59171234567")
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.existe").value(false))
                    .andExpect(jsonPath("$.usuarioId").doesNotExist());
        }

        @Test
        @DisplayName("CU-01 · verificar titularidad contesta si coincide, no que hay guardado")
        void verificarTitularidad() throws Exception {
            when(titularidad.coincide(any(), any(), any(), any())).thenReturn(false);

            String cuerpo = mvc.perform(post("/usuarios/{id}/titularidad/verificacion", USUARIO)
                            .with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "DATOS_SENSIBLES_LEER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombreCompleto\":\"Pablo Arauz\",\"documento\":\"1234567\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coincide").value(false))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Si contestara «no coincide, el guardado es X», seria un oraculo para
            // adivinar el documento de cualquiera a fuerza de intentos.
            org.assertj.core.api.Assertions.assertThat(cuerpo)
                    .doesNotContain("Pablo")
                    .doesNotContain("1234567");
        }

        @Test
        @DisplayName("CU-01 · 400: la titularidad con un documento mas corto que el minimo del contrato")
        void documentoDemasiadoCorto() throws Exception {
            mvc.perform(post("/usuarios/{id}/titularidad/verificacion", USUARIO)
                            .with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "DATOS_SENSIBLES_LEER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombreCompleto\":\"Pablo Arauz\",\"documento\":\"12\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(titularidad);
        }

        @Test
        @DisplayName("CU-69 · emitir un token de invitacion exige GRUPO_ADMINISTRAR")
        void tokenDeInvitacionEsUnaOperacionDeOrganizador() throws Exception {
            mvc.perform(post("/usuarios/tokens/invitacion")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"canal\":\"WHATSAPP\",\"destinoEnmascarado\":\"+591*****567\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(tokens);
        }
    }
}
