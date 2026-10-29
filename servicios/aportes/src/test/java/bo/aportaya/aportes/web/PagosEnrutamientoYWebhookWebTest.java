package bo.aportaya.aportes.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela;
import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela.Resultado;
import bo.aportaya.aportes.aplicacion.CU19ReembolsarPago;
import bo.aportaya.aportes.aplicacion.CU99EnrutarProveedor;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tableros · H8.S3.M4 — las dos operaciones de {@code /pagos} que la matriz mostraba sin prueba web. El webhook de la
 * pasarela es público (no trae nuestro JWT: se autentica con HMAC, CU-100) y por eso lo que importa en HTTP es que una
 * firma inválida termine en 401 sin cuerpo ni detalle. El enrutamiento elige por dónde cobrar (CU-99).
 */
@PruebaWeb(PagosController.class)
class PagosEnrutamientoYWebhookWebTest {
    private static final UUID PROVEEDOR = UUID.fromString("ffffffff-0000-4000-8000-000000000010");
    private static final UUID PAGO = UUID.fromString("ffffffff-0000-4000-8000-000000000011");
    private static final String FIRMA = "a".repeat(64);
    private static final String CUERPO = "{\"evento\":\"pago.confirmado\",\"referencia\":\"REF-1\"}";

    @Autowired
    private MockMvc mvc;

    // Sistemas › Proveedores y Webhooks: comparte controlador.
    @MockitoBean
    private bo.aportaya.aportes.aplicacion.ConsultarPlataformaDePagos consultarPlataforma;

    @MockitoBean
    private CU19ReembolsarPago cu19;

    @MockitoBean
    private CU99EnrutarProveedor cu99;

    @MockitoBean
    private CU100RecibirWebhookPasarela cu100;

    private org.springframework.test.web.servlet.ResultActions webhook(String firma) throws Exception {
        return mvc.perform(post("/pagos/webhooks/{codigo}", "PASARELA_A")
                .header("X-Firma", firma)
                .header("X-Timestamp", "1760700000")
                .contentType(MediaType.TEXT_PLAIN)
                .content(CUERPO));
    }

    @Test
    @DisplayName("webhook · sin sesión, una firma válida responde 200 con el estado y el pago; el cuerpo llega crudo")
    void webhookProcesado() throws Exception {
        when(cu100.recibir(any())).thenReturn(new Resultado(Resultado.Estado.PROCESADO, UUID.randomUUID(), PAGO, null));

        webhook(FIRMA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PROCESADO"))
                .andExpect(jsonPath("$.pagoId").value(PAGO.toString()));

        var entrada = ArgumentCaptor.forClass(CU100RecibirWebhookPasarela.Entrada.class);
        verify(cu100).recibir(entrada.capture());
        assertThat(entrada.getValue().cuerpoCrudo()).isEqualTo(CUERPO);
        assertThat(entrada.getValue().proveedorCodigo()).isEqualTo("PASARELA_A");
    }

    @Test
    @DisplayName("webhook · rechaza una firma inválida con 401 y SIN cuerpo: no se le dice al atacante por qué")
    void webhookFirmaInvalida() throws Exception {
        when(cu100.recibir(any())).thenReturn(new Resultado(Resultado.Estado.FIRMA_INVALIDA, null, null, null));

        webhook(FIRMA).andExpect(status().isUnauthorized()).andExpect(content().string(""));
    }

    @Test
    @DisplayName("webhook · 400 con una firma que no tiene 64 caracteres, sin llegar al caso de uso")
    void webhookFirmaMalFormada() throws Exception {
        webhook("corta").andExpect(status().isBadRequest());
        verifyNoInteractions(cu100);
    }

    @Test
    @DisplayName("enrutamiento · 200 con el proveedor elegido; sin yaIntentados se entiende lista vacía (el contrato)")
    void enrutar() throws Exception {
        when(cu99.enrutar(any(), any()))
                .thenReturn(new CU99EnrutarProveedor.SalidaEnrutamiento(
                        PROVEEDOR, "PASARELA_A", "cobro-1", "REINTENTABLE", true));

        mvc.perform(post("/pagos/enrutamiento")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveIdempotencia\":\"cobro-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proveedorCodigo").value("PASARELA_A"))
                .andExpect(jsonPath("$.estadoAnteTimeout").value("REINTENTABLE"));

        var entrada = ArgumentCaptor.forClass(CU99EnrutarProveedor.EntradaEnrutamiento.class);
        verify(cu99).enrutar(entrada.capture(), any());
        assertThat(entrada.getValue().yaIntentados()).isEmpty();
    }

    @Test
    @DisplayName("enrutamiento · 403 con solo BILLETERA_VER y 400 con una clave de más de 120 caracteres")
    void enrutarRechazos() throws Exception {
        mvc.perform(post("/pagos/enrutamiento")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveIdempotencia\":\"cobro-1\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/pagos/enrutamiento")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveIdempotencia\":\"" + "x".repeat(121) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu99);
    }
}
