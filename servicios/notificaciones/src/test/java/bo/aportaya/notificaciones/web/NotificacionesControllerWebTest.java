package bo.aportaya.notificaciones.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.notificaciones.aplicacion.CU82ProcesarRespuesta;
import bo.aportaya.notificaciones.aplicacion.ConsultarSupresion;
import bo.aportaya.notificaciones.aplicacion.SolicitarAviso;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.List;
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
 * El contrato HTTP de {@code /notificaciones}.
 *
 * <p>Tres afirmaciones, y la del medio es la que importa.
 *
 * <p><b>Emitir responde 202, no 201.</b> El aviso queda encolado, no entregado: el
 * despacho ocurre después y puede fallar. Un {@code 201} prometería una entrega que
 * nadie hizo todavía.
 *
 * <p><b>El webhook del proveedor es público y se autentica por firma.</b> Es la única
 * ruta de este servicio sin sesión, y por eso la firma tiene que llegar entera al caso
 * de uso: si se perdiera en el camino, cualquiera podría confirmar el pago de otro
 * mandando un POST.
 *
 * <p><b>La consulta de supresión contesta si, o no.</b> Nunca el motivo ni el
 * histórico: quién pidió la baja de qué canal es un dato de la persona.
 */
@PruebaWeb(NotificacionesController.class)
class NotificacionesControllerWebTest {

    private static final UUID DESTINATARIO = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID NOTIFICACION = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final String CLAVE = "d0000000-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SolicitarAviso avisos;

    @MockitoBean
    private ConsultarSupresion supresiones;

    @MockitoBean
    private CU82ProcesarRespuesta cu82;

    @Nested
    @DisplayName("POST /notificaciones — emitir")
    class Emitir {

        private org.springframework.test.web.servlet.ResultActions emitir(String cuerpo) throws Exception {
            return mvc.perform(post("/notificaciones")
                    .with(Sesiones.como("SOPORTE"))
                    .header("Idempotency-Key", CLAVE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo));
        }

        @Test
        @DisplayName("CU-80 · 202 con los canales por los que va a salir — encolado, no entregado")
        void caminoFeliz() throws Exception {
            when(avisos.ejecutar(any(), any()))
                    .thenReturn(new SolicitarAviso.Salida(NOTIFICACION, List.of("IN_APP", "PUSH")));

            emitir(
                            """
                            {"destinatarioId":"d0000000-0000-4000-8000-000000000001",
                             "evento":"APORTE_VENCIDO","datos":{"monto":"350.00"}}
                            """)
                    // 202 y no 201: el despacho ocurre despues y puede fallar. Un 201
                    // prometeria una entrega que todavia no hizo nadie.
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.notificacionId").value(NOTIFICACION.toString()))
                    .andExpect(jsonPath("$.canales[0]").value("IN_APP"))
                    .andExpect(jsonPath("$.canales[1]").value("PUSH"));
        }

