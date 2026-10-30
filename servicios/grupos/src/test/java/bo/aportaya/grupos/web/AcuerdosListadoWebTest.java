package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU63Acordar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.ResumenDeAcuerdo;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-63 · listar los acuerdos de un grupo: lo usa quien pidio traspasar su cupo (CU-64) para
 * encontrar la votacion de ese cupo. Solo lo ven los participantes del grupo.
 */
@PruebaWeb(
        value = AcuerdosController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
class AcuerdosListadoWebTest {

    private static final UUID GRUPO = UUID.fromString("c2100000-0000-4000-8000-000000000001");
    private static final UUID ACUERDO = UUID.fromString("c2100000-0000-4000-8000-000000000002");
    private static final UUID CUPO = UUID.fromString("c2100000-0000-4000-8000-000000000003");
    private static final UUID PARTICIPANTE = UUID.fromString("c2100000-0000-4000-8000-000000000004");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU63Acordar cu63;

    @MockitoBean
    private Consultas consultas;

    @Test
    @DisplayName("un participante ve la votacion de su cupo, con el tipo del contrato (TRASPASO_CUPO)")
    void participanteVeLosAcuerdos() throws Exception {
        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.of(PARTICIPANTE));
        when(cu63.delGrupo(
                        eq(GRUPO),
                        eq(Optional.of("TRASPASO_CUPO")),
                        eq(Optional.of("APROBADO")),
                        eq(Optional.of(CUPO)),
                        any()))
                .thenReturn(List.of(new ResumenDeAcuerdo(
                        ACUERDO,
                        "ADMISION_REEMPLAZO",
                        "APROBADO",
                        "Traspaso del cupo 2 a Ana",
                        Optional.of(CUPO),
                        OffsetDateTime.of(2026, 11, 5, 12, 0, 0, 0, ZoneOffset.UTC))));

        mvc.perform(get("/acuerdos")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .param("grupoId", GRUPO.toString())
                        .param("tipo", "TRASPASO_CUPO")
                        .param("estado", "APROBADO")
                        .param("referenciaAfectadaId", CUPO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].acuerdoId").value(ACUERDO.toString()))
                .andExpect(jsonPath("$[0].tipo").value("TRASPASO_CUPO"))
                .andExpect(jsonPath("$[0].estado").value("APROBADO"))
                .andExpect(jsonPath("$[0].referenciaAfectadaId").value(CUPO.toString()));
    }

    @Test
    @DisplayName("quien no participa del grupo no lista sus votaciones (AP-CU63-01)")
    void deAfueraNoLista() throws Exception {
        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.empty());

        mvc.perform(get("/acuerdos").with(Sesiones.como("PARTICIPANTE")).param("grupoId", GRUPO.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU63-01"));
        verify(consultas).participanteDe(eq(GRUPO), any(), any());
        verifyNoInteractions(cu63);
    }

    @Test
    @DisplayName("sin el permiso de participante, no hay listado")
    void sinPermiso() throws Exception {
        mvc.perform(get("/acuerdos").with(Sesiones.como("BILLETERA_VER")).param("grupoId", GRUPO.toString()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu63);
    }
}
