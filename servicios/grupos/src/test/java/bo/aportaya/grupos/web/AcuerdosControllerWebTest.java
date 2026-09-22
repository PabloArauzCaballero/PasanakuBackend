package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU63Acordar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.PoliticaDelGrupo;
import bo.aportaya.plataforma.pruebas.web.ContextoWebDePrueba;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-63 · las decisiones que el grupo toma votando.
 *
 * <p>Dos cosas se fijan acá y en ningún otro nivel.
 *
 * <p><b>El quórum sale de la política del grupo, no del cuerpo de la petición.</b> Si
 * quien propone pudiera declarar el quórum, cualquiera aprobaría una expulsión con su
 * propio voto.
 *
 * <p><b>El plazo de votación se calcula con el reloj INYECTADO.</b> Con
 * {@code Instant.now()} el vencimiento cambiaría con la máquina que atiende la
 * petición, y una votación es un plazo con consecuencia.
 */
@PruebaWeb(
        value = AcuerdosController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
class AcuerdosControllerWebTest {

    private static final UUID GRUPO = UUID.fromString("c2000000-0000-4000-8000-000000000001");
    private static final UUID ACUERDO = UUID.fromString("c2000000-0000-4000-8000-000000000002");
    private static final String CLAVE = "c2000000-0000-4000-8000-0000000000ff";

    private static final String PROPUESTA =
            """
            {"grupoId":"c2000000-0000-4000-8000-000000000001","tipo":"EXPULSION",
             "descripcion":"El cupo 4 lleva tres periodos sin aportar","diasVotacion":7}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU63Acordar cu63;

    @MockitoBean
    private Consultas consultas;

    private org.springframework.test.web.servlet.ResultActions proponer(String cuerpo) throws Exception {
        return mvc.perform(post("/acuerdos")
                .with(Sesiones.como("PARTICIPANTE"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Test
    @DisplayName("CU-63 · 201 y el acuerdo nace EN_VOTACION, no aprobado")
    void proponerAbreLaVotacion() throws Exception {
        when(consultas.politicaDelGrupo(eq(GRUPO), any()))
                .thenReturn(Optional.of(new PoliticaDelGrupo("ESTANDAR", 60, new BigDecimal("0.66"), 2)));
        when(cu63.proponer(any(), any())).thenReturn(ACUERDO);

        proponer(PROPUESTA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acuerdoId").value(ACUERDO.toString()))
                // Proponer no decide: si naciera aprobado, el que propone expulsaria solo.
                .andExpect(jsonPath("$.estado").value("EN_VOTACION"));
    }

    @Test
    @DisplayName("CU-63 · el QUÓRUM sale de la política del grupo, no de quien propone")
    void elQuorumNoLoDeclaraElProponente() throws Exception {
        when(consultas.politicaDelGrupo(eq(GRUPO), any()))
                .thenReturn(Optional.of(new PoliticaDelGrupo("ESTANDAR", 60, new BigDecimal("0.66"), 2)));
        when(cu63.proponer(any(), any())).thenReturn(ACUERDO);

        // Se manda un quorum en el cuerpo a proposito: el contrato no lo tiene, y si
        // alguna vez lo aceptara, esta prueba lo diria.
        proponer(PROPUESTA.replace("\"diasVotacion\":7", "\"diasVotacion\":7,\"quorum\":\"0.01\""))
                .andExpect(status().isCreated());

        var capturada = ArgumentCaptor.forClass(CU63Acordar.EntradaPropuesta.class);
        verify(cu63).proponer(capturada.capture(), any());
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().quorumRequerido())
                .as("el quorum tiene que venir de la politica del grupo")
                .isEqualByComparingTo(new BigDecimal("0.66"));
        // Y el proponente es quien tiene la sesion, no un campo del cuerpo.
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().propuestoPor())
                .isEqualTo(Sesiones.USUARIO);
    }

    @Test
    @DisplayName("CU-63 · el vencimiento se calcula con el reloj INYECTADO, no con la hora de la máquina")
    void elPlazoUsaElRelojInyectado() throws Exception {
        when(consultas.politicaDelGrupo(eq(GRUPO), any()))
                .thenReturn(Optional.of(new PoliticaDelGrupo("ESTANDAR", 60, new BigDecimal("0.66"), 2)));
        when(cu63.proponer(any(), any())).thenReturn(ACUERDO);

        proponer(PROPUESTA).andExpect(status().isCreated());

        var capturada = ArgumentCaptor.forClass(CU63Acordar.EntradaPropuesta.class);
        verify(cu63).proponer(capturada.capture(), any());

        // El reloj del corte esta parado en ContextoWebDePrueba.AHORA. Siete dias
        // despues es una fecha exacta y no «mas o menos ahora»: es la unica forma de
        // que la prueba no falle sola el dia que el borde caiga mal.
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().cierraEn())
                .isEqualTo(ContextoWebDePrueba.AHORA.atOffset(ZoneOffset.UTC).plusDays(7));
    }

    @Test
    @DisplayName("CU-63 · AP-CU63-01: un grupo que no existe no abre votación")
    void grupoInexistente() throws Exception {
        when(consultas.politicaDelGrupo(any(), any())).thenReturn(Optional.empty());

        proponer(PROPUESTA)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU63-01"));

        verifyNoInteractions(cu63);
    }

    @ParameterizedTest(name = "400 · tipo de acuerdo fuera del contrato: {0}")
    @ValueSource(strings = {"MULTA", "expulsion", "CAMBIO_DE_ORGANIZADOR"})
    @DisplayName("CU-63 · un tipo de acuerdo que el contrato no enumera no llega al caso de uso")
    void tipoFueraDelContrato(String tipo) throws Exception {
        proponer(PROPUESTA.replace("EXPULSION", tipo)).andExpect(status().isBadRequest());
        verifyNoInteractions(cu63);
        verifyNoInteractions(consultas);
    }

    @Test
    @DisplayName("CU-63 · 400: una propuesta sin descripción — nadie vota lo que no puede leer")
    void propuestaSinDescripcion() throws Exception {
        proponer(
                        """
                        {"grupoId":"c2000000-0000-4000-8000-000000000001","tipo":"EXPULSION","diasVotacion":7}
                        """)
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu63);
    }

    @Test
    @DisplayName("CU-63 · el voto se registra a nombre de QUIEN TIENE LA SESIÓN")
    void elVotoEsDeQuienTieneLaSesion() throws Exception {
        when(cu63.resolver(eq(ACUERDO), any())).thenReturn("EN_VOTACION");

        mvc.perform(post("/acuerdos/{id}/votos", ACUERDO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voto\":\"A_FAVOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_VOTACION"));

        // El votante sale del token. Si viniera del cuerpo, uno votaria por todos.
        verify(cu63).votar(eq(ACUERDO), eq(Sesiones.USUARIO), eq("A_FAVOR"), any());
    }

    @Test
    @DisplayName("CU-63 · 400: un voto que el contrato no enumera")
    void votoFueraDelContrato() throws Exception {
        mvc.perform(post("/acuerdos/{id}/votos", ACUERDO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voto\":\"MAS_O_MENOS\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu63);
    }
}
