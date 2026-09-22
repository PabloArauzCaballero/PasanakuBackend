package bo.aportaya.plataforma.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import bo.aportaya.plataforma.pruebas.Espera;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * H2.S2 — punta a punta con Kafka REAL de Testcontainers, no un doble.
 *
 * <p>{@link RelevoRepositorioTest} ya fija la maquina de estados de {@link Relevo} contra
 * PostgreSQL real con Kafka mockeado — eso prueba la logica, no el transporte. Esta prueba
 * verifica lo que un mock no puede: que el mensaje sale por la red, con las 8 cabeceras del
 * contrato ({@code docs/auditoria-produccion/contratos/evento-kafka.md}) tal como las lee un
 * consumidor real, y que la fila queda {@code PUBLICADO} solo despues de esa confirmacion.
 *
 * <p>Nombrada {@code OutboxE2ETest} (no {@code *RepositorioTest}) a proposito: cae en el
 * corredor {@code e2eTest} (patron {@code **&#47;*E2ETest.class}), el mismo nombre que ya
 * anticipaba el javadoc de {@link RelevoRepositorioTest}.
 */
class OutboxE2ETest extends BaseDePruebaMensajeria {

    private static final String TEMA = "aportaya.nucleo_financiero.evento_de_prueba";

    @Test
    @DisplayName("fila PENDIENTE -> Kafka real -> consumidor recibe las 8 cabeceras -> PUBLICADO")
    void filaPendienteLlegaAKafkaReal() {
        UUID id = insertarPendiente();
        Relevo relevo = new Relevo(
                ESQUEMA,
                dsl,
                kafkaTemplateReal(),
                new SimpleMeterRegistry(),
                10,
                Duration.ofSeconds(1),
                Duration.ofMinutes(5),
                Duration.ofSeconds(10));

        // No hay contexto de Spring en esta prueba, asi que no hay @Scheduled: se simula el
        // tick reintentando relevar() hasta que la fila quede PUBLICADO, con presupuesto
        // acotado (Espera, sin sleep fijo).
        Espera.hasta(Duration.ofSeconds(20), Duration.ofMillis(200), "evento_dominio PUBLICADO", () -> {
            relevo.relevar();
            return "PUBLICADO".equals(filaDe(id).get("estado", String.class));
        });

        try (KafkaConsumer<String, String> consumidor = consumidorReal()) {
            consumidor.subscribe(List.of(TEMA));
            ConsumerRecord<String, String> registro = esperarRegistro(consumidor);

            assertThat(registro.value()).contains("{}");
            assertThat(cabecera(registro, "event_id")).isEqualTo(id.toString());
            assertThat(cabecera(registro, "type")).isEqualTo("nucleo_financiero.evento_de_prueba");
            assertThat(cabecera(registro, "version")).isEqualTo("1");
            assertThat(cabecera(registro, "producer")).isEqualTo(ESQUEMA);
            assertThat(cabecera(registro, "occurred_at")).isNotBlank();
            assertThat(cabecera(registro, "correlation_id")).isNotBlank();
            // Sin metadatos.trace_id (default '{}'), trazaDe() cae al correlation_id (H2.S2.M4).
            assertThat(cabecera(registro, "trace_id")).isEqualTo(cabecera(registro, "correlation_id"));
            // insertarPendiente() no fija causation_id (columna nullable): la cabecera no viaja.
            assertThat(registro.headers().lastHeader("causation_id")).isNull();
        }

        assertThat(filaDe(id).get("estado", String.class)).isEqualTo("PUBLICADO");
    }

    private KafkaTemplate<String, String> kafkaTemplateReal() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BaseDePrueba.kafka().getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> fabrica = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(fabrica);
    }

    private KafkaConsumer<String, String> consumidorReal() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BaseDePrueba.kafka().getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "outbox-e2e-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }

    private ConsumerRecord<String, String> esperarRegistro(KafkaConsumer<String, String> consumidor) {
        Instant limite = Instant.now().plus(Duration.ofSeconds(15));
        while (Instant.now().isBefore(limite)) {
            ConsumerRecords<String, String> registros = consumidor.poll(Duration.ofMillis(500));
            if (!registros.isEmpty()) {
                return registros.iterator().next();
            }
        }
        throw new IllegalStateException("timeout esperando el mensaje en Kafka real (" + TEMA + ")");
    }

    private String cabecera(ConsumerRecord<String, String> registro, String nombre) {
        Header h = registro.headers().lastHeader(nombre);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }
}
