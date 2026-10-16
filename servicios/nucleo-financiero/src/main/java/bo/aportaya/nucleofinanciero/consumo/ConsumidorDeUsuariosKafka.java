package bo.aportaya.nucleofinanciero.consumo;

import bo.aportaya.nucleofinanciero.aplicacion.AperturaDeBilleteras;
import bo.aportaya.nucleofinanciero.aplicacion.AperturaDeBilleteras.UsuarioRegistrado;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * El borde con Kafka de {@link AperturaDeBilleteras#abrirLaDeUsuario}: escucha {@code identidad.usuario_registrado}
 * (CU-01) y abre la billetera de la persona. No decide nada. Encendido por omision
 * ({@code aportaya.billetera.apertura.usuarios.habilitado}): sin el, nadie tiene billetera. Un rechazo (excepcion) NO marca
 * el evento como consumido; el manejo de errores es el del contenedor de Spring Kafka.
 */
@Component
@ConditionalOnProperty(
        name = "aportaya.billetera.apertura.usuarios.habilitado",
        havingValue = "true",
        matchIfMissing = true)
public class ConsumidorDeUsuariosKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeUsuariosKafka.class);

    private final AperturaDeBilleteras apertura;

    public ConsumidorDeUsuariosKafka(AperturaDeBilleteras apertura) {
        this.apertura = apertura;
    }

    @KafkaListener(topics = "aportaya.identidad.usuario_registrado", groupId = "${spring.application.name}-usuarios")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        UsuarioRegistrado usuario = leer(registro);
        var salida = apertura.abrirLaDeUsuario(usuario);
        // Identificadores y resultados, nunca datos de personas (regla 90.2).
        BITACORA.info("usuario registrado {} -> {}", usuario.eventoId(), salida.resultado());
    }

    static UsuarioRegistrado leer(ConsumerRecord<String, String> registro) {
        var sobre = SobreDeEvento.de(registro);
        return new UsuarioRegistrado(sobre.eventoId(), sobre.correlationId(), sobre.uuid("usuarioId"));
    }
}
