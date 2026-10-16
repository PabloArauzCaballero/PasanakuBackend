package bo.aportaya.plataforma.pruebas;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Las pruebas de un servicio arman el contexto con un Kafka que NO existe ({@code localhost:9092}) y
 * miran el outbox como una tabla: cuentan filas, no esperan que alguien las publique. Con el relevo
 * del outbox funcionando de verdad (carril G: antes no existia en ningun servicio) un contexto de prueba
 * se pondria a publicar contra un broker inexistente, a tomar y devolver filas y a esperar timeouts.
 *
 * <p>Este post-procesador pone {@code aportaya.outbox.habilitado=false} con la PRECEDENCIA MAS BAJA:
 * una prueba que quiera el relevo lo enciende con su propia propiedad, y {@code application.yml} o el
 * entorno de un despliegue ganan siempre. Solo viaja en el classpath de pruebas ({@code comun-pruebas}
 * jamas es una dependencia de runtime), asi que no cambia nada en un servicio desplegado.
 */
public final class SinRelevoEnPruebas implements EnvironmentPostProcessor {

    static final String FUENTE = "aportaya-sin-relevo-en-pruebas";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment entorno, SpringApplication aplicacion) {
        if (entorno.getPropertySources().contains(FUENTE)) {
            return;
        }
        entorno.getPropertySources()
                .addLast(new MapPropertySource(FUENTE, Map.of("aportaya.outbox.habilitado", "false")));
    }
}
