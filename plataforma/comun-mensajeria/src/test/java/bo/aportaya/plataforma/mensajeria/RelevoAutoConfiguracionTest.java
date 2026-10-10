package bo.aportaya.plataforma.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Carril G: el defecto que dejaba el outbox sin relevo en TODOS los servicios.
 *
 * <p>{@link RelevoConfiguracionTest} pone el {@code KafkaTemplate} como configuracion de usuario, que SIEMPRE se
 * procesa antes, y por eso nunca vio el problema: en un servicio real el {@code KafkaTemplate} lo crea
 * {@link KafkaAutoConfiguration}, y si {@link ConfiguracionMensajeria} no corre despues de ella la condicion
 * {@code @ConditionalOnBean(KafkaTemplate)} da falso y el {@link Relevo} no existe. Aca el template sale de la
 * auto-configuracion de verdad, y las dos se listan en el orden MALO a proposito: lo que las ordena es la
 * anotacion de la clase.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class RelevoAutoConfiguracionTest {

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
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withInitializer(ctx -> ctx.getBeanFactory()
                    .setConversionService(
                            org.springframework.boot.convert.ApplicationConversionService.getSharedInstance()))
            .withPropertyValues("aportaya.esquema=aportes", "spring.kafka.bootstrap-servers=localhost:9092")
            .withUserConfiguration(Dependencias.class)
            .withConfiguration(AutoConfigurations.of(ConfiguracionMensajeria.class, KafkaAutoConfiguration.class));

    @Test
    @DisplayName(
            "el relevo existe cuando el KafkaTemplate lo crea la auto-configuracion de Kafka (orden real de un servicio)")
    void elRelevoExisteConElKafkaTemplateDeLaAutoConfiguracion() {
        contexto.run(ctx -> {
            assertThat(ctx).hasSingleBean(KafkaTemplate.class);
            assertThat(ctx).hasSingleBean(Relevo.class);
        });
    }

    @Test
    @DisplayName("el relevo no existe si el despliegue lo apaga con aportaya.outbox.habilitado=false")
    void apagadoPorConfiguracion() {
        contexto.withPropertyValues("aportaya.outbox.habilitado=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(Relevo.class));
    }

    @Test
    @DisplayName(
            "la auto-configuracion esta registrada en el archivo imports del modulo (si no, ningun servicio la carga)")
    void registradaParaQueLaCarguenLosServicios() {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader())
                        .getCandidates())
                .contains(ConfiguracionMensajeria.class.getName());
    }
}
