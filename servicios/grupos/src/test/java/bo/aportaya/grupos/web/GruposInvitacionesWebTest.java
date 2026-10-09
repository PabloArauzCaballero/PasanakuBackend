package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Enlace;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.InvitacionRegistrada;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-69 · emisión y revocación de invitaciones a nivel HTTP: la matriz negativa de la ruta de
 * revocación y la compensación del enlace cuando la invitación no llega a existir.
 */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
@Import(InvitacionesWeb.class)
class GruposInvitacionesWebTest {

    private static final UUID GRUPO = UUID.fromString("c9000000-0000-4000-8000-000000000001");
    private static final UUID INVITACION = UUID.fromString("c9000000-0000-4000-8000-000000000002");
    private static final UUID TOKEN = UUID.fromString("c9000000-0000-4000-8000-000000000003");
    private static final String CLAVE = "c9000000-0000-4000-8000-0000000000ff";
    private static final String SECRETO = "cd".repeat(32);
    private static final String ENTRADA = "{\"telefonoInvitado\":\"+59171234567\",\"canal\":\"ENLACE\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CanjearInvitacion canjearInvitacion;

    @MockitoBean
    private CU20CrearGrupo cu20;

    @MockitoBean
    private CU59CalcularPlazo cu59;

    @MockitoBean
    private CU64TraspasarCupo cu64;

    @MockitoBean
    private CU65Retirarse cu65;

    @MockitoBean
    private CU68Postular cu68;

    @MockitoBean
    private AdmisionDelGrupo admision;

    @MockitoBean
    private CU68AceptarIngreso cu68Decision;

    @MockitoBean
    private CU69Enlace enlaces;

    @MockitoBean
    private CU69Invitar cu69;

    @MockitoBean
    private Consultas consultas;

    @MockitoBean
    private HechosDeOtrosServicios afuera;

    @MockitoBean
    private RespuestasAOtrosServicios respuestas;

    @MockitoBean
    private SorteoDelGrupo sorteo;

    private InvitacionRegistrada invitacion(UUID emisor, UUID grupo) {
        return new InvitacionRegistrada(
                "ENVIADA", (short) 1, grupo, OffsetDateTime.now().plusDays(1), TOKEN, emisor);
    }

    private org.springframework.test.web.servlet.ResultActions revocar(
            org.springframework.test.web.servlet.request.RequestPostProcessor sesion) throws Exception {
        return mvc.perform(post("/grupos/{g}/invitaciones/{i}/revocacion", GRUPO, INVITACION)
                .with(sesion));
    }

