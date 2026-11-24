package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.identidad.aplicacion.GobiernoDeRoles;
import bo.aportaya.identidad.aplicacion.GobiernoDeRoles.Resultado;
import bo.aportaya.identidad.dominio.ReglasDeRolPersonalizado;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * GU-16 (CU-135) en la capa web: el contrato, la matriz de autorizacion negativa (sin token, sin permiso, permiso de
 * lectura donde se pide el de gestion, rol de otro dominio) y que cada regla llegue como 422 con su codigo. El caso de
 * uso va con doble: la persistencia se prueba contra PostgreSQL en {@code GobiernoDeRolesTest}.
 */
@PruebaWeb(RolesController.class)
class RolesGobiernoWebTest {

    private static final UUID ROL = UUID.fromString("33333333-4444-4555-8666-777777777777");
    private static final UUID SOLICITUD = UUID.fromString("44444444-5555-4666-8777-888888888888");
    private static final String EVIDENCIA = "evidencia-step-up-sintetica-de-prueba";
    private static final String CUERPO_CREAR =
            """
            {"codigo":"ANALISTA_DE_ZONA","nombre":"Analista de zona","permisos":["TABLERO_VER"],
             "motivo":"Nuevo puesto aprobado","evidenciaMfa":"%s"}
            """
                    .formatted(EVIDENCIA);
    private static final String CUERPO_EDITAR =
            """
            {"nombre":"Analista de zona","permisos":["TABLERO_VER"],"motivo":"Ajuste de funcion","evidenciaMfa":"%s"}
            """
                    .formatted(EVIDENCIA);
    private static final String CUERPO_MOTIVO =
            """
            {"motivo":"Resuelto por segunda persona","evidenciaMfa":"%s"}
            """
                    .formatted(EVIDENCIA);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GestionarUsuariosInternos casoDeUso;

    @MockitoBean
    private GobiernoDeRoles gobierno;

