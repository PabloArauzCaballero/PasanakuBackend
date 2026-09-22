package bo.aportaya.plataforma.pruebas.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * La sabana de seguridad de un servicio entero: <b>todas</b> sus rutas, de una vez.
 *
 * <p>Un servicio la hereda declarando sus dobles y hereda con ella tres afirmaciones
 * que no se pueden olvidar de escribir para un endpoint nuevo, porque no se escriben
 * por endpoint. Es la diferencia entre una matriz de seguridad que hay que acordarse de
 * mantener y una que crece sola con el servicio.
 *
 * <p>La sesion del {@code 403} usa un rol que el catalogo no tiene
 * ({@link Sesiones#ROL_SIN_NADA}) y ningun permiso. Con un rol real la sabana pasaria
 * por casualidad justo en los endpoints de ese rol, que son los que mas importan.
 */
public abstract class SabanaDeSeguridadWeb {

    /** Lo que una respuesta de error jamas puede contener (plan §40, §98). */
    private static final List<String> NUNCA_EN_UN_ERROR = List.of(
            "java.lang",
            "java.util",
            "org.springframework",
            "org.postgresql",
            "org.jooq",
            "at bo.aportaya",
            "Caused by",
            "SQLException",
            "password",
            "contrasena",
            "passwordHash",
            "hash_busqueda",
            "pimienta",
            "refreshToken",
            "jdbc:");

    @Autowired
    protected MockMvc mvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    protected RequestMappingHandlerMapping rutas;

    @Test
    @DisplayName("seguridad: el servicio expone al menos una ruta, y todas se pueden inspeccionar")
    void hayRutasQueProteger() {
        // Sin esto, un contexto que no cargo ningun controlador dejaria las tres
        // pruebas de abajo verdes por recorrer una lista vacia. Una sabana que no
        // barre nada es peor que no tenerla: dice que barrio.
        assertThat(Endpoint.de(rutas))
                .as("el corte no cargo ningun controlador")
                .isNotEmpty();
    }

    @Test
    @DisplayName("seguridad: sin sesion, toda ruta que no sea @Publico responde 401")
    void sinSesionNadieEntra() throws Exception {
        for (Endpoint endpoint : Endpoint.conSesion(rutas)) {
            var respuesta = pedir(endpoint, null);
            assertThat(respuesta.getStatus())
                    .as("%s sin token tendria que ser 401", endpoint)
                    .isEqualTo(401);
            sinFiltrarNadaInterno(endpoint, respuesta.getContentAsString());
        }
    }

    @Test
    @DisplayName("seguridad: autenticado sin el permiso, toda ruta con @Permiso responde 403")
    void sinPermisoNadieOpera() throws Exception {
        List<Endpoint> exigentes = Endpoint.conPermiso(rutas);
        assertThat(exigentes)
                .as("ninguna ruta declara @Permiso: revisar los controladores")
                .isNotEmpty();

        for (Endpoint endpoint : exigentes) {
            var respuesta = pedir(endpoint, Sesiones.sinPermisos());
            assertThat(respuesta.getStatus())
                    .as("%s con sesion pero sin %s tendria que ser 403", endpoint, endpoint.permiso())
                    .isEqualTo(403);
            sinFiltrarNadaInterno(endpoint, respuesta.getContentAsString());
        }
    }

    @Test
    @DisplayName("seguridad: 401 y 403 no distinguen entre un recurso que existe y uno que no")
    void laNegativaNoCuentaNada() throws Exception {
        for (Endpoint endpoint : Endpoint.conPermiso(rutas)) {
            String cuerpo = pedir(endpoint, Sesiones.sinPermisos()).getContentAsString();
            assertThat(cuerpo)
                    .as("%s explica por que niega, y explicarlo ya cuenta que el recurso existe", endpoint)
                    .doesNotContain(endpoint.permiso());
        }
    }

    @Test
    @DisplayName("seguridad: el permiso de UNA operacion no abre las de al lado")
    void unPermisoNoAbreOtro() throws Exception {
        // El barrido cruzado. `sinPermisoNadieOpera` prueba que sin nada no se entra;
        // este prueba lo que de verdad pasa en produccion: una sesion legitima, con
        // permisos legitimos, que no incluyen el de ESTA operacion. Es la diferencia
        // entre «la guardia existe» y «la guardia distingue».
        //
        // Los permisos de al lado se leen del mapeo, no de una lista: un endpoint nuevo
        // entra al barrido cruzado de todos los demas el dia que se escribe.
        var todos = Endpoint.permisosDelServicio(rutas);
        if (todos.size() < 2) {
            // Un servicio con un solo permiso no tiene «al lado». No es un fallo, y
            // afirmarlo con una lista de uno seria afirmar nada.
            return;
        }

        for (Endpoint endpoint : Endpoint.conPermiso(rutas)) {
            String[] ajenos =
                    todos.stream().filter(p -> !p.equals(endpoint.permiso())).toArray(String[]::new);

            var respuesta = pedir(endpoint, Sesiones.de(Sesiones.USUARIO, Sesiones.ROL_SIN_NADA, ajenos));

            assertThat(respuesta.getStatus())
                    .as(
                            "%s se abrio con los permisos de OTRAS operaciones del servicio (%s); exige %s",
                            endpoint, String.join(", ", ajenos), endpoint.permiso())
                    .isEqualTo(403);
        }
    }

    @Test
    @DisplayName("seguridad: sin Idempotency-Key, ninguna operacion con efecto se ejecuta")
    void sinClaveNoHayEfecto() throws Exception {
        // Reintentar tiene que ser seguro, y lo que lo hace seguro es la clave. Un
        // endpoint que la declara obligatoria y la acepta ausente convierte cada
        // timeout de red en un efecto duplicado — un pago de mas, un alta de mas.
        //
        // Se pide CON el permiso correcto a proposito: un 403 tambien seria «no se
        // ejecuto», y taparia que la clave no se estaba exigiendo.
        for (Endpoint endpoint : Endpoint.conClave(rutas)) {
            // El alta de usuario tambien exige clave y es @Publico: ahi no hay sesion
            // que armar, y armarla con un permiso nulo era el fallo.
            var sesion = endpoint.publico()
                    ? null
                    : Sesiones.de(Sesiones.USUARIO, Sesiones.ROL_SIN_NADA, endpoint.permiso());

            var respuesta = pedir(endpoint, sesion);

            assertThat(respuesta.getStatus())
                    .as("%s exige Idempotency-Key y sin ella tendria que ser 400", endpoint)
                    .isEqualTo(400);
        }
    }

    private org.springframework.mock.web.MockHttpServletResponse pedir(Endpoint endpoint, RequestPostProcessor sesion)
            throws Exception {
        var peticion = request(HttpMethod.valueOf(endpoint.metodo()), endpoint.ruta());
        if (llevaCuerpo(endpoint.metodo())) {
            // Con tipo de contenido y un cuerpo vacio a proposito. Sin esto la
            // condicion `consumes` del mapeo devuelve 415 ANTES de que corra la
            // guardia, y la sabana estaria comprobando que un endpoint protegido
            // rechaza texto plano — no que niega a quien no tiene permiso.
            if (endpoint.recibeArchivo()) {
                // Un endpoint que recibe un archivo responde 415 a cualquier JSON, y la
                // sabana terminaba comprobando que rechaza el tipo equivocado en vez de
                // que exige la clave de idempotencia. Se le habla en su tipo.
                peticion.contentType(MediaType.MULTIPART_FORM_DATA).content(new byte[0]);
            } else {
                peticion.contentType(MediaType.APPLICATION_JSON).content("{}");
            }
        }
        if (sesion != null) {
            peticion.with(sesion);
        }
        return mvc.perform(peticion).andReturn().getResponse();
    }

    private static boolean llevaCuerpo(String metodo) {
        return "POST".equals(metodo) || "PUT".equals(metodo) || "PATCH".equals(metodo);
    }

    private static void sinFiltrarNadaInterno(Endpoint endpoint, String cuerpo) {
        for (String prohibido : NUNCA_EN_UN_ERROR) {
            assertThat(cuerpo)
                    .as("la respuesta de %s filtra «%s»", endpoint, prohibido)
                    .doesNotContain(prohibido);
        }
    }
}
