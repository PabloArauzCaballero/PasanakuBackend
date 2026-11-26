package bo.aportaya.aportes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.ConsultarEstadoDelParticipante;
import bo.aportaya.aportes.aplicacion.ConsultarPrimeraCuota;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDelPeriodo;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDiario;
import bo.aportaya.aportes.aplicacion.GenerarObligacionesDelPeriodo;
import bo.aportaya.aportes.aplicacion.HechosDeGrupos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * F1.L1.M11 (ANF-DEFECTO-001, ANF-FLUJO-QR-001): el cobro de la cuota no toma del cliente ni el monto, ni el canal,
 * ni la referencia, ni el recargo, ni el proveedor. {@code pagarCuotaConSaldo} no admite campos;
 * {@code cobrarAporte}, que se retira, solo acepta lo que el servidor habria fijado.
 */
@PruebaWeb(AportesController.class)
class PagoConSaldoWebTest {

    private static final UUID OBLIGACION = UUID.fromString("c1100000-0000-4000-8000-000000000001");
    private static final UUID PAGO = UUID.fromString("c1100000-0000-4000-8000-000000000002");
    private static final UUID DUENO = UUID.fromString("c1100000-0000-4000-8000-000000000003");
    private static final UUID PERIODO = UUID.fromString("c1100000-0000-4000-8000-000000000004");
    private static final String CLAVE = "c1100000-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU21CobrarAporte cu21;

    @MockitoBean
    private bo.aportaya.aportes.aplicacion.CU21PagarConSaldo conSaldo;

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

    @BeforeEach
    void admisible() {
        when(cu21.contextoDe(any(), any()))
                .thenReturn(Optional.of(new CU21CobrarAporte.ContextoDeObligacion(DUENO, PERIODO)));
        when(grupos.admisibilidad(DUENO, PERIODO))
                .thenReturn(Optional.of(new HechosDeGrupos.Admisibilidad(true, true)));
        when(conSaldo.pagarConSaldo(anyString(), any(), any(), anyBoolean(), any()))
                .thenReturn(
                        new CU21CobrarAporte.SalidaCobro(PAGO, OBLIGACION, "PAGADO", Dinero.cero(Moneda.BOB), true));
    }

