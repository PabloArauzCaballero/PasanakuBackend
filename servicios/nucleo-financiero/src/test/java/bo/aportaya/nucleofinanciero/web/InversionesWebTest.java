package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.nucleofinanciero.aplicacion.BandejaDeRetiros;
import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion;
import bo.aportaya.nucleofinanciero.aplicacion.CU15EmitirExtracto;
import bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarMovimientosBilletera;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarSaldo;
import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.nucleofinanciero.dominio.puertos.SegundoFactor;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de CONTRATO del lado productor de lo que {@code inversiones} y {@code entregas} le piden a nucleo, y
 * negativa de autorizacion: son operaciones de servicio a servicio, un usuario final con {@code BILLETERA_OPERAR}
 * recibe 403 y el caso de uso nunca se invoca. Las formas del cuerpo son las que arma {@code LibroPorHttp}.
 */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
@org.springframework.context.annotation.Import(FondeoDeLaBilletera.class)
class InversionesWebTest {

    private static final UUID CUENTA = UUID.fromString("eeeeeeee-0000-4000-8000-000000000001");
    private static final UUID RETENCION = UUID.fromString("eeeeeeee-0000-4000-8000-000000000002");
    private static final UUID TRANSACCION = UUID.fromString("eeeeeeee-0000-4000-8000-000000000003");
    private static final String CLAVE = "eeeeeeee-0000-4000-8000-0000000000ff";

