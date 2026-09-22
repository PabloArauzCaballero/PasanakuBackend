package bo.aportaya.plataforma.mensajeria;

import io.micrometer.core.instrument.MeterRegistry;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * H2.S1 — lo que hace que {@link Relevo} exista de verdad en un servicio, en vez de ser
 * una clase que nadie instancia (el kill-test del encargo: insertar {@code PENDIENTE} y
 * esperar 10s — hoy se queda ahi para siempre).
 *
 * <p>{@code @EnableSchedulerLock(defaultLockAtMostFor = "PT30S")} y el {@code
 * @SchedulerLock} que ya tenia {@code Relevo.relevar()} son los que garantizan que, con
 * varias replicas, solo una corre el relevo a la vez — {@code FOR UPDATE SKIP LOCKED}
 * asume eso; sin el lock, dos relevos a la vez seria un caso mas raro de proteger, no
 * uno imposible.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class ConfiguracionMensajeria {

    /**
     * Una tabla {@code shedlock} por esquema (ya generada en {@code sql/15_infra/mensajeria.sql},
     * ADR-018/027) — nunca una compartida entre servicios, que convertiria el lock de UNO en
     * el lock de TODOS.
     */
    @Bean
    @ConditionalOnMissingBean
    public LockProvider lockProvider(DataSource dataSource, @Value("${aportaya.esquema}") String esquema) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withTableName(esquema + ".shedlock")
                .build());
    }

    /**
     * Condicionado a {@code aportaya.outbox.habilitado} (default {@code true}, Q-02) y a que
     * haya un {@link KafkaTemplate}: en {@code webTest}/{@code test} (sin Kafka real) el bean
     * simplemente no se registra, en vez de fallar el arranque.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnProperty(name = "aportaya.outbox.habilitado", havingValue = "true", matchIfMissing = true)
    public Relevo relevo(
            @Value("${aportaya.esquema}") String esquema,
            DSLContext dsl,
            KafkaTemplate<String, String> kafka,
            MeterRegistry metricas) {
        return new Relevo(esquema, dsl, kafka, metricas);
    }
}
