package bo.aportaya.grupos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU63Acordar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.PoliticaDelGrupo;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * CU-63 · la votacion de un traspaso (CU-64) se hace sobre un cupo Y una persona: sin quien entra,
 * una votacion aprobada le serviria al titular para meter a cualquiera.
 */
@PruebaWeb(
        value = AcuerdosController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
class AcuerdoDeTraspasoWebTest {

    private static final UUID GRUPO = UUID.fromString("c2200000-0000-4000-8000-000000000001");
    private static final UUID ACUERDO = UUID.fromString("c2200000-0000-4000-8000-000000000002");
    private static final UUID CUPO = UUID.fromString("c2200000-0000-4000-8000-000000000003");
    private static final UUID USUARIO_ENTRANTE = UUID.fromString("c2200000-0000-4000-8000-000000000004");
    private static final UUID PARTICIPANTE_ENTRANTE = UUID.fromString("c2200000-0000-4000-8000-000000000005");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU63Acordar cu63;

    @MockitoBean
    private Consultas consultas;

    private static final UUID PROPONENTE = UUID.fromString("c2200000-0000-4000-8000-000000000006");

    @BeforeEach
    void politica() {
        when(consultas.politicaDelGrupo(eq(GRUPO), any()))
                .thenReturn(Optional.of(new PoliticaDelGrupo("ESTANDAR", 60, new BigDecimal("0.66"), 2)));
        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.of(PROPONENTE));
    }

    @Test
    @DisplayName("quien no participa del grupo no averigua quien es del grupo (AP-CU63-01, sin mirar al entrante)")
    void deAfueraNoAveriguaIntegrantes() throws Exception {
        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.empty());

        proponer(",\"referenciaAfectadaId\":\"" + CUPO + "\",\"usuarioEntranteId\":\"" + USUARIO_ENTRANTE + "\"")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU63-01"));
        verify(consultas, never()).participanteDeOtro(any(), any(), any());
        verifyNoInteractions(cu63);
    }

    private ResultActions proponer(String extra) throws Exception {
        return mvc.perform(post("/acuerdos")
                .with(Sesiones.como("PARTICIPANTE"))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"grupoId\":\"" + GRUPO + "\",\"tipo\":\"TRASPASO_CUPO\","
                        + "\"descripcion\":\"Traspaso del cupo 1 a Ana porque me mudo\"" + extra + "}"));
    }

    @Test
    @DisplayName("el traspaso se vota sobre el cupo y la persona que entra, que queda en el acuerdo")
    void guardaQuienEntra() throws Exception {
        when(consultas.participanteDeOtro(eq(GRUPO), eq(USUARIO_ENTRANTE), any()))
                .thenReturn(Optional.of(PARTICIPANTE_ENTRANTE));
        when(cu63.proponer(any(), any())).thenReturn(ACUERDO);

        proponer(",\"referenciaAfectadaId\":\"" + CUPO + "\",\"usuarioEntranteId\":\"" + USUARIO_ENTRANTE + "\"")
                .andExpect(status().isCreated());

        var capturada = ArgumentCaptor.forClass(CU63Acordar.EntradaPropuesta.class);
        verify(cu63).proponer(capturada.capture(), any());
        assertThat(capturada.getValue().entrante()).contains(PARTICIPANTE_ENTRANTE);
        assertThat(capturada.getValue().afectado()).contains(CUPO);
    }

    @Test
    @DisplayName("sin quien entra no se abre la votacion del traspaso (AP-CU63-06)")
    void sinEntrante() throws Exception {
        proponer(",\"referenciaAfectadaId\":\"" + CUPO + "\"")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU63-06"));
        verifyNoInteractions(cu63);
    }

    @Test
    @DisplayName("sin el cupo no se abre la votacion del traspaso (AP-CU63-06)")
    void sinCupo() throws Exception {
        proponer(",\"usuarioEntranteId\":\"" + USUARIO_ENTRANTE + "\"")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU63-06"));
        verifyNoInteractions(cu63);
    }

    @Test
    @DisplayName("quien entra tiene que participar del grupo (AP-CU63-06)")
    void entranteDeAfuera() throws Exception {
        when(consultas.participanteDeOtro(eq(GRUPO), eq(USUARIO_ENTRANTE), any()))
                .thenReturn(Optional.empty());

        proponer(",\"referenciaAfectadaId\":\"" + CUPO + "\",\"usuarioEntranteId\":\"" + USUARIO_ENTRANTE + "\"")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU63-06"));
        verifyNoInteractions(cu63);
    }
}
