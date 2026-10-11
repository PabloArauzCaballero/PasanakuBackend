package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.TitularDeLaBilletera;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a {@code identidad} si el titular declarado es el de la billetera.
 *
 * <p>Cuando {@code identidad} no responde, la respuesta es <b>no</b>. No es pesimismo:
 * es denegar por omision (invariante 9). Dejar registrar una cuenta porque el servicio
 * que verifica estaba caido es exactamente el hueco por el que entra una cuenta ajena.
 *
 * <p>Con tiempo maximo y circuit breaker ({@link ClienteConCircuito}); con el circuito abierto el veredicto
 * es el mismo «no» sin esperar la red.
 *
 * <p><b>Quien firma (carril I, CU-18).</b> La llamada va con la credencial de SERVICIO de entregas
 * ({@code SERVICIO_ENTREGAS}, emitida por identidad y renovada sola), NO con el token del participante. Antes se
 * propagaba el del participante: {@code verificarTitularidad} exige {@code DATOS_SENSIBLES_LEER} (de quien lee documentos
 * de clientes), el participante no lo tiene, identidad respondia 403 y ese 403 se leia como «no coincide»: NINGUN
 * participante podia registrar su cuenta de destino. Con la credencial de servicio, identidad contesta solo si/no (jamas
 * el dato) y este servicio sigue atando la pregunta a la persona de la sesion en curso (la identidad sale del token,
 * nunca del cuerpo). Sin credencial configurada la respuesta es <b>no</b> y no sale ninguna peticion (fail closed).
 * Que otros roles puedan verificar es una decision de Seguridad sobre el modelo de roles, no de este adaptador.
 */
@Component
public class TitularPorHttp implements TitularDeLaBilletera {

    private static final Logger BITACORA = LoggerFactory.getLogger(TitularPorHttp.class);

    private final ClienteConCircuito identidad;
    private final CredencialDeServicio servicio;

    @Autowired
    public TitularPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.identidad}") String urlDeIdentidad,
            ConfiguracionDeCircuito circuito,
            CredencialDeServicio servicio) {
        this.identidad = new ClienteConCircuito(constructor, urlDeIdentidad, "identidad", circuito);
        this.servicio = servicio;
    }

    /** Con los parametros del plan de resiliencia: la usan las pruebas del adaptador. */
    TitularPorHttp(RestClient.Builder constructor, String urlDeIdentidad, CredencialDeServicio servicio) {
        this(constructor, urlDeIdentidad, ConfiguracionDeCircuito.porOmision(), servicio);
    }

    @Override
    public boolean esElMismo(UUID usuarioId, String nombreDeclarado, String documentoDeclarado) {
        var token = servicio.vigente();
        if (token.isEmpty()) {
            // Sin credencial de servicio no sale ninguna peticion (falla cerrado): una cuenta no se registra "por las
            // dudas".
            BITACORA.warn("verificar titularidad sin credencial de servicio: no coincide");
            return false;
        }
        try {
            var respuesta = identidad.ejecutar(rest -> rest.post()
                    .uri("/usuarios/{id}/titularidad/verificacion", usuarioId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.get())
                    .body(Map.of("nombreCompleto", nombreDeclarado, "documento", documentoDeclarado))
                    .retrieve()
                    .body(Veredicto.class));
            return respuesta != null && respuesta.coincide();
        } catch (RuntimeException noSePudo) {
            return false;
        }
    }

    private record Veredicto(boolean coincide) {}
}
