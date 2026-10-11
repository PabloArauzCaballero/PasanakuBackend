package bo.aportaya.plataforma.pruebas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class SinRelevoEnPruebasTest {

    @Test
    @DisplayName("por omision, un contexto de prueba no tiene relevo del outbox")
    void porOmisionApagado() {
        var entorno = new StandardEnvironment();
        new SinRelevoEnPruebas().postProcessEnvironment(entorno, null);
        assertThat(entorno.getProperty("aportaya.outbox.habilitado")).isEqualTo("false");
    }

    @Test
    @DisplayName("una prueba (o un despliegue) que lo enciende explicitamente gana: precedencia mas baja")
    void loExplicitoGana() {
        var entorno = new StandardEnvironment();
        entorno.getPropertySources()
                .addFirst(new MapPropertySource("la-prueba", Map.of("aportaya.outbox.habilitado", "true")));
        new SinRelevoEnPruebas().postProcessEnvironment(entorno, null);
        assertThat(entorno.getProperty("aportaya.outbox.habilitado")).isEqualTo("true");
    }

    @Test
    @DisplayName("esta registrado en spring.factories: sin el registro no se ejecuta en ningun contexto")
    void registrado() throws Exception {
        var recursos =
                java.util.Collections.list(getClass().getClassLoader().getResources("META-INF/spring.factories"));
        var texto = new StringBuilder();
        for (var recurso : recursos) {
            try (var entrada = recurso.openStream()) {
                texto.append(new String(entrada.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        assertThat(texto.toString())
                .contains("org.springframework.boot.env.EnvironmentPostProcessor")
                .contains(SinRelevoEnPruebas.class.getName());
    }
}
