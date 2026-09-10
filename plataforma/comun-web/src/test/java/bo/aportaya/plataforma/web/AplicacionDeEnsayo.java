package bo.aportaya.plataforma.web;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * El servicio que {@code comun-web} no tiene.
 *
 * <p>Un corte MVC arranca buscando hacia arriba una clase de configuracion de Spring
 * Boot, y este modulo es una libreria: no tiene ninguna, porque no se despliega. Sin
 * esta clase las pruebas de la capa web solo podrian escribirse dentro de un servicio,
 * y entonces el manejador global de errores —que es de los catorce— se probaria en uno
 * y por casualidad.
 *
 * <p>Vive en las fuentes de prueba. No se empaqueta, no se despliega y ningun servicio
 * la ve.
 */
@SpringBootApplication
public class AplicacionDeEnsayo {}
