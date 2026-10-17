package bo.aportaya.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import bo.aportaya.identidad.dominio.ExpedienteDeIdentidad;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * {@code GET /usuarios/{id}/verificacion}: sin sesion obligatoria (quien recien se registro todavia
 * no puede abrir una) pero NO abierta: el {@code usuarioId} de la URL no autoriza nada, igual que en
 * {@code subirDocumento}. Aparte de {@link UsuariosControllerWebTest} por el limite de 300 lineas del
 * barrido; el controlador es el mismo.
 *
 * <p>Hallazgo de seguridad (IDOR/BOLA): antes bastaba conocer un UUID para leer el estado del
 * expediente ajeno, con su {@code verificacionId} y el motivo del rechazo. Las pruebas usan dos
 * personas: la titular y otra que conoce su identificador.
 */
@PruebaWeb(
        value = UsuariosController.class,
        properties = {"aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion"})
@MockitoBean(
        types = {
            bo.aportaya.identidad.aplicacion.ActivarRolOrganizador.class,
            bo.aportaya.identidad.aplicacion.ConsultarMiPerfil.class,
            bo.aportaya.identidad.aplicacion.CU02IniciarVerificacion.class
        })
@DisplayName("GET /usuarios/{id}/verificacion — solo su titular, sin datos personales")
class EstadoDeVerificacionWebTest {

    private static final String PIMIENTA = "pimienta-de-prueba-no-es-la-de-produccion";
    private static final UUID USUARIO = Sesiones.USUARIO;
    private static final UUID OTRA = Sesiones.OTRO_USUARIO;
    private static final UUID NO_EXISTE = UUID.fromString("99999999-0000-4000-8000-000000000009");
    private static final UUID VERIFICACION = UUID.fromString("dddddddd-0000-4000-8000-000000000001");
    private static final String MOTIVO = "La cara no coincide con el documento.";
    private static final List<String> LAS_CINCO =
            List.of("ANVERSO", "REVERSO", "SELFIE", "PERFIL_IZQUIERDO", "PERFIL_DERECHO");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Reloj reloj;

    @Autowired
    private ObjectMapper json;

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
    private ConsumirInvitacion consumirInvitacion;

    @MockitoBean
    private ConsultarNivelKyc nivelesKyc;

    @MockitoBean
    private SolicitarVerificacionCorreo solicitarCorreo;

    @MockitoBean
    private ConfirmarVerificacionCorreo confirmarCorreo;

    private String tokenDe(UUID usuario) {
        return new TokenDeAlta(PIMIENTA, reloj, Duration.ofMinutes(30))
                .emitir(usuario)
                .token();
    }

    private void existeElExpediente(String estado, String motivo, List<String> fotos) {
        when(estadoDeVerificacion.ejecutar(eq(USUARIO), any()))
                .thenReturn(new ExpedienteDeIdentidad(
                        VERIFICACION,
                        USUARIO,
                        "Pablo Arauz",
                        "CI SC",
                        estado,
                        OffsetDateTime.now(),
                        null,
                        motivo,
                        fotos,
                        LocalDate.of(2030, 1, 15)));
    }

    private MockHttpServletRequestBuilder consulta(UUID enLaRuta, String token) {
        MockHttpServletRequestBuilder peticion = get("/usuarios/{id}/verificacion", enLaRuta);
        return token == null ? peticion : peticion.header("Token-De-Alta", token);
    }

