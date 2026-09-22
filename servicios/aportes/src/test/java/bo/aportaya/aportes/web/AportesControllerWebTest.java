package bo.aportaya.aportes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.ConsultarEstadoDelParticipante;
import bo.aportaya.aportes.dominio.EstadoDePagos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contrato HTTP de {@code /aportes}: por donde entra el dinero.
 *
 * <p>Lo que se fija aca y no en {@code CU21Test}: que el <b>reintento se distinga en el
 * estado HTTP</b>. El caso de uso ya garantiza que dos cobros con la misma clave
 * producen un solo pago; lo que el cliente necesita ademas es poder saber cual de las
 * dos cosas paso, y eso vive en el {@code 201} contra el {@code 200}. Si las dos
 * respondieran igual, un cliente que reintenta por timeout no podria distinguir «se
 * acredito recien» de «ya estaba», y la unica salida seria preguntar — que es
 * exactamente lo que la clave de idempotencia existe para evitar.
 */
@PruebaWeb(AportesController.class)
class AportesControllerWebTest {

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
    @DisplayName("POST /aportes/obligaciones/{id}/pagos — cobrar")
    class Cobrar {

        @Test
        @DisplayName("201 cuando el cobro se acredita ahora")
        void cobroNuevo() throws Exception {
            when(cu21.acreditar(any(), any())).thenReturn(salida(true));

            cobrar(COBRO)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.pagoId").value(PAGO.toString()))
                    .andExpect(jsonPath("$.estadoObligacion").value("PAGADO"))
                    .andExpect(jsonPath("$.esNuevo").value(true))
                    .andExpect(jsonPath("$.pendiente.monto").value("0.00"));
        }

        @Test
        @DisplayName("200 cuando el reintento devuelve el pago que ya existia — no 201 ni 409")
        void reintento() throws Exception {
            when(cu21.acreditar(any(), any())).thenReturn(salida(false));

            cobrar(COBRO)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pagoId").value(PAGO.toString()))
                    .andExpect(jsonPath("$.esNuevo").value(false));
        }

        @Test
        @DisplayName("la clave de idempotencia de la CABECERA es la que llega al caso de uso")
        void laClaveViajaEntera() throws Exception {
            when(cu21.acreditar(any(), any())).thenReturn(salida(true));

            cobrar(COBRO).andExpect(status().isCreated());

            var capturada = ArgumentCaptor.forClass(CU21CobrarAporte.EntradaCobro.class);
            verify(cu21).acreditar(capturada.capture(), any());
            var entrada = capturada.getValue();

            // Si la clave se perdiera o se reescribiera en la pagina, el caso de uso
            // trataria cada reintento como un cobro nuevo — y el participante pagaria
            // dos veces. El caso de uso no puede detectarlo solo.
            org.assertj.core.api.Assertions.assertThat(entrada.claveIdempotencia())
                    .isEqualTo(CLAVE);
            org.assertj.core.api.Assertions.assertThat(entrada.obligacionId()).isEqualTo(OBLIGACION);
            org.assertj.core.api.Assertions.assertThat(entrada.monto()).isEqualByComparingTo(bob("350.00"));
            // Sin recargo declarado, cero de la MISMA moneda que el monto.
            org.assertj.core.api.Assertions.assertThat(entrada.comision()).isEqualByComparingTo(bob("0.00"));
            org.assertj.core.api.Assertions.assertThat(entrada.esManual()).isFalse();
        }

        @Test
        @DisplayName("400: sin Idempotency-Key no se cobra — y el caso de uso ni se entera")
        void sinClaveNoSeCobra() throws Exception {
            mvc.perform(post("/aportes/obligaciones/{id}/pagos", OBLIGACION)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(COBRO))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu21);
        }

        @Test
        @DisplayName("400: un canal de cobro que el contrato no enumera")
        void canalFueraDelContrato() throws Exception {
            cobrar(COBRO.replace("QR_INTEROPERABLE", "TRUEQUE")).andExpect(status().isBadRequest());
            verifyNoInteractions(cu21);
        }

        @Test
        @DisplayName("400: falta la referencia del proveedor, que el contrato exige")
        void faltaLaReferencia() throws Exception {
            cobrar(
                            """
                    {"monto":{"monto":"350.00","moneda":"BOB"},"canal":"QR_INTEROPERABLE"}
                    """)
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu21);
        }

        @Test
        @DisplayName("422: la regla del caso de uso sale con su codigo y sin filtrar nada")
        void reglaDeNegocio() throws Exception {
            when(cu21.acreditar(any(), any()))
                    .thenThrow(new ErrorDeNegocio(
                            CodigoError.de(21, 3),
                            "El aporte no cubre la obligacion.",
                            java.util.Map.of("faltan", "50.00")));

            cobrar(COBRO)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("AP-CU21-03"))
                    .andExpect(jsonPath("$.detalle.faltan").value("50.00"));
        }
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
