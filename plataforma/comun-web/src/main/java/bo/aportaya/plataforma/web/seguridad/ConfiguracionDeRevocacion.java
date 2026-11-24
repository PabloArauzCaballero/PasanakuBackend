package bo.aportaya.plataforma.web.seguridad;

import bo.aportaya.plataforma.dominio.Reloj;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El corte inmediato de GU-15 cableado en cada servicio: la lista, quien la llena y la sonda.
 *
 * <p>{@code aportaya.revocacion.lector.habilitado=false} apaga la lectura del tema (una maquina de desarrollo sin
 * Kafka). Apagada, la lista se declara al dia vacia: el servicio vuelve al comportamiento anterior —el token vive hasta
 * su {@code exp}— y lo dice en el log, en vez de rechazar todo token por una lista que nunca se va a llenar.
 */
@Configuration
public class ConfiguracionDeRevocacion {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConfiguracionDeRevocacion.class);

    @Bean
    public ListaDeRevocaciones listaDeRevocaciones(
            @Value("${aportaya.revocacion.lector.habilitado:true}") boolean lectorHabilitado) {
        ListaDeRevocaciones lista = new ListaDeRevocaciones();
        if (!lectorHabilitado) {
            BITACORA.warn("aportaya.revocacion.lector.habilitado=false: una sesion cerrada sigue valiendo hasta que "
                    + "venza su token (sin corte inmediato, GU-15)");
            lista.marcarAlDia();
        }
        return lista;
    }

    @Bean
    public ValidadorDeRevocacion validadorDeRevocacion(ListaDeRevocaciones lista, Reloj reloj) {
        return new ValidadorDeRevocacion(lista, reloj);
    }

    @Bean("revocacion")
    public SaludDeRevocaciones saludDeRevocaciones(ListaDeRevocaciones lista) {
        return new SaludDeRevocaciones(lista);
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            name = "aportaya.revocacion.lector.habilitado",
            havingValue = "true",
            matchIfMissing = true)
    public LectorDeRevocaciones lectorDeRevocaciones(
            ListaDeRevocaciones lista,
            Reloj reloj,
            MeterRegistry metricas,
            @Value("${spring.kafka.bootstrap-servers}") String servidores,
            @Value("${spring.application.name:servicio}") String aplicacion,
            @Value("${aportaya.revocacion.lector.redescubrir:PT30S}") Duration redescubrir) {
        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servidores);
        // Sin group.id: se asigna a mano y no se confirma ningun offset (ver LectorDeRevocaciones).
        propiedades.put(ConsumerConfig.CLIENT_ID_CONFIG, aplicacion + "-revocaciones-" + UUID.randomUUID());
        propiedades.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        propiedades.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        propiedades.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propiedades.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new LectorDeRevocaciones(() -> new KafkaConsumer<>(propiedades), lista, reloj, metricas, redescubrir);
    }
}