    private static String cuerpoDelRechazo(ResultActions resultado) throws Exception {
        return resultado
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DisplayName("CU-02 · 200 con el token de SU alta: quien recien se registro todavia no puede abrir sesion")
    void contestaConElTokenDelAlta() throws Exception {
        existeElExpediente("EN_REVISION", null, List.of("ANVERSO", "REVERSO"));

        mvc.perform(consulta(USUARIO, tokenDe(USUARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificacionId").value(VERIFICACION.toString()))
                .andExpect(jsonPath("$.estado").value("EN_REVISION"))
                .andExpect(jsonPath("$.fechaExpiracionDocumento").value("2030-01-15"))
                .andExpect(jsonPath("$.fotos", org.hamcrest.Matchers.hasSize(2)));
        verify(estadoDeVerificacion).ejecutar(eq(USUARIO), any());
    }

    @Test
    @DisplayName("CU-02 · 200 con su propia sesion, sin token de alta")
    void contestaConLaPropiaSesion() throws Exception {
        existeElExpediente("APROBADA", null, List.of("ANVERSO"));

        mvc.perform(consulta(USUARIO, null).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));
        verify(estadoDeVerificacion).ejecutar(eq(USUARIO), any());
    }

    @Test
    @DisplayName("rechaza: sin token y sin sesion no se lee nada (el UUID en la ruta no autoriza nada)")
    void sinTokenNiSesion() throws Exception {
        mvc.perform(consulta(USUARIO, null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        verifyNoInteractions(estadoDeVerificacion);
    }

    @Test
    @DisplayName("rechaza: otra persona que conoce el UUID, con SU token, no lee el expediente ajeno")
    void tokenDeOtraPersonaNoSirveEnLaRutaAjena() throws Exception {
        mvc.perform(consulta(USUARIO, tokenDe(OTRA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        verifyNoInteractions(estadoDeVerificacion);
    }

    @Test
    @DisplayName("rechaza: con la sesion de OTRA persona tampoco se lee el expediente del titular")
    void sesionDeOtraPersona() throws Exception {
        mvc.perform(consulta(USUARIO, null).with(Sesiones.comoOtro("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(estadoDeVerificacion);
    }

    @Test
    @DisplayName("rechaza: el 403 es IDENTICO exista o no el usuario de la ruta (no revela existencia)")
    void respuestaIdenticaSiExisteONo() throws Exception {
        existeElExpediente("RECHAZADA", MOTIVO, List.of());
        String tokenDeOtra = tokenDe(OTRA);

        JsonNode existente = json.readTree(cuerpoDelRechazo(mvc.perform(consulta(USUARIO, tokenDeOtra))));
        JsonNode inexistente = json.readTree(cuerpoDelRechazo(mvc.perform(consulta(NO_EXISTE, tokenDeOtra))));

        assertThat(existente.get("codigo")).isEqualTo(inexistente.get("codigo"));
        assertThat(existente.get("mensaje")).isEqualTo(inexistente.get("mensaje"));
        assertThat(existente.toString())
                .doesNotContain(USUARIO.toString())
                .doesNotContain(OTRA.toString())
                .doesNotContain(MOTIVO);
        verifyNoInteractions(estadoDeVerificacion);
    }

    @Test
    @DisplayName("rechaza: un token vencido no sirve, ni siquiera para su titular")
    void tokenVencido() throws Exception {
        var hace =
                new TokenDeAlta(PIMIENTA, Reloj.fijo(reloj.ahora().minus(Duration.ofHours(2))), Duration.ofMinutes(30));
        mvc.perform(consulta(USUARIO, hace.emitir(USUARIO).token())).andExpect(status().isForbidden());
        verifyNoInteractions(estadoDeVerificacion);
    }

    @Test
    @DisplayName("rechaza: un token firmado con otra clave (forjado) no sirve")
    void tokenForjado() throws Exception {
        var ajeno = new TokenDeAlta("otra-clave-que-no-es-la-del-servicio", reloj, Duration.ofMinutes(30));
        mvc.perform(consulta(USUARIO, ajeno.emitir(USUARIO).token())).andExpect(status().isForbidden());
        verifyNoInteractions(estadoDeVerificacion);
    }

    @ParameterizedTest(name = "rechaza: token mal formado «{0}»")
    @ValueSource(strings = {"x", ".", "123.", ".abc", "no-es-un-numero.abc", "9999999999.00", "1.2.3.4"})
    void tokenMalFormado(String token) throws Exception {
        mvc.perform(consulta(USUARIO, token)).andExpect(status().isForbidden());
        verifyNoInteractions(estadoDeVerificacion);
    }

    @Test
    @DisplayName("CU-02 · la respuesta no trae nombre ni documento, solo lo minimo")
    void laRespuestaNoTraeDatosPersonales() throws Exception {
        existeElExpediente("APROBADA", null, LAS_CINCO);

        String cuerpo = mvc.perform(consulta(USUARIO, tokenDe(USUARIO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(cuerpo)
                .doesNotContain("nombreCompleto")
                .doesNotContain("Pablo")
                .doesNotContain("documento")
                .doesNotContain("CI SC")
                .doesNotContain("usuarioId");
    }

    @Test
    @DisplayName("CU-02 · el motivo del rechazo viaja solo a su titular y solo cuando el estado es RECHAZADA")
    void motivoDeRechazoSoloSiRechazada() throws Exception {
        existeElExpediente("RECHAZADA", MOTIVO, List.of("ANVERSO"));
        mvc.perform(consulta(USUARIO, tokenDe(USUARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value(MOTIVO));

        // Un motivo que quedo en la fila de un expediente que ya no esta rechazado no sale.
        existeElExpediente("EN_REVISION", MOTIVO, List.of("ANVERSO"));
        String cuerpo = mvc.perform(consulta(USUARIO, tokenDe(USUARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(json.readTree(cuerpo).path("motivoRechazo").isValueNode()).isTrue();
        assertThat(json.readTree(cuerpo).path("motivoRechazo").isNull()).isTrue();
        assertThat(cuerpo).doesNotContain(MOTIVO);
    }

    @Test
    @DisplayName("CU-02 · 422 cuando todavia no existe un expediente para su titular")
    void sinExpedienteEs422() throws Exception {
        when(estadoDeVerificacion.ejecutar(eq(USUARIO), any()))
                .thenThrow(new ErrorDeDominio("Todavia no hay un expediente abierto para esa persona"));

        mvc.perform(consulta(USUARIO, tokenDe(USUARIO))).andExpect(status().isUnprocessableEntity());
    }
}
