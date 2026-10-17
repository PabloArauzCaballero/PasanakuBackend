package bo.aportaya.garantia.consumo;

import bo.aportaya.garantia.aplicacion.IngresoAlFondo;
import bo.aportaya.garantia.aplicacion.IngresoAlFondo.AporteAplicado;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.Moneda;
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
 * El borde con Kafka de {@link IngresoAlFondo}: escucha {@code nucleo_financiero.aporte_aplicado}.
 *
 * <p>No decide nada: lee el sobre (cabeceras {@code event_id} y {@code correlation_id}; cuerpo con la transaccion, el
 * grupo, el importe CON su moneda y el {@code destino}) y se lo entrega. Un evento sin {@code destino} es anterior al
 * campo (2026-10-16) y era siempre al pozo del grupo: se lee como {@code GRUPO}. Un mensaje mal formado se RECHAZA
 * (excepcion) en vez de ignorarse, y un rechazo NO marca el evento como consumido.
 */
@Component
@ConditionalOnProperty(
        name = "aportaya.garantia.consumo-de-aportes-aplicados.habilitado",
        havingValue = "true",
        matchIfMissing = true)
public class ConsumidorDeAportesAplicadosKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeAportesAplicadosKafka.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final IngresoAlFondo ingreso;

    public ConsumidorDeAportesAplicadosKafka(IngresoAlFondo ingreso) {
        this.ingreso = ingreso;
    }

    @KafkaListener(
            topics = "aportaya.nucleo_financiero.aporte_aplicado",
            groupId = "${spring.application.name}-aportes-aplicados")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        AporteAplicado aplicado = leer(registro);
        var resultado = ingreso.anotar(aplicado);
        // Identificadores y resultados, nunca importes ni datos de personas (regla 90.2).
        if (resultado != IngresoAlFondo.Resultado.NO_ES_DEL_FONDO) {
            BITACORA.info(
                    "aporte aplicado {} (transaccion {}) -> {}",
                    aplicado.eventoId(),
                    aplicado.transaccionId(),
                    resultado);
        }
    }

    static AporteAplicado leer(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = JSON.readTree(registro.value());
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("El cuerpo del evento no es un objeto");
            }
            Moneda moneda = Moneda.valueOf(texto(cuerpo, "moneda"));
            String destino = cuerpo.hasNonNull("destino")
                            && !cuerpo.get("destino").asText().isBlank()
                    ? cuerpo.get("destino").asText()
                    : "GRUPO";
            return new AporteAplicado(
                    UUID.fromString(cabecera(registro, "event_id")),
                    UUID.fromString(cabecera(registro, "correlation_id")),
                    uuid(cuerpo, "transaccionId"),
                    uuid(cuerpo, "grupoId"),
                    Dinero.de(texto(cuerpo, "monto"), moneda),
                    destino);
        } catch (JsonProcessingException | IllegalArgumentException | ErrorDeDominio | ArithmeticException ilegible) {
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
