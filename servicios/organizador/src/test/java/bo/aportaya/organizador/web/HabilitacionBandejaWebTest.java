package bo.aportaya.organizador.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.organizador.aplicacion.CU90ConsultarBandeja;
import bo.aportaya.organizador.aplicacion.CU90PostularOrganizador;
import bo.aportaya.organizador.aplicacion.CU90ResolverHabilitacion;
import bo.aportaya.organizador.aplicacion.CU91FirmarContrato;
import bo.aportaya.organizador.aplicacion.CU92EvaluarDesempeno;
import bo.aportaya.organizador.aplicacion.CU93SancionarOrganizador;
import bo.aportaya.organizador.aplicacion.ConsultarHabilitacion;
import bo.aportaya.organizador.dominio.DecisionDeHabilitacion;
import bo.aportaya.organizador.dominio.ExpedienteDeHabilitacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-90 · contrato HTTP de la bandeja, el expediente y la resolución humana de habilitaciones, con
 * su matriz negativa: sin sesión, permiso insuficiente, rol que el caso de uso vuelve a negar y
 * entrada fuera del contrato. La sábana de seguridad cubre además 401/403/Idempotency-Key de todas.
 */
@PruebaWeb(OrganizadoresController.class)
class HabilitacionBandejaWebTest {

    private static final UUID SOLICITUD = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID USUARIO = UUID.fromString("b9000000-0000-4000-8000-000000000002");
    private static final UUID ACTOR = UUID.fromString("b9000000-0000-4000-8000-000000000003");
    private static final String CLAVE = "b9000000-0000-4000-8000-0000000000ff";
    private static final OffsetDateTime FECHA = OffsetDateTime.of(2026, 10, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarHabilitacion habilitaciones;

    @MockitoBean
    private CU90PostularOrganizador cu90;

    @MockitoBean
    private CU91FirmarContrato cu91;

    @MockitoBean
    private CU92EvaluarDesempeno cu92;

    @MockitoBean
    private CU93SancionarOrganizador cu93;

    @MockitoBean
    private CU90ResolverHabilitacion resolverHabilitacion;

    @MockitoBean
    private CU90ConsultarBandeja consultarBandeja;

    private ExpedienteDeHabilitacion solicitud() {
        return new ExpedienteDeHabilitacion(
                SOLICITUD, USUARIO, "PENDIENTE", new BigDecimal("82.50"), UUID.randomUUID(), null, FECHA, null, 0);
    }

    private DecisionDeHabilitacion decision() {
        return new DecisionDeHabilitacion(
                UUID.randomUUID(),
                SOLICITUD,
                UUID.fromString(CLAVE),
                "RESOLUCION",
                "RECHAZAR",
                ACTOR,
                "No cumple el perfil",
                1,
                null,
                "habilitable=false",
                FECHA,
                UUID.randomUUID());
    }

    @Test
    @DisplayName("200: la bandeja devuelve las filas con sus acciones y el cursor de la página siguiente")
    void bandejaConCursor() throws Exception {
        when(consultarBandeja.bandeja(any(), any(), any(), eq(1), any())).thenReturn(List.of(solicitud()));

        mvc.perform(get("/organizadores/postulaciones/bandeja")
                        .param("limite", "1")
                        .with(Sesiones.como("BACKOFFICE", "ADMIN_PLATAFORMA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].solicitudId").value(SOLICITUD.toString()))
                .andExpect(jsonPath("$.items[0].acciones[0]").value("RESOLVER"))
                .andExpect(jsonPath("$.proximoId").value(SOLICITUD.toString()));
    }

    @Test
    @DisplayName("403: sin el permiso, o con el permiso pero sin ser backoffice, la bandeja no se abre")
    void bandejaNegada() throws Exception {
        mvc.perform(get("/organizadores/postulaciones/bandeja").with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE")))
                .andExpect(status().isForbidden());
        when(consultarBandeja.bandeja(any(), any(), any(), anyInt(), any())).thenThrow(new AccessDeniedException("no"));
        mvc.perform(get("/organizadores/postulaciones/bandeja").with(Sesiones.como("ORGANIZADOR", "ADMIN_PLATAFORMA")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/organizadores/postulaciones/bandeja")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName(
            "400: un límite fuera del contrato no llega al caso de uso (el estado inventado lo rechaza el caso de uso)")
    void entradaFueraDelContrato() throws Exception {
        mvc.perform(get("/organizadores/postulaciones/bandeja")
                        .param("limite", "5000")
                        .with(Sesiones.como("BACKOFFICE", "ADMIN_PLATAFORMA")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(consultarBandeja);
    }

    @Test
    @DisplayName("El expediente propio NO trae quién decidió ni la evidencia interna; el del backoffice sí")
    void expedientePropioSinDatosInternos() throws Exception {
        var expediente = new CU90ConsultarBandeja.Expediente(solicitud(), List.of(decision()));
        when(consultarBandeja.expedienteDelUsuario(any())).thenReturn(expediente);
        when(consultarBandeja.expediente(eq(SOLICITUD), any())).thenReturn(expediente);

        String propio = mvc.perform(
                        get("/organizadores/postulaciones/mia").with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisiones[0].motivo").value("No cumple el perfil"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(propio)
                .doesNotContain(ACTOR.toString())
                .doesNotContain("habilitable=")
                .doesNotContain("kycReforzadoId\":\"");

        String interno = mvc.perform(get("/organizadores/postulaciones/{id}/expediente", SOLICITUD)
                        .with(Sesiones.como("BACKOFFICE", "ADMIN_PLATAFORMA")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(interno).contains(ACTOR.toString()).contains("habilitable=");
    }

    @Test
    @DisplayName("200 al resolver con la decisión, 422 si el caso de uso la rechaza, y 400 con un resultado inventado")
    void resolver() throws Exception {
        when(resolverHabilitacion.resolver(any(), any()))
                .thenReturn(new CU90ResolverHabilitacion.Resultado(decision(), true));

        mvc.perform(
                        post("/organizadores/postulaciones/{id}/resolucion", SOLICITUD)
                                .with(Sesiones.como("BACKOFFICE", "ADMIN_PLATAFORMA"))
                                .header("Idempotency-Key", CLAVE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"decision\":\"RECHAZAR\",\"motivo\":\"No cumple el perfil\",\"revisionEsperada\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nueva").value(true))
                .andExpect(jsonPath("$.decision").value("RECHAZAR"));

        when(resolverHabilitacion.resolver(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(90, 4), "Quien aprueba no puede ser quien postulo."));
        mvc.perform(post("/organizadores/postulaciones/{id}/resolucion", SOLICITUD)
                        .with(Sesiones.como("BACKOFFICE", "ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APROBAR\",\"motivo\":\"Me apruebo\",\"revisionEsperada\":0}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU90-04"));

        mvc.perform(post("/organizadores/postulaciones/{id}/resolucion", SOLICITUD)
                        .with(Sesiones.como("BACKOFFICE", "ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"AUTO\",\"motivo\":\"x\",\"revisionEsperada\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("403: quien tiene PARTICIPANTE no resuelve ni revisa habilitaciones")
    void participanteNoResuelve() throws Exception {
        for (String ruta : List.of("resolucion", "revision")) {
            mvc.perform(post("/organizadores/postulaciones/{id}/" + ruta, SOLICITUD)
                            .with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"APROBAR\",\"motivo\":\"x\",\"revisionEsperada\":0}"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(resolverHabilitacion);
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