    private org.springframework.test.web.servlet.ResultActions invitar() throws Exception {
        return mvc.perform(post("/grupos/{g}/invitaciones", GRUPO)
                .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ENTRADA));
    }

    @Test
    @DisplayName("204: el emisor revoca primero el enlace y despues la invitacion")
    void emisorRevoca() throws Exception {
        when(cu69.consultar(any(), any())).thenReturn(invitacion(Sesiones.USUARIO, GRUPO));
        when(afuera.revocarTokenDeInvitacion(TOKEN)).thenReturn(true);

        revocar(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")).andExpect(status().isNoContent());

        verify(afuera).revocarTokenDeInvitacion(TOKEN);
        verify(cu69).revocar(any(), any());
    }

    @Test
    @DisplayName("422: otra persona no revoca la invitacion ajena, y no se toca ni el enlace ni el estado")
    void otroUsuarioNoRevoca() throws Exception {
        when(cu69.consultar(any(), any())).thenReturn(invitacion(Sesiones.OTRO_USUARIO, GRUPO));

        revocar(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU69-05"));

        verify(afuera, never()).revocarTokenDeInvitacion(any());
        verify(cu69, never()).revocar(any(), any());
    }

    @Test
    @DisplayName("422: la invitacion de OTRO grupo no se revoca por la ruta de este")
    void otroGrupoNoRevoca() throws Exception {
        when(cu69.consultar(any(), any())).thenReturn(invitacion(Sesiones.USUARIO, UUID.randomUUID()));

        revocar(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")).andExpect(status().isUnprocessableEntity());

        verify(afuera, never()).revocarTokenDeInvitacion(any());
        verify(cu69, never()).revocar(any(), any());
    }

    @Test
    @DisplayName("422: si identidad no confirma la revocacion del enlace, la invitacion NO se da por revocada")
    void sinConfirmacionDelEnlaceNoSeRevoca() throws Exception {
        when(cu69.consultar(any(), any())).thenReturn(invitacion(Sesiones.USUARIO, GRUPO));
        when(afuera.revocarTokenDeInvitacion(TOKEN)).thenReturn(false);

        revocar(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")).andExpect(status().isUnprocessableEntity());

        verify(cu69, never()).revocar(any(), any());
    }

    @Test
    @DisplayName("401 sin sesion y 403 sin GRUPO_ADMINISTRAR: no llega al caso de uso")
    void sinSesionOSinPermiso() throws Exception {
        mvc.perform(post("/grupos/{g}/invitaciones/{i}/revocacion", GRUPO, INVITACION))
                .andExpect(status().isUnauthorized());
        revocar(Sesiones.como("PARTICIPANTE", "PARTICIPANTE")).andExpect(status().isForbidden());
        verify(cu69, never()).consultar(any(), any());
    }

    @Test
    @DisplayName("201: la invitacion devuelve el enlace una vez, sin cache")
    void invitacionDevuelveElEnlaceSinCache() throws Exception {
        when(afuera.contactoSuprimido(any(), any())).thenReturn(false);
        when(afuera.usuarioDelTelefono(any())).thenReturn(Optional.empty());
        when(afuera.tokenDeInvitacion(any(), any(), any(), any()))
                .thenReturn(new HechosDeOtrosServicios.TokenInvitacion(
                        TOKEN, SECRETO, OffsetDateTime.now().plusDays(1)));
        when(cu69.invitar(any(), any()))
                .thenReturn(
                        new CU69Invitar.Resultado(Optional.of(INVITACION), "Invitacion disponible para compartir."));

        invitar()
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.token").value(SECRETO));
    }

    @Test
    @DisplayName("Si la invitacion no llega a existir (ya hay una vigente), el enlace recien emitido se revoca")
    void enlaceHuerfanoSeRevoca() throws Exception {
        when(afuera.contactoSuprimido(any(), any())).thenReturn(false);
        when(afuera.usuarioDelTelefono(any())).thenReturn(Optional.empty());
        when(afuera.tokenDeInvitacion(any(), any(), any(), any()))
                .thenReturn(new HechosDeOtrosServicios.TokenInvitacion(
                        TOKEN, SECRETO, OffsetDateTime.now().plusDays(1)));
        when(cu69.invitar(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(69, 9), "Ya hay una invitacion vigente."));

        invitar()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU69-09"));

        verify(afuera).revocarTokenDeInvitacion(TOKEN);
    }

    @Test
    @DisplayName("Una clave que corresponde a OTRA invitacion (69-06) NO revoca el enlace: ese enlace tiene dueno")
    void claveDeOtraInvitacionNoRevocaElEnlaceAjeno() throws Exception {
        when(afuera.contactoSuprimido(any(), any())).thenReturn(false);
        when(afuera.usuarioDelTelefono(any())).thenReturn(Optional.empty());
        when(afuera.tokenDeInvitacion(any(), any(), any(), any()))
                .thenReturn(new HechosDeOtrosServicios.TokenInvitacion(
                        TOKEN, SECRETO, OffsetDateTime.now().plusDays(1)));
        when(cu69.invitar(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(69, 6), "La clave corresponde a otra invitacion."));

        invitar().andExpect(status().isUnprocessableEntity());

        verify(afuera, never()).revocarTokenDeInvitacion(any());
    }
}
