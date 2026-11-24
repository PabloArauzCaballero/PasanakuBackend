package bo.aportaya.identidad.infraestructura;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cablea {@link VigiaDeRevocaciones} (GU-15). La lectura corre en una transaccion corta de solo lectura con el rol de
 * sistema: {@code sesion} tiene politica de fila por titular, y el vigia necesita ver las de todos.
 *
 * <p>{@code aportaya.revocacion.publicacion.habilitada=false} lo apaga (una maquina de desarrollo sin Kafka).
 */
@Configuration
public class ConfiguracionDeRevocaciones {

    /** El «usuario» del proceso en {@code app.usuario_id}: un trabajo no es una persona. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-000000000015");

    private static final int POR_TANDA = 500;

    @Bean
    @ConditionalOnProperty(
            name = "aportaya.revocacion.publicacion.habilitada",
            havingValue = "true",
            matchIfMissing = true)
    public VigiaDeRevocaciones vigiaDeRevocaciones(
            Datos datos,
            SesionesRevocadasRepositorio sesiones,
            PlatformTransactionManager transacciones,
            KafkaTemplate<String, String> kafka,
            Reloj reloj,
            @Value("${aportaya.revocacion.publicacion.intervalo:PT0.25S}") Duration intervalo,
            @Value("${aportaya.revocacion.publicacion.margen:PT2M}") Duration margen,
            @Value("${aportaya.revocacion.publicacion.espera-de-kafka:PT5S}") Duration esperaDeKafka) {
        TransactionTemplate lectura = new TransactionTemplate(transacciones);
        lectura.setReadOnly(true);
        VigiaDeRevocaciones.Lectura lector =
                (desde, despuesDe, porTanda) -> lectura.execute(estado -> datos.conContexto(
                        ContextoSesion.deSistema(
                                PROCESO, new Traza(UUID.randomUUID().toString())),
                        dsl -> sesiones.despuesDe(dsl, desde, despuesDe, porTanda)));
        return new VigiaDeRevocaciones(
                lector, POR_TANDA, kafka, reloj, intervalo, margen, EmisorDeAcceso.TOPE_TRAS_REVOCAR, esperaDeKafka);
    }
}
