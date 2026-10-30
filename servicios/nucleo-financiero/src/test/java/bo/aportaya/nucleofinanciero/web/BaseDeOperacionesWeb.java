package bo.aportaya.nucleofinanciero.web;

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
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tableros · H8.S3.M4 — lo común de las pruebas web de las operaciones de billetera que la matriz mostraba sin
 * cubrir: los dobles del controlador (FondeoDeLaBilletera es el real, con sus colaboradores doblados), las constantes
 * y el armado de una petición con sesión y clave de idempotencia. {@code @PruebaWeb} va en cada clase concreta: no se
 * hereda.
 */
abstract class BaseDeOperacionesWeb {
    protected static final UUID CUENTA = UUID.fromString("eeeeeeee-0000-4000-8000-000000000001");
    protected static final UUID INSTRUMENTO = UUID.fromString("eeeeeeee-0000-4000-8000-000000000005");
    protected static final UUID RETENCION = UUID.fromString("eeeeeeee-0000-4000-8000-000000000006");
    protected static final String CLAVE = "eeeeeeee-0000-4000-8000-0000000000ff";
    // El contrato pide 20 caracteres como mínimo a la evidencia (un JWT) y 6 a 8 al factor viejo.
    protected static final String EVIDENCIA = "eyJhbGciOiJFUzI1NiJ9.evidencia-de-prueba";

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected ConsultarSaldo saldos;

    @MockitoBean
    protected CU10RecargarSaldo cu10;

    @MockitoBean
    protected RecargasConProveedor recargas;

    @MockitoBean
    protected CU11RetirarSaldo cu11;

    @MockitoBean
    protected ConsultarMovimientosBilletera consultasDeMovimientos;

    @MockitoBean
    protected CU13RetenerSaldo cu13;

    @MockitoBean
    protected CU14ReversarTransaccion cu14;

    @MockitoBean
    protected CU15EmitirExtracto cu15;

    @MockitoBean
    protected CU17BloquearPorAutoridad cu17;

    @MockitoBean
    protected MovimientosDeLaBilletera movimientos;

    @MockitoBean
    protected CotizadorDeComision cotizador;

    @MockitoBean
    protected SegundoFactor segundoFactor;

    @MockitoBean
    protected BandejaDeRetiros bandeja;

    @MockitoBean
    protected PagoDeRecargaDePrueba pagoDePrueba;

    @MockitoBean
    protected CotizarOperacion cotizaciones;

    @MockitoBean
    protected CuentaPropia cuentaPropia;

    @MockitoBean
    protected OrdenesExistentes existentes;

    @MockitoBean
    protected QrDeLaBilletera qrDeLaBilletera;

    protected static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    protected static MockHttpServletRequestBuilder operar(
            MockHttpServletRequestBuilder pedido, String cuerpo, String... permisos) {
        return pedido.with(Sesiones.como("PARTICIPANTE", permisos))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }
}
