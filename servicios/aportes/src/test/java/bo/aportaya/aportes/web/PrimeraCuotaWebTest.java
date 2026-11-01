package bo.aportaya.aportes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.ConsultarEstadoDelParticipante;
import bo.aportaya.aportes.aplicacion.ConsultarPrimeraCuota;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDelPeriodo;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDiario;
import bo.aportaya.aportes.aplicacion.GenerarObligacionesDelPeriodo;
import bo.aportaya.aportes.dominio.PrimeraCuotaDelGrupo;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** El contrato HTTP de {@code GET /aportes/grupos/{grupoId}/primera-cuota} (R-GRP-20), con su matriz de autorizacion. */
@PruebaWeb(AportesController.class)
class PrimeraCuotaWebTest {

    private static final UUID GRUPO = UUID.fromString("cccccccc-0000-4000-8000-000000000001");
    private static final UUID PERIODO = UUID.fromString("cccccccc-0000-4000-8000-000000000002");
    private static final UUID CUPO_PAGADO = UUID.fromString("cccccccc-0000-4000-8000-000000000003");
    private static final UUID CUPO_PENDIENTE = UUID.fromString("cccccccc-0000-4000-8000-000000000004");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarPrimeraCuota primeraCuota;

    // El resto del controlador: sin estos dobles no carga.
    @MockitoBean
    private CU21CobrarAporte cu21;

    @MockitoBean
    private ConsultarEstadoDelParticipante estados;

    @MockitoBean
    private ConsultarRecaudoDelPeriodo recaudos;

    @MockitoBean
    private GenerarObligacionesDelPeriodo generarObligaciones;

    @MockitoBean
    private bo.aportaya.aportes.aplicacion.HechosDeGrupos grupos;

    @MockitoBean
    private ConsultarRecaudoDiario recaudosDiarios;

    @Test
    @DisplayName("200: el organizador ve que cupos pagaron la cuota 1 y cuales faltan, solo identificadores")
    void organizador() throws Exception {
        when(primeraCuota.delGrupo(any(), any()))
                .thenReturn(
                        new PrimeraCuotaDelGrupo(Optional.of(PERIODO), List.of(CUPO_PAGADO), List.of(CUPO_PENDIENTE)));

        mvc.perform(get("/aportes/grupos/{id}/primera-cuota", GRUPO)
                        .with(Sesiones.como("ORGANIZADOR", "BILLETERA_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupoId").value(GRUPO.toString()))
                .andExpect(jsonPath("$.periodoId").value(PERIODO.toString()))
                .andExpect(jsonPath("$.cuposPagados[0]").value(CUPO_PAGADO.toString()))
                .andExpect(jsonPath("$.cuposPendientes[0]").value(CUPO_PENDIENTE.toString()));
    }

    @Test
    @DisplayName("200 con listas vacias y periodo nulo cuando el grupo todavia no tiene obligaciones")
    void sinObligaciones() throws Exception {
        when(primeraCuota.delGrupo(any(), any()))
                .thenReturn(new PrimeraCuotaDelGrupo(Optional.empty(), List.of(), List.of()));

        mvc.perform(get("/aportes/grupos/{id}/primera-cuota", GRUPO)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodoId").isEmpty())
                .andExpect(jsonPath("$.cuposPagados").isEmpty())
                .andExpect(jsonPath("$.cuposPendientes").isEmpty());
    }

    @Test
    @DisplayName("401 sin sesion: nadie consulta la cuota de un grupo sin identificarse")
    void sinSesion() throws Exception {
        mvc.perform(get("/aportes/grupos/{id}/primera-cuota", GRUPO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(primeraCuota);
    }

    @Test
    @DisplayName("403 con sesion pero sin BILLETERA_VER")
    void sinPermiso() throws Exception {
        mvc.perform(get("/aportes/grupos/{id}/primera-cuota", GRUPO).with(Sesiones.sinPermisos()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(primeraCuota);
    }

    @Test
    @DisplayName("400: un identificador de grupo que no es un identificador")
    void grupoQueNoEsUuid() throws Exception {
        mvc.perform(get("/aportes/grupos/{id}/primera-cuota", "no-soy-un-uuid")
                        .with(Sesiones.como("ORGANIZADOR", "BILLETERA_VER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(primeraCuota);
    }
}
