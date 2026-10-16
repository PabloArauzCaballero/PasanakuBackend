package bo.aportaya.entregas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.entregas.aplicacion.CU28EmitirDesembolso;
import bo.aportaya.entregas.aplicacion.ConsultarDesembolsos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Backoffice carril A · H8 · {@code GET /desembolsos}: la bandeja de Tesoreria. Permiso (ENTREGA_EJECUTAR
 * o AUDITORIA_LEER), respuesta minima y rechazos con codigo. El recorrido real vive en
 * {@code CU28DesembolsosTest}.
 */
@PruebaWeb(DesembolsosController.class)
class DesembolsosBandejaWebTest {

    private static final UUID ORDEN = UUID.fromString("b1000000-0000-4000-8000-000000000011");
    private static final UUID ENTREGA = UUID.fromString("b1000000-0000-4000-8000-000000000012");
    private static final UUID PROVEEDOR = UUID.fromString("b1000000-0000-4000-8000-000000000013");
    private static final OffsetDateTime CREADA = OffsetDateTime.of(2026, 10, 16, 15, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU28EmitirDesembolso cu28;

    @MockitoBean
    private ConsultarDesembolsos consultas;

    private static ConsultarDesembolsos.Pagina unaRechazada(boolean llena) {
        var d = new ConsultarDesembolsos.Desembolso(
                ORDEN,
                ENTREGA,
                PROVEEDOR,
                Dinero.de("5982.00", Moneda.BOB),
                "RECHAZADA",
                1,
                Optional.of("FALLIDO"),
                Optional.of("CUENTA_INEXISTENTE"),
                Optional.empty(),
                CREADA,
                Optional.empty());
        return new ConsultarDesembolsos.Pagina(
                List.of(d),
                llena ? Optional.of(CREADA) : Optional.empty(),
                llena ? Optional.of(ORDEN) : Optional.empty());
    }

    @Test
    @DisplayName("200: ENTREGA_EJECUTAR ve la bandeja con monto y moneda, ultimo error y cursor, sin cuenta destino")
    void tesoreriaVe() throws Exception {
        when(consultas.listar(any(), any(), any(), eq(1), any())).thenReturn(unaRechazada(true));

        String cuerpo = mvc.perform(
                        get("/desembolsos").param("limite", "1").with(Sesiones.como("TESORERIA", "ENTREGA_EJECUTAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ordenId").value(ORDEN.toString()))
                .andExpect(jsonPath("$.items[0].monto.monto").value("5982.00"))
                .andExpect(jsonPath("$.items[0].monto.moneda").value("BOB"))
                .andExpect(jsonPath("$.items[0].estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.items[0].ultimoCodigoError").value("CUENTA_INEXISTENTE"))
                .andExpect(jsonPath("$.proximoAntesDeId").value(ORDEN.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        // Respuesta minima (96.5.3): la bandeja dice que mirar, no a quien se le paga.
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("cuentaDestino")
                .doesNotContain("glosa")
                .doesNotContain("referenciaProveedor");
    }

    @Test
    @DisplayName("200: AUDITORIA_LEER tambien la lee; sin estados pasa la lista vacia y el default 25")
    void auditoriaLee() throws Exception {
        when(consultas.listar(any(), any(), any(), eq(25), any())).thenReturn(unaRechazada(false));

        mvc.perform(get("/desembolsos").with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximoAntesDeId").doesNotExist());
        verify(consultas).listar(any(), eq(Optional.empty()), eq(Optional.empty()), eq(25), any());
    }

    @Test
    @DisplayName("403: quien no opera entregas ni audita no ve la bandeja")
    void sinPermiso() throws Exception {
        mvc.perform(get("/desembolsos").with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        verifyNoInteractions(consultas);
    }

    @Test
    @DisplayName("401: sin token no hay bandeja")
    void sinToken() throws Exception {
        mvc.perform(get("/desembolsos")).andExpect(status().isUnauthorized());
        verifyNoInteractions(consultas);
    }

    @Test
    @DisplayName("422: un estado desconocido sale con AP-CU28-07, nunca como 500")
    void estadoDesconocido() throws Exception {
        when(consultas.listar(any(), any(), any(), any(Integer.class), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(28, 7), "Filtro de listado invalido."));

        mvc.perform(get("/desembolsos").param("estados", "PAGADA").with(Sesiones.como("TESORERIA", "ENTREGA_EJECUTAR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU28-07"));
    }

    @Test
    @DisplayName("400: limite fuera de 1..100 o cursor que no es UUID lo corta el contrato")
    void entradaInvalida() throws Exception {
        mvc.perform(get("/desembolsos").param("limite", "0").with(Sesiones.como("TESORERIA", "ENTREGA_EJECUTAR")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/desembolsos").param("antesDeId", "x").with(Sesiones.como("TESORERIA", "ENTREGA_EJECUTAR")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(consultas);
    }
}
