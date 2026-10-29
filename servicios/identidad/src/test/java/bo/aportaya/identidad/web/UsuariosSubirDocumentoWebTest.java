package bo.aportaya.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import bo.aportaya.identidad.dominio.AperturaDeCuenta;
import bo.aportaya.plataforma.archivos.ArchivoGuardado;
import bo.aportaya.plataforma.archivos.ClaveObjeto;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * H13.S1.M2 hallazgo 1 · {@code POST /usuarios/{usuarioId}/documentos} ya no escribe en el
 * expediente de quien solo conoce un UUID.
 *
 * <p>Antes era {@code @Publico} y confiaba en el {@code usuarioId} de la ruta (IDOR sobre el KYC).
 * Ahora exige el token que devolvio el alta de ESE usuario, o su propia sesion. La prueba usa dos
 * personas: la titular y otra que conoce su identificador.
 */
@MockitoBean(
        types = {
            bo.aportaya.identidad.aplicacion.ActivarRolOrganizador.class,
            bo.aportaya.identidad.aplicacion.ConsultarMiPerfil.class,
            bo.aportaya.identidad.aplicacion.CU07FotoDePerfil.class,
            bo.aportaya.identidad.aplicacion.CU02IniciarVerificacion.class,
            bo.aportaya.identidad.aplicacion.GuardarBorradorOnboarding.class
        })
@PruebaWeb(
        value = UsuariosController.class,
        properties = {"aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion"})
class UsuariosSubirDocumentoWebTest {

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
    private ConsultarNivelKyc nivelesKyc;

    private TokenDeAlta emisor() {
        return new TokenDeAlta(PIMIENTA, reloj, Duration.ofMinutes(30));
    }

    private void guardaBien() {
        when(guardarFoto.ejecutar(any(), any(), any(), anyLong(), any(), any()))
                .thenReturn(new ArchivoGuardado(
                        ClaveObjeto.de("local://identidad/f.jpg"), "a".repeat(64), "image/jpeg", 7));
    }

    private MockHttpServletRequestBuilder subida(UUID enLaRuta, String token) {
        MockHttpServletRequestBuilder peticion = multipart("/usuarios/{id}/documentos", enLaRuta)
                .file(new MockMultipartFile("archivo", "anverso.jpg", "image/jpeg", FOTO))
                .param("cara", "ANVERSO")
                .header("Idempotency-Key", CLAVE);
        return token == null ? peticion : peticion.header("Token-De-Alta", token);
    }

