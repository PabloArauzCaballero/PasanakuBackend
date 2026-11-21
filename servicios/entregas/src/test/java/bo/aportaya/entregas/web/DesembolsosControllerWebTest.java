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
        @DisplayName("F1.L2.M05 · un éxito anotado por una persona responde 410 y no acredita ni publica nada")
        void exitoAnotadoPorPersonaRetirado() throws Exception {
            anotar(
                            """
                            {"iniciado":"2026-03-15T14:30:00Z","exitoso":true,
                             "referenciaProveedor":"BNB-77123"}
                            """)
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.codigo").value("AP-CU28-08"));
            // Sin llamada al caso de uso no hay ACREDITADA ni `desembolso_acreditado` en el outbox.
            verifyNoInteractions(cu28);
        }

        @Test
        @DisplayName("F1.L2.M05 · tampoco se anota un fallo a mano: la respuesta solo la trae el webhook firmado")
        void falloAnotadoPorPersonaRetirado() throws Exception {
            anotar(
                            """
                            {"iniciado":"2026-03-15T14:30:00Z","exitoso":false,
                             "codigoError":"TIEMPO_AGOTADO","mensajeProveedor":"el banco no respondio"}
                            """)
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.codigo").value("AP-CU28-08"));
            verifyNoInteractions(cu28);
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

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private bo.aportaya.entregas.aplicacion.ConsultarDesembolsos consultas;
}
