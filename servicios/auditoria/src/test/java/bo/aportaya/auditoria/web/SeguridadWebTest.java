package bo.aportaya.auditoria.web;

import bo.aportaya.auditoria.aplicacion.CU07EjercerDerechos;
import bo.aportaya.auditoria.aplicacion.CU58DescargarExportacion;
import bo.aportaya.auditoria.aplicacion.CU58EjecutarReporte;
import bo.aportaya.auditoria.aplicacion.CU98PublicarTablero;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.SabanaDeSeguridadWeb;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Todas las rutas de auditoria, barridas de una vez.
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
    private CU07EjercerDerechos cu07EjercerDerechos;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.EndpointsDelDia endpointsDelDia;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CatalogoDeDatosVigente catalogoDeDatos;

    // operacion/estado comparte controlador: no se llama aca.
    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.SaludDeServicios saludDeServicios;

    // CU-130: el mismo controlador guarda la ficha de gobierno.
    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU130GobernarFichaDeTabla cu130;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU131RevisarColumna cu131;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.ConsultarCatalogoDeDatos consultarCatalogo;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.ConsultarIncidentesOperativos consultarIncidentes;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.OutboxPorServicio outboxPorServicio;

    @MockitoBean
    private CU58DescargarExportacion cu58DescargarExportacion;

    @MockitoBean
    private CU58EjecutarReporte cu58EjecutarReporte;

    @MockitoBean
    private CU98PublicarTablero cu98PublicarTablero;

    // El controlador tambien lista el catalogo y el historial (CU-58, carril A · A6).
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private bo.aportaya.auditoria.aplicacion.ConsultarHistorialDeReportes historialDeReportes;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.ConsultarBitacora consultarBitacora;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.GestionarCatalogosOperativos catalogosOperativos;
}
