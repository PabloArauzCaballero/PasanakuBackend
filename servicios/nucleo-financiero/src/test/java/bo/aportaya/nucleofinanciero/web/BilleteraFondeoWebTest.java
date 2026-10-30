package bo.aportaya.nucleofinanciero.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CotizarOperacion;
import bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas.CobroQr;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

/**
 * Tableros · H8.S3.M4 — recargar y retirar. Lo que solo se ve en HTTP: sin clave de idempotencia ni un campo no
 * declarado llegan al caso de uso, el permiso de VER no alcanza para OPERAR, y las reglas que decide la traducción (el
 * umbral de doble aprobación y la evidencia de segundo factor que manda).
 */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
@org.springframework.context.annotation.Import(FondeoDeLaBilletera.class)
@org.springframework.test.context.bean.override.mockito.MockitoBean(
        types = bo.aportaya.nucleofinanciero.aplicacion.CU13LiberarRetencionDelMercado.class)
class BilleteraFondeoWebTest extends BaseDeOperacionesWeb {

    private static final String RECARGA =
            """
            {"cuentaBilleteraId":"%s","monto":{"monto":"500.00","moneda":"BOB"},"medio":"QR"}"""
                    .formatted(CUENTA);

    @Test
    @DisplayName("recarga · 201 con lo que se acreditará y el QR dinámico; la Idempotency-Key llega como clave")
    void recarga() throws Exception {
        when(recargas.solicitarConQr(any(), any()))
                .thenReturn(solicitada(Optional.of(new CobroQr("PASANAKU-PRUEBA|NO-PAGABLE|ref=x", "PRUEBA"))));

        mvc.perform(operar(post("/billetera/recargas"), RECARGA, "BILLETERA_OPERAR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acreditara.monto").value("500.00"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.cobroQr.contenido").value("PASANAKU-PRUEBA|NO-PAGABLE|ref=x"))
                .andExpect(jsonPath("$.cobroQr.origen").value("PRUEBA"));

        var entrada = ArgumentCaptor.forClass(CU10RecargarSaldo.EntradaSolicitud.class);
        verify(recargas).solicitarConQr(entrada.capture(), any());
        assertThat(entrada.getValue().claveIdempotencia()).isEqualTo(CLAVE);
    }

    @Test
    @DisplayName("recarga · si el proveedor no emite QR, cobroQr no viaja")
    void recargaSinQr() throws Exception {
        when(recargas.solicitarConQr(any(), any())).thenReturn(solicitada(Optional.empty()));

        mvc.perform(operar(post("/billetera/recargas"), RECARGA, "BILLETERA_OPERAR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cobroQr").doesNotExist());
    }

    @Test
    @DisplayName("pago de prueba · 200 con el saldo resultante cuando el ambiente tiene el doble de prueba")
    void pagoDePrueba() throws Exception {
        UUID orden = UUID.randomUUID();
        when(pagoDePrueba.disponible()).thenReturn(true);
        when(pagoDePrueba.pagar(eq(orden), any()))
                .thenReturn(new CU10RecargarSaldo.SalidaAcreditacion(orden, UUID.randomUUID(), bob("650.00")));

        mvc.perform(operar(post("/billetera/recargas/{id}/pago-de-prueba", orden), "", "BILLETERA_OPERAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordenRecargaId").value(orden.toString()))
                .andExpect(jsonPath("$.saldoDespues.monto").value("650.00"));
    }

    @Test
    @DisplayName("pago de prueba · 404 como ruta inexistente sin el doble; 403 con solo BILLETERA_VER")
    void pagoDePruebaRechazos() throws Exception {
        UUID orden = UUID.randomUUID();
        when(pagoDePrueba.disponible()).thenReturn(false);

        mvc.perform(operar(post("/billetera/recargas/{id}/pago-de-prueba", orden), "", "BILLETERA_OPERAR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("AP-VAL-05"));
        mvc.perform(operar(post("/billetera/recargas/{id}/pago-de-prueba", orden), "", "BILLETERA_VER"))
                .andExpect(status().isForbidden());
        verify(pagoDePrueba, org.mockito.Mockito.never()).pagar(any(), any());
    }

    private static RecargasConProveedor.Solicitada solicitada(Optional<CobroQr> qr) {
        return new RecargasConProveedor.Solicitada(
                new CU10RecargarSaldo.SalidaSolicitud(
                        UUID.randomUUID(),
                        "PENDIENTE",
                        OffsetDateTime.of(2026, 10, 17, 12, 0, 0, 0, ZoneOffset.UTC),
                        bob("500.00")),
                qr);
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
}
