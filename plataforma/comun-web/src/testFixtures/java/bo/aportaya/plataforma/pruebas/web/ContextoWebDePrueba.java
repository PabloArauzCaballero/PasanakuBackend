package bo.aportaya.plataforma.pruebas.web;

import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.web.dinero.ConfiguracionDeDinero;
import bo.aportaya.plataforma.web.errores.ManejadorGlobalDeErrores;
import bo.aportaya.plataforma.web.errores.TraduccionDeRestricciones;
import bo.aportaya.plataforma.web.seguridad.ConfiguracionDeSeguridad;
import bo.aportaya.plataforma.web.seguridad.GuardiaDePermiso;
import bo.aportaya.plataforma.web.seguridad.RegistroDeGuardias;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.FiltroDeTraza;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Lo que un corte MVC necesita de {@code comun-web}, y nada mas.
 *
 * <p>Se importan las piezas <b>una por una</b> en vez de traer
 * {@code ConfiguracionComunWeb} entera. No es preferencia: esa configuracion barre
 * todo {@code bo.aportaya.plataforma.web}, y ahi vive {@code SaludDeLaBase}, que pide
 * un {@code DataSource}. Traerla obligaria a cada prueba de contrato HTTP a levantar
 * una base para probar un {@code 400}, y entonces la capa rapida deja de ser rapida y
 * nadie la corre.
 *
 * <p><b>Por que este paquete y no {@code ...web.pruebas}.</b> Estaba ahi, y
 * {@code ConfiguracionComunWeb} barre {@code bo.aportaya.plataforma.web} entero: en
 * cuanto un servicio ponia los fixtures en su classpath de prueba, este
 * {@code @TestConfiguration} entraba tambien al contexto REAL de {@code ArranqueTest} y
 * su {@code JwtDecoder} de mentira competia con el de verdad. El proceso no levantaba.
 * Vivir fuera del arbol que el barrido alcanza es lo que impide que un arnes de prueba
 * se cuele en un contexto de produccion.
 *
 * <p>Lo que SI entra es exactamente lo que decide una respuesta HTTP: la guardia por
 * omision, la guardia de permiso, el manejador global de errores, la traza y la
 * frontera del dinero. Si manana una respuesta depende de otra pieza, se agrega aca
 * — y que haya que agregarla es la señal de que la lista es honesta.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({
    ConfiguracionDeSeguridad.class,
    ConfiguracionDeDinero.class,
    SesionDeLaPeticion.class,
    GuardiaDePermiso.class,
    RegistroDeGuardias.class,
    ManejadorGlobalDeErrores.class,
    FiltroDeTraza.class,
})
public class ContextoWebDePrueba {

    /**
     * El instante en el que ocurre todo lo que pasa en un corte MVC.
     *
     * <p>Parado a proposito (plan §25, §54): un controlador que ponga
     * {@code Instant.now()} en una respuesta hace fallar la prueba el dia que el borde
     * caiga mal, en la maquina de otro y sin explicacion. Con el reloj quieto, ese
     * defecto se ve al escribirlo.
     */
    public static final java.time.Instant AHORA = java.time.Instant.parse("2026-03-15T14:30:00Z");

    @Bean
    public Reloj reloj() {
        return Reloj.fijo(AHORA);
    }

    @Bean
    public TraduccionDeRestricciones traduccionDeRestricciones() {
        // El catalogo real, generado desde sql/. Un catalogo de mentira dejaria pasar
        // una traduccion que en produccion no existe, y el sintoma seria un 500 en vez
        // del 409 que la prueba dice que hay.
        return TraduccionDeRestricciones.cargar();
    }

    /**
     * Un decodificador que nunca se usa.
     *
     * <p>La cadena de seguridad declara {@code oauth2ResourceServer(jwt)} y por eso el
     * contexto exige el bean; las pruebas fabrican la sesion con
     * {@link Sesiones}, que escribe el {@code SecurityContext} directamente y no pasa
     * por aca. Que el decodificador de verdad este bien cableado lo prueba
     * {@code ArranqueTest} de cada servicio, contra su configuracion real.
     *
     * <p>Falla si alguien lo llama, en vez de devolver un token: un decodificador de
     * prueba que acepta cualquier cosa es una forma silenciosa de probar sin seguridad.
     */
    @Bean
    public JwtDecoder decodificadorQueNoSeUsa() {
        return token -> {
            throw new IllegalStateException(
                    "La prueba mando un token de verdad. El corte MVC arma la sesion con Sesiones, "
                            + "no con un JWT firmado: si hace falta firmar, la prueba es de ArranqueTest.");
        };
    }
}
