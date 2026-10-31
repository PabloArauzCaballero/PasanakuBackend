package bo.aportaya.aportes.consumo;

import bo.aportaya.aportes.aplicacion.CU21AbrirPrimeraCuota;
import bo.aportaya.aportes.aplicacion.CU21AbrirPrimeraCuota.GrupoIniciado;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * El borde con Kafka de {@link CU21AbrirPrimeraCuota}: escucha {@code grupos.grupo_iniciado}.
 *
 * <p>No decide nada: lee el sobre del evento (cabeceras {@code event_id} y {@code correlation_id}, cuerpo JSON con el
 * grupo, la moneda, el periodo 1 y los cupos ocupados con su aporte) y se lo entrega. Un mensaje al que le falta una
 * cabecera o un campo, con un identificador que no es UUID, una fecha que no es ISO o un importe que no es una cadena
 * de dos decimales, se RECHAZA (excepcion) en vez de ignorarse: perderlo en silencio deja un grupo CONFORMADO cuyo
 * sorteo nunca se va a poder comprometer, sin que nadie sepa por que.
 *
 * <p><b>Encendido por omision</b> ({@code aportaya.grupo-iniciado.consumo-habilitado}): es el camino normal de todo
 * grupo que arranca; apagarlo es una decision de operaciones. El manejo de errores es el del contenedor de Spring Kafka
 * (reintentos y descarte tras agotarlos). Un rechazo NO marca el evento como consumido.
 */
@Component
@ConditionalOnProperty(name = "aportaya.grupo-iniciado.consumo-habilitado", havingValue = "true", matchIfMissing = true)
public class ConsumidorDeGrupoIniciadoKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeGrupoIniciadoKafka.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final CU21AbrirPrimeraCuota abrir;

    public ConsumidorDeGrupoIniciadoKafka(CU21AbrirPrimeraCuota abrir) {
        this.abrir = abrir;
    }

    @KafkaListener(topics = "aportaya.grupos.grupo_iniciado", groupId = "${spring.application.name}-grupo-iniciado")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        GrupoIniciado inicio = leer(registro);
        var salida = abrir.procesar(inicio);
        // Identificadores y resultados, nunca importes ni datos de personas (regla 90.2).
        BITACORA.info(
                "grupo iniciado {} (grupo {}, periodo {}) -> {} ({} obligaciones nuevas)",
                inicio.eventoId(),
                inicio.grupoId(),
                inicio.periodoId(),
                salida.resultado(),
                salida.creadas());
    }

    static GrupoIniciado leer(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = JSON.readTree(registro.value());
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("El cuerpo del evento no es un objeto");
            }
            JsonNode periodo = objeto(cuerpo, "periodo");
            Moneda moneda = Moneda.valueOf(texto(cuerpo, "moneda"));
            JsonNode cupos = cuerpo.get("cupos");
            if (cupos == null || !cupos.isArray()) {
                throw new IllegalArgumentException("El evento no trae cupos");
            }
            List<CU21AbrirPrimeraCuota.Cupo> leidos = new ArrayList<>();
            for (JsonNode c : cupos) {
                leidos.add(new CU21AbrirPrimeraCuota.Cupo(
                        uuid(c, "cupoId"), uuid(c, "participanteId"), Dinero.de(texto(c, "montoAporte"), moneda)));
            }
            return new GrupoIniciado(
                    UUID.fromString(cabecera(registro, "event_id")),
                    UUID.fromString(cabecera(registro, "correlation_id")),
                    uuid(cuerpo, "grupoId"),
                    uuid(periodo, "periodoId"),
                    periodo.path("numero").asInt(0),
                    fecha(periodo, "fechaLimitePago"),
                    fecha(periodo, "fechaFinGracia"),
                    List.copyOf(leidos));
        } catch (JsonProcessingException | IllegalArgumentException | DateTimeParseException ilegible) {
            throw new IllegalArgumentException("El evento no se puede leer: " + ilegible.getMessage(), ilegible);
        }
    }

    private static JsonNode objeto(JsonNode cuerpo, String campo) {
        JsonNode n = cuerpo.get(campo);
        if (n == null || !n.isObject()) {
            throw new IllegalArgumentException("El evento no trae " + campo);
        }
        return n;
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

    private static LocalDate fecha(JsonNode cuerpo, String campo) {
        return LocalDate.parse(texto(cuerpo, campo));
    }

    private static String cabecera(ConsumerRecord<String, String> registro, String nombre) {
        Header h = registro.headers().lastHeader(nombre);
        if (h == null || h.value() == null) {
            throw new IllegalArgumentException("Falta la cabecera " + nombre);
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
