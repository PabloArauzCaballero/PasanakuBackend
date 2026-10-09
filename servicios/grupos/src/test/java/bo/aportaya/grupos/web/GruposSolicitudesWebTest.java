package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** CU-68 · el camino directo del organizador sobre las solicitudes de ingreso, y mis participaciones. */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
class GruposSolicitudesWebTest {

    private static final UUID GRUPO = UUID.fromString("c4000000-0000-4000-8000-000000000001");
    private static final UUID SOLICITUD = UUID.fromString("c4000000-0000-4000-8000-000000000002");
    private static final String CLAVE = "c4000000-0000-4000-8000-0000000000ff";

    private static final String GRUPO_NUEVO =
            """
            {"nombre":"Pasanaku del barrio","montoAporte":{"monto":"500.00","moneda":"BOB"},
             "periodicidad":"MENSUAL","cupos":10,"diaCobro":5,
             "modalidadTurnos":"SORTEO_ALEATORIO","fechaDeInicio":"2026-05-01",
             "permitePermutaDeTurnos":true}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private bo.aportaya.grupos.aplicacion.CanjearInvitacion canjearInvitacion;

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
    private bo.aportaya.grupos.aplicacion.CU68AceptarIngreso cu68Decision;

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

    @Nested
    @DisplayName("CU-68 · decisión del organizador sobre una solicitud de ingreso")
    class DecisionDeIngreso {
        private final java.util.UUID solicitud = java.util.UUID.fromString("c6800000-0000-4000-8000-000000000001");
        private final java.util.UUID participante = java.util.UUID.fromString("c6800000-0000-4000-8000-000000000002");

        @Test
        @DisplayName("CU-68 · 200: el organizador acepta y el participante queda pendiente de firma")
        void acepta() throws Exception {
            when(cu68Decision.solicitante(org.mockito.ArgumentMatchers.eq(solicitud), any()))
                    .thenReturn(java.util.UUID.randomUUID());
            when(afuera.reputacion(any()))
                    .thenReturn(new HechosDeOtrosServicios.Reputacion(true, new java.math.BigDecimal("700")));
            when(cu68Decision.decidir(
                            org.mockito.ArgumentMatchers.eq(solicitud),
                            org.mockito.ArgumentMatchers.eq(true),
                            any(),
                            any(),
                            any()))
                    .thenReturn(new bo.aportaya.grupos.aplicacion.CU68AceptarIngreso.Resultado(
                            solicitud, "APROBADA", participante, null));

            mvc.perform(post("/grupos/solicitudes/{id}/decision", solicitud)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                            .header(
                                    "Idempotency-Key",
                                    java.util.UUID.randomUUID().toString())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"ACEPTAR\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("APROBADA"))
                    .andExpect(jsonPath("$.participanteId").value(participante.toString()));
        }

        @Test
        @DisplayName("CU-68 · 403: un participante, sin GRUPO_ADMINISTRAR, no decide")
        void participanteNoDecide() throws Exception {
            mvc.perform(post("/grupos/solicitudes/{id}/decision", solicitud)
                            .with(Sesiones.como("PARTICIPANTE"))
                            .header(
                                    "Idempotency-Key",
                                    java.util.UUID.randomUUID().toString())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"ACEPTAR\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu68Decision);
        }

        @Test
        @DisplayName("CU-68 · 401: sin sesión")
        void sinSesion() throws Exception {
            mvc.perform(post("/grupos/solicitudes/{id}/decision", solicitud)
                            .header(
                                    "Idempotency-Key",
                                    java.util.UUID.randomUUID().toString())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"ACEPTAR\"}"))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(cu68Decision);
        }

        @Test
        @DisplayName("CU-68 · 400: una decisión que el contrato no enumera")
        void decisionInvalida() throws Exception {
            mvc.perform(post("/grupos/solicitudes/{id}/decision", solicitud)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                            .header(
                                    "Idempotency-Key",
                                    java.util.UUID.randomUUID().toString())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"TAL_VEZ\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu68Decision);
        }
    }

    @Nested
    @DisplayName("CU-68 · mis participaciones (lo que la app necesita para abrir sus pantallas)")
    class MisParticipaciones {
        @Test
        @DisplayName("200: devuelve MIS participaciones, resueltas con la sesión y no con un id del cliente")
        void lista() throws Exception {
            var grupo = java.util.UUID.fromString("c6900000-0000-4000-8000-000000000001");
            var participante = java.util.UUID.fromString("c6900000-0000-4000-8000-000000000002");
            when(consultas.participacionesDe(any()))
                    .thenReturn(java.util.List.of(
                            new bo.aportaya.grupos.aplicacion.Consultas.Participacion(grupo, participante, "ACTIVO")));

            mvc.perform(get("/grupos/participaciones").with(Sesiones.como("PARTICIPANTE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].participanteId").value(participante.toString()))
                    .andExpect(jsonPath("$[0].estado").value("ACTIVO"));
        }

        @Test
        @DisplayName("401: sin sesión")
        void sinSesion() throws Exception {
            mvc.perform(get("/grupos/participaciones")).andExpect(status().isUnauthorized());
        }
    }
}
