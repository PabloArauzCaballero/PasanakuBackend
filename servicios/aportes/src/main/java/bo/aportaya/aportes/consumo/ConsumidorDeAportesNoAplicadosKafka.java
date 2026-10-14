package bo.aportaya.aportes.consumo;

import bo.aportaya.aportes.aplicacion.CU21CompensarAporte;
import bo.aportaya.aportes.aplicacion.CU21CompensarAporte.AporteNoAplicado;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * El borde con Kafka de {@link CU21CompensarAporte}: escucha {@code nucleo_financiero.aporte_no_aplicado}.
 *
 * <p>No decide nada: lee el sobre del evento (cabeceras {@code event_id} y {@code correlation_id}, cuerpo JSON con el pago,
 * la obligacion y el motivo) y se lo entrega. Un mensaje al que le falta una cabecera o un campo, o con un identificador que
 * no es UUID, se RECHAZA (excepcion) en vez de ignorarse: perderlo en silencio deja la obligacion {@code PAGADA} sin que el
 * dinero se haya movido.
 *
 * <p><b>Encendido por omision</b> ({@code aportaya.compensacion.consumo-habilitado}): es el camino normal de un aporte sin
 * saldo; apagarlo es una decision de operaciones. El manejo de errores es el del contenedor de Spring Kafka (reintentos y
 * descarte tras agotarlos); una cola de mensajes muertos con alerta es la decision pendiente de operaciones. Un rechazo NO
 * marca el evento como consumido.
 */
@Component
@ConditionalOnProperty(name = "aportaya.compensacion.consumo-habilitado", havingValue = "true", matchIfMissing = true)
public class ConsumidorDeAportesNoAplicadosKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeAportesNoAplicadosKafka.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final CU21CompensarAporte compensar;

    public ConsumidorDeAportesNoAplicadosKafka(CU21CompensarAporte compensar) {
        this.compensar = compensar;
    }

    @KafkaListener(
            topics = "aportaya.nucleo_financiero.aporte_no_aplicado",
            groupId = "${spring.application.name}-compensacion")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        AporteNoAplicado aviso = leer(registro);
        var salida = compensar.procesar(aviso);
        // Identificadores y resultados, nunca importes ni datos de personas (regla 90.2).
        BITACORA.info("aporte no aplicado {} (pago {}) -> {}", aviso.eventoId(), aviso.pagoId(), salida.resultado());
        if (salida.resultado() == CU21CompensarAporte.Resultado.NO_COMPENSABLE) {
            BITACORA.warn(
                    "pago {}: no se compensa solo (reembolsado o en disputa); lo resuelve una persona de operaciones",
                    aviso.pagoId());
        }
    }

    static AporteNoAplicado leer(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = JSON.readTree(registro.value());
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("El cuerpo del evento no es un objeto");
            }
            return new AporteNoAplicado(
                    UUID.fromString(cabecera(registro, "event_id")),
                    UUID.fromString(cabecera(registro, "correlation_id")),
                    uuid(cuerpo, "pagoId"),
                    uuid(cuerpo, "obligacionId"),
                    texto(cuerpo, "motivo"));
        } catch (JsonProcessingException | IllegalArgumentException ilegible) {
            throw new IllegalArgumentException("El evento no se puede leer: " + ilegible.getMessage(), ilegible);
        }
    }

    private static String texto(JsonNode cuerpo, String campo) {
        if (!cuerpo.hasNonNull(campo) || cuerpo.get(campo).asText().isBlank()) {
            throw new IllegalArgumentException("El evento no trae " + campo);
        }
        return cuerpo.get(campo).asText();
    }

    private static UUID uuid(JsonNode cuerpo, String campo) {
        return UUID.fromString(texto(cuerpo, campo));
    }

    private static String cabecera(ConsumerRecord<String, String> registro, String nombre) {
        Header h = registro.headers().lastHeader(nombre);
        if (h == null || h.value() == null) {
            throw new IllegalArgumentException("Falta la cabecera " + nombre);
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
