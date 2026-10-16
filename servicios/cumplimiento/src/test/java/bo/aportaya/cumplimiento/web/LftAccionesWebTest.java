package bo.aportaya.cumplimiento.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.cumplimiento.aplicacion.CU02ElevarDiligencia;
import bo.aportaya.cumplimiento.aplicacion.CU03DeclararPep;
import bo.aportaya.cumplimiento.aplicacion.CU05AceptarContrato;
import bo.aportaya.cumplimiento.aplicacion.CU05ConsultarContratosVigentes;
import bo.aportaya.cumplimiento.aplicacion.CU44DecidirComoRevisor;
import bo.aportaya.cumplimiento.aplicacion.CU44InvestigarYReportar;
import bo.aportaya.cumplimiento.aplicacion.CU54RegistrarRiesgoOperativo;
import bo.aportaya.cumplimiento.aplicacion.CU55GestionarIncidente;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejasLft;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Backoffice carril A · H5.S2 · el contrato HTTP de abrir y decidir un caso LFT.
 *
 * <p>Lo que solo se ve aca: que el cuerpo de la decision NO puede declarar quien revisa ni quien
 * analizo (el contrato rechaza esos campos: el revisor es la sesion), que sin {@code CUMPLIMIENTO_CASOS}
 * un analista no decide, y que los rechazos de negocio llegan con su codigo.
 */
@PruebaWeb(CumplimientoController.class)
class LftAccionesWebTest {

    private static final UUID CASO = UUID.fromString("c1000000-0000-4000-8000-0000000000b1");
    private static final UUID ALERTA = UUID.fromString("c1000000-0000-4000-8000-0000000000b2");
    private static final UUID ANALISTA = UUID.fromString("c1000000-0000-4000-8000-0000000000b3");
    private static final String CLAVE = "c1000000-0000-4000-8000-0000000000ff";
    private static final String CONCLUSION = "Las operaciones responden al giro declarado del titular.";
    private static final OffsetDateTime PLAZO = OffsetDateTime.of(2026, 11, 15, 0, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU02ElevarDiligencia cu02;

    @MockitoBean
    private CU03DeclararPep cu03;

    @MockitoBean
    private CU05AceptarContrato cu05;

    @MockitoBean
    private CU05ConsultarContratosVigentes contratosVigentes;

    @MockitoBean
    private CU54RegistrarRiesgoOperativo cu54;

    @MockitoBean
    private CU55GestionarIncidente cu55;

    @MockitoBean
    private ConsultarBandejasLft bandejas;

    @MockitoBean
    private CU44InvestigarYReportar cu44;

    @MockitoBean
    private CU44DecidirComoRevisor decision;

    private static String cuerpoDecision(String extra) {
        return "{\"decision\":\"DESCARTAR\",\"conclusion\":\"" + CONCLUSION + "\"" + extra + "}";
    }

    @Test
    @DisplayName("201: el oficial abre un caso con alertas y analista, y recibe el plazo guardado")
    void abreCaso() throws Exception {
        when(cu44.abrirCaso(any(), any()))
                .thenReturn(new CU44InvestigarYReportar.SalidaCaso(CASO, "ABIERTO", PLAZO, null));

        mvc.perform(post("/cumplimiento/casos")
                        .with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "CUMPLIMIENTO_CASOS"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alertaIds\":[\"" + ALERTA + "\"],\"analistaId\":\"" + ANALISTA
                                + "\",\"resumen\":\"Fraccionamiento detectado\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.casoId").value(CASO.toString()))
                .andExpect(jsonPath("$.estado").value("ABIERTO"));
        verify(cu44)
                .abrirCaso(
                        eq(new CU44InvestigarYReportar.EntradaCaso(
                                List.of(ALERTA), ANALISTA, "Fraccionamiento detectado")),
                        any());
    }

    @Test
    @DisplayName("200: la decision llega al caso de uso sin revisor en el cuerpo (el revisor sale de la sesion)")
    void decide() throws Exception {
        UUID oficial = UUID.fromString("c1000000-0000-4000-8000-0000000000b4");
        when(decision.decidir(any(), any()))
                .thenReturn(new CU44DecidirComoRevisor.Decision(CASO, "DESCARTAR", oficial, 2, false, null));

        mvc.perform(post("/cumplimiento/casos/{id}/decision", CASO)
                        .with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "CUMPLIMIENTO_CASOS"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDecision("")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisadoPor").value(oficial.toString()))
                .andExpect(jsonPath("$.alertasCerradas").value(2));
        verify(decision).decidir(eq(new CU44DecidirComoRevisor.Entrada(CASO, "DESCARTAR", CONCLUSION, null)), any());
    }

    @Test
    @DisplayName(
            "400: el cuerpo no puede declarar revisadoPor ni analistaId: suplantar al revisor no llega al caso de uso")
    void noSeDeclaraElRevisor() throws Exception {
        mvc.perform(post("/cumplimiento/casos/{id}/decision", CASO)
                        .with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "CUMPLIMIENTO_CASOS"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDecision(",\"revisadoPor\":\"" + ANALISTA + "\"")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/cumplimiento/casos/{id}/decision", CASO)
                        .with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "CUMPLIMIENTO_CASOS"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDecision(",\"analistaId\":\"" + ANALISTA + "\"")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(decision);
    }

    @Test
    @DisplayName("400: una conclusion de menos de 20 caracteres la corta el contrato (R-UIF-07)")
    void conclusionCorta() throws Exception {
        mvc.perform(post("/cumplimiento/casos/{id}/decision", CASO)
                        .with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "CUMPLIMIENTO_CASOS"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"DESCARTAR\",\"conclusion\":\"revisado\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(decision);
    }

    @Test
    @DisplayName("403: el analista (solo CUMPLIMIENTO_ALERTAS) no abre ni decide casos")
    void analistaNoDecide() throws Exception {
        mvc.perform(post("/cumplimiento/casos/{id}/decision", CASO)
                        .with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "CUMPLIMIENTO_ALERTAS"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDecision("")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/cumplimiento/casos")
                        .with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "CUMPLIMIENTO_ALERTAS"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alertaIds\":[\"" + ALERTA + "\"],\"analistaId\":\"" + ANALISTA
                                + "\",\"resumen\":\"x\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(decision, cu44);
    }

    @Test
    @DisplayName("422: otra decision sobre un caso ya decidido sale con AP-CU44-07")
    void yaDecidido() throws Exception {
        when(decision.decidir(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(44, 7), "Ese caso ya tiene su decision."));

        mvc.perform(post("/cumplimiento/casos/{id}/decision", CASO)
                        .with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "CUMPLIMIENTO_CASOS"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDecision("")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU44-07"));
    }
}
