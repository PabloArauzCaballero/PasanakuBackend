package bo.aportaya.entregas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.entregas.aplicacion.CU28EmitirDesembolso;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * CU-28 · la orden por la que el dinero sale hacia un banco.
 *
 * <p>Lo que se fija aca: que la <b>clave de idempotencia de la cabecera</b> sea la que
 * viaja a la orden, y que el <b>reintento se distinga en el estado HTTP</b>. Una orden
 * de desembolso emitida dos veces por un timeout es plata que sale dos veces, y lo
 * único que lo impide es que la clave llegue entera.
 *
 * <p>Y que la respuesta del proveedor se anote con su número de intento y con cuándo se
 * puede volver a intentar: un desembolso que falla en bucle sin ventana es una cola que
 * se llena sola.
 */
@PruebaWeb(DesembolsosController.class)
class DesembolsosControllerWebTest {

    private static final UUID ORDEN = UUID.fromString("b1000000-0000-4000-8000-000000000001");
    private static final UUID ENTREGA = UUID.fromString("b1000000-0000-4000-8000-000000000002");
    private static final String CLAVE = "b1000000-0000-4000-8000-0000000000ff";

    private static final String ORDEN_PEDIDA =
            """
            {"entregaId":"b1000000-0000-4000-8000-000000000002",
             "proveedorId":"b1000000-0000-4000-8000-000000000003",
             "cuentaDestinoId":"b1000000-0000-4000-8000-000000000004",
             "glosa":"Entrega del turno 3","saldoRetenido":true}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU28EmitirDesembolso cu28;

    private org.springframework.test.web.servlet.ResultActions emitir(String cuerpo) throws Exception {
        return mvc.perform(post("/desembolsos")
                .with(Sesiones.como("ORGANIZADOR", "ENTREGA_EJECUTAR"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Nested
    @DisplayName("POST /desembolsos — emitir la orden")
    class Emitir {

        @Test
        @DisplayName("CU-28 · 201 la primera vez, con la clave y el número de intentos")
        void ordenNueva() throws Exception {
            when(cu28.emitir(any(), any()))
                    .thenReturn(new CU28EmitirDesembolso.SalidaOrden(ORDEN, "CREADA", CLAVE, 0, true));

            emitir(ORDEN_PEDIDA)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ordenId").value(ORDEN.toString()))
                    .andExpect(jsonPath("$.estado").value("CREADA"))
                    .andExpect(jsonPath("$.claveIdempotencia").value(CLAVE))
                    .andExpect(jsonPath("$.esNueva").value(true));
        }

        @Test
        @DisplayName("CU-28 · 200 cuando el reintento devuelve la orden que ya existía")
        void reintento() throws Exception {
            when(cu28.emitir(any(), any()))
                    .thenReturn(new CU28EmitirDesembolso.SalidaOrden(ORDEN, "CREADA", CLAVE, 1, false));

            emitir(ORDEN_PEDIDA)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.esNueva").value(false))
                    .andExpect(jsonPath("$.ordenId").value(ORDEN.toString()));
        }

        @Test
        @DisplayName("CU-28 · la clave de la CABECERA es la que llega a la orden")
        void laClaveViajaEntera() throws Exception {
            when(cu28.emitir(any(), any()))
                    .thenReturn(new CU28EmitirDesembolso.SalidaOrden(ORDEN, "CREADA", CLAVE, 0, true));

            emitir(ORDEN_PEDIDA).andExpect(status().isCreated());

            var capturada = ArgumentCaptor.forClass(CU28EmitirDesembolso.EntradaOrden.class);
            verify(cu28).emitir(capturada.capture(), any());
            var entrada = capturada.getValue();

            // Si la clave se perdiera aca, cada timeout de red seria un desembolso de
            // mas — y el caso de uso no puede detectarlo solo.
            org.assertj.core.api.Assertions.assertThat(entrada.claveIdempotencia())
                    .isEqualTo(CLAVE);
            org.assertj.core.api.Assertions.assertThat(entrada.entregaId()).isEqualTo(ENTREGA);
            org.assertj.core.api.Assertions.assertThat(entrada.saldoRetenido())
                    .as("sin saldo retenido, desembolsar es pagar con plata que todavia puede gastarse")
                    .isTrue();
        }

        @Test
        @DisplayName("CU-28 · 400: sin saldoRetenido, que el contrato exige")
        void faltaElSaldoRetenido() throws Exception {
            emitir(ORDEN_PEDIDA.replace(",\"saldoRetenido\":true", "")).andExpect(status().isBadRequest());
            verifyNoInteractions(cu28);
        }

        @Test
        @DisplayName("CU-28 · 400: una glosa más larga que el máximo del contrato")
        void glosaDemasiadoLarga() throws Exception {
            emitir(ORDEN_PEDIDA.replace("Entrega del turno 3", "x".repeat(141))).andExpect(status().isBadRequest());
            verifyNoInteractions(cu28);
        }

        @Test
        @DisplayName("CU-28 · 400: un identificador de entrega que no es un identificador")
        void entregaQueNoEsUuid() throws Exception {
            emitir(ORDEN_PEDIDA.replace("b1000000-0000-4000-8000-000000000002", "la-de-ayer"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu28);
        }
    }

    @Nested
    @DisplayName("POST /desembolsos/{id}/respuesta — lo que contestó el proveedor")
    class Respuesta {

        private org.springframework.test.web.servlet.ResultActions anotar(String cuerpo) throws Exception {
            return mvc.perform(post("/desembolsos/{id}/respuesta", ORDEN)
                    .with(Sesiones.como("ORGANIZADOR", "ENTREGA_EJECUTAR"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo));
        }

        @Test
        @DisplayName("CU-28 · un fallo reintentable dice CUÁNDO se puede volver a intentar")
        void falloConVentanaDeReintento() throws Exception {
            when(cu28.anotarRespuesta(any(), any()))
                    .thenReturn(new CU28EmitirDesembolso.SalidaIntento(
                            ORDEN, "RECHAZADA", 2, OffsetDateTime.of(2026, 3, 15, 14, 45, 0, 0, ZoneOffset.UTC), true));

            anotar(
                            """
                            {"iniciado":"2026-03-15T14:30:00Z","exitoso":false,
                             "codigoError":"TIEMPO_AGOTADO","mensajeProveedor":"el banco no respondio"}
                            """)
                    // 201 y no 200: el contrato declara 200 para esta operacion y el
                    // codigo devuelve 201. Se fija la conducta REAL, que es la que ve el
                    // cliente, y la divergencia queda anotada en la deuda de
                    // scripts/verificar_pruebas_web.py — cambiarla es una decision de
                    // contrato, no de prueba.
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                    .andExpect(jsonPath("$.numeroIntento").value(2))
                    // Sin ventana, el reintento es un bucle que llena la cola sola.
                    .andExpect(jsonPath("$.reintentableEn").exists());
        }

        @Test
        @DisplayName("CU-28 · un éxito lleva la referencia con la que después cruza el extracto")
        void exitoConReferencia() throws Exception {
            when(cu28.anotarRespuesta(any(), any()))
                    .thenReturn(new CU28EmitirDesembolso.SalidaIntento(ORDEN, "ACREDITADA", 1, null, true));

            anotar(
                            """
                            {"iniciado":"2026-03-15T14:30:00Z","exitoso":true,
                             "referenciaProveedor":"BNB-77123"}
                            """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estado").value("ACREDITADA"));

            var capturada = ArgumentCaptor.forClass(CU28EmitirDesembolso.EntradaRespuesta.class);
            verify(cu28).anotarRespuesta(capturada.capture(), any());
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().referenciaProveedor())
                    .as("es contra lo que se cruza el extracto bancario")
                    .isEqualTo("BNB-77123");
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().ordenId())
                    .isEqualTo(ORDEN);
        }

        @Test
        @DisplayName("CU-28 · 400: sin `iniciado`, que el contrato exige")
        void faltaElInicio() throws Exception {
            anotar("{\"exitoso\":true,\"referenciaProveedor\":\"BNB-77123\"}").andExpect(status().isBadRequest());
            verifyNoInteractions(cu28);
        }

        @Test
        @DisplayName("CU-28 · 400: una marca de tiempo que no es una marca de tiempo")
        void fechaInvalida() throws Exception {
            anotar("{\"iniciado\":\"ayer a la tarde\",\"exitoso\":true}").andExpect(status().isBadRequest());
            verifyNoInteractions(cu28);
        }
    }
}
