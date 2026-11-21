package bo.aportaya.entregas.consumo;

import bo.aportaya.entregas.aplicacion.AplicarTurnosAsignados;
import bo.aportaya.entregas.aplicacion.AplicarTurnosAsignados.TurnosAsignados;
import bo.aportaya.entregas.infraestructura.TurnoDelGrupoRepositorio.Turno;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * El borde con Kafka de {@link AplicarTurnosAsignados}: escucha {@code grupos.turnos_asignados}
 * ({@code contratos/pactos/grupos-entregas-turnos-asignados.json}).
 *
 * <p>Un mensaje sin cabeceras, sin turnos o con un turno incompleto se RECHAZA entero: una copia a medias haria que se
 * entregue el pozo a quien no corresponde. <b>Apagado por omision</b>, junto con la entrega automatica.
 */
@Component
@ConditionalOnProperty(name = "aportaya.entrega-automatica.habilitada", havingValue = "true")
public class ConsumidorDeTurnosAsignadosKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeTurnosAsignadosKafka.class);

    private final AplicarTurnosAsignados aplicar;

    public ConsumidorDeTurnosAsignadosKafka(AplicarTurnosAsignados aplicar) {
        this.aplicar = aplicar;
    }

    @KafkaListener(topics = "aportaya.grupos.turnos_asignados", groupId = "${spring.application.name}-turnos")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        TurnosAsignados evento = leer(registro);
        var salida = aplicar.procesar(evento);
        BITACORA.info(
                "turnos asignados del grupo {} (evento {}) -> {}: {} aplicados, {} descartados",
                evento.grupoId(),
                evento.eventoId(),
                salida.resultado(),
                salida.aplicados(),
                salida.descartados());
    }

    static TurnosAsignados leer(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = LecturaDelSobre.cuerpo(registro);
            UUID grupo = LecturaDelSobre.uuid(cuerpo, "grupoId");
            JsonNode lista = cuerpo.get("turnos");
            if (lista == null || !lista.isArray() || lista.isEmpty()) {
                throw new IllegalArgumentException("El evento no trae turnos");
            }
            List<Turno> turnos = new ArrayList<>();
            for (JsonNode t : lista) {
                turnos.add(new Turno(
                        LecturaDelSobre.uuid(t, "turnoId"),
                        grupo,
                        LecturaDelSobre.uuid(t, "periodoId"),
                        LecturaDelSobre.uuid(t, "cupoId"),
                        LecturaDelSobre.uuid(t, "titularParticipanteId"),
                        LecturaDelSobre.entero(t, "version")));
            }
            return new TurnosAsignados(
                    LecturaDelSobre.cabecera(registro, "event_id"),
                    LecturaDelSobre.cabecera(registro, "correlation_id"),
                    grupo,
                    List.copyOf(turnos));
        } catch (IllegalArgumentException ilegible) {
            throw new IllegalArgumentException("El evento no se puede leer: " + ilegible.getMessage(), ilegible);
        }
    }
}
