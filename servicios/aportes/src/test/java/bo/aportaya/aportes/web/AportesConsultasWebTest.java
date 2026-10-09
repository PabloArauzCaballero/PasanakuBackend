package bo.aportaya.aportes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.ConsultarEstadoDelParticipante;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDelPeriodo;
import bo.aportaya.aportes.dominio.EstadoDePagos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** El contrato HTTP de lo que {@code /aportes} le contesta a los otros servicios. */
@PruebaWeb(AportesController.class)
class AportesConsultasWebTest {

    private static final UUID OBLIGACION = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000001");
    private static final UUID PAGO = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000002");
    private static final UUID PARTICIPANTE = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000003");
    private static final String CLAVE = "bbbbbbbb-0000-4000-8000-0000000000ff";

    private static final String COBRO =
            """
            {
              "monto": {"monto": "350.00", "moneda": "BOB"},
              "canal": "QR_INTEROPERABLE",
              "referenciaProveedor": "REF-000123"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU21CobrarAporte cu21;

    @MockitoBean
    private ConsultarEstadoDelParticipante estados;

    @MockitoBean
    private bo.aportaya.aportes.aplicacion.HechosDeGrupos grupos;

    private static final java.util.UUID DUENO = java.util.UUID.fromString("a2100000-0000-4000-8000-000000000001");
    private static final java.util.UUID PERIODO = java.util.UUID.fromString("a2100000-0000-4000-8000-000000000002");

    /** Por omision: la obligacion es de quien paga y el periodo esta abierto; cada caso cambia lo suyo. */
    @org.junit.jupiter.api.BeforeEach
    void admisible() {
        when(cu21.contextoDe(any(), any()))
                .thenReturn(java.util.Optional.of(new CU21CobrarAporte.ContextoDeObligacion(DUENO, PERIODO)));
        when(grupos.admisibilidad(DUENO, PERIODO))
                .thenReturn(java.util.Optional.of(
                        new bo.aportaya.aportes.aplicacion.HechosDeGrupos.Admisibilidad(true, true)));
    }

    // El recaudo del periodo tiene su propia prueba (RecaudoWebTest).
    @MockitoBean
    private ConsultarRecaudoDelPeriodo recaudos;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static CU21CobrarAporte.SalidaCobro salida(boolean esNuevo) {
        return new CU21CobrarAporte.SalidaCobro(PAGO, OBLIGACION, "PAGADO", bob("0.00"), esNuevo);
    }

    private org.springframework.test.web.servlet.ResultActions cobrar(String cuerpo) throws Exception {
        return mvc.perform(post("/aportes/obligaciones/{id}/pagos", OBLIGACION)
                .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Nested
    @DisplayName("GET — lo que este servicio le contesta a los otros")
    class Consultas {

        @Test
        @DisplayName("200 con el estado del participante y los importes como cadena decimal")
        void estadoDelParticipante() throws Exception {
            when(estados.ejecutar(any(), any()))
                    .thenReturn(new EstadoDePagos(
                            false,
                            new BigDecimal("1050.00"),
                            new BigDecimal("350.00"),
                            new BigDecimal("700.00"),
                            2,
                            "BOB"));

            mvc.perform(get("/aportes/participantes/{id}/estado", PARTICIPANTE)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alDia").value(false))
                    .andExpect(jsonPath("$.totalAportado").value("1050.00"))
                    .andExpect(jsonPath("$.deudaVigente").value("350.00"))
                    .andExpect(jsonPath("$.obligacionesAbiertas").value(2))
                    .andExpect(jsonPath("$.moneda").value("BOB"));
        }

        @Test
        @DisplayName("quien no tiene obligaciones esta al dia, y eso viaja como tal")
        void sinObligacionesEstaAlDia() throws Exception {
            when(estados.ejecutar(any(), any())).thenReturn(EstadoDePagos.sinObligaciones());

            mvc.perform(get("/aportes/participantes/{id}/estado", PARTICIPANTE)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alDia").value(true))
                    .andExpect(jsonPath("$.obligacionesAbiertas").value(0));
        }

        @Test
        @DisplayName("400: un identificador de participante que no es un identificador")
        void participanteQueNoEsUuid() throws Exception {
            mvc.perform(get("/aportes/participantes/{id}/estado", "no-soy-un-uuid")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(estados);
        }

        @Test
        @DisplayName("200 con la cuenta de morosos del grupo")
        void morosos() throws Exception {
            when(estados.morososDelGrupo(any(), any())).thenReturn(3);

            mvc.perform(get("/aportes/grupos/{id}/morosos", OBLIGACION)
                            .with(Sesiones.como("ORGANIZADOR", "BILLETERA_VER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.morosos").value(3))
                    .andExpect(jsonPath("$.grupoId").value(OBLIGACION.toString()));
        }
    }
}
