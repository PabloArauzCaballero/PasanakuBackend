package bo.aportaya.nucleofinanciero.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import bo.aportaya.nucleofinanciero.aplicacion.CotizarOperacion;
import bo.aportaya.nucleofinanciero.aplicacion.CuentaPropia;
import bo.aportaya.nucleofinanciero.aplicacion.OrdenesExistentes;
import bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor;
import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.nucleofinanciero.dominio.puertos.SegundoFactor;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaCierreBilletera;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaTransferencia;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tableros · H8.S3.M4 — las siete operaciones de la billetera que la matriz de pruebas mostraba sin prueba web:
 * recargar, retirar, retener, cerrar una retención, transferir, cerrar la billetera y listar movimientos. Lo que solo
 * se ve en HTTP: que sin clave de idempotencia ni un campo no declarado llegan al caso de uso, que el permiso de VER
 * no alcanza para OPERAR, y las reglas que decide la traducción (el umbral de doble aprobación, la evidencia de
 * segundo factor que manda, el desenlace de la retención).
 */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
@org.springframework.context.annotation.Import(FondeoDeLaBilletera.class)
class BilleteraOperacionesWebTest {
    private static final UUID CUENTA = UUID.fromString("eeeeeeee-0000-4000-8000-000000000001");
    private static final UUID INSTRUMENTO = UUID.fromString("eeeeeeee-0000-4000-8000-000000000005");
    private static final UUID RETENCION = UUID.fromString("eeeeeeee-0000-4000-8000-000000000006");
    private static final String CLAVE = "eeeeeeee-0000-4000-8000-0000000000ff";
    // El contrato pide 20 caracteres como mínimo a la evidencia (un JWT) y 6 a 8 al factor viejo.
    private static final String EVIDENCIA = "eyJhbGciOiJFUzI1NiJ9.evidencia-de-prueba";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarSaldo saldos;

    @MockitoBean
    private CU10RecargarSaldo cu10;

    @MockitoBean
    private RecargasConProveedor recargas;

    @MockitoBean
    private CU11RetirarSaldo cu11;

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
    private CotizarOperacion cotizaciones;

    @MockitoBean
    private CuentaPropia cuentaPropia;

    @MockitoBean
    private OrdenesExistentes existentes;

