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
import bo.aportaya.identidad.aplicacion.CU02GuardarFotoDelExpediente;
import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.identidad.dominio.AperturaDeCuenta;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    private static final String ALTA =
            """
            {
              "telefonoE164": "+59171234567",
              "nombres": "Pablo",
              "apellidos": "Arauz",
              "fechaNacimiento": "1995-06-15",
              "documento": {"tipo": "CI", "numero": "1234567", "lugarExpedicion": "SC"},
              "contrasena": "clave-de-prueba-2026",
              "aceptaContratos": ["dddddddd-0000-4000-8000-000000000002"]
            }
            """;

    @Autowired
    private MockMvc mvc;

    // La subida de la foto del expediente es dependencia de UsuariosController desde
    // que existe el portal de riesgo: sin doblarla, el contexto de esta prueba no levanta.
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
            when(cu01.ejecutar(any(), any()))
                    .thenReturn(
                            new CU01RegistrarUsuario.SalidaRegistro(USUARIO, AperturaDeCuenta.PENDIENTE_VERIFICACION));

            registrar(ALTA)
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.usuarioId").value(USUARIO.toString()))
                    .andExpect(jsonPath("$.estado").value("PENDIENTE_VERIFICACION"));
        }

        @Test
        @DisplayName("CU-01 · el alta entra SIN sesion: es la ruta por la que se llega al sistema")
        void elAltaNoPideSesion() throws Exception {
            when(cu01.ejecutar(any(), any()))
                    .thenReturn(
                            new CU01RegistrarUsuario.SalidaRegistro(USUARIO, AperturaDeCuenta.PENDIENTE_VERIFICACION));

            // Sin `.with(Sesiones...)`. Si esta prueba diera 401, no habria forma de
            // conseguir un token y por lo tanto no habria forma de usar el sistema.
            registrar(ALTA).andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("CU-01 · la respuesta no devuelve nada de lo que se mando")
        void elAltaNoDevuelveDatosPersonales() throws Exception {
            when(cu01.ejecutar(any(), any()))
                    .thenReturn(
                            new CU01RegistrarUsuario.SalidaRegistro(USUARIO, AperturaDeCuenta.PENDIENTE_VERIFICACION));

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

    /** Que el identificador del contrato exista lo decide `cumplimiento`, no esta ruta. */
    @Test
    @DisplayName("CU-01 · el contrato aceptado viaja como identificador, no como texto")
    void elContratoViajaComoIdentificador() throws Exception {
        registrar(ALTA.replace("\"" + CONTRATO + "\"", "\"acepto todo\"")).andExpect(status().isBadRequest());
        verifyNoInteractions(cu01);
    }
}
