package bo.aportaya.nucleofinanciero.consumo;

import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes;
import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes.AporteCobrado;
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
 * El borde con Kafka de {@link ConsumoDeAportes}: escucha {@code aportes.aporte_cobrado}.
 *
 * <p>No decide nada: lee el sobre del evento (cabeceras {@code event_id} y {@code correlation_id}, cuerpo JSON con el pago,
 * la obligacion, el grupo, quien paga, el canal y el importe CON su moneda) y se lo entrega. Un mensaje al que le falta una
 * cabecera o un campo, con un id que no es UUID, un importe mal formado o una moneda desconocida se RECHAZA (excepcion) en
 * vez de ignorarse: perder en silencio un aporte deja al participante con la cuota pagada y el libro sin moverse.
 *
 * <p><b>Encendido por omision</b> ({@code aportaya.contabilidad.consumo-de-aportes.habilitado}): es el camino normal del
 * dinero, no una funcion opcional; apagarlo es una decision de operaciones. Es independiente del consumo de asientos de
 * garantia (que depende del mapeo contable pendiente A5). El manejo de errores es el del contenedor de Spring Kafka
 * (reintentos y descarte tras agotarlos); una cola de mensajes muertos con alerta es la decision pendiente de
 * operaciones. Un rechazo NO marca el evento como consumido: reprocesar el tema repite solo lo que falto.
 */
@Component
@ConditionalOnProperty(
        name = "aportaya.contabilidad.consumo-de-aportes.habilitado",
        havingValue = "true",
        matchIfMissing = true)
public class ConsumidorDeAportesKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeAportesKafka.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ConsumoDeAportes consumo;

    public ConsumidorDeAportesKafka(ConsumoDeAportes consumo) {
        this.consumo = consumo;
    }

    @KafkaListener(topics = "aportaya.aportes.aporte_cobrado", groupId = "${spring.application.name}-aportes")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        AporteCobrado aporte = leer(registro);
        var salida = consumo.procesar(aporte);
        // Identificadores y resultados, nunca importes ni datos de personas (regla 90.2).
        BITACORA.info("aporte cobrado {} (pago {}) -> {}", aporte.eventoId(), aporte.pagoId(), salida.resultado());
        if (salida.resultado() == ConsumoDeAportes.Resultado.RECHAZADO_POR_SALDO) {
            BITACORA.warn(
                    "pago {}: el saldo no alcanza; se publico aporte_no_aplicado para que aportes compense el pago",
                    aporte.pagoId());
        }
        if (salida.resultado() == ConsumoDeAportes.Resultado.CANAL_SIN_BILLETERA) {
            BITACORA.warn(
                    "pago {}: el canal {} no sale de una billetera; su contrapartida entra por la conciliacion, no por"
                            + " este consumidor",
                    aporte.pagoId(),
                    aporte.canal());
        }
    }

    static AporteCobrado leer(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = JSON.readTree(registro.value());
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("El cuerpo del evento no es un objeto");
            }
            Moneda moneda = Moneda.valueOf(texto(cuerpo, "moneda"));
            return new AporteCobrado(
                    UUID.fromString(cabecera(registro, "event_id")),
                    UUID.fromString(cabecera(registro, "correlation_id")),
                    uuid(cuerpo, "pagoId"),
                    uuid(cuerpo, "obligacionId"),
                    uuid(cuerpo, "grupoId"),
                    uuid(cuerpo, "usuarioId"),
                    Dinero.de(texto(cuerpo, "monto"), moneda),
                    texto(cuerpo, "canal"));
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
