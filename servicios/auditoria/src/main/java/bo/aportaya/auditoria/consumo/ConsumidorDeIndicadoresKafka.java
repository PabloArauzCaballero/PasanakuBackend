package bo.aportaya.auditoria.consumo;

import bo.aportaya.auditoria.aplicacion.RegistroDeIndicadores;
import bo.aportaya.auditoria.aplicacion.RegistroDeIndicadores.IndicadorCalculado;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
 * El borde con Kafka de {@link RegistroDeIndicadores}: escucha TODOS los temas {@code aportaya.<modulo>.indicador_calculado}
 * por patron, asi un servicio nuevo que publique sus indicadores no obliga a tocar esta clase. Un mensaje mal formado
 * se RECHAZA en vez de ignorarse.
 */
@Component
@ConditionalOnProperty(
        name = "aportaya.auditoria.consumo-de-indicadores.habilitado",
        havingValue = "true",
        matchIfMissing = true)
public class ConsumidorDeIndicadoresKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeIndicadoresKafka.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RegistroDeIndicadores registro;

    public ConsumidorDeIndicadoresKafka(RegistroDeIndicadores registro) {
        this.registro = registro;
    }

    @KafkaListener(
            topicPattern = "aportaya\\.[a-z_]+\\.indicador_calculado",
            groupId = "${spring.application.name}-indicadores")
    public void alRecibir(ConsumerRecord<String, String> mensaje) {
        IndicadorCalculado calculado = leer(mensaje);
        var resultado = registro.registrar(calculado);
        if (resultado == RegistroDeIndicadores.Resultado.SIN_DEFINICION) {
            BITACORA.warn(
                    "indicador {} del periodo {} sin definicion vigente: no se publica (AP-CU98-02)",
                    calculado.codigo(),
                    calculado.periodo());
        }
    }

    static IndicadorCalculado leer(ConsumerRecord<String, String> mensaje) {
        try {
            JsonNode cuerpo = JSON.readTree(mensaje.value());
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("El cuerpo del evento no es un objeto");
            }
            String periodo = texto(cuerpo, "periodo");
            if (!periodo.matches("^\\d{4}(-\\d{2}(-\\d{2})?)?$")) {
                throw new IllegalArgumentException("Periodo invalido: " + periodo);
            }
            if (!cuerpo.hasNonNull("casos") || !cuerpo.get("casos").canConvertToLong()) {
                throw new IllegalArgumentException("El evento no trae casos");
            }
            return new IndicadorCalculado(
                    UUID.fromString(cabecera(mensaje, "event_id")),
                    UUID.fromString(cabecera(mensaje, "correlation_id")),
                    texto(cuerpo, "codigo"),
                    texto(cuerpo, "nombre"),
                    new BigDecimal(texto(cuerpo, "valor")),
                    texto(cuerpo, "unidad"),
                    periodo,
                    cuerpo.get("casos").asLong(),
                    cuerpo.path("provisorio").asBoolean(false));
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

    private static String cabecera(ConsumerRecord<String, String> mensaje, String nombre) {
        Header h = mensaje.headers().lastHeader(nombre);
        if (h == null || h.value() == null) {
            throw new IllegalArgumentException("Falta la cabecera " + nombre);
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