    @MockitoBean
    private QrDeLaBilletera qrDeLaBilletera;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static MockHttpServletRequestBuilder operar(
            MockHttpServletRequestBuilder pedido, String cuerpo, String... permisos) {
        return pedido.with(Sesiones.como("PARTICIPANTE", permisos))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    private static final String RECARGA =
            """
            {"cuentaBilleteraId":"%s","monto":{"monto":"500.00","moneda":"BOB"},"medio":"QR"}"""
                    .formatted(CUENTA);

    @Test
    @DisplayName("recarga · 201 con lo que se acreditará, y la Idempotency-Key llega como clave del caso de uso")
    void recarga() throws Exception {
        when(recargas.solicitar(any(), any()))
                .thenReturn(new CU10RecargarSaldo.SalidaSolicitud(
                        UUID.randomUUID(),
                        "PENDIENTE",
                        OffsetDateTime.of(2026, 10, 17, 12, 0, 0, 0, ZoneOffset.UTC),
                        bob("500.00")));

        mvc.perform(operar(post("/billetera/recargas"), RECARGA, "BILLETERA_OPERAR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acreditara.monto").value("500.00"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        var entrada = ArgumentCaptor.forClass(CU10RecargarSaldo.EntradaSolicitud.class);
        verify(recargas).solicitar(entrada.capture(), any());
        assertThat(entrada.getValue().claveIdempotencia()).isEqualTo(CLAVE);
    }

    @Test
    @DisplayName("recarga · 400 sin Idempotency-Key o con un campo no declarado; 403 con solo BILLETERA_VER")
    void recargaRechazos() throws Exception {
        mvc.perform(post("/billetera/recargas")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RECARGA))
                .andExpect(status().isBadRequest());
        mvc.perform(operar(
                        post("/billetera/recargas"),
                        RECARGA.replace("\"medio\":\"QR\"", "\"medio\":\"QR\",\"acreditarYa\":true"),
                        "BILLETERA_OPERAR"))
                .andExpect(status().isBadRequest());
        mvc.perform(operar(post("/billetera/recargas"), RECARGA, "BILLETERA_VER"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recargas);
    }

    private static String retiro(String monto, String mfa) {
        return """
                {"cuentaBilleteraId":"%s","monto":{"monto":"%s","moneda":"BOB"},"instrumentoDestinoId":"%s"%s}"""
                .formatted(CUENTA, monto, INSTRUMENTO, mfa);
    }

    private ArgumentCaptor<CU11RetirarSaldo.EntradaRetiro> retirar(String cuerpo) throws Exception {
        when(cotizaciones.confirmar(any(), any(), any(), anyString(), any(), anyBoolean()))
                .thenReturn(new CotizarOperacion.CostoConfirmado(bob("5.00"), Optional.empty()));
        when(cu11.solicitar(any(), any()))
                .thenReturn(new CU11RetirarSaldo.SalidaRetiro(
                        UUID.randomUUID(), "AUTORIZADA", bob("5.00"), bob("95.00"), UUID.randomUUID()));
        mvc.perform(operar(post("/billetera/retiros"), cuerpo, "BILLETERA_OPERAR"))
                .andExpect(status().isCreated());
        var entrada = ArgumentCaptor.forClass(CU11RetirarSaldo.EntradaRetiro.class);
        verify(cu11).solicitar(entrada.capture(), any());
        return entrada;
    }

    @Test
    @DisplayName("retiro · por debajo del umbral NO pide doble aprobación; la evidenciaMfa manda sobre factorMfa")
    void retiroChico() throws Exception {
        when(segundoFactor.verificado(any(), eq(EVIDENCIA))).thenReturn(true);

        var entrada = retirar(retiro("100.00", ",\"evidenciaMfa\":\"" + EVIDENCIA + "\",\"factorMfa\":\"123456\""));

        assertThat(entrada.getValue().requiereDobleAprobacion()).isFalse();
        assertThat(entrada.getValue().mfaVerificado()).isTrue();
    }

    @Test
    @DisplayName("retiro · límite: EXACTAMENTE el umbral (5000.00) ya pide doble aprobación")
    void retiroEnElUmbral() throws Exception {
        assertThat(retirar(retiro("5000.00", "")).getValue().requiereDobleAprobacion())
                .isTrue();
    }

    @Test
    @DisplayName("retiro · 400 sin el destino que el contrato exige, sin llegar al caso de uso")
    void retiroSinDestino() throws Exception {
        mvc.perform(operar(
                        post("/billetera/retiros"),
                        """
                        {"cuentaBilleteraId":"%s","monto":{"monto":"100.00","moneda":"BOB"}}"""
                                .formatted(CUENTA),
                        "BILLETERA_OPERAR"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu11);
    }

    @Test
    @DisplayName("retención · 201 con disponible y retenido; la clave llega al caso de uso")
    void retener() throws Exception {
        when(cu13.retener(any(), eq(CLAVE), any()))
                .thenReturn(new CU13RetenerSaldo.SalidaRetencion(RETENCION, bob("400.00"), bob("100.00"), null));

        mvc.perform(operar(
                        post("/billetera/retenciones"),
                        """
                        {"cuentaBilleteraId":"%s","monto":{"monto":"100.00","moneda":"BOB"},"motivo":"APORTE"}"""
                                .formatted(CUENTA),
                        "BILLETERA_OPERAR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoRetenido.monto").value("100.00"));
    }

    @Test
    @DisplayName("cierre de retención · LIBERADA libera y EJECUTADA ejecuta: son dos finales y los dice el llamador")
    void cerrarRetencion() throws Exception {
        var cierre = new CU13RetenerSaldo.SalidaCierre(RETENCION, "LIBERADA", bob("500.00"), bob("0.00"));
        when(cu13.liberar(eq(RETENCION), any())).thenReturn(cierre);
        when(cu13.ejecutar(eq(RETENCION), any()))
                .thenReturn(new CU13RetenerSaldo.SalidaCierre(RETENCION, "EJECUTADA", bob("400.00"), bob("0.00")));
        mvc.perform(operar(
                        post("/billetera/retenciones/{id}/cierre", RETENCION),
                        "{\"desenlace\":\"LIBERADA\"}",
                        "BILLETERA_OPERAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LIBERADA"));
        mvc.perform(operar(
                        post("/billetera/retenciones/{id}/cierre", RETENCION),
                        "{\"desenlace\":\"EJECUTADA\"}",
                        "BILLETERA_OPERAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EJECUTADA"));
        verify(cu13).liberar(eq(RETENCION), any());
        verify(cu13).ejecutar(eq(RETENCION), any());
    }

    @Test
    @DisplayName("cierre de retención · 400 con un desenlace que el contrato no declara")
    void cerrarRetencionInvalida() throws Exception {
        mvc.perform(operar(
                        post("/billetera/retenciones/{id}/cierre", RETENCION),
                        "{\"desenlace\":\"BORRADA\"}",
                        "BILLETERA_OPERAR"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu13);
    }

    @Test
    @DisplayName("transferencia · 200 con lo que responde el caso de uso; 403 con solo BILLETERA_VER")
    void transferir() throws Exception {
        var salida = new SalidaTransferencia();
        salida.setTransaccionId(UUID.randomUUID());
        salida.setObligacionSaldada(false);
        when(movimientos.transferir(any(), any(), any())).thenReturn(ResponseEntity.ok(salida));
        String cuerpo =
                """
                {"cuentaOrigenId":"%s","destino":{"tipo":"ALIAS","valor":"ana.qr"},
                 "monto":{"monto":"50.00","moneda":"BOB"},"concepto":"Cuota"}"""
                        .formatted(CUENTA);

        mvc.perform(operar(post("/billetera/transferencias"), cuerpo, "BILLETERA_OPERAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligacionSaldada").value(false));
        mvc.perform(operar(post("/billetera/transferencias"), cuerpo, "BILLETERA_VER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("cierre de billetera · 201 y 400 con un destino del saldo que el contrato no declara")
    void cerrarBilletera() throws Exception {
        var salida = new SalidaCierreBilletera();
        salida.setSolicitudId(UUID.randomUUID());
        when(movimientos.cerrar(any(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(salida));

        mvc.perform(operar(
                        post("/billetera/cierres"),
                        """
                        {"cuentaBilleteraId":"%s","motivo":"Ya no la uso","destinoSaldo":"RETIRO"}"""
                                .formatted(CUENTA),
                        "BILLETERA_OPERAR"))
                .andExpect(status().isCreated());
        mvc.perform(operar(
                        post("/billetera/cierres"),
                        """
                        {"cuentaBilleteraId":"%s","motivo":"Ya no la uso","destinoSaldo":"REGALO"}"""
                                .formatted(CUENTA),
                        "BILLETERA_OPERAR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("movimientos · 200 con cada línea; 400 con un límite fuera de 1..100")
    void movimientos() throws Exception {
        when(consultasDeMovimientos.ejecutar(eq(CUENTA), eq(5), any()))
                .thenReturn(new ConsultarMovimientosBilletera.Salida(
                        CUENTA,
                        List.of(new ConsultarMovimientosBilletera.Movimiento(
                                UUID.randomUUID(),
                                "CREDITO",
                                bob("500.00"),
                                bob("500.00"),
                                bob("0.00"),
                                "Recarga",
                                OffsetDateTime.of(2026, 10, 17, 12, 0, 0, 0, ZoneOffset.UTC)))));

        mvc.perform(get("/billetera/{id}/movimientos", CUENTA).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movimientos[0].monto.monto").value("500.00"));
        mvc.perform(get("/billetera/{id}/movimientos", CUENTA)
                        .param("limite", "101")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isBadRequest());
    }
}
