package bo.aportaya.garantia.web;

import bo.aportaya.garantia.aplicacion.CU23CubrirIncumplimiento;
import bo.aportaya.garantia.aplicacion.CU25DeclararIncumplimiento;
import bo.aportaya.garantia.aplicacion.CU26EjecutarAval;
import bo.aportaya.garantia.aplicacion.CU27RestringirDeudor;
import bo.aportaya.garantia.aplicacion.CU29DevolverFondo;
import bo.aportaya.garantia.aplicacion.CU66ReemplazarParticipante;
import bo.aportaya.garantia.aplicacion.CU67DisolverGrupo;
import bo.aportaya.garantia.aplicacion.ConsultarRestriccion;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.SabanaDeSeguridadWeb;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Todas las rutas de garantia, barridas de una vez.
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
    private CU23CubrirIncumplimiento cu23CubrirIncumplimiento;

    @MockitoBean
    private CU25DeclararIncumplimiento cu25DeclararIncumplimiento;

    @MockitoBean
    private CU26EjecutarAval cu26EjecutarAval;

    @MockitoBean
    private CU27RestringirDeudor cu27RestringirDeudor;

    @MockitoBean
    private CU29DevolverFondo cu29DevolverFondo;

    @MockitoBean
    private CU66ReemplazarParticipante cu66ReemplazarParticipante;

    @MockitoBean
    private CU67DisolverGrupo cu67DisolverGrupo;

    @MockitoBean
    private ConsultarRestriccion consultarRestriccion;
}
