package bo.aportaya.inversiones.web;

import bo.aportaya.inversiones.aplicacion.CU120Catalogo;
import bo.aportaya.inversiones.aplicacion.CU121OrdenarInversion;
import bo.aportaya.inversiones.aplicacion.CU122ConfirmarPosicion;
import bo.aportaya.inversiones.aplicacion.CU123Devengo;
import bo.aportaya.inversiones.aplicacion.CU123Posiciones;
import bo.aportaya.inversiones.aplicacion.CU124SolicitarRescate;
import bo.aportaya.inversiones.aplicacion.CU125LiquidarRescate;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.SabanaDeSeguridadWeb;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Todas las rutas de inversiones, barridas de una vez: sin sesion es 401 y con una sesion
 * sin permisos es 403, y los casos de uso doblados nunca se llaman (la peticion muere en la
 * guardia). Sin controladores declarados: uno nuevo entra a la sabana el dia que se escribe.
 */
@PruebaWeb
class SeguridadWebTest extends SabanaDeSeguridadWeb {

    @MockitoBean
    private CU120Catalogo cu120;

    @MockitoBean
    private CU121OrdenarInversion cu121;

    @MockitoBean
    private CU122ConfirmarPosicion cu122;

    @MockitoBean
    private CU123Posiciones cu123;

    @MockitoBean
    private CU123Devengo cu123Devengo;

    @MockitoBean
    private CU124SolicitarRescate cu124;

    @MockitoBean
    private CU125LiquidarRescate cu125;
}