    private static MockHttpServletRequestBuilder conSaldo() {
        return post("/aportes/obligaciones/{id}/pago-con-saldo", OBLIGACION)
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private void nuncaSeCobro() {
        verify(conSaldo, never()).pagarConSaldo(anyString(), any(), any(), anyBoolean(), any());
        verify(cu21, never()).acreditar(any(), any());
    }

    @Nested
    @DisplayName("POST /aportes/obligaciones/{id}/pago-con-saldo")
    class PagarConSaldo {

        @Test
        @DisplayName("correcto · sin cuerpo: 201 y el caso de uso recibe la clave, sin monto declarado")
        void sinCuerpo() throws Exception {
            mvc.perform(conSaldo().with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.pagoId").value(PAGO.toString()))
                    .andExpect(jsonPath("$.estadoObligacion").value("PAGADO"));
            verify(conSaldo).pagarConSaldo(eq(CLAVE), eq(OBLIGACION), eq(Optional.empty()), eq(true), any());
        }

        @Test
        @DisplayName("CU-134 · correcto: una cuenta suspendida (sesion restringida) paga su cuota: 201")
        void cuentaSuspendidaPaga() throws Exception {
            mvc.perform(conSaldo().with(Sesiones.restringida("PARTICIPANTE", "BILLETERA_OPERAR")))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("limite · cuerpo {} vacio: 201, es lo mismo que no mandar nada")
        void cuerpoVacio() throws Exception {
            mvc.perform(conSaldo()
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .content("{}"))
                    .andExpect(status().isCreated());
        }

        @ParameterizedTest(name = "invalido · cuerpo {0} → 400 y no se cobra")
        @ValueSource(
                strings = {
                    "{\"monto\":{\"monto\":\"1.00\",\"moneda\":\"BOB\"}}",
                    "{\"canal\":\"QR_INTEROPERABLE\"}",
                    "{\"recargo\":{\"monto\":\"50.00\",\"moneda\":\"BOB\"}}",
                    "{\"referenciaProveedor\":\"REF-1\"}",
                    "{\"proveedorId\":\"c1100000-0000-4000-8000-000000000009\"}",
                    "{\"esManual\":true}",
                    "[]",
                    "\"1.00\""
                })
        void campoNoDeclarado(String cuerpo) throws Exception {
            mvc.perform(conSaldo()
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
            nuncaSeCobro();
        }

        @Test
        @DisplayName("invalido · la obligacion de otro: 422 AP-CU21-06 y no se cobra")
        void obligacionAjena() throws Exception {
            when(grupos.admisibilidad(DUENO, PERIODO))
                    .thenReturn(Optional.of(new HechosDeGrupos.Admisibilidad(false, true)));
            mvc.perform(conSaldo().with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("AP-CU21-06"));
            nuncaSeCobro();
        }

        @Test
        @DisplayName("invalido · sin Idempotency-Key: 400 y el caso de uso ni se entera")
        void sinClave() throws Exception {
            mvc.perform(post("/aportes/obligaciones/{id}/pago-con-saldo", OBLIGACION)
                            .contentType(MediaType.APPLICATION_JSON)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu21);
        }

        @Test
        @DisplayName("invalido · sin sesion: 401")
        void sinSesion() throws Exception {
            mvc.perform(conSaldo()).andExpect(status().isUnauthorized());
            verifyNoInteractions(cu21);
        }

        @Test
        @DisplayName("invalido · sin BILLETERA_OPERAR: 403")
        void sinPermiso() throws Exception {
            mvc.perform(conSaldo().with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu21);
        }
    }

    @Nested
    @DisplayName("POST /aportes/obligaciones/{id}/pagos (se retira): solo lo que fijaria el servidor")
    class CobrarAporteRetirandose {

        private org.springframework.test.web.servlet.ResultActions cobrar(String extra) throws Exception {
            return mvc.perform(post("/aportes/obligaciones/{id}/pagos", OBLIGACION)
                    .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                    .header("Idempotency-Key", CLAVE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"monto\":{\"monto\":\"100.00\",\"moneda\":\"BOB\"},\"referenciaProveedor\":\"REF-1\""
                            + extra + "}"));
        }

        @ParameterizedTest(name = "invalido · {0} → 422 AP-CU21-10 y no se cobra")
        @ValueSource(
                strings = {
                    ",\"canal\":\"QR_INTEROPERABLE\"",
                    ",\"canal\":\"EFECTIVO_AL_ORGANIZADOR\"",
                    ",\"canal\":\"BILLETERA_MOVIL\",\"recargo\":{\"monto\":\"50.00\",\"moneda\":\"BOB\"}",
                    ",\"canal\":\"BILLETERA_MOVIL\",\"proveedorId\":\"c1100000-0000-4000-8000-000000000009\"",
                    ",\"canal\":\"BILLETERA_MOVIL\",\"esManual\":true"
                })
        void loQueFijaElServidor(String extra) throws Exception {
            cobrar(extra)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("AP-CU21-10"));
            nuncaSeCobro();
        }

        @Test
        @DisplayName("limite · recargo cero y esManual=false se aceptan: es lo que el servidor fijaria")
        void lasOmisionesExplicitas() throws Exception {
            cobrar(
                            ",\"canal\":\"BILLETERA_MOVIL\",\"recargo\":{\"monto\":\"0.00\",\"moneda\":\"BOB\"},\"esManual\":false")
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("correcto · con saldo: cobra por el mismo camino, con el monto declarado para compararlo")
        void conSaldo() throws Exception {
            cobrar(",\"canal\":\"BILLETERA_MOVIL\"").andExpect(status().isCreated());
            verify(conSaldo)
                    .pagarConSaldo(
                            eq(CLAVE),
                            eq(OBLIGACION),
                            eq(Optional.of(Dinero.de("100.00", Moneda.BOB))),
                            eq(true),
                            any());
            verify(cu21, never()).acreditar(any(), any());
        }
    }
}
