package bo.aportaya.gateway;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * {@code GET /api/v1/version}: la unica respuesta que el gateway compone por su cuenta.
 *
 * <p>Existe porque un cliente necesita una forma barata de preguntar «¿llego al backend?»
 * sin pedir datos de nadie. La app movil ya lo consultaba —es su sonda de conectividad— y
 * recibia un 404: funcionaba de casualidad, porque la sonda trata cualquier respuesta por
 * debajo de 500 como señal de que hay conexion. Un 404 que en realidad significa «todo
 * bien» es exactamente la clase de cosa que despues nadie sabe leer.
 *
 * <p>No dice que version corre: ni el commit ni el numero de build. Eso es informacion
 * util para quien ataca y de ningun valor para quien solo quiere saber si hay linea. El
 * cuerpo es el mismo siempre, y el punto es que llegue.
 *
 * <p>Ruta publica y sin autenticacion, como corresponde a una sonda; no toca la base ni
 * llama a ningun servicio, asi que responder no cuesta nada.
 */
@Configuration
public class Version {

    @Bean
    public RouterFunction<ServerResponse> rutaDeVersion() {
        return route(
                GET("/api/v1/version"),
                peticion -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("servicio", "gateway", "estado", "en linea")));
    }
}