    private static final String DEBITO =
            """
            {"retencionId":"eeeeeeee-0000-4000-8000-000000000002","monto":{"monto":"300.00","moneda":"BOB"}}
            """;
    private static final String RESCATE =
            """
            {"cuentaBilleteraId":"eeeeeeee-0000-4000-8000-000000000001","monto":{"monto":"300.00","moneda":"BOB"},
             "referenciaId":"eeeeeeee-0000-4000-8000-000000000004"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarSaldo saldos;

    @MockitoBean
    private CU10RecargarSaldo cu10;

    @MockitoBean
    private bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor recargas;

    @MockitoBean
    private CU11RetirarSaldo cu11;

    // El controlador tambien lee los movimientos recientes: sin este doble el contexto no carga.
    @MockitoBean
    private ConsultarMovimientosBilletera consultasDeMovimientos;

    @MockitoBean
    private CU13RetenerSaldo cu13;

    @MockitoBean
    private CU14ReversarTransaccion cu14;

    @MockitoBean
    private CU15EmitirExtracto cu15;

    @MockitoBean
    private CU17BloquearPorAutoridad cu17;

    @MockitoBean
    private MovimientosDeLaBilletera movimientos;

    @MockitoBean
    private CotizadorDeComision cotizador;

    @MockitoBean
    private SegundoFactor segundoFactor;

    @MockitoBean
    private BandejaDeRetiros bandeja;

    @MockitoBean
    private bo.aportaya.nucleofinanciero.aplicacion.CotizarOperacion cotizaciones;

    @MockitoBean
    private bo.aportaya.nucleofinanciero.aplicacion.CuentaPropia cuentaPropia;

    @MockitoBean
    private bo.aportaya.nucleofinanciero.aplicacion.OrdenesExistentes existentes;

    @MockitoBean
    private QrDeLaBilletera qrDeLaBilletera;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static ResponseEntity<bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaEjecucionDeRetencion> ok() {
        var salida = new bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaEjecucionDeRetencion();
        salida.setTransaccionId(TRANSACCION);
        salida.setSaldoDisponible(MapeoDeBilletera.dinero(bob("700.00")));
        salida.setSaldoRetenido(MapeoDeBilletera.dinero(bob("0.00")));
        return ResponseEntity.ok(salida);
    }

    @Nested
    @DisplayName("Saldo con titular y comprometido en pozos")
    class Saldo {
        @Test
        @DisplayName("200 con titularId y comprometidoEnPozos como cadena decimal, tal como lo lee LibroPorHttp")
        void conCompromisos() throws Exception {
            UUID titular = UUID.fromString("eeeeeeee-0000-4000-8000-0000000000aa");
            when(saldos.ejecutar(any(), any()))
                    .thenReturn(new ConsultarSaldo.Salida(
                            CUENTA,
                            bob("5000.00"),
                            bob("0.00"),
                            OffsetDateTime.of(2026, 10, 9, 10, 0, 0, 0, ZoneOffset.UTC),
                            titular,
                            bob("4500.00")));
            mvc.perform(get("/billetera/{id}/saldo", CUENTA).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titularId").value(titular.toString()))
                    .andExpect(jsonPath("$.comprometidoEnPozos.monto").value("4500.00"))
                    .andExpect(jsonPath("$.comprometidoEnPozos.moneda").value("BOB"));
        }

        @Test
        @DisplayName("sin respuesta de aportes el campo se OMITE (no es null ni cero): el consumidor falla cerrado")
        void sinCompromisos() throws Exception {
            when(saldos.ejecutar(any(), any()))
                    .thenReturn(new ConsultarSaldo.Salida(
                            CUENTA, bob("5000.00"), bob("0.00"), OffsetDateTime.now(ZoneOffset.UTC)));
            mvc.perform(get("/billetera/{id}/saldo", CUENTA).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disponible.monto").value("5000.00"))
                    .andExpect(jsonPath("$.comprometidoEnPozos").doesNotExist())
                    .andExpect(jsonPath("$.titularId").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Solo servicios: el usuario final, aunque opere su billetera, recibe 403")
    class SoloServicios {
        @Test
        @DisplayName(
                "POST /billetera/inversiones/debitos: BILLETERA_OPERAR no alcanza (403); SERVICIO_INVERSIONES si (200)")
        void debitos() throws Exception {
            mvc.perform(post("/billetera/inversiones/debitos")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR", "BILLETERA_VER"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(DEBITO))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(movimientos);

            when(movimientos.debitarInversion(any(), any(), any())).thenReturn(ok());
            mvc.perform(post("/billetera/inversiones/debitos")
                            .with(Sesiones.como("SERVICIO_INVERSIONES"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(DEBITO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transaccionId").value(TRANSACCION.toString()))
                    .andExpect(jsonPath("$.saldoDisponible.monto").value("700.00"));
        }

        @Test
        @DisplayName(
                "POST /billetera/inversiones/acreditaciones: ni BILLETERA_OPERAR ni TESORERIA ni otro servicio (403)")
        void acreditaciones() throws Exception {
            for (var sesion : new org.springframework.test.web.servlet.request.RequestPostProcessor[] {
                Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"),
                Sesiones.como("TESORERIA"),
                Sesiones.como("SERVICIO_ENTREGAS")
            }) {
                mvc.perform(post("/billetera/inversiones/acreditaciones")
                                .with(sesion)
                                .header("Idempotency-Key", CLAVE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(RESCATE))
                        .andExpect(status().isForbidden());
            }
            verifyNoInteractions(movimientos);

            var salida = new bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaAcreditacionRescate();
            salida.setTransaccionId(TRANSACCION);
            salida.setSaldoDespues(MapeoDeBilletera.dinero(bob("300.00")));
            when(movimientos.acreditarRescate(any(), any(), any())).thenReturn(ResponseEntity.ok(salida));
            mvc.perform(post("/billetera/inversiones/acreditaciones")
                            .with(Sesiones.como("SERVICIO_INVERSIONES"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(RESCATE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transaccionId").value(TRANSACCION.toString()));
        }

        @Test
        @DisplayName(
                "POST /billetera/retenciones/{id}/pago-a-tercero: BILLETERA_OPERAR no alcanza (403) y no admite cuerpo con beneficiario")
        void pagoATercero() throws Exception {
            mvc.perform(post("/billetera/retenciones/{id}/pago-a-tercero", RETENCION)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"beneficiarioUsuarioId\":\"eeeeeeee-0000-4000-8000-0000000000bb\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(movimientos);

            when(movimientos.pagarAlTercero(any(), any(), any())).thenReturn(ok());
            mvc.perform(post("/billetera/retenciones/{id}/pago-a-tercero", RETENCION)
                            .with(Sesiones.como("SERVICIO_ENTREGAS"))
                            .header("Idempotency-Key", CLAVE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transaccionId").value(TRANSACCION.toString()));
        }

        @Test
        @DisplayName("sin sesion las tres rutas responden 401")
        void sinSesion() throws Exception {
            mvc.perform(post("/billetera/inversiones/debitos")
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(DEBITO))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post("/billetera/retenciones/{id}/pago-a-tercero", RETENCION)
                            .header("Idempotency-Key", CLAVE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName(
                "el contrato rechaza campos que no declara: un debito con beneficiario o con titular mal formado es 400")
        void camposFueraDeContrato() throws Exception {
            mvc.perform(
                            post("/billetera/inversiones/debitos")
                                    .with(Sesiones.como("SERVICIO_INVERSIONES"))
                                    .header("Idempotency-Key", CLAVE)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"retencionId":"eeeeeeee-0000-4000-8000-000000000002",
                                     "monto":{"monto":"300.00","moneda":"BOB"},"destino":"otra-cuenta"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName(
                "el token que emite identidad (rol BACKOFFICE, el rol de servicio en permisos) llega al caso de uso como servicio; sin el permiso, 403")
        void tokenRealDeIdentidad() throws Exception {
            var contexto = org.mockito.ArgumentCaptor.forClass(bo.aportaya.plataforma.dominio.ContextoSesion.class);
            when(movimientos.debitarInversion(any(), any(), contexto.capture())).thenReturn(ok());
            mvc.perform(post("/billetera/inversiones/debitos")
                            .with(Sesiones.como("BACKOFFICE", "SERVICIO_INVERSIONES"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(DEBITO))
                    .andExpect(status().isOk());
            org.assertj.core.api.Assertions.assertThat(contexto.getValue().rol())
                    .isEqualTo("SERVICIO_INVERSIONES");

            org.mockito.Mockito.clearInvocations(movimientos);
            mvc.perform(post("/billetera/inversiones/debitos")
                            .with(Sesiones.como("BACKOFFICE", "TESORERIA", "SERVICIO_ENTREGAS"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(DEBITO))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(movimientos);
        }
    }
}
