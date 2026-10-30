package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
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
import bo.aportaya.nucleofinanciero.aplicacion.PagoDeRecargaDePrueba;
import bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor;
import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.nucleofinanciero.dominio.puertos.SegundoFactor;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Carril G · {@code GET /billetera/cuentas/propia}: la billetera operativa de quien pregunta.
 *
 * <p>Lo que se fija: el usuario sale de la SESION (no hay parametro para preguntar por otra persona), la moneda
 * es del contrato, y sin billetera activa el rechazo es de negocio. La consulta contra la base real, con la
 * fila de otra persona que no se ve, esta en {@code CuentaPropiaIntegracionTest}.
 */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
@org.springframework.context.annotation.Import(FondeoDeLaBilletera.class)
@org.springframework.test.context.bean.override.mockito.MockitoBean(
        types = bo.aportaya.nucleofinanciero.aplicacion.CU13LiberarRetencionDelMercado.class)
class CuentaPropiaWebTest {

    private static final UUID CUENTA = UUID.fromString("eeeeeeee-0000-4000-8000-0000000000a1");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CuentaPropia cuentaPropia;

    @MockitoBean
    private ConsultarSaldo saldos;

    @MockitoBean
    private CU10RecargarSaldo cu10;

    @MockitoBean
    private RecargasConProveedor recargas;

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

    @MockitoBean
    private BandejaDeRetiros bandeja;

    @MockitoBean
    private PagoDeRecargaDePrueba pagoDePrueba;

    @MockitoBean
    private CotizarOperacion cotizaciones;

    @MockitoBean
    private OrdenesExistentes existentes;

    @MockitoBean
    private QrDeLaBilletera qrDeLaBilletera;

    @Test
    @DisplayName("200 con la cuenta de la sesion y su moneda; la consulta va con el usuario del TOKEN")
    void devuelveLaCuentaDeLaSesion() throws Exception {
        when(cuentaPropia.deLaSesion(any(), any())).thenReturn(CUENTA);
        mvc.perform(get("/billetera/cuentas/propia")
                        .param("moneda", "BOB")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuentaId").value(CUENTA.toString()))
                .andExpect(jsonPath("$.moneda").value("BOB"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
        verify(cuentaPropia).deLaSesion(org.mockito.ArgumentMatchers.eq(Moneda.BOB), any());
    }

    @Test
    @DisplayName("rechaza: una persona sin billetera activa en esa moneda recibe 422 de negocio, no 404 ni 500")
    void sinBilletera() throws Exception {
        when(cuentaPropia.deLaSesion(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(13, 1), "No tenes una billetera activa en USD."));
        mvc.perform(get("/billetera/cuentas/propia")
                        .param("moneda", "USD")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU13-01"));
    }

    @Test
    @DisplayName("rechaza: una moneda que el contrato no declara es 400 y no llega al caso de uso")
    void monedaQueNoEs() throws Exception {
        mvc.perform(get("/billetera/cuentas/propia")
                        .param("moneda", "EUR")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/billetera/cuentas/propia").with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cuentaPropia);
    }

    @Test
    @DisplayName(
            "rechaza: sin sesion es 401 y sin BILLETERA_VER es 403; un servicio no pregunta por la billetera de otro")
    void sinPermiso() throws Exception {
        mvc.perform(get("/billetera/cuentas/propia").param("moneda", "BOB")).andExpect(status().isUnauthorized());
        mvc.perform(get("/billetera/cuentas/propia")
                        .param("moneda", "BOB")
                        .with(Sesiones.como("BACKOFFICE", "SERVICIO_ENTREGAS")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cuentaPropia);
    }
}
