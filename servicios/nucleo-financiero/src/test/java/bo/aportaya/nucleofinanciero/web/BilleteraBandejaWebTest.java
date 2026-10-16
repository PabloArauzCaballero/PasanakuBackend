package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Backoffice carril A · H2.S2 · {@code GET /billetera/retiros}, la bandeja de la segunda aprobacion.
 *
 * <p>Contrato HTTP: el permiso, la forma de la pagina (importes como cadena con su moneda, cursor),
 * que no viajen destino ni datos de la persona, y que el cursor a medias o el limite fuera de rango
 * no lleguen como 500. El recorrido real de la cola contra la base vive en {@code CU11BandejaTest}.
 */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
@Import(FondeoDeLaBilletera.class)
class BilleteraBandejaWebTest {
    private static final UUID CUENTA = UUID.fromString("eeeeeeee-0000-4000-8000-000000000001");
    private static final UUID TRANSACCION = UUID.fromString("eeeeeeee-0000-4000-8000-000000000002");
    private static final String CLAVE = "eeeeeeee-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarSaldo saldos;

    @MockitoBean
    private CU10RecargarSaldo cu10;

    @MockitoBean
    private CU11RetirarSaldo cu11;

    // El controlador tambien lee los movimientos recientes: sin este doble el contexto no carga.
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

    // FondeoDeLaBilletera va REAL (@Import): es quien traduce la aprobacion y la bandeja. Se doblan sus
    // colaboradores, como al resto de los casos de uso del corte.
    @MockitoBean
    private RecargasConProveedor recargas;

    @MockitoBean
    private CotizarOperacion cotizaciones;

    @MockitoBean
    private CuentaPropia cuentaPropia;

    @MockitoBean
    private OrdenesExistentes existentes;

    @MockitoBean
    private BandejaDeRetiros bandeja;

    @MockitoBean
    private QrDeLaBilletera qrDeLaBilletera;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static final UUID ORDEN = UUID.fromString("eeeeeeee-0000-4000-8000-000000000007");
    private static final UUID SOLICITANTE = UUID.fromString("eeeeeeee-0000-4000-8000-000000000008");
    private static final OffsetDateTime SOLICITADA = OffsetDateTime.of(2026, 10, 16, 14, 30, 0, 0, ZoneOffset.UTC);

    private BandejaDeRetiros.Pagina unaFila(boolean llena) {
        var fila = new BandejaDeRetiros.Fila(
                ORDEN,
                CUENTA,
                SOLICITANTE,
                bob("6000.00"),
                bob("5.00"),
                bob("5995.00"),
                true,
                SOLICITADA,
                Optional.empty(),
                true);
        return new BandejaDeRetiros.Pagina(
                List.of(fila),
                llena ? Optional.of(SOLICITADA) : Optional.empty(),
                llena ? Optional.of(ORDEN) : Optional.empty());
    }

    @Test
    @DisplayName("200: con RETIRO_APROBAR devuelve la pagina con importes como cadena, moneda y cursor")
    void devuelveLaPagina() throws Exception {
        when(bandeja.enRevision(any(), any(), eq(1), any())).thenReturn(unaFila(true));

        String cuerpo = mvc.perform(get("/billetera/retiros")
                        .param("limite", "1")
                        .with(Sesiones.como("TESORERIA", "RETIRO_APROBAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ordenRetiroId").value(ORDEN.toString()))
                .andExpect(jsonPath("$.items[0].solicitadaPor").value(SOLICITANTE.toString()))
                .andExpect(jsonPath("$.items[0].solicitado.monto").value("6000.00"))
                .andExpect(jsonPath("$.items[0].solicitado.moneda").value("BOB"))
                .andExpect(jsonPath("$.items[0].neto.monto").value("5995.00"))
                .andExpect(jsonPath("$.items[0].puedeDecidir").value(true))
                .andExpect(jsonPath("$.proximoId").value(ORDEN.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        // Respuesta minima (96.5.3): ni destino ni datos de la persona viajan en la bandeja.
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("instrumento")
                .doesNotContain("referenciaProveedor");
    }

    @Test
    @DisplayName("200: sin limite usa el default del contrato (25) y la ultima pagina no trae cursor")
    void limitePorOmision() throws Exception {
        when(bandeja.enRevision(any(), any(), eq(25), any())).thenReturn(unaFila(false));

        mvc.perform(get("/billetera/retiros").with(Sesiones.como("TESORERIA", "RETIRO_APROBAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximoId").doesNotExist());
        verify(bandeja).enRevision(eq(Optional.empty()), eq(Optional.empty()), eq(25), any());
    }

    @Test
    @DisplayName("403: quien opera su billetera (BILLETERA_OPERAR) no ve la cola de nadie")
    void sinElPermisoNoVe() throws Exception {
        mvc.perform(get("/billetera/retiros").with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        verifyNoInteractions(bandeja);
    }

    @Test
    @DisplayName("401: sin token no hay bandeja")
    void sinToken() throws Exception {
        mvc.perform(get("/billetera/retiros")).andExpect(status().isUnauthorized());
        verifyNoInteractions(bandeja);
    }

    @Test
    @DisplayName("400: limite fuera de 1..100 lo corta el contrato antes del caso de uso")
    void limiteFueraDeRango() throws Exception {
        mvc.perform(get("/billetera/retiros").param("limite", "0").with(Sesiones.como("TESORERIA", "RETIRO_APROBAR")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/billetera/retiros").param("limite", "101").with(Sesiones.como("TESORERIA", "RETIRO_APROBAR")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bandeja);
    }

    @Test
    @DisplayName("422: el cursor a medias sale con AP-CU11-14, nunca como 500")
    void cursorIncompleto() throws Exception {
        when(bandeja.enRevision(any(), any(), any(Integer.class), any()))
                .thenThrow(new bo.aportaya.plataforma.dominio.ErrorDeNegocio(
                        bo.aportaya.plataforma.dominio.CodigoError.de(11, 14), "El cursor de la bandeja va completo."));

        mvc.perform(get("/billetera/retiros")
                        .param("despuesDeId", ORDEN.toString())
                        .with(Sesiones.como("TESORERIA", "RETIRO_APROBAR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU11-14"));
    }

    @Test
    @DisplayName("400: un despuesDeId que no es UUID es entrada invalida, no un 500")
    void cursorMalFormado() throws Exception {
        mvc.perform(get("/billetera/retiros")
                        .param("despuesDeId", "no-es-uuid")
                        .with(Sesiones.como("TESORERIA", "RETIRO_APROBAR")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bandeja);
    }
}
