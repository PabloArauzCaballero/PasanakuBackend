package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU62Permutar;
import bo.aportaya.grupos.aplicacion.CU63Acordar;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Enlace;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.SabanaDeSeguridadWeb;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Todas las rutas de grupos, barridas de una vez.
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
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
        })
class SeguridadWebTest extends SabanaDeSeguridadWeb {

    @MockitoBean
    private CU20CrearGrupo cu20CrearGrupo;

    @MockitoBean
    private CU59CalcularPlazo cu59CalcularPlazo;

    @MockitoBean
    private CU62Permutar cu62Permutar;

    @MockitoBean
    private CU63Acordar cu63Acordar;

    @MockitoBean
    private CU64TraspasarCupo cu64TraspasarCupo;

    @MockitoBean
    private CU65Retirarse cu65Retirarse;

    @MockitoBean
    private CU68Postular cu68Postular;

    @MockitoBean
    private CU69Invitar cu69Invitar;

    @MockitoBean
    private CU69Enlace enlaces;

    @MockitoBean
    private Consultas consultas;

    @MockitoBean
    private HechosDeOtrosServicios hechosDeOtrosServicios;

    @MockitoBean
    private RespuestasAOtrosServicios respuestasAOtrosServicios;

    @MockitoBean
    private SorteoDelGrupo sorteoDelGrupo;
}
