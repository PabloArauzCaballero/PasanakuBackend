package bo.aportaya.aportes.web;

import bo.aportaya.aportes.aplicacion.CU19ReembolsarPago;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.CU99EnrutarProveedor;
import bo.aportaya.aportes.aplicacion.ConsultarEstadoDelParticipante;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.SabanaDeSeguridadWeb;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Todas las rutas de aportes, barridas de una vez.
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
    private CU19ReembolsarPago cu19ReembolsarPago;

    @MockitoBean
    private CU21CobrarAporte cu21CobrarAporte;

    @MockitoBean
    private CU99EnrutarProveedor cu99EnrutarProveedor;

    @MockitoBean
    private ConsultarEstadoDelParticipante consultarEstadoDelParticipante;
}
