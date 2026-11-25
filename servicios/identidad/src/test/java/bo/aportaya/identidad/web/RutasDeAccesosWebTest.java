package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.CU08DarDeBaja;
import bo.aportaya.identidad.aplicacion.CU08InformeDeAccesos;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos.ResultadoDeAsignacion;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos.ResultadoDeRevocacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
 * CU-08 por asignacion en la capa web ({@code RutasDeAccesos}, servidas por {@code UsuariosInternosController}) (GU-05, GU-08, GU-11, GU-14): el contrato, la matriz de autorizacion
 * negativa (sin token, sin permiso, rol de otro dominio) y que las reglas de gobierno lleguen como 422 con su
 * codigo. Los casos de uso van con doble: aca se prueba la traduccion, no la persistencia (esa va en
 * {@code CU08GobiernoTest} contra PostgreSQL).
 */
@PruebaWeb(UsuariosInternosController.class)
@MockitoBean(
        types = {
            bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos.class,
            bo.aportaya.identidad.aplicacion.ListarUsuariosInternos.class,
            bo.aportaya.identidad.aplicacion.ResolverNombresDeUsuariosInternos.class
        })
class RutasDeAccesosWebTest {

    private static final UUID OPERADOR = UUID.fromString("33333333-4444-4555-8666-777777777777");
    private static final UUID ROL = UUID.fromString("44444444-5555-4666-8777-888888888888");
    private static final UUID ASIGNACION = UUID.fromString("55555555-6666-4777-8888-999999999999");
    private static final String EVIDENCIA = "evidencia-step-up-sintetica-de-prueba";

    private static final String CUERPO_ASIGNAR =
            """
            {"rolId":"%s","ambito":"GLOBAL","justificacion":"Cobertura de vacaciones del titular","evidenciaMfa":"%s"}
            """
                    .formatted(ROL, EVIDENCIA);
    private static final String CUERPO_REVOCAR =
            """
            {"motivoRevocacion":"Cambio de funcion aprobado","cerrarSesiones":true,"evidenciaMfa":"%s"}
            """
                    .formatted(EVIDENCIA);
    private static final String CUERPO_BAJA =
            """
            {"motivo":"Fin de la relacion laboral","evidenciaMfa":"%s"}
            """
                    .formatted(EVIDENCIA);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GobiernoDeAccesos gobierno;

    @MockitoBean
    private CU08DarDeBaja baja;

    @MockitoBean
    private CU08InformeDeAccesos informe;

