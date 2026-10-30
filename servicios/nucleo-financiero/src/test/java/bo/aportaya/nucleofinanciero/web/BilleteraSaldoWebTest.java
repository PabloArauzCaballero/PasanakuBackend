package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
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
import bo.aportaya.nucleofinanciero.aplicacion.PagoDeRecargaDePrueba;
import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.nucleofinanciero.dominio.puertos.SegundoFactor;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code GET /billetera/{cuentaId}/saldo}: disponible y retenido separados, como cadena decimal.
 *
 * <p>Salio de {@code BilleteraControllerWebTest} cuando esa clase llego al tope de 300 lineas
 * del barrido; las aserciones son las mismas.
 */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
@org.springframework.context.annotation.Import(FondeoDeLaBilletera.class)
@org.springframework.test.context.bean.override.mockito.MockitoBean(
        types = bo.aportaya.nucleofinanciero.aplicacion.CU13LiberarRetencionDelMercado.class)
class BilleteraSaldoWebTest {
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
    private bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor recargas;

    @MockitoBean
    private CU11RetirarSaldo cu11;

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

    // El controlador delega recargas y retiros en FondeoDeLaBilletera: se usa el real, con sus colaboradores
    // doblados, para que "acreditar" siga probando el camino HTTP de punta a punta.
    @MockitoBean
    private bo.aportaya.nucleofinanciero.aplicacion.CotizarOperacion cotizaciones;

    @MockitoBean
    private bo.aportaya.nucleofinanciero.aplicacion.CuentaPropia cuentaPropia;

    @MockitoBean
    private bo.aportaya.nucleofinanciero.aplicacion.OrdenesExistentes existentes;

    @MockitoBean
    private QrDeLaBilletera qrDeLaBilletera;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName("200 con disponible y retenido separados, los dos como cadena decimal")
    void consultarSaldo() throws Exception {
        when(saldos.ejecutar(any(), any()))
                .thenReturn(new ConsultarSaldo.Salida(
                        CUENTA,
                        bob("1234.56"),
                        bob("200.00"),
                        OffsetDateTime.of(2026, 3, 15, 14, 30, 0, 0, ZoneOffset.UTC)));
        mvc.perform(get("/billetera/{id}/saldo", CUENTA).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuentaId").value(CUENTA.toString()))
                // Separados y no sumados: el retenido no se puede gastar, y un
                // «saldo» unico haria creer que si.
                .andExpect(jsonPath("$.disponible.monto").value("1234.56"))
                .andExpect(jsonPath("$.retenido.monto").value("200.00"))
                .andExpect(jsonPath("$.disponible.moneda").value("BOB"));
    }

    @Test
    @DisplayName("400: un identificador de cuenta que no es un identificador")
    void cuentaQueNoEsUuid() throws Exception {
        mvc.perform(get("/billetera/{id}/saldo", "la-cuenta-de-al-lado")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(saldos);
    }
}
