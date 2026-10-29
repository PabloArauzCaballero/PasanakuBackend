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
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.ConsultarGruposDeBackoffice;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Backoffice · consulta de grupos (2026-10-29): el borde HTTP y su matriz de autorización. Datos sintéticos. */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
            "aportaya.grupos.admision.humana-obligatoria=false",
        })
@Import(GruposDeBackoffice.class)
@MockitoBean(types = bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo.class)
class GruposDeBackofficeWebTest {

    private static final UUID GRUPO = UUID.fromString("c9900000-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarGruposDeBackoffice consultasDeBackoffice;

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

    private static ConsultarGruposDeBackoffice.Grupo grupo() {
        return new ConsultarGruposDeBackoffice.Grupo(
                GRUPO,
                "PASA-0001",
                "Pasanaku del barrio",
                "EN_CURSO",
                new BigDecimal("500.00"),
                "BOB",
                "MENSUAL",
                10,
                7,
                LocalDate.parse("2026-11-01"));
    }

    @Test
    @DisplayName("200: el administrador de la plataforma ve los grupos, con el aporte como decimal exacto en texto")
    void lista() throws Exception {
        when(consultasDeBackoffice.listar(any(), any(), any())).thenReturn(List.of(grupo()));

        mvc.perform(get("/grupos/backoffice").with(Sesiones.como("ADMIN_PLATAFORMA", "ADMIN_PLATAFORMA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].grupoId").value(GRUPO.toString()))
                .andExpect(jsonPath("$[0].codigo").value("PASA-0001"))
                .andExpect(jsonPath("$[0].montoAporte").value("500.00"))
                .andExpect(jsonPath("$[0].moneda").value("BOB"))
                .andExpect(jsonPath("$[0].cuposOcupados").value(7))
                .andExpect(jsonPath("$[0].fechaInicio").value("2026-11-01"))
                // Datos del grupo, nunca de una persona: el contrato no tiene organizador ni contacto.
                .andExpect(jsonPath("$[0].organizadorId").doesNotExist());
    }

    @Test
    @DisplayName("200: quien audita (AUDITORIA_LEER) también lee; el filtro y el límite llegan tal cual")
    void auditorYFiltro() throws Exception {
        when(consultasDeBackoffice.listar(any(), any(), any())).thenReturn(List.of());

        mvc.perform(get("/grupos/backoffice")
                        .param("estado", "EN_CURSO")
                        .param("limite", "50")
                        .with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk());
        verify(consultasDeBackoffice).listar(eq("EN_CURSO"), eq(50), any());

        mvc.perform(get("/grupos/backoffice").with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk());
        // Sin parametros el contrato aplica su `default: 200` al limite; el estado llega vacio.
        verify(consultasDeBackoffice).listar(isNull(), eq(200), any());
    }

    @Test
    @DisplayName("200: el detalle trae cada cupo con alias y estado, sin datos de contacto")
    void detalle() throws Exception {
        when(consultasDeBackoffice.detalle(eq(GRUPO), any()))
                .thenReturn(new ConsultarGruposDeBackoffice.Detalle(
                        grupo(),
                        List.of(
                                new ConsultarGruposDeBackoffice.Cupo(1, "OCUPADO", "Ana", "ACTIVO", true),
                                new ConsultarGruposDeBackoffice.Cupo(2, "LIBRE", null, null, false))));

        mvc.perform(get("/grupos/backoffice/{id}", GRUPO).with(Sesiones.como("ADMIN_PLATAFORMA", "ADMIN_PLATAFORMA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupo.codigo").value("PASA-0001"))
                .andExpect(jsonPath("$.cupos.length()").value(2))
                .andExpect(jsonPath("$.cupos[0].alias").value("Ana"))
                .andExpect(jsonPath("$.cupos[0].esOrganizador").value(true))
                .andExpect(jsonPath("$.cupos[1].estado").value("LIBRE"))
                .andExpect(jsonPath("$.cupos[0].usuarioId").doesNotExist())
                .andExpect(jsonPath("$.cupos[0].telefono").doesNotExist());
    }

    @Test
    @DisplayName("422: un grupo que no existe responde con el código del contrato, no un 500")
    void inexistente() throws Exception {
        when(consultasDeBackoffice.detalle(eq(GRUPO), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(64, 1), "Ese grupo no existe."));

        mvc.perform(get("/grupos/backoffice/{id}", GRUPO).with(Sesiones.como("ADMIN_PLATAFORMA", "ADMIN_PLATAFORMA")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU64-01"));
    }

    @Test
    @DisplayName("403: un participante no lista ni abre grupos ajenos desde el backoffice")
    void participanteNoEntra() throws Exception {
        mvc.perform(get("/grupos/backoffice").with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/grupos/backoffice/{id}", GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(consultasDeBackoffice);
    }

    @Test
    @DisplayName("403: administrar UN grupo no da la consulta de TODOS")
    void organizadorNoEntra() throws Exception {
        mvc.perform(get("/grupos/backoffice").with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(consultasDeBackoffice);
    }

    @Test
    @DisplayName("403: soporte, con permisos de ver solicitudes, tampoco abre la consulta de grupos")
    void soporteNoEntra() throws Exception {
        mvc.perform(get("/grupos/backoffice").with(Sesiones.como("SOPORTE", "SOLICITUD_INGRESO_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(consultasDeBackoffice);
    }

    @Test
    @DisplayName("401: sin sesión")
    void sinSesion() throws Exception {
        mvc.perform(get("/grupos/backoffice")).andExpect(status().isUnauthorized());
        mvc.perform(get("/grupos/backoffice/{id}", GRUPO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(consultasDeBackoffice);
    }

    @Test
    @DisplayName("400: un límite fuera de 1..200 lo rechaza el contrato")
    void limiteFueraDeRango() throws Exception {
        for (var limite : new String[] {"0", "201"}) {
            mvc.perform(get("/grupos/backoffice")
                            .param("limite", limite)
                            .with(Sesiones.como("ADMIN_PLATAFORMA", "ADMIN_PLATAFORMA")))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(consultasDeBackoffice);
    }
}
