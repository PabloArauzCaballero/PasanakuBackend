package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.DecisionIngreso;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Interruptor {@code aportaya.grupos.admision.humana-obligatoria} ENCENDIDO (el valor de produccion), puerta 2: la decision directa del
 * organizador ({@code decidirSolicitudDeIngreso}) deja entrar sin backoffice. Se cierra con 422 {@code AP-CU68-15} sin invocar el
 * caso de uso; el camino con expediente (propuesta del administrador, resolucion de backoffice) sigue disponible. El modo APAGADO
 * lo cubre {@code GruposSolicitudesWebTest}. DECISION PENDIENTE de negocio y cumplimiento.
 */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
            "aportaya.grupos.admision.humana-obligatoria=true",
        })
@MockitoBean(types = bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo.class)
class GruposAdmisionHumanaWebTest {

    private static final UUID SOLICITUD = UUID.fromString("c6a00000-0000-4000-8000-000000000001");

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

    private org.springframework.test.web.servlet.ResultActions decidir() throws Exception {
        return mvc.perform(post("/grupos/solicitudes/{id}/decision", SOLICITUD)
                .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"ACEPTAR\"}"));
    }

    @Test
    @DisplayName(
            "ENCENDIDO · la decision directa del organizador se rechaza con AP-CU68-15 y no invoca ni al caso de uso ni a reputacion")
    void ladecisionDirectaSeCierra() throws Exception {
        decidir()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU68-15"));

        verifyNoInteractions(cu68Decision, afuera);
    }

    @Test
    @DisplayName(
            "ENCENDIDO · rechazar tampoco es una decision directa: ninguna decision de un organizador solo cierra la solicitud")
    void rechazarTambienSeCierra() throws Exception {
        mvc.perform(post("/grupos/solicitudes/{id}/decision", SOLICITUD)
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"RECHAZAR\",\"motivo\":\"no corresponde\"}"))
                .andExpect(status().isUnprocessableEntity());

        verifyNoInteractions(cu68Decision);
    }

    @Test
    @DisplayName("ENCENDIDO · sin permiso sigue siendo 403 y sin sesion 401: el interruptor no abre nada")
    void laGuardiaSigueIgual() throws Exception {
        mvc.perform(post("/grupos/solicitudes/{id}/decision", SOLICITUD)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"ACEPTAR\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/grupos/solicitudes/{id}/decision", SOLICITUD)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"ACEPTAR\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cu68Decision);
    }

    @Test
    @DisplayName("ENCENDIDO · el camino con expediente sigue abierto: el administrador PROPONE y backoffice resuelve")
    void elCaminoConExpedienteSigueAbierto() throws Exception {
        when(admision.decidir(any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new DecisionIngreso());

        mvc.perform(
                        post("/grupos/solicitudes/{id}/propuestas", SOLICITUD)
                                .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"decision\":\"ACEPTAR\",\"motivo\":\"conozco a la persona\",\"revisionEsperada\":0}"))
                .andExpect(status().isOk());

        verify(admision).decidir(any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(false));
    }
}
