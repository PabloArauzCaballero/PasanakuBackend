package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU62Permutar;
import bo.aportaya.grupos.aplicacion.CU63Acordar;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Carril I (quinta vuelta) · la guardia de {@code GET /grupos/solicitudes/{id}/decisiones} (historial del expediente
 * de admision).
 *
 * <p>Antes la ruta llevaba solo {@code @Permiso("PARTICIPANTE")}: ADMIN_PLATAFORMA (que tiene
 * {@code SOLICITUD_INGRESO_VER}) y el administrador del grupo (rol ORGANIZADOR con {@code GRUPO_ADMINISTRAR}) recibian
 * 403 de la guardia aunque {@code CU68AceptarIngreso.historial} los admite. Ahora la guardia tiene tres puertas y el
 * caso de uso sigue decidiendo sobre el RECURSO (esta prueba dobla el caso de uso: lo que fija es QUIEN llega a el).
 * Que un administrador de OTRO grupo, un participante ajeno o el soporte que no administra reciban la negativa del
 * caso de uso lo prueba {@code CU68AdmisionTest} con la base real.
 */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
@MockitoBean(types = bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo.class)
class GruposHistorialAdmisionWebTest {

    private static final UUID SOLICITUD = UUID.fromString("c6a00000-0000-4000-8000-000000000002");
    private static final String RUTA = "/grupos/solicitudes/{id}/decisiones";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CanjearInvitacion canjearInvitacion;

    @MockitoBean
    private CU20CrearGrupo cu20;

    @MockitoBean
    private CU59CalcularPlazo cu59;

    @MockitoBean
    private CU62Permutar cu62;

    @MockitoBean
    private CU63Acordar cu63;

    @MockitoBean
    private CU64TraspasarCupo cu64;

    @MockitoBean
    private CU65Retirarse cu65;

    @MockitoBean
    private CU68Postular cu68;

    @MockitoBean
    private CU68AceptarIngreso cu68Decision;

    @MockitoBean
    private InvitacionesWeb invitaciones;

    @MockitoBean
    private AdmisionDelGrupo admision;

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

    @MockitoBean
    private IntegrantesDelGrupo integrantesDelGrupo;

    @MockitoBean
    private GruposDeBackoffice gruposDeBackoffice;

    @Test
    @DisplayName(
            "backoffice con SOLICITUD_INGRESO_VER (ADMIN_PLATAFORMA) llega al caso de uso: ya no es 403 de la guardia")
    void backofficeLlegaAlExpediente() throws Exception {
        when(admision.historial(any(), any())).thenReturn(List.of());

        mvc.perform(get(RUTA, SOLICITUD).with(Sesiones.como("ADMIN_PLATAFORMA", "SOLICITUD_INGRESO_VER")))
                .andExpect(status().isOk());

        verify(admision).historial(any(), any());
    }

    @Test
    @DisplayName(
            "el administrador del grupo (ORGANIZADOR con GRUPO_ADMINISTRAR) y el participante siguen llegando al caso"
                    + " de uso")
    void administradorYParticipanteLlegan() throws Exception {
        when(admision.historial(any(), any())).thenReturn(List.of());

        mvc.perform(get(RUTA, SOLICITUD).with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                .andExpect(status().isOk());
        mvc.perform(get(RUTA, SOLICITUD).with(Sesiones.como("PARTICIPANTE"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("rechaza: un tercero sin ninguna de las tres puertas es 403 y el caso de uso no se invoca")
    void rechazaALosTercerosEnLaGuardia() throws Exception {
        // Otros roles de operacion, con permisos reales que NO son de esta ruta.
        mvc.perform(get(RUTA, SOLICITUD).with(Sesiones.como("TESORERIA", "ENTREGA_EJECUTAR")))
                .andExpect(status().isForbidden());
        mvc.perform(get(RUTA, SOLICITUD).with(Sesiones.como("RESPONSABLE_RIESGOS", "ENTREGA_AUTORIZAR")))
                .andExpect(status().isForbidden());
        // Con sesion pero sin ningun permiso.
        mvc.perform(get(RUTA, SOLICITUD).with(Sesiones.sinPermisos())).andExpect(status().isForbidden());

        verifyNoInteractions(admision);
    }

    @Test
    @DisplayName("rechaza: sin token es 401 (hay que volver a entrar), no 403, y el caso de uso no se invoca")
    void rechazaSinSesion() throws Exception {
        mvc.perform(get(RUTA, SOLICITUD)).andExpect(status().isUnauthorized());

        verifyNoInteractions(admision);
    }
}
