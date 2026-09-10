package bo.aportaya.publicidad.web;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.SabanaDeSeguridadWeb;
import bo.aportaya.publicidad.aplicacion.CU110AltaDeAnunciante;
import bo.aportaya.publicidad.aplicacion.CU111CrearCampana;
import bo.aportaya.publicidad.aplicacion.CU112ModerarPieza;
import bo.aportaya.publicidad.aplicacion.CU113EntregarAnuncio;
import bo.aportaya.publicidad.aplicacion.CU114LiquidarPublicidad;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Todas las rutas de publicidad, barridas de una vez.
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
    private CU110AltaDeAnunciante cu110AltaDeAnunciante;

    @MockitoBean
    private CU111CrearCampana cu111CrearCampana;

    @MockitoBean
    private CU112ModerarPieza cu112ModerarPieza;

    @MockitoBean
    private CU113EntregarAnuncio cu113EntregarAnuncio;

    @MockitoBean
    private CU114LiquidarPublicidad cu114LiquidarPublicidad;
}
