package bo.aportaya.plataforma.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class InterceptorDeTrazaKafkaTest {

    private final InterceptorDeTrazaKafka interceptor = new InterceptorDeTrazaKafka();

    @BeforeEach
    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    @Test
    void propagaTrazaYCorrelationAlMdc() {
        var cabeceras = new RecordHeaders();
        cabeceras.add("trace_id", "traza-123".getBytes(StandardCharsets.UTF_8));
        cabeceras.add("correlation_id", "corr-456".getBytes(StandardCharsets.UTF_8));

        var record = new ConsumerRecord<>("test-topic", 0, 0L, "key", "value");
        cabeceras.forEach(h -> record.headers().add(h));

        interceptor.intercept(record, null);

        assertThat(MDC.get("trazaId")).isEqualTo("traza-123");
        assertThat(MDC.get("correlationId")).isEqualTo("corr-456");

        interceptor.success(record, null);
        assertThat(MDC.get("trazaId")).isNull();
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void siFaltaTraceIdUsaCorrelationId() {
        var cabeceras = new RecordHeaders();
        cabeceras.add("correlation_id", "corr-789".getBytes(StandardCharsets.UTF_8));

        var record = new ConsumerRecord<>("test-topic", 0, 0L, "key", "value");
        cabeceras.forEach(h -> record.headers().add(h));

        interceptor.intercept(record, null);

        assertThat(MDC.get("trazaId")).isEqualTo("corr-789");
        assertThat(MDC.get("correlationId")).isEqualTo("corr-789");

        interceptor.failure(record, new RuntimeException("boom"), null);
        assertThat(MDC.get("trazaId")).isNull();
    }
}