        @Test
        @DisplayName("CU-80 · el destinatario y el evento llegan sin tocar al caso de uso")
        void loQueLlegaAlCasoDeUso() throws Exception {
            when(avisos.ejecutar(any(), any())).thenReturn(new SolicitarAviso.Salida(NOTIFICACION, List.of("IN_APP")));

            emitir(
                            """
                            {"destinatarioId":"d0000000-0000-4000-8000-000000000001",
                             "evento":"APORTE_VENCIDO","datos":{"monto":"350.00"}}
                            """)
                    .andExpect(status().isAccepted());

            var capturada = ArgumentCaptor.forClass(SolicitarAviso.Entrada.class);
            verify(avisos).ejecutar(capturada.capture(), any());
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().destinatarioId())
                    .isEqualTo(DESTINATARIO);
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().evento())
                    .isEqualTo("APORTE_VENCIDO");
        }

        @Test
        @DisplayName("CU-80 · 400: sin `datos`, que el contrato exige")
        void faltanLosDatos() throws Exception {
            emitir("{\"destinatarioId\":\"d0000000-0000-4000-8000-000000000001\",\"evento\":\"X\"}")
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(avisos);
        }

        @Test
        @DisplayName("CU-80 · 400: un destinatario que no es un identificador")
        void destinatarioQueNoEsUuid() throws Exception {
            emitir("{\"destinatarioId\":\"el-de-siempre\",\"evento\":\"X\",\"datos\":{}}")
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(avisos);
        }
    }

    @Nested
    @DisplayName("POST /notificaciones/webhooks/{proveedor} — la respuesta del proveedor")
    class Webhook {

        private static final String CUERPO =
                """
                {"canal":"WHATSAPP","remitente":"+59171234567","firma":"sha256=abc123",
                 "cargaUtil":"{\\"body\\":\\"ya pague\\"}","contenido":"ya pague",
                 "claveIdempotencia":"wamid-77123"}
                """;

        private org.springframework.test.web.servlet.ResultActions entrante(String cuerpo) throws Exception {
            // Sin sesion a proposito: el proveedor no tiene una, lo autentica su firma.
            return mvc.perform(post("/notificaciones/webhooks/{proveedor}", "whatsapp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo));
        }

        @Test
        @DisplayName("CU-82 · el webhook entra SIN sesión y devuelve la intención interpretada")
        void entraSinSesion() throws Exception {
            when(cu82.ejecutar(any(), any()))
                    .thenReturn(new CU82ProcesarRespuesta.SalidaRespuesta(
                            UUID.fromString("d0000000-0000-4000-8000-000000000003"),
                            "YA_PAGUE",
                            "COMPROBANTE_SOLICITADO",
                            NOTIFICACION));

            entrante(CUERPO)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.intencion").value("YA_PAGUE"))
                    .andExpect(jsonPath("$.accion").value("COMPROBANTE_SOLICITADO"));
        }

        @Test
        @DisplayName("CU-82 · la FIRMA y la clave del proveedor llegan enteras: es lo único que lo autentica")
        void laFirmaLlegaEntera() throws Exception {
            when(cu82.ejecutar(any(), any()))
                    .thenReturn(new CU82ProcesarRespuesta.SalidaRespuesta(
                            UUID.fromString("d0000000-0000-4000-8000-000000000003"),
                            "YA_PAGUE",
                            "COMPROBANTE_SOLICITADO",
                            NOTIFICACION));

            entrante(CUERPO).andExpect(status().isOk());

            var capturada = ArgumentCaptor.forClass(CU82ProcesarRespuesta.EntradaRespuesta.class);
            verify(cu82).ejecutar(capturada.capture(), any());
            var entrada = capturada.getValue();

            // Sin la firma no hay autenticacion, y esta ruta es publica: cualquiera
            // podria confirmar el pago de otro mandando un POST.
            org.assertj.core.api.Assertions.assertThat(entrada.firma()).isEqualTo("sha256=abc123");
            // Y sin la clave del proveedor, un webhook reintentado produce dos efectos.
            org.assertj.core.api.Assertions.assertThat(entrada.claveIdempotencia())
                    .isEqualTo("wamid-77123");
            org.assertj.core.api.Assertions.assertThat(entrada.canal()).isEqualTo("WHATSAPP");
        }

        @Test
        @DisplayName("CU-82 · sin `canal` en el cuerpo, manda el proveedor de la ruta")
        void elCanalCaeEnElProveedorDeLaRuta() throws Exception {
            when(cu82.ejecutar(any(), any()))
                    .thenReturn(new CU82ProcesarRespuesta.SalidaRespuesta(
                            null, "DESCONOCIDA", "TICKET_ABIERTO", NOTIFICACION));

            entrante(CUERPO.replace("\"canal\":\"WHATSAPP\",", "")).andExpect(status().isOk());

            var capturada = ArgumentCaptor.forClass(CU82ProcesarRespuesta.EntradaRespuesta.class);
            verify(cu82).ejecutar(capturada.capture(), any());
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().canal())
                    .isEqualTo("whatsapp");
        }

        @Test
        @DisplayName("CU-82 · 400: sin firma, que el contrato exige — no llega al caso de uso")
        void sinFirmaNoSeProcesa() throws Exception {
            entrante(CUERPO.replace("\"firma\":\"sha256=abc123\",", "")).andExpect(status().isBadRequest());
            verifyNoInteractions(cu82);
        }

        @Test
        @DisplayName("CU-82 · 400: sin clave de idempotencia, un reintento del proveedor sería un efecto de más")
        void sinClaveNoSeProcesa() throws Exception {
            entrante(
                            """
                            {"canal":"WHATSAPP","remitente":"+59171234567","firma":"sha256=abc123",
                             "cargaUtil":"{}","contenido":"ya pague"}
                            """)
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu82);
        }

        @Test
        @DisplayName("CU-82 · 400: JSON corrupto en una ruta pública no es un 500")
        void jsonCorrupto() throws Exception {
            entrante("{ roto").andExpect(status().isBadRequest());
            verifyNoInteractions(cu82);
        }
    }

    @Test
    @DisplayName("CU-80 · la consulta de supresión contesta sí o no, y nada más")
    void supresionContestaSiONo() throws Exception {
        when(supresiones.estaSuprimido(any(), any(), any())).thenReturn(true);

        String cuerpo = mvc.perform(get("/notificaciones/supresion")
                        .param("identificador", "+59171234567")
                        .param("categoria", "COMERCIAL")
                        .with(Sesiones.como("SOPORTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suprimido").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Quien pidio la baja, cuando y de que canal es un dato de la persona.
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("+59171234567")
                .doesNotContain("motivo")
                .doesNotContain("fecha");
    }
}