    @Test
    @DisplayName("el alta devuelve el token de SU expediente, y con el se sube la foto de esa misma persona")
    void elTokenDelAltaPermiteSubirLoPropio() throws Exception {
        when(cu01.ejecutar(any(), any()))
                .thenReturn(new CU01RegistrarUsuario.SalidaRegistro(TITULAR, AperturaDeCuenta.PENDIENTE_VERIFICACION));
        guardaBien();

        String cuerpo = mvc.perform(post("/usuarios")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ALTA))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.tokenDeAlta").isNotEmpty())
                .andExpect(jsonPath("$.tokenDeAltaVenceEn").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode salida = json.readTree(cuerpo);

        mvc.perform(subida(TITULAR, salida.get("tokenDeAlta").asText()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cara").value("ANVERSO"));
        verify(guardarFoto).ejecutar(eq(TITULAR), any(), any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("rechaza: otra persona que conoce el UUID, con SU token, no escribe en el expediente ajeno")
    void tokenDeOtraPersonaNoSirveEnLaRutaAjena() throws Exception {
        String tokenDeOtra = emisor().emitir(OTRA).token();

        mvc.perform(subida(TITULAR, tokenDeOtra))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        verifyNoInteractions(guardarFoto);
    }

    @Test
    @DisplayName("rechaza: sin token y sin sesion no hay escritura (el UUID en la ruta no autoriza nada)")
    void sinTokenNiSesion() throws Exception {
        mvc.perform(subida(TITULAR, null)).andExpect(status().isForbidden());
        verifyNoInteractions(guardarFoto);
    }

    @Test
    @DisplayName("rechaza: con la sesion de OTRA persona tampoco se escribe en el expediente del titular")
    void sesionDeOtraPersona() throws Exception {
        mvc.perform(subida(TITULAR, null).with(Sesiones.comoOtro("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(guardarFoto);
    }

    @Test
    @DisplayName("la propia sesion del titular tambien puede completar su expediente")
    void sesionDelTitular() throws Exception {
        guardaBien();
        mvc.perform(subida(TITULAR, null).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isCreated());
        verify(guardarFoto).ejecutar(eq(TITULAR), any(), any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("rechaza: un token vencido no sirve, ni siquiera para su titular")
    void tokenVencido() throws Exception {
        var hace =
                new TokenDeAlta(PIMIENTA, Reloj.fijo(reloj.ahora().minus(Duration.ofHours(2))), Duration.ofMinutes(30));
        mvc.perform(subida(TITULAR, hace.emitir(TITULAR).token())).andExpect(status().isForbidden());
        verifyNoInteractions(guardarFoto);
    }

    @Test
    @DisplayName("rechaza: un token firmado con otra clave (forjado) no sirve")
    void tokenForjado() throws Exception {
        var ajeno = new TokenDeAlta("otra-clave-que-no-es-la-del-servicio", reloj, Duration.ofMinutes(30));
        mvc.perform(subida(TITULAR, ajeno.emitir(TITULAR).token())).andExpect(status().isForbidden());
        verifyNoInteractions(guardarFoto);
    }

    @ParameterizedTest(name = "rechaza: token mal formado «{0}»")
    @ValueSource(strings = {"x", ".", "123.", ".abc", "no-es-un-numero.abc", "9999999999.00", "1.2.3.4"})
    void tokenMalFormado(String token) throws Exception {
        mvc.perform(subida(TITULAR, token)).andExpect(status().isForbidden());
        verifyNoInteractions(guardarFoto);
    }

    @Test
    @DisplayName("rechaza: el 403 es IDENTICO exista o no el usuario de la ruta (no revela existencia)")
    void respuestaIdenticaSiExisteONo() throws Exception {
        String tokenDeOtra = emisor().emitir(OTRA).token();

        JsonNode existente = json.readTree(cuerpoDelRechazo(mvc.perform(subida(TITULAR, tokenDeOtra))));
        JsonNode inexistente = json.readTree(cuerpoDelRechazo(mvc.perform(subida(NO_EXISTE, tokenDeOtra))));

        assertThat(existente.get("codigo")).isEqualTo(inexistente.get("codigo"));
        assertThat(existente.get("mensaje")).isEqualTo(inexistente.get("mensaje"));
        assertThat(existente.toString()).doesNotContain(TITULAR.toString()).doesNotContain(OTRA.toString());
        verify(guardarFoto, never()).ejecutar(any(), any(), any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("rechaza: el token de alta no es una sesion y no abre ninguna otra ruta")
    void elTokenNoEsUnaSesion() throws Exception {
        String token = emisor().emitir(TITULAR).token();
        mvc.perform(post("/usuarios/{id}/titularidad/verificacion", TITULAR)
                        .header("Token-De-Alta", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCompleto\":\"Pablo Arauz\",\"documento\":\"1234567\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(titularidad);
    }

    @Test
    @DisplayName("el titular con sesión puede completar su fecha de documento")
    void completaFechaConSesionPropia() throws Exception {
        mvc.perform(put("/usuarios/{id}/documento/fecha-expiracion", TITULAR)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaExpiracionDocumento\":\"2030-01-15\"}"))
                .andExpect(status().isNoContent());
        verify(actualizarFechaExpiracion)
                .ejecutar(eq(TITULAR), eq(LocalDate.parse("2030-01-15")), any(), eq(reloj.hoy()));
    }

    @Test
    @DisplayName("una sesión de otra persona no puede modificar la fecha del expediente")
    void otraSesionNoCompletaFecha() throws Exception {
        mvc.perform(put("/usuarios/{id}/documento/fecha-expiracion", TITULAR)
                        .with(Sesiones.comoOtro("PARTICIPANTE", "BILLETERA_VER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaExpiracionDocumento\":\"2030-01-15\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(actualizarFechaExpiracion);
    }

    private static String cuerpoDelRechazo(ResultActions resultado) throws Exception {
        return resultado
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
