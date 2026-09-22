package bo.aportaya.plataforma.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

/**
 * H2.S1.M1 — sin {@link ConfiguracionMensajeria} todavia, ninguno de los tres existe:
 * ni el bean {@link Relevo}, ni el {@link LockProvider}, ni el post-processor que hace
 * que {@code @Scheduled} funcione. El kill-test del encargo (insertar {@code PENDIENTE}
 * y esperar 10s) es exactamente la consecuencia visible de esto.
 *
 * <p>{@code @Timeout(30s)}: arma un {@code ApplicationContext} de Spring (igual que
 * {@code webTest}, que por la misma razon tiene 30s y no los 5s de un atomo puro —
 * ADR-043), y encima carga las clases de ShedLock por primera vez en la JVM.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class RelevoConfiguracionTest {

    /** Los beans que un servicio real trae y esta prueba no necesita reales. */
    @Configuration
    static class Dependencias {
        @Bean
        DataSource dataSource() {
            return mock(DataSource.class);
        }

        @Bean
        DSLContext dsl() {
            return mock(DSLContext.class);
        }

        @Bean
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withPropertyValues("aportaya.esquema=aportes")
            .withUserConfiguration(Dependencias.class, ConfiguracionMensajeria.class);

    @Test
    @DisplayName("hay un bean Relevo, con KafkaTemplate presente")
    void hayBeanRelevo() {
        contexto.run(ctx -> assertThat(ctx).hasSingleBean(Relevo.class));
    }

    @Test
    @DisplayName("hay un LockProvider sobre <esquema>.shedlock")
    void hayLockProvider() {
        contexto.run(ctx -> assertThat(ctx).hasSingleBean(LockProvider.class));
    }

    @Test
    @DisplayName("@EnableScheduling esta activo: el post-processor de @Scheduled existe")
    void hayScheduledAnnotationBeanPostProcessor() {
        contexto.run(ctx -> assertThat(ctx.getBeanNamesForType(ScheduledAnnotationBeanPostProcessor.class))
                .isNotEmpty());
    }
}
