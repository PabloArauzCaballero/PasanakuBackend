package bo.aportaya.tarifas.consumo;

import bo.aportaya.tarifas.aplicacion.CongelarParametrosDelGrupo;
import bo.aportaya.tarifas.aplicacion.CongelarParametrosDelGrupo.GrupoIniciado;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * El borde con Kafka de {@link CongelarParametrosDelGrupo}: escucha {@code grupos.grupo_iniciado} (CU-69).
 *
 * <p>Del evento usa solo las cabeceras {@code event_id} y {@code correlation_id} y, del cuerpo, {@code grupoId},
 * {@code iniciadoEn} y la instantánea {@code nivelesAplicables} (pacto
 * {@code contratos/pactos/grupos-tarifas-grupo-iniciado.json}). Lo demás (cupos,
 * participantes, montos) no se lee: no lo necesita y no se lleva a ningún registro (regla 90.2). Un mensaje al que
 * le falta algo se RECHAZA (excepción) y no se marca consumido: perderlo dejaría al grupo sin parámetros y a
 * {@code garantia} fallando cerrado para siempre.
 *
 * <p>Si quedaron códigos sin valor, se registra un aviso con los códigos (no son datos de personas): es lo que
 * Operaciones mira para cargar el valor antes del próximo grupo.
 */
@Component
@ConditionalOnProperty(name = "aportaya.parametros.consumo-habilitado", havingValue = "true", matchIfMissing = true)
public class ConsumidorDeGruposIniciadosKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeGruposIniciadosKafka.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final CongelarParametrosDelGrupo congelar;

    public ConsumidorDeGruposIniciadosKafka(CongelarParametrosDelGrupo congelar) {
        this.congelar = congelar;
    }

    @KafkaListener(topics = "aportaya.grupos.grupo_iniciado", groupId = "${spring.application.name}-parametros")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        GrupoIniciado grupo = leer(registro);
        var resultado = congelar.procesar(grupo);
        BITACORA.info("grupo iniciado {} (grupo {}) -> {}", grupo.eventoId(), grupo.grupoId(), resultado.desenlace());
        if (!resultado.faltantes().isEmpty()) {
            BITACORA.warn("grupo {} congelado sin valor aprobado para {}", grupo.grupoId(), resultado.faltantes());
        }
    }

    static GrupoIniciado leer(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = JSON.readTree(registro.value());
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("El cuerpo del evento no es un objeto");
            }
            return new GrupoIniciado(
                    UUID.fromString(cabecera(registro, "event_id")),
                    UUID.fromString(cabecera(registro, "correlation_id")),
                    UUID.fromString(texto(cuerpo, "grupoId")),
                    OffsetDateTime.parse(texto(cuerpo, "iniciadoEn")),
                    niveles(cuerpo));
        } catch (JsonProcessingException | IllegalArgumentException | DateTimeParseException ilegible) {
            throw new IllegalArgumentException("El evento no se puede leer: " + ilegible.getMessage(), ilegible);
        }
    }

    private static List<String> niveles(JsonNode cuerpo) {
        JsonNode lista = cuerpo.get("nivelesAplicables");
        if (lista == null || !lista.isArray() || lista.isEmpty()) {
            throw new IllegalArgumentException("El evento no trae nivelesAplicables válidos");
        }
        List<String> niveles = new ArrayList<>();
        HashSet<String> vistos = new HashSet<>();
        for (JsonNode nivel : lista) {
            if (!nivel.isTextual() || nivel.asText().isBlank() || !vistos.add(nivel.asText())) {
                throw new IllegalArgumentException("nivelesAplicables contiene un código vacío, repetido o inválido");
            }
            niveles.add(nivel.asText());
        }
        return List.copyOf(niveles);
    }

    private static String texto(JsonNode cuerpo, String campo) {
        if (!cuerpo.hasNonNull(campo)) {
            throw new IllegalArgumentException("El evento no trae " + campo);
        }
        return cuerpo.get(campo).asText();
    }

    private static String cabecera(ConsumerRecord<String, String> registro, String nombre) {
        Header h = registro.headers().lastHeader(nombre);
        if (h == null || h.value() == null) {
            throw new IllegalArgumentException("Falta la cabecera " + nombre);
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
