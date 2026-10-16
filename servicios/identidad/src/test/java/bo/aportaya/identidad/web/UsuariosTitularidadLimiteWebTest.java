package bo.aportaya.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.BuscarPorTelefono;
import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario;
import bo.aportaya.identidad.aplicacion.CU02ConsultarEstadoDeVerificacion;
import bo.aportaya.identidad.aplicacion.CU02ActualizarFechaExpiracion;
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
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * CU-18 · la verificacion de titularidad contesta si/no, y un si/no repetido contra un documento es un oraculo: cada actor
 * (para {@code entregas}, su credencial de servicio) tiene un tope por minuto; pasado el tope recibe 429 con
 * {@code Retry-After} y el expediente ni se toca. Mismo mecanismo que la consulta de supresion de {@code notificaciones}.
 */
@PruebaWeb(
        value = UsuariosController.class,
        properties = {
            "aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion",
            "aportaya.titularidad.consultas-por-minuto=3"
        })
class UsuariosTitularidadLimiteWebTest {

    private static final UUID USUARIO = UUID.fromString("dddddddd-0000-4000-8000-0000000000b1");
    private static final String DOCUMENTO = "98765432";
    private static final String CUERPO =
            "{\"nombreCompleto\":\"Persona Sintetica\",\"documento\":\"" + DOCUMENTO + "\"}";

    @Autowired
    private MockMvc mvc;

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

    private ResultActions verificar(RequestPostProcessor sesion) throws Exception {
        return mvc.perform(post("/usuarios/{id}/titularidad/verificacion", USUARIO)
                .with(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO));
    }

    @Test
    @DisplayName(
            "rechaza: pasado el tope por minuto el actor de servicio recibe 429 con Retry-After, sin tocar el expediente, y OTRO actor sigue pudiendo")
    void topeDeVerificacionesPorActor() throws Exception {
        when(titularidad.coincide(any(), any(), any(), any())).thenReturn(false);
        // Actores propios de esta prueba: el reloj de las pruebas esta parado y el cupo de un actor compartido no se
        // renueva.
        var abusador = Sesiones.de(UUID.randomUUID(), "SERVICIO_ENTREGAS");

        for (int i = 0; i < 3; i++) {
            verificar(abusador)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coincide").value(false));
        }
        var corte = verificar(abusador)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.codigo").value("AP-LIM-01"))
                .andReturn();

        // El corte no repite lo consultado y el caso de uso no se entera de la cuarta.
        assertThat(corte.getResponse().getContentAsString()).doesNotContain(DOCUMENTO);
        verify(titularidad, times(3)).coincide(any(), any(), any(), any());

        // Otra persona (o el backoffice), con su propio cupo, no queda bloqueada por lo que hizo la primera.
        verificar(Sesiones.de(UUID.randomUUID(), "SERVICIO_ENTREGAS")).andExpect(status().isOk());
        verificar(Sesiones.de(UUID.randomUUID(), "BACKOFFICE", "DATOS_SENSIBLES_LEER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("rechaza: el rechazo por permiso no gasta cupo del tope (un 403 no consulta nada)")
    void elRechazoPorPermisoNoGastaCupo() throws Exception {
        var sinPermiso = Sesiones.de(UUID.randomUUID(), "PARTICIPANTE", "BILLETERA_VER");

        for (int i = 0; i < 6; i++) {
            verificar(sinPermiso).andExpect(status().isForbidden());
        }
    }
}
