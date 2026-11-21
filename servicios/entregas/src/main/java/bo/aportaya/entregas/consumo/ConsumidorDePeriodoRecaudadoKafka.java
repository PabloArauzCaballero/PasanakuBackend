package bo.aportaya.entregas.consumo;

import bo.aportaya.entregas.aplicacion.EntregaAutomatica;
import bo.aportaya.entregas.aplicacion.EntregaAutomatica.PeriodoRecaudado;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * El borde con Kafka de {@link EntregaAutomatica}: escucha {@code aportes.periodo_recaudado}
 * ({@code contratos/pactos/aportes-entregas-periodo-recaudado.json}).
 *
 * <p>No decide nada: lee el sobre y se lo entrega. Un mensaje al que le falta una cabecera o un campo, o con un motivo
 * fuera del pacto, se RECHAZA: perderlo en silencio deja un turno sin cobrar. Que el turno no se conozca todavia o que
 * {@code aportes} no responda NO es un rechazo: la saga lo guarda y el barrido reintenta.
 *
 * <p><b>Apagado por omision</b> ({@code aportaya.entrega-automatica.habilitada}): el productor todavia no existe y el
 * metodo de desembolso es DECISION_REQUIRED.
 */
@Component
@ConditionalOnProperty(name = "aportaya.entrega-automatica.habilitada", havingValue = "true")
public class ConsumidorDePeriodoRecaudadoKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDePeriodoRecaudadoKafka.class);

    static final Set<String> MOTIVOS = Set.of("COMPLETO", "GRACIA_VENCIDA");

    private final EntregaAutomatica entregaAutomatica;

    public ConsumidorDePeriodoRecaudadoKafka(EntregaAutomatica entregaAutomatica) {
        this.entregaAutomatica = entregaAutomatica;
    }

    @KafkaListener(
            topics = "aportaya.aportes.periodo_recaudado",
            groupId = "${spring.application.name}-periodo-recaudado")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        PeriodoRecaudado aviso = leer(registro);
        var resultado = entregaAutomatica.recibir(aviso);
        // Identificadores y resultados, nunca datos de personas (regla 90.2).
        BITACORA.info("periodo recaudado {} (evento {}) -> {}", aviso.periodoId(), aviso.eventoId(), resultado);
    }

    static PeriodoRecaudado leer(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = LecturaDelSobre.cuerpo(registro);
            String motivo = LecturaDelSobre.texto(cuerpo, "motivo");
            if (!MOTIVOS.contains(motivo)) {
                throw new IllegalArgumentException("Motivo fuera del pacto: " + motivo);
            }
            return new PeriodoRecaudado(
                    LecturaDelSobre.cabecera(registro, "event_id"),
                    LecturaDelSobre.cabecera(registro, "correlation_id"),
                    LecturaDelSobre.uuid(cuerpo, "periodoId"),
                    LecturaDelSobre.uuid(cuerpo, "grupoId"),
                    motivo);
        } catch (IllegalArgumentException ilegible) {
            throw new IllegalArgumentException("El evento no se puede leer: " + ilegible.getMessage(), ilegible);
        }
    }
}
