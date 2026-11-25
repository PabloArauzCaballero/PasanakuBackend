package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.MedidaDeCuenta;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a {@code identidad} si el beneficiario tiene una medida vigente que retiene su entrega (CU-134,
 * D-11), con la credencial de SERVICIO de entregas, tiempo maximo y circuit breaker ({@link ClienteConCircuito}).
 *
 * <p>Falla cerrado: sin credencial, con identidad caida, con el circuito abierto o con una respuesta ilegible,
 * la entrega se retiene. La plata no se pierde ni se mueve: la entrega sigue AUTORIZADA con su saldo retenido y
 * se desembolsa cuando identidad confirme que no hay medida.
 */
@Component
public class MedidaDeCuentaPorHttp implements MedidaDeCuenta {

    private static final Logger BITACORA = LoggerFactory.getLogger(MedidaDeCuentaPorHttp.class);

    private final ClienteConCircuito identidad;
    private final CredencialDeServicio servicio;

    @Autowired
    public MedidaDeCuentaPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.identidad}") String urlDeIdentidad,
            ConfiguracionDeCircuito circuito,
            CredencialDeServicio servicio) {
        this.identidad = new ClienteConCircuito(constructor, urlDeIdentidad, "identidad", circuito);
        this.servicio = servicio;
    }

    /** Con los parametros del plan de resiliencia: la usan las pruebas del adaptador. */
    MedidaDeCuentaPorHttp(RestClient.Builder constructor, String urlDeIdentidad, CredencialDeServicio servicio) {
        this(constructor, urlDeIdentidad, ConfiguracionDeCircuito.porOmision(), servicio);
    }

    @Override
    public boolean retieneEntrega(UUID usuarioId) {
        var token = servicio.vigente();
        if (token.isEmpty()) {
            BITACORA.warn("medida vigente sin credencial de servicio: se retiene la entrega");
            return true;
        }
        try {
            var respuesta = identidad.ejecutar(rest -> rest.get()
                    .uri("/identidad/participantes/{id}/medida-vigente", usuarioId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.get())
                    .retrieve()
                    .body(Respuesta.class));
            return respuesta == null || respuesta.retenerEntrega() == null || respuesta.retenerEntrega();
        } catch (RuntimeException noSePudo) {
            BITACORA.warn("medida vigente sin respuesta de identidad: se retiene la entrega");
            return true;
        }
    }

    private record Respuesta(Boolean retenerEntrega) {}
}
