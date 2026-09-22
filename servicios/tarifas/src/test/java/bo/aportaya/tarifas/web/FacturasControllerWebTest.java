package bo.aportaya.tarifas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.tarifas.aplicacion.CU32EmitirFactura;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-32 · la factura electrónica.
 *
 * <p>Lo que se fija acá es el caso que decide si el negocio puede seguir operando cuando
 * el SIN no responde: la factura se emite <b>offline</b>, con su CUF, y la respuesta lo
 * dice. Si un servicio fiscal caído devolviera un error, no se podría cobrar nada
 * mientras dure la caída — y la norma justamente prevé la emisión offline.
 *
 * <p>El CUF y el número viajan siempre: son lo que el cliente presenta ante el fisco.
 */
@PruebaWeb(FacturasController.class)
class FacturasControllerWebTest {

    private static final UUID FACTURA = UUID.fromString("b6000000-0000-4000-8000-000000000001");
    private static final UUID DEVENGO = UUID.fromString("b6000000-0000-4000-8000-000000000002");
    private static final UUID EVENTO = UUID.fromString("b6000000-0000-4000-8000-000000000003");

    private static final String CUERPO =
            """
            {"devengoId":"b6000000-0000-4000-8000-000000000002",
             "montoIva":{"monto":"1.95","moneda":"BOB"},
             "urlPdf":"https://aportaya.bo/f/1.pdf"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU32EmitirFactura cu32;

    private org.springframework.test.web.servlet.ResultActions emitir(String cuerpo) throws Exception {
        return mvc.perform(post("/facturas")
                .with(Sesiones.como("CONTABILIDAD"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Test
    @DisplayName("CU-32 · 201 con el CUF y el número: es lo que el cliente presenta ante el fisco")
    void facturaValidada() throws Exception {
        when(cu32.consultarAlServicio()).thenReturn(new CU32EmitirFactura.ConsultaFiscal("CUFD-123", false));
        when(cu32.emitir(any(), any(), any()))
                .thenReturn(new CU32EmitirFactura.SalidaFactura(FACTURA, "CUF-ABC123", 4521L, "VALIDADA", null));

        emitir(CUERPO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.facturaId").value(FACTURA.toString()))
                .andExpect(jsonPath("$.cuf").value("CUF-ABC123"))
                .andExpect(jsonPath("$.numeroFactura").value(4521))
                .andExpect(jsonPath("$.estadoFiscal").value("VALIDADA"))
                .andExpect(jsonPath("$.eventoSignificativoId").doesNotExist());
    }

    @Test
    @DisplayName("CU-32 · con el servicio fiscal caído se factura OFFLINE, y la respuesta lo dice")
    void facturaOffline() throws Exception {
        when(cu32.consultarAlServicio()).thenReturn(new CU32EmitirFactura.ConsultaFiscal("CUFD-123", true));
        when(cu32.emitir(any(), any(), any()))
                .thenReturn(new CU32EmitirFactura.SalidaFactura(
                        FACTURA, "CUF-OFFLINE-1", 4522L, "EMITIDA_OFFLINE", EVENTO));

        emitir(CUERPO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoFiscal").value("EMITIDA_OFFLINE"))
                // El evento significativo es la constancia de la caida: sin el, una
                // factura offline no se puede justificar despues ante el fisco.
                .andExpect(jsonPath("$.eventoSignificativoId").value(EVENTO.toString()))
                .andExpect(jsonPath("$.cuf").value("CUF-OFFLINE-1"));
    }

    @Test
    @DisplayName("CU-32 · el estado del servicio fiscal se consulta ANTES de emitir, no dentro")
    void seConsultaAntesDeEmitir() throws Exception {
        when(cu32.consultarAlServicio()).thenReturn(new CU32EmitirFactura.ConsultaFiscal("CUFD-123", false));
        when(cu32.emitir(any(), any(), any()))
                .thenReturn(new CU32EmitirFactura.SalidaFactura(FACTURA, "CUF-ABC123", 4521L, "VALIDADA", null));

        emitir(CUERPO).andExpect(status().isCreated());

        // Es una llamada de red, y una llamada de red dentro de la transaccion es el
        // invariante 6. Que llegue como argumento prueba que se resolvio afuera.
        verify(cu32).consultarAlServicio();
        var capturada = ArgumentCaptor.forClass(CU32EmitirFactura.ConsultaFiscal.class);
        verify(cu32).emitir(any(), capturada.capture(), any());
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().cufd()).isEqualTo("CUFD-123");
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().servicioCaido())
                .isFalse();

        var entrada = ArgumentCaptor.forClass(CU32EmitirFactura.EntradaFactura.class);
        verify(cu32).emitir(entrada.capture(), any(), any());
        org.assertj.core.api.Assertions.assertThat(entrada.getValue().devengoId())
                .isEqualTo(DEVENGO);
        org.assertj.core.api.Assertions.assertThat(entrada.getValue().montoIva())
                .isEqualByComparingTo(Dinero.de("1.95", Moneda.BOB));
    }

    @Test
    @DisplayName("CU-32 · 400: sin el IVA, que el contrato exige")
    void faltaElIva() throws Exception {
        emitir("{\"devengoId\":\"b6000000-0000-4000-8000-000000000002\"}").andExpect(status().isBadRequest());
        verifyNoInteractions(cu32);
    }

    @Test
    @DisplayName("CU-32 · 400: un devengo que no es un identificador")
    void devengoQueNoEsUuid() throws Exception {
        emitir(CUERPO.replace("b6000000-0000-4000-8000-000000000002", "el-de-ayer"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu32);
    }
}
