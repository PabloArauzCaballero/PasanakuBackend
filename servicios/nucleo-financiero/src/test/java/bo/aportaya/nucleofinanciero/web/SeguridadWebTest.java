package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion;
import bo.aportaya.nucleofinanciero.aplicacion.CU15EmitirExtracto;
import bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarSaldo;
import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.nucleofinanciero.dominio.puertos.SegundoFactor;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.SabanaDeSeguridadWeb;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Todas las rutas de nucleo-financiero, barridas de una vez.
 *
 * <p>Sin controladores declarados: el corte carga <b>todos</b> los del servicio, y por
 * eso un controlador nuevo entra a la sabana el dia que se escribe, sin que nadie tenga
 * que acordarse de agregarlo aca. Es la unica forma de que una matriz de seguridad no
 * envejezca.
 *
 * <p>Los casos de uso van doblados porque lo que se prueba es la guardia, no el caso:
 * la peticion tiene que morir en el {@code 401} o el {@code 403} <b>antes</b> de
 * llegar a ellos, y un doble que nunca se llama es la prueba de que asi fue.
 */
@PruebaWeb(
        properties = {
            "aportaya.retiro.doble-aprobacion-desde=5000.00",
        })
class SeguridadWebTest extends SabanaDeSeguridadWeb {

    @MockitoBean
    private CU10RecargarSaldo cu10RecargarSaldo;

    @MockitoBean
    private CU11RetirarSaldo cu11RetirarSaldo;

    @MockitoBean
    private CU13RetenerSaldo cu13RetenerSaldo;

    @MockitoBean
    private CU14ReversarTransaccion cu14ReversarTransaccion;

    @MockitoBean
    private CU15EmitirExtracto cu15EmitirExtracto;

    @MockitoBean
    private CU17BloquearPorAutoridad cu17BloquearPorAutoridad;

    @MockitoBean
    private ConsultarSaldo consultarSaldo;

    @MockitoBean
    private CotizadorDeComision cotizadorDeComision;

    @MockitoBean
    private MovimientosDeLaBilletera movimientosDeLaBilletera;

    @MockitoBean
    private SegundoFactor segundoFactor;
}
