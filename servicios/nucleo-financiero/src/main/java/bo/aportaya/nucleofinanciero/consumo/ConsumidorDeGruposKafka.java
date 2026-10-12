package bo.aportaya.nucleofinanciero.consumo;

import bo.aportaya.nucleofinanciero.aplicacion.AperturaDeBilleteras;
import bo.aportaya.nucleofinanciero.aplicacion.AperturaDeBilleteras.GrupoCreado;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * El borde con Kafka de {@link AperturaDeBilleteras#abrirLaDeGrupo}: escucha {@code grupos.grupo_creado} (CU-20) y abre la
 * billetera del grupo (titular: el grupo, R-GRP-04). No decide nada. Encendido por omision
 * ({@code aportaya.billetera.apertura.grupos.habilitado}): sin el, el aporte cobrado no tiene donde acreditarse
 * (AP-CU12-02). El diseno llama a este evento {@code grupo_activado}; el que {@code grupos} publica de verdad es
 * {@code grupo_creado}, y es el que se consume. Un rechazo (excepcion) NO marca el evento como consumido.
 */
@Component
@ConditionalOnProperty(
        name = "aportaya.billetera.apertura.grupos.habilitado",
        havingValue = "true",
        matchIfMissing = true)
public class ConsumidorDeGruposKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeGruposKafka.class);

    private final AperturaDeBilleteras apertura;

    public ConsumidorDeGruposKafka(AperturaDeBilleteras apertura) {
        this.apertura = apertura;
    }

    @KafkaListener(topics = "aportaya.grupos.grupo_creado", groupId = "${spring.application.name}-grupos")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        GrupoCreado grupo = leer(registro);
        var salida = apertura.abrirLaDeGrupo(grupo);
        BITACORA.info("grupo creado {} (grupo {}) -> {}", grupo.eventoId(), grupo.grupoId(), salida.resultado());
    }

    static GrupoCreado leer(ConsumerRecord<String, String> registro) {
        var sobre = SobreDeEvento.de(registro);
        return new GrupoCreado(sobre.eventoId(), sobre.correlationId(), sobre.uuid("grupoId"));
    }
}
