package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Complemento de {@link EstadoDeVerificacionWebTest}: casos adicionales del endpoint
 * {@code GET /usuarios/{id}/verificacion} separados para mantener el limite de 300 lineas por archivo.
 */
@PruebaWeb(
        value = UsuariosController.class,
        properties = {"aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion"})
@MockitoBean(
        types = {
            bo.aportaya.identidad.aplicacion.ActivarRolOrganizador.class,
            bo.aportaya.identidad.aplicacion.ConsultarMiPerfil.class,
            bo.aportaya.identidad.aplicacion.CU07FotoDePerfil.class,
            bo.aportaya.identidad.aplicacion.CU02IniciarVerificacion.class,
            bo.aportaya.identidad.aplicacion.GuardarBorradorOnboarding.class
        })
@DisplayName("GET /usuarios/{id}/verificacion — casos complementarios")
class EstadoDeVerificacionWebTestComplemento {

    private static final String PIMIENTA = "pimienta-de-prueba-no-es-la-de-produccion";
    private static final UUID USUARIO = Sesiones.USUARIO;
    private static final String MOTIVO = "La cara no coincide con el documento.";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Reloj reloj;

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

    private void existeElExpediente(String estado, String motivo) {
        when(estadoDeVerificacion.ejecutar(eq(USUARIO), any()))
                .thenReturn(new ExpedienteDeIdentidad(
                        UUID.fromString("dddddddd-0000-4000-8000-000000000001"),
                        USUARIO,
                        "Pablo Arauz",
                        "CI SC",
                        estado,
                        OffsetDateTime.now(),
                        null,
                        motivo,
                        List.of("ANVERSO"),
                        LocalDate.of(2030, 1, 15)));
    }

    @Test
    @DisplayName("CU-02 · 422 cuando todavia no existe un expediente para su titular")
    void sinExpedienteEs422() throws Exception {
        when(estadoDeVerificacion.ejecutar(eq(USUARIO), any()))
                .thenThrow(new ErrorDeDominio("Todavia no hay un expediente abierto para esa persona"));

        mvc.perform(get("/usuarios/{id}/verificacion", USUARIO).header("Token-De-Alta", tokenDe(USUARIO)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("CU-02 · RECHAZADA con motivo llega correctamente al titular")
    void rechazadaConMotivo() throws Exception {
        existeElExpediente("RECHAZADA", MOTIVO);

        mvc.perform(get("/usuarios/{id}/verificacion", USUARIO).header("Token-De-Alta", tokenDe(USUARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value(MOTIVO));
    }
}
