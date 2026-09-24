package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.BuscarPorTelefono;
import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario;
import bo.aportaya.identidad.aplicacion.CU02GuardarFotoDelExpediente;
import bo.aportaya.identidad.aplicacion.CU02RevisarExpediente;
import bo.aportaya.identidad.aplicacion.CU04Autenticar;
import bo.aportaya.identidad.aplicacion.EmitirAcceso;
import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.ValidarTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.SabanaDeSeguridadWeb;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Todas las rutas de identidad, barridas de una vez.
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
            "aportaya.acceso.duracion-bloqueo=PT15M",
            "aportaya.acceso.intentos-maximos=3",
            "aportaya.acceso.vigencia-sesion=PT15M",
            "aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion",
        })
class SeguridadWebTest extends SabanaDeSeguridadWeb {

    // La subida de la foto del expediente es dependencia de UsuariosController desde
    // que existe el portal de riesgo: sin doblarla, el contexto de esta prueba no levanta.
    // El portal de riesgo tambien cuelga de la sabana de seguridad: sus rutas son las
    // que miran cedulas ajenas.
    @MockitoBean
    private CU02RevisarExpediente revisarExpediente;

    @MockitoBean
    private CU02GuardarFotoDelExpediente guardarFoto;

    @MockitoBean
    private BuscarPorTelefono buscarPorTelefono;

    @MockitoBean
    private CU01RegistrarUsuario cu01RegistrarUsuario;

    @MockitoBean
    private CU04Autenticar cu04Autenticar;

    @MockitoBean
    private EmitirAcceso emitirAcceso;

    @MockitoBean
    private EmitirTokenDeInvitacion emitirTokenDeInvitacion;

    @MockitoBean
    private ValidarTokenDeInvitacion validarTokenDeInvitacion;

    @MockitoBean
    private VerificarTitularidad verificarTitularidad;
}
