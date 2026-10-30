package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarMovimientosBilletera;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaCierreBilletera;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaTransferencia;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tableros · H8.S3.M4 — retener, cerrar una retención, transferir, cerrar la billetera y listar movimientos: el
 * desenlace de la retención lo dice el llamador, y lo que el contrato no declara es 400 sin llegar al caso de uso.
 */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
@org.springframework.context.annotation.Import(FondeoDeLaBilletera.class)
@org.springframework.test.context.bean.override.mockito.MockitoBean(
        types = bo.aportaya.nucleofinanciero.aplicacion.CU13LiberarRetencionDelMercado.class)
class BilleteraMovimientosWebTest extends BaseDeOperacionesWeb {

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