    @Test
    @DisplayName("crear: 201 con el rol creado cuando no hay riesgo alto")
    void crearAplicado() throws Exception {
        when(gobierno.crear(any(), any())).thenReturn(new Resultado("APLICADO", ROL, null));

        mvc.perform(crear(Sesiones.como("INTERNAL_IDENTITY_ADMIN", "ROLES_INTERNOS_GESTIONAR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("APLICADO"))
                .andExpect(jsonPath("$.rolId").value(ROL.toString()));
    }

    @Test
    @DisplayName("crear: 202 con la solicitud cuando agrega permisos de riesgo alto (cuatro ojos)")
    void crearPendiente() throws Exception {
        when(gobierno.crear(any(), any())).thenReturn(new Resultado("PENDIENTE_DE_APROBACION", null, SOLICITUD));

        mvc.perform(crear(Sesiones.como("INTERNAL_IDENTITY_ADMIN", "ROLES_INTERNOS_GESTIONAR")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_DE_APROBACION"))
                .andExpect(jsonPath("$.solicitudId").value(SOLICITUD.toString()));
    }

    @Test
    @DisplayName("crear: el par incompatible llega como 422 con su codigo y el par")
    void crearConParIncompatible() throws Exception {
        when(gobierno.crear(any(), any()))
                .thenThrow(new ErrorDeNegocio(
                        ReglasDeRolPersonalizado.PAR_INCOMPATIBLE,
                        "Nadie autoriza y ejecuta la misma operacion.",
                        java.util.Map.of("parIncompatible", "ENTREGA_AUTORIZAR con ENTREGA_EJECUTAR")));

        mvc.perform(crear(Sesiones.como("INTERNAL_IDENTITY_ADMIN", "ROLES_INTERNOS_GESTIONAR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU135-05"));
    }

    @Test
    @DisplayName("editar un rol de sistema: 422 AP-CU135-06")
    void editarRolDeSistema() throws Exception {
        when(gobierno.editar(eq(ROL), any(), any()))
                .thenThrow(new ErrorDeNegocio(
                        ReglasDeRolPersonalizado.ROL_DE_SISTEMA, "Los roles del sistema no se editan ni se retiran."));

        mvc.perform(editar(Sesiones.como("INTERNAL_IDENTITY_ADMIN", "ROLES_INTERNOS_GESTIONAR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU135-06"));
    }

    @Test
    @DisplayName("aprobar y rechazar: 200; quien propuso no resuelve (422 AP-CU135-12)")
    void resolverSolicitud() throws Exception {
        when(gobierno.aprobar(eq(SOLICITUD), any(), any(), any()))
                .thenReturn(new Resultado("APLICADO", ROL, SOLICITUD));
        when(gobierno.rechazar(eq(SOLICITUD), any(), any(), any()))
                .thenThrow(new ErrorDeNegocio(
                        ReglasDeRolPersonalizado.APROBADOR_ES_EL_PROPONENTE, "Lo tiene que resolver otra persona."));
        RequestPostProcessor gestor = Sesiones.como("INTERNAL_IDENTITY_ADMIN", "ROLES_INTERNOS_GESTIONAR");

        mvc.perform(json(post("/roles/solicitudes/{id}/aprobacion", SOLICITUD), gestor, CUERPO_MOTIVO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APLICADO"));
        mvc.perform(json(post("/roles/solicitudes/{id}/rechazo", SOLICITUD), gestor, CUERPO_MOTIVO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU135-12"));
    }

    @Test
    @DisplayName(
            "autorizacion negativa: sin token 401; sin permiso, con solo lectura o de otro dominio 403; el caso de uso"
                    + " no se toca")
    void matrizDeAutorizacionNegativa() throws Exception {
        mvc.perform(crear(null)).andExpect(status().isUnauthorized());
        mvc.perform(editar(null)).andExpect(status().isUnauthorized());
        mvc.perform(retirar(null)).andExpect(status().isUnauthorized());
        mvc.perform(get("/roles/solicitudes")).andExpect(status().isUnauthorized());

        RequestPostProcessor soloLectura = Sesiones.como("AUDITOR_INTERNO", "ROLES_INTERNOS_LEER");
        RequestPostProcessor otroDominio = Sesiones.como("TESORERIA", "TESORERIA_OPERAR");
        RequestPostProcessor accesosSolamente = Sesiones.como("ADMIN_PLATAFORMA", "ACCESOS_ADMINISTRAR");
        for (RequestPostProcessor sesion : new RequestPostProcessor[] {soloLectura, otroDominio, accesosSolamente}) {
            mvc.perform(crear(sesion)).andExpect(status().isForbidden());
            mvc.perform(editar(sesion)).andExpect(status().isForbidden());
            mvc.perform(retirar(sesion)).andExpect(status().isForbidden());
            mvc.perform(json(post("/roles/solicitudes/{id}/aprobacion", SOLICITUD), sesion, CUERPO_MOTIVO))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/roles/solicitudes").with(otroDominio)).andExpect(status().isForbidden());
        verifyNoInteractions(gobierno);
    }

    @Test
    @DisplayName("listar solicitudes: alcanza el permiso de lectura")
    void listarConLectura() throws Exception {
        when(gobierno.solicitudesPendientes(any())).thenReturn(java.util.List.of());

        mvc.perform(get("/roles/solicitudes").with(Sesiones.como("AUDITOR_INTERNO", "ROLES_INTERNOS_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("entrada invalida: un campo no declarado o un codigo en minusculas es 400, nunca 500")
    void entradaInvalida() throws Exception {
        RequestPostProcessor gestor = Sesiones.como("INTERNAL_IDENTITY_ADMIN", "ROLES_INTERNOS_GESTIONAR");
        String conCampoDeMas = CUERPO_CREAR.replace("\"motivo\"", "\"esSistema\":true,\"motivo\"");
        String enMinusculas = CUERPO_CREAR.replace("ANALISTA_DE_ZONA", "analista");

        mvc.perform(json(post("/roles"), gestor, conCampoDeMas)).andExpect(status().isBadRequest());
        mvc.perform(json(post("/roles"), gestor, enMinusculas)).andExpect(status().isBadRequest());
        verifyNoInteractions(gobierno);
    }

    private static MockHttpServletRequestBuilder crear(RequestPostProcessor sesion) {
        return json(post("/roles"), sesion, CUERPO_CREAR);
    }

    private static MockHttpServletRequestBuilder editar(RequestPostProcessor sesion) {
        return json(patch("/roles/{id}", ROL), sesion, CUERPO_EDITAR);
    }

    private static MockHttpServletRequestBuilder retirar(RequestPostProcessor sesion) {
        return json(post("/roles/{id}/retiro", ROL), sesion, CUERPO_MOTIVO);
    }

    private static MockHttpServletRequestBuilder json(
            MockHttpServletRequestBuilder pedido, RequestPostProcessor sesion, String cuerpo) {
        pedido.header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
        return sesion == null ? pedido : pedido.with(sesion);
    }
}
