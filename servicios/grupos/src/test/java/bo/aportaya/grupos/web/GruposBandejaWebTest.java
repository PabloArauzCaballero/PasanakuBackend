package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso.FilaDeBandeja;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * H4.B.M1 · la bandeja de «Admisiones» del backoffice: {@code GET /grupos/solicitudes}. Lo que prueba es
 * el contrato HTTP (quien entra, que sale, que el filtro llega al caso de uso); la consulta contra
 * PostgreSQL —incluida la RLS— es de {@code BandejaDeAdmisionesRepositorioTest}.
 */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
            "aportaya.grupos.admision.humana-obligatoria=false",
        })
@MockitoBean(types = bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo.class)
class GruposBandejaWebTest {

    private static final UUID SOLICITUD = UUID.fromString("c4800000-0000-4000-8000-000000000001");
    private static final UUID USUARIO = UUID.fromString("ab12cd34-0000-4000-8000-000000000002");
    private static final UUID GRUPO = UUID.fromString("c4800000-0000-4000-8000-000000000003");

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

    private static FilaDeBandeja fila(String estado, String nombreVisible) {
        return new FilaDeBandeja(
                SOLICITUD,
                USUARIO,
                GRUPO,
                "PASA-0001",
                2,
                "Quiero entrar",
                new BigDecimal("0.82"),
                estado,
                OffsetDateTime.parse("2026-10-20T12:00:00Z"),
                nombreVisible,
                true);
    }

    @Test
    @DisplayName(
            "200: el backoffice con SOLICITUD_INGRESO_VER ve las solicitudes de todos los grupos, con el estado de la pantalla")
    void lista() throws Exception {
        when(cu68Decision.bandeja(any(), any(), any()))
                .thenReturn(List.of(fila("ACEPTADA", "Ana Rojas"), fila("PENDIENTE", null)));

        mvc.perform(get("/grupos/solicitudes").with(Sesiones.como("SOPORTE", "SOLICITUD_INGRESO_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].solicitudId").value(SOLICITUD.toString()))
                .andExpect(jsonPath("$[0].grupoId").value(GRUPO.toString()))
                .andExpect(jsonPath("$[0].grupoCodigo").value("PASA-0001"))
                .andExpect(jsonPath("$[0].estado").value("ACEPTADA"))
                .andExpect(jsonPath("$[0].postulante").value("Ana Rojas"))
                .andExpect(jsonPath("$[0].puntaje").value("0.82"))
                .andExpect(jsonPath("$[0].porEnlace").value(true))
                // Sin nombre visible, `grupos` no puede leer `identidad`: el id corto identifica a la persona.
                .andExpect(jsonPath("$[1].postulante").value("Usuario ab12cd34"))
                // Sin telefono ni correo de nadie: el contrato ni siquiera tiene el campo.
                .andExpect(jsonPath("$[0].telefonoContacto").doesNotExist())
                .andExpect(jsonPath("$[0].correoContacto").doesNotExist());
    }

    @Test
    @DisplayName("200: el filtro y el limite llegan al caso de uso tal como los pidio el cliente")
    void filtro() throws Exception {
        when(cu68Decision.bandeja(any(), any(), any())).thenReturn(List.of());

        mvc.perform(get("/grupos/solicitudes")
                        .param("estado", "RECHAZADA")
                        .param("limite", "50")
                        .with(Sesiones.como("SOPORTE", "SOLICITUD_INGRESO_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(cu68Decision).bandeja(eq("RECHAZADA"), eq(50), any());
    }

    @Test
    @DisplayName("200: sin parametros el filtro va vacio y el limite es el del contrato (200, el tope)")
    void sinParametros() throws Exception {
        when(cu68Decision.bandeja(any(), any(), any())).thenReturn(List.of());

        mvc.perform(get("/grupos/solicitudes").with(Sesiones.como("SOPORTE", "SOLICITUD_INGRESO_VER")))
                .andExpect(status().isOk());

        // El contrato declara `limite` con default 200 y el generador lo aplica en la interfaz: el caso de uso
        // recibe 200, no null. Es el mismo tope que el caso de uso aplicaria ante un null.
        verify(cu68Decision).bandeja(isNull(), eq(200), any());
    }

    @Test
    @DisplayName("403: un participante, sin SOLICITUD_INGRESO_VER, no abre la bandeja del backoffice")
    void participanteNoEntra() throws Exception {
        mvc.perform(get("/grupos/solicitudes").with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu68Decision);
    }

    @Test
    @DisplayName("403: administrar UN grupo no da la bandeja de TODOS (otro permiso, mismo recurso)")
    void organizadorNoEntra() throws Exception {
        mvc.perform(get("/grupos/solicitudes").with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu68Decision);
    }

    @Test
    @DisplayName("401: sin sesión")
    void sinSesion() throws Exception {
        mvc.perform(get("/grupos/solicitudes")).andExpect(status().isUnauthorized());
        verifyNoInteractions(cu68Decision);
    }

    @Test
    @DisplayName("400: un limite fuera de 1..200 lo rechaza el contrato, no el caso de uso")
    void limiteFueraDeRango() throws Exception {
        for (var limite : new String[] {"0", "201"}) {
            mvc.perform(get("/grupos/solicitudes")
                            .param("limite", limite)
                            .with(Sesiones.como("SOPORTE", "SOLICITUD_INGRESO_VER")))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(cu68Decision);
    }
}
