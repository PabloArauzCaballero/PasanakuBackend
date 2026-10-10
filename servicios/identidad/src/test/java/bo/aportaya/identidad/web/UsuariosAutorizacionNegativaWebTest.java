package bo.aportaya.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import bo.aportaya.identidad.aplicacion.ValidarTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * H13.S1.M2 / M3 · Lo que identidad rechaza por rol y por forma, con dos personas y los roles que
 * NO deben poder: un participante, otro participante, el personal de otra funcion y el token de un
 * servicio. Cada rechazo pasa por la guardia real y el caso de uso ni se entera.
 *
 * <p>Ademas fija que la busqueda por telefono —que dice si un numero tiene cuenta— viaja por
 * CUERPO: una URL queda en historiales, proxies y logs de acceso de cada salto (regla 90.2.2).
 */
@PruebaWeb(
        value = UsuariosController.class,
        properties = {"aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion"})
class UsuariosAutorizacionNegativaWebTest {

    // Sintetico: con la forma de un telefono boliviano, no es de nadie.
    private static final String TELEFONO = "+59170000002";
    private static final UUID USUARIO = UUID.fromString("dddddddd-0000-4000-8000-0000000000a1");
    private static final String CLAVE = "dddddddd-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsumirInvitacion consumirInvitacion;

    @MockitoBean
    private CU02GuardarFotoDelExpediente guardarFoto;

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

    private org.springframework.test.web.servlet.ResultActions buscar(RequestPostProcessor sesion, String cuerpo)
            throws Exception {
        var peticion = post("/usuarios/busquedas/por-telefono")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
        return mvc.perform(sesion == null ? peticion : peticion.with(sesion));
    }

    @Test
    @DisplayName("la busqueda por telefono lleva el numero en el CUERPO y no en la URL, y responde existe + id")
    void busquedaPorCuerpo() throws Exception {
        when(busqueda.ejecutar(any(), any())).thenReturn(Optional.of(USUARIO));

        var resultado = buscar(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"), "{\"telefono\":\"" + TELEFONO + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true))
                .andExpect(jsonPath("$.usuarioId").value(USUARIO.toString()))
                .andReturn();

        assertThat(resultado.getRequest().getRequestURI() + "?"
                        + resultado.getRequest().getQueryString())
                .doesNotContain(TELEFONO);
        assertThat(resultado.getResponse().getContentAsString())
                .doesNotContain(TELEFONO)
                .doesNotContain("nombre");
        verify(busqueda).ejecutar(org.mockito.ArgumentMatchers.eq(TELEFONO), any());
    }

    @Test
    @DisplayName("rechaza: sin token no se pregunta si un telefono tiene cuenta (401)")
    void busquedaSinToken() throws Exception {
        buscar(null, "{\"telefono\":\"" + TELEFONO + "\"}").andExpect(status().isUnauthorized());
        verifyNoInteractions(busqueda);
    }

    @Test
    @DisplayName("rechaza: un participante, otro participante o el token de un servicio no enumeran clientes")
    void busquedaRolInsuficiente() throws Exception {
        for (RequestPostProcessor sesion : new RequestPostProcessor[] {
            Sesiones.como("PARTICIPANTE", "BILLETERA_VER"),
            Sesiones.comoOtro("PARTICIPANTE", "BILLETERA_VER", "BILLETERA_OPERAR"),
            Sesiones.como("SERVICIO_ENTREGAS", "ENTREGA_EJECUTAR"),
            Sesiones.como("SOPORTE", "RECLAMO_ATENDER")
        }) {
            buscar(sesion, "{\"telefono\":\"" + TELEFONO + "\"}").andExpect(status().isForbidden());
        }
        verifyNoInteractions(busqueda);
    }

    @Test
    @DisplayName("rechaza: un campo de mas o un telefono fuera del patron es 400 y no llega al caso de uso")
    void busquedaMalFormada() throws Exception {
        var admin = Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR");
        buscar(admin, "{\"telefono\":\"" + TELEFONO + "\",\"incluirNombre\":true}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        buscar(admin, "{\"telefono\":\"70000002\"}").andExpect(status().isBadRequest());
        buscar(admin, "{}").andExpect(status().isBadRequest());
        verifyNoInteractions(busqueda);
    }

    @Test
    @DisplayName("rechaza: verificar la titularidad (nombre y documento) exige DATOS_SENSIBLES_LEER")
    void titularidadSoloParaQuienLeeDatosSensibles() throws Exception {
        String cuerpo = "{\"nombreCompleto\":\"Pablo Arauz\",\"documento\":\"1234567\"}";
        mvc.perform(post("/usuarios/{id}/titularidad/verificacion", USUARIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isUnauthorized());
        for (RequestPostProcessor sesion : new RequestPostProcessor[] {
            Sesiones.como("PARTICIPANTE", "BILLETERA_VER"),
            Sesiones.comoOtro("PARTICIPANTE", "BILLETERA_VER"),
            Sesiones.como("SERVICIO_INVERSIONES", "ENTREGA_EJECUTAR"),
            Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")
        }) {
            mvc.perform(post("/usuarios/{id}/titularidad/verificacion", USUARIO)
                            .with(sesion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(titularidad);
    }

    @Test
    @DisplayName("rechaza: emitir un enlace de invitacion exige GRUPO_ADMINISTRAR; el token de un servicio no invita")
    void emitirInvitacionSoloAdministradores() throws Exception {
        String cuerpo = "{\"canal\":\"WHATSAPP\",\"grupoId\":\"" + UUID.randomUUID() + "\",\"telefonoDestino\":\""
                + TELEFONO + "\"}";
        for (RequestPostProcessor sesion : new RequestPostProcessor[] {
            Sesiones.como("PARTICIPANTE", "BILLETERA_VER"),
            Sesiones.como("SERVICIO_ENTREGAS", "ENTREGA_EJECUTAR"),
            Sesiones.como("SOPORTE", "RECLAMO_ATENDER")
        }) {
            mvc.perform(post("/usuarios/tokens/invitacion")
                            .with(sesion)
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(tokens);
    }
}
