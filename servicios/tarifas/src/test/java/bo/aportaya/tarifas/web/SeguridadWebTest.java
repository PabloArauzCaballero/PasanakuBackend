package bo.aportaya.tarifas.web;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.SabanaDeSeguridadWeb;
import bo.aportaya.tarifas.aplicacion.CU30CotizarComision;
import bo.aportaya.tarifas.aplicacion.CU31DevengarComision;
import bo.aportaya.tarifas.aplicacion.CU32EmitirFactura;
import bo.aportaya.tarifas.aplicacion.CU33DevolverComision;
import bo.aportaya.tarifas.aplicacion.CU34PublicarTarifario;
import bo.aportaya.tarifas.aplicacion.CU35CerrarLiquidacion;
import bo.aportaya.tarifas.aplicacion.CU36ResolverPrecio;
import bo.aportaya.tarifas.aplicacion.ConsultarTarifarioVigente;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Todas las rutas de tarifas, barridas de una vez.
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
@PruebaWeb
class SeguridadWebTest extends SabanaDeSeguridadWeb {

    @MockitoBean
    private CU30CotizarComision cu30CotizarComision;

    @MockitoBean
    private CU31DevengarComision cu31DevengarComision;

    @MockitoBean
    private CU32EmitirFactura cu32EmitirFactura;

    @MockitoBean
    private CU33DevolverComision cu33DevolverComision;

    @MockitoBean
    private CU34PublicarTarifario cu34PublicarTarifario;

    @MockitoBean
    private CU35CerrarLiquidacion cu35CerrarLiquidacion;

    @MockitoBean
    private CU36ResolverPrecio cu36ResolverPrecio;

    @MockitoBean
    private ConsultarTarifarioVigente consultarTarifarioVigente;
}
