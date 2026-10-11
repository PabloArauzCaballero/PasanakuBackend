package bo.aportaya.entregas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto;
import bo.aportaya.entregas.aplicacion.CU22LiquidarEntrega;
import bo.aportaya.entregas.aplicacion.RegistroDelFondeo;
import bo.aportaya.entregas.dominio.FondeoDelPozo;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
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

/** El contrato HTTP de {@code POST /entregas/pozo-completo}. */
@PruebaWeb(EntregasController.class)
class FondeoDelPozoWebTest {

    private static final UUID ENTREGA = UUID.fromString("a7000000-0000-4000-8000-000000000001");
    private static final UUID FONDEO = UUID.fromString("a7000000-0000-4000-8000-000000000002");
    private static final String CLAVE = "a7000000-0000-4000-8000-0000000000ff";

    private static final String CUERPO =
            """
            {"grupoId":"a7000000-0000-4000-8000-000000000011","periodoId":"a7000000-0000-4000-8000-000000000012",
             "turnoId":"a7000000-0000-4000-8000-000000000013","cupoId":"a7000000-0000-4000-8000-000000000014",
             "beneficiarioId":"a7000000-0000-4000-8000-000000000015","metodoDesembolso":"TRANSFERENCIA_BANCARIA",
             "fechaProgramada":"2026-10-20"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU22EntregarPozoCompleto pozo;

    @MockitoBean
    private CU22LiquidarEntrega cu22;

    @MockitoBean
    private MercadoWeb mercado;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static RegistroDelFondeo.Salida salida(boolean nuevo, String pendiente, String estado) {
        Dinero empresa = bob("1000.00").menos(bob(pendiente));
        return new RegistroDelFondeo.Salida(
                ENTREGA,
                FONDEO,
                estado,
                new FondeoDelPozo.Resultado(
                        bob("6000.00"), bob("5000.00"), bob("0.00"), bob("1000.00"), empresa, bob(pendiente)),
                nuevo);
    }

    @Test
    @DisplayName(
            "201 con el pozo completo, lo que puso la empresa y la deuda en cero; el cuerpo trae solo identificadores")
    void fondeado() throws Exception {
        when(pozo.fondear(any(), any())).thenReturn(salida(true, "0.00", "FONDEADO"));

        mvc.perform(post("/entregas/pozo-completo")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("FONDEADO"))
                .andExpect(jsonPath("$.pozo.monto").value("6000.00"))
                .andExpect(jsonPath("$.cubiertoEmpresa.monto").value("1000.00"))
                .andExpect(jsonPath("$.pendiente.monto").value("0.00"));

        var captor = ArgumentCaptor.forClass(CU22EntregarPozoCompleto.Entrada.class);
        verify(pozo).fondear(captor.capture(), any());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().clave()).isEqualTo("fondeo:" + CLAVE);
    }

    @Test
    @DisplayName("201 CON_PENDIENTE conserva la deuda en la respuesta; 200 si el pedido ya existia")
    void conPendienteYReintento() throws Exception {
        when(pozo.fondear(any(), any())).thenReturn(salida(true, "1000.00", "CON_PENDIENTE"));
        mvc.perform(post("/entregas/pozo-completo")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CON_PENDIENTE"))
                .andExpect(jsonPath("$.pendiente.monto").value("1000.00"));

        when(pozo.fondear(any(), any())).thenReturn(salida(false, "0.00", "FONDEADO"));
        mvc.perform(post("/entregas/pozo-completo")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esNuevo").value(false));
    }

    @Test
    @DisplayName(
            "403 sin el permiso, 401 sin sesion, 400 sin clave o con EFECTIVO_ORGANIZADOR; el caso de uso no se invoca")
    void guardiaYValidacion() throws Exception {
        mvc.perform(post("/entregas/pozo-completo")
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isForbidden());
        mvc.perform(post("/entregas/pozo-completo")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/entregas/pozo-completo")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/entregas/pozo-completo")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO.replace("TRANSFERENCIA_BANCARIA", "EFECTIVO_ORGANIZADOR")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(pozo);
    }

    @Test
    @DisplayName("422 con el codigo del contrato cuando aportes o garantia no responden")
    void rechazosDeNegocio() throws Exception {
        when(pozo.fondear(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(22, 6), "No se pudo confirmar el recaudo."));

        mvc.perform(post("/entregas/pozo-completo")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU22-06"));
    }
}
