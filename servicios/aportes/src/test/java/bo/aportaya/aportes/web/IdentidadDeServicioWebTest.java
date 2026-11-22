package bo.aportaya.aportes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.ConsultarEstadoDelParticipante;
import bo.aportaya.aportes.aplicacion.ConsultarPrimeraCuota;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDelPeriodo;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDiario;
import bo.aportaya.aportes.aplicacion.GenerarObligacionesDelPeriodo;
import bo.aportaya.aportes.aplicacion.HechosDeGrupos;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * F1.L1.M07 (ANF-SEG-002, BOLA; regla 98.6) · las lecturas de {@code aportes} que hacen otros servicios las hace el
 * servicio con su identidad, no una persona con un permiso generico: morosos y primera cuota solo
 * {@code SERVICIO_GRUPOS}; el recaudo {@code SERVICIO_ENTREGAS} o {@code SERVICIO_GARANTIA} (y, hasta el paso 3,
 * {@code ENTREGA_EJECUTAR}).
 */
@PruebaWeb(AportesController.class)
class IdentidadDeServicioWebTest {

    private static final UUID GRUPO = UUID.fromString("e7000000-0000-4000-8000-000000000001");
    private static final UUID PERIODO = UUID.fromString("e7000000-0000-4000-8000-000000000002");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU21CobrarAporte cu21;

    @MockitoBean
    private HechosDeGrupos grupos;

    @MockitoBean
    private ConsultarEstadoDelParticipante estados;

    @MockitoBean
    private ConsultarRecaudoDelPeriodo recaudos;

    @MockitoBean
    private GenerarObligacionesDelPeriodo calendario;

    @MockitoBean
    private ConsultarRecaudoDiario recaudosDiarios;

    @MockitoBean
    private ConsultarPrimeraCuota primeraCuota;

    @Test
    @DisplayName("correcto · grupos con SERVICIO_GRUPOS cuenta los morosos: 200")
    void gruposCuentaMorosos() throws Exception {
        when(estados.morososDelGrupo(any(), any())).thenReturn(0);
        mvc.perform(get("/aportes/grupos/{id}/morosos", GRUPO).with(Sesiones.como("PARTICIPANTE", "SERVICIO_GRUPOS")))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "invalido · {0} con {1} pide los morosos de un grupo: 403 y no se lee nada")
    @CsvSource({
        "PARTICIPANTE, BILLETERA_VER",
        "ORGANIZADOR, BILLETERA_VER",
        "BACKOFFICE, BILLETERA_VER",
        "PARTICIPANTE, SERVICIO_ENTREGAS",
        "PARTICIPANTE, SERVICIO_GARANTIA"
    })
    void morososSoloGrupos(String rol, String permiso) throws Exception {
        mvc.perform(get("/aportes/grupos/{id}/morosos", GRUPO).with(Sesiones.como(rol, permiso)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(estados);
    }

    @ParameterizedTest(name = "invalido · {0} con {1} pide la primera cuota de un grupo: 403 y no se lee nada")
    @CsvSource({
        "PARTICIPANTE, BILLETERA_VER",
        "ORGANIZADOR, BILLETERA_VER",
        "PARTICIPANTE, SERVICIO_ENTREGAS",
        "PARTICIPANTE, SERVICIO_GARANTIA"
    })
    void primeraCuotaSoloGrupos(String rol, String permiso) throws Exception {
        mvc.perform(get("/aportes/grupos/{id}/primera-cuota", GRUPO).with(Sesiones.como(rol, permiso)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(primeraCuota);
    }

    @Test
    @DisplayName("invalido · sin token, morosos y primera cuota responden 401")
    void sinToken() throws Exception {
        mvc.perform(get("/aportes/grupos/{id}/morosos", GRUPO)).andExpect(status().isUnauthorized());
        mvc.perform(get("/aportes/grupos/{id}/primera-cuota", GRUPO)).andExpect(status().isUnauthorized());
        mvc.perform(get("/aportes/periodos/{id}/recaudo", PERIODO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(estados, primeraCuota, recaudos);
    }

    @ParameterizedTest(name = "invalido · {0} con {1} pide el recaudo del periodo: 403 y no se lee nada")
    @CsvSource({"PARTICIPANTE, BILLETERA_VER", "ORGANIZADOR, BILLETERA_VER", "PARTICIPANTE, SERVICIO_GRUPOS"})
    void recaudoSoloEntregasYGarantia(String rol, String permiso) throws Exception {
        mvc.perform(get("/aportes/periodos/{id}/recaudo", PERIODO).with(Sesiones.como(rol, permiso)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recaudos);
    }

    @ParameterizedTest(name = "correcto · {0} pasa la guardia del recaudo (no 401 ni 403)")
    @CsvSource({"SERVICIO_ENTREGAS", "SERVICIO_GARANTIA", "ENTREGA_EJECUTAR"})
    void recaudoDeEntregasYGarantia(String permiso) throws Exception {
        // El caso de uso es un doble sin respuesta: lo que se prueba aca es que la guardia deja pasar y se lo llama.
        mvc.perform(get("/aportes/periodos/{id}/recaudo", PERIODO).with(Sesiones.como("PARTICIPANTE", permiso)));
        org.mockito.Mockito.verify(recaudos).ejecutar(any(), any());
    }
}
