package bo.aportaya.transparencia.web;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.SabanaDeSeguridadWeb;
import bo.aportaya.transparencia.aplicacion.CU61VerificarSorteo;
import bo.aportaya.transparencia.aplicacion.CU70RegistrarEventoReputacion;
import bo.aportaya.transparencia.aplicacion.CU71RecalcularPuntaje;
import bo.aportaya.transparencia.aplicacion.CU72SellarBloque;
import bo.aportaya.transparencia.aplicacion.CU73VerificarCadena;
import bo.aportaya.transparencia.aplicacion.CU74EvaluarInsignias;
import bo.aportaya.transparencia.aplicacion.CU75EmitirCertificado;
import bo.aportaya.transparencia.aplicacion.CU76PublicarResena;
import bo.aportaya.transparencia.aplicacion.CU97EvaluarRiesgo;
import bo.aportaya.transparencia.aplicacion.ConsultarPuntaje;
import bo.aportaya.transparencia.aplicacion.ListarBloques;
import bo.aportaya.transparencia.dominio.puertos.PaquetesDeSorteo;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Todas las rutas de transparencia, barridas de una vez.
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
    private CU61VerificarSorteo cu61VerificarSorteo;

    @MockitoBean
    private CU70RegistrarEventoReputacion cu70RegistrarEventoReputacion;

    @MockitoBean
    private CU71RecalcularPuntaje cu71RecalcularPuntaje;

    @MockitoBean
    private CU72SellarBloque cu72SellarBloque;

    @MockitoBean
    private CU73VerificarCadena cu73VerificarCadena;

    @MockitoBean
    private CU74EvaluarInsignias cu74EvaluarInsignias;

    @MockitoBean
    private CU75EmitirCertificado cu75EmitirCertificado;

    @MockitoBean
    private CU76PublicarResena cu76PublicarResena;

    @MockitoBean
    private CU97EvaluarRiesgo cu97EvaluarRiesgo;

    @MockitoBean
    private ConsultarPuntaje consultarPuntaje;

    @MockitoBean
    private ListarBloques listarBloques;

    @MockitoBean
    private PaquetesDeSorteo paquetesDeSorteo;
}
