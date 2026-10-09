package bo.aportaya.organizador.web;

import bo.aportaya.organizador.aplicacion.CU90PostularOrganizador;
import bo.aportaya.organizador.aplicacion.CU91FirmarContrato;
import bo.aportaya.organizador.aplicacion.CU92EvaluarDesempeno;
import bo.aportaya.organizador.aplicacion.CU93SancionarOrganizador;
import bo.aportaya.organizador.aplicacion.CU95DefinirAutomatizacion;
import bo.aportaya.organizador.aplicacion.CU96EjecutarTarea;
import bo.aportaya.organizador.aplicacion.ConsultarHabilitacion;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.SabanaDeSeguridadWeb;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Todas las rutas de organizador, barridas de una vez.
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
    private CU90PostularOrganizador cu90PostularOrganizador;

    @MockitoBean
    private CU91FirmarContrato cu91FirmarContrato;

    @MockitoBean
    private CU92EvaluarDesempeno cu92EvaluarDesempeno;

    @MockitoBean
    private CU93SancionarOrganizador cu93SancionarOrganizador;

    @MockitoBean
    private CU95DefinirAutomatizacion cu95DefinirAutomatizacion;

    @MockitoBean
    private CU96EjecutarTarea cu96EjecutarTarea;

    @MockitoBean
    private ConsultarHabilitacion consultarHabilitacion;
}
