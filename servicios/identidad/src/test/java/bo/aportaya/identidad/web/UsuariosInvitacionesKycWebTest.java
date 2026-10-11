package bo.aportaya.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.BuscarPorTelefono;
import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario;
import bo.aportaya.identidad.aplicacion.CU02GuardarFotoDelExpediente;
import bo.aportaya.identidad.aplicacion.ConfirmarVerificacionCorreo;
import bo.aportaya.identidad.aplicacion.ConsultarNivelKyc;
import bo.aportaya.identidad.aplicacion.ConsumirInvitacion;
import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.SolicitarVerificacionCorreo;
import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Canje y revocación del enlace de invitación, y la consulta del nivel de KYC: matriz negativa por
 * endpoint (sin token, rol insuficiente, otra persona, entrada mal formada) y que el secreto nunca
 * salga en la respuesta.
 */
@PruebaWeb(
        value = UsuariosController.class,
        properties = {"aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion"})
class UsuariosInvitacionesKycWebTest {

    private static final UUID TOKEN_ID = UUID.fromString("eeeeeeee-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("eeeeeeee-0000-4000-8000-000000000002");
    private static final String CLAVE = "eeeeeeee-0000-4000-8000-0000000000ff";
    private static final String SECRETO = "ab".repeat(32);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsumirInvitacion consumirInvitacion;

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

    private org.springframework.test.web.servlet.ResultActions canjear(String token) throws Exception {
        return mvc.perform(post("/usuarios/tokens/invitacion/{id}/consumos", TOKEN_ID)
                .with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"grupoId\":\"" + GRUPO + "\",\"token\":\"" + token + "\"}"));
    }

    @Test
    @DisplayName("200: el canje devuelve el recibo y NUNCA el secreto")
    void canjeValido() throws Exception {
        when(consumirInvitacion.ejecutar(eq(TOKEN_ID), eq(GRUPO), any(), eq(SECRETO), any(), any()))
                .thenReturn(Optional.of(new ConsumirInvitacion.Consumo(
                        TOKEN_ID, GRUPO, Sesiones.USUARIO, UUID.fromString(CLAVE), OffsetDateTime.now())));

        String cuerpo = canjear(SECRETO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenId").value(TOKEN_ID.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(cuerpo).doesNotContain(SECRETO).doesNotContain("\"token\":");
    }

    @Test
    @DisplayName("422 genérico: token ajeno, vencido o teléfono distinto no se distinguen hacia afuera")
    void canjeRechazadoNoCuentaElMotivo() throws Exception {
        when(consumirInvitacion.ejecutar(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        canjear(SECRETO)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU69-05"));
    }

    @Test
    @DisplayName("400: un secreto que no son 64 hexadecimales no llega al caso de uso")
    void secretoMalFormado() throws Exception {
        canjear("corto").andExpect(status().isBadRequest());
        verifyNoInteractions(consumirInvitacion);
    }

    @Test
    @DisplayName("401 sin sesión y 403 sin el permiso: el canje no llega al caso de uso")
    void canjeSinSesionOSinPermiso() throws Exception {
        mvc.perform(post("/usuarios/tokens/invitacion/{id}/consumos", TOKEN_ID)
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grupoId\":\"" + GRUPO + "\",\"token\":\"" + SECRETO + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/usuarios/tokens/invitacion/{id}/consumos", TOKEN_ID)
                        .with(Sesiones.sinPermisos())
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grupoId\":\"" + GRUPO + "\",\"token\":\"" + SECRETO + "\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(consumirInvitacion);
    }

    @Test
    @DisplayName("204 revoca el emisor; 422 si no es suyo o ya no es revocable; 403 sin el permiso")
    void revocacion() throws Exception {
        when(consumirInvitacion.revocar(eq(TOKEN_ID), any())).thenReturn(true, false);

        mvc.perform(post("/usuarios/tokens/invitacion/{id}/revocacion", TOKEN_ID)
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                .andExpect(status().isNoContent());
        mvc.perform(post("/usuarios/tokens/invitacion/{id}/revocacion", TOKEN_ID)
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/usuarios/tokens/invitacion/{id}/revocacion", TOKEN_ID)
                        .with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("KYC: 200 con el nivel y nada más; 403 si lee a otra persona sin ser backoffice; 401 sin sesión")
    void nivelDeKyc() throws Exception {
        when(nivelesKyc.ejecutar(eq(Sesiones.USUARIO), any())).thenReturn("BASICO");
        when(nivelesKyc.ejecutar(eq(Sesiones.OTRO_USUARIO), any())).thenThrow(new AccessDeniedException("no"));

        mvc.perform(get("/usuarios/{id}/kyc", Sesiones.USUARIO).with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivel").value("BASICO"))
                .andExpect(jsonPath("$.usuarioId").value(Sesiones.USUARIO.toString()));
        mvc.perform(get("/usuarios/{id}/kyc", Sesiones.OTRO_USUARIO)
                        .with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/usuarios/{id}/kyc", Sesiones.USUARIO)).andExpect(status().isUnauthorized());
        mvc.perform(get("/usuarios/{id}/kyc", Sesiones.USUARIO).with(Sesiones.sinPermisos()))
                .andExpect(status().isForbidden());
    }
}
