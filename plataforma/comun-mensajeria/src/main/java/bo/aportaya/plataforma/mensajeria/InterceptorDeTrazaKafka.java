package bo.aportaya.plataforma.mensajeria;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

/**
 * Propaga el trace_id y correlation_id del evento Kafka al MDC de SLF4J
 * para que los hilos de los consumidores tengan trazabilidad completa de punta a punta.
 */
public class InterceptorDeTrazaKafka implements RecordInterceptor<String, String> {

    private static final String CLAVE_TRAZA = "trazaId";
    private static final String CLAVE_CORRELATION = "correlationId";

    @Override
    public ConsumerRecord<String, String> intercept(
            ConsumerRecord<String, String> record, Consumer<String, String> consumer) {
        String traceId = extraerCabecera(record, "trace_id");
        String correlationId = extraerCabecera(record, "correlation_id");
        if (traceId == null || traceId.isBlank()) {
            traceId = correlationId;
        }
        if (traceId != null && !traceId.isBlank()) {
            MDC.put(CLAVE_TRAZA, traceId);
        }
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put(CLAVE_CORRELATION, correlationId);
        }
        return record;
    }

    @Override
    public void success(ConsumerRecord<String, String> record, Consumer<String, String> consumer) {
        MDC.remove(CLAVE_TRAZA);
        MDC.remove(CLAVE_CORRELATION);
    }

    @Override
    public void failure(ConsumerRecord<String, String> record, Exception exception, Consumer<String, String> consumer) {
        MDC.remove(CLAVE_TRAZA);
        MDC.remove(CLAVE_CORRELATION);
    }

    private static String extraerCabecera(ConsumerRecord<String, String> record, String nombre) {
        if (record == null || record.headers() == null) {
            return null;
        }
        Header h = record.headers().lastHeader(nombre);
        if (h != null && h.value() != null) {
            return new String(h.value(), StandardCharsets.UTF_8);
        }
        return null;
    }
}