    @Test
    @DisplayName("asignar: 201 con ACCESOS_ADMINISTRAR, permisos efectivos y aviso de MFA faltante")
    void asignarCrea() throws Exception {
        when(gobierno.asignar(any(), any())).thenReturn(resultado(false));

        mvc.perform(asignar(Sesiones.como("ADMIN_PLATAFORMA", "ACCESOS_ADMINISTRAR"), CUERPO_ASIGNAR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.asignacionId").value(ASIGNACION.toString()))
                .andExpect(jsonPath("$.rolCodigo").value("SOPORTE"))
                .andExpect(jsonPath("$.ambito").value("GLOBAL"))
                .andExpect(jsonPath("$.permisosEfectivos[0]").value("RECLAMO_ATENDER"))
                .andExpect(jsonPath("$.requiereMfa").value(true))
                .andExpect(jsonPath("$.yaExistia").value(false));
    }

    @Test
    @DisplayName("asignar: 200 y la misma asignacion si ya existia una viva igual (idempotente)")
    void asignarIdempotente() throws Exception {
        when(gobierno.asignar(any(), any())).thenReturn(resultado(true));

        mvc.perform(asignar(Sesiones.como("ADMIN_PLATAFORMA", "ACCESOS_ADMINISTRAR"), CUERPO_ASIGNAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asignacionId").value(ASIGNACION.toString()))
                .andExpect(jsonPath("$.yaExistia").value(true));
    }

    @Test
    @DisplayName("asignar: INTERNAL_IDENTITY_ADMIN entra por USUARIOS_INTERNOS_GESTIONAR (P-2b)")
    void asignarConGestionDePersonal() throws Exception {
        when(gobierno.asignar(any(), any())).thenReturn(resultado(false));

        mvc.perform(asignar(Sesiones.como("INTERNAL_IDENTITY_ADMIN", "USUARIOS_INTERNOS_GESTIONAR"), CUERPO_ASIGNAR))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("asignar: 401 sin token y 403 sin permiso de gobierno, sin tocar el caso de uso")
    void asignarMatrizNegativa() throws Exception {
        mvc.perform(asignar(null, CUERPO_ASIGNAR)).andExpect(status().isUnauthorized());
        for (RequestPostProcessor sinPermiso : List.of(
                Sesiones.como("PARTICIPANTE", "BILLETERA_VER"),
                Sesiones.como("SOPORTE", "USUARIOS_INTERNOS_LEER"),
                Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"),
                Sesiones.sinPermisos())) {
            mvc.perform(asignar(sinPermiso, CUERPO_ASIGNAR)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(gobierno, baja, informe);
    }

    @Test
    @DisplayName("asignar: 400 si el cuerpo trae un campo no declarado o una justificacion corta")
    void asignarCuerpoInvalido() throws Exception {
        String conCampoDeMas = CUERPO_ASIGNAR.replace("\"ambito\"", "\"otorgadaPor\":\"%s\",\"ambito\"".formatted(ROL));
        String justificacionCorta = CUERPO_ASIGNAR.replace("Cobertura de vacaciones del titular", "corta");
        String sinEvidencia = CUERPO_ASIGNAR.replace(",\"evidenciaMfa\":\"" + EVIDENCIA + "\"", "");
        for (String cuerpo : List.of(conCampoDeMas, justificacionCorta, sinEvidencia)) {
            mvc.perform(asignar(Sesiones.como("ADMIN_PLATAFORMA", "ACCESOS_ADMINISTRAR"), cuerpo))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(gobierno);
    }

    @Test
    @DisplayName("asignar: 400 sin Idempotency-Key")
    void asignarSinClave() throws Exception {
        mvc.perform(post("/usuarios-internos/{id}/asignaciones", OPERADOR)
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "ACCESOS_ADMINISTRAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_ASIGNAR))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(gobierno);
    }

    @Test
    @DisplayName("asignar: 422 AP-CU08-09 sin step-up y 422 AP-CU08-04 sobre si mismo")
    void asignarReglasDeGobierno() throws Exception {
        when(gobierno.asignar(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(8, 9), "Confirma con tu segundo factor."))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(8, 4), "Nadie gobierna su propio acceso."));

        mvc.perform(asignar(Sesiones.como("ADMIN_PLATAFORMA", "ACCESOS_ADMINISTRAR"), CUERPO_ASIGNAR))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU08-09"));
        mvc.perform(asignar(Sesiones.como("ADMIN_PLATAFORMA", "ACCESOS_ADMINISTRAR"), CUERPO_ASIGNAR))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU08-04"));
    }

    @Test
    @DisplayName("revocar: 200 con sesiones cerradas, y yaEstabaRevocada en el reintento")
    void revocar() throws Exception {
        OffsetDateTime revocada = OffsetDateTime.parse("2026-11-24T12:00:00Z");
        when(gobierno.revocar(eq(ASIGNACION), any(), anyBoolean(), any(), any()))
                .thenReturn(new ResultadoDeRevocacion(ASIGNACION, OPERADOR, revocada, 2, false))
                .thenReturn(new ResultadoDeRevocacion(ASIGNACION, OPERADOR, revocada, 0, true));

        mvc.perform(revocar(Sesiones.como("ADMIN_PLATAFORMA", "ACCESOS_ADMINISTRAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesionesCerradas").value(2))
                .andExpect(jsonPath("$.yaEstabaRevocada").value(false));
        mvc.perform(revocar(Sesiones.como("ADMIN_PLATAFORMA", "ACCESOS_ADMINISTRAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesionesCerradas").value(0))
                .andExpect(jsonPath("$.yaEstabaRevocada").value(true));
        verify(gobierno, times(2))
                .revocar(eq(ASIGNACION), eq("Cambio de funcion aprobado"), eq(true), eq(EVIDENCIA), any());
    }

    @Test
    @DisplayName("revocar: 422 AP-CU08-06 si dejaria la plataforma sin administrador de accesos")
    void revocarUltimoAdministrador() throws Exception {
        when(gobierno.revocar(any(), any(), anyBoolean(), any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(8, 6), "Sin administrador."));

        mvc.perform(revocar(Sesiones.como("ADMIN_PLATAFORMA", "ACCESOS_ADMINISTRAR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU08-06"));
    }

    @Test
    @DisplayName("revocar: 401 sin token y 403 sin permiso")
    void revocarMatrizNegativa() throws Exception {
        mvc.perform(revocar(null)).andExpect(status().isUnauthorized());
        mvc.perform(revocar(Sesiones.como("SOPORTE", "USUARIOS_INTERNOS_LEER"))).andExpect(status().isForbidden());
        verifyNoInteractions(gobierno);
    }

    @Test
    @DisplayName("baja: 200 con asignaciones revocadas y sesiones cerradas; 403 sin permiso; 422 sobre si mismo")
    void baja() throws Exception {
        when(baja.ejecutar(eq(OPERADOR), any(), any(), any()))
                .thenReturn(new CU08DarDeBaja.ResultadoDeBaja(OPERADOR, "BAJA_LABORAL", 3, 1, false))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(8, 4), "Nadie gobierna su propio acceso."));

        mvc.perform(darDeBaja(Sesiones.como("ADMIN_PLATAFORMA", "ACCESOS_ADMINISTRAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BAJA_LABORAL"))
                .andExpect(jsonPath("$.asignacionesRevocadas").value(3))
                .andExpect(jsonPath("$.yaEstabaDeBaja").value(false));
        mvc.perform(darDeBaja(Sesiones.como("ADMIN_PLATAFORMA", "ACCESOS_ADMINISTRAR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU08-04"));
        mvc.perform(darDeBaja(null)).andExpect(status().isUnauthorized());
        mvc.perform(darDeBaja(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))).andExpect(status().isForbidden());
    }

    private static ResultadoDeAsignacion resultado(boolean yaExistia) {
        return new ResultadoDeAsignacion(
                ASIGNACION,
                OPERADOR,
                "SOPORTE",
                "GLOBAL",
                Optional.empty(),
                Optional.empty(),
                Set.of("RECLAMO_ATENDER"),
                true,
                yaExistia);
    }

    private static MockHttpServletRequestBuilder conSesion(
            MockHttpServletRequestBuilder pedido, RequestPostProcessor s) {
        pedido.header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON);
        return s == null ? pedido : pedido.with(s);
    }

    private static MockHttpServletRequestBuilder asignar(RequestPostProcessor sesion, String cuerpo) {
        return conSesion(post("/usuarios-internos/{id}/asignaciones", OPERADOR), sesion)
                .content(cuerpo);
    }

    private static MockHttpServletRequestBuilder revocar(RequestPostProcessor sesion) {
        return conSesion(post("/usuarios-internos/asignaciones/{asignacionId}/revocacion", ASIGNACION), sesion)
                .content(CUERPO_REVOCAR);
    }

    private static MockHttpServletRequestBuilder darDeBaja(RequestPostProcessor sesion) {
        return conSesion(post("/usuarios-internos/{id}/baja", OPERADOR), sesion).content(CUERPO_BAJA);
    }
}
