package bo.aportaya.nucleofinanciero.consumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril Q · el borde con Kafka de la apertura de billeteras, sin broker: lo que se lee del sobre de
 * {@code identidad.usuario_registrado} y de {@code grupos.grupo_creado}, y que los PRODUCTORES publican esos tipos y esos
 * campos (comprobacion ESTATICA contra su codigo: no hay todavia una prueba con los dos procesos y Kafka reales).
 */
class ConsumidorDeAperturasKafkaTest {

    private static ConsumerRecord<String, String> registro(String cuerpo, String... cabeceras) {
        var r = new ConsumerRecord<>("aportaya.identidad.usuario_registrado", 0, 0L, "clave", cuerpo);
        for (int i = 0; i < cabeceras.length; i += 2) {
            r.headers().add(cabeceras[i], cabeceras[i + 1].getBytes(StandardCharsets.UTF_8));
        }
        return r;
    }

    private static String codigoDe(String ruta) throws IOException {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isRegularFile(p.resolve(ruta))) {
            p = p.getParent();
        }
        assertThat(p).as(ruta).isNotNull();
        return Files.readString(p.resolve(ruta), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("correcto: usuario_registrado trae usuarioId y nivelSolicitado; grupo_creado trae grupoId y tarifarioId; se lee"
            + " el id del evento, la correlacion y el titular")
    void correcto() {
        UUID evento = UUID.randomUUID();
        UUID correlacion = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID grupo = UUID.randomUUID();

        var u = ConsumidorDeUsuariosKafka.leer(registro(
                "{\"usuarioId\":\"%s\",\"nivelSolicitado\":\"BASICO\"}".formatted(usuario),
                "event_id",
                evento.toString(),
                "correlation_id",
                correlacion.toString()));
        var g = ConsumidorDeGruposKafka.leer(registro(
                "{\"grupoId\":\"%s\",\"tarifarioId\":\"%s\"}".formatted(grupo, UUID.randomUUID()),
                "event_id",
                evento.toString(),
                "correlation_id",
                correlacion.toString()));

        assertThat(u.eventoId()).isEqualTo(evento);
        assertThat(u.correlationId()).isEqualTo(correlacion);
        assertThat(u.usuarioId()).isEqualTo(usuario);
        assertThat(g.grupoId()).isEqualTo(grupo);
    }

    @Test
    @DisplayName("invalido: falta una cabecera o el titular, un id que no es UUID o un cuerpo que no es JSON se RECHAZAN,"
            + " nunca se ignoran (ignorarlos deja a una persona o a un grupo sin billetera)")
    void invalido() {
        String id = UUID.randomUUID().toString();
        String cuerpo = "{\"usuarioId\":\"%s\"}".formatted(UUID.randomUUID());
        assertThatThrownBy(() -> ConsumidorDeUsuariosKafka.leer(registro(cuerpo, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeUsuariosKafka.leer(registro(cuerpo, "event_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeUsuariosKafka.leer(
                        registro(cuerpo, "event_id", "no-es-uuid", "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeUsuariosKafka.leer(
                        registro("{\"nivelSolicitado\":\"BASICO\"}", "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeUsuariosKafka.leer(
                        registro("{\"usuarioId\":\"no-es-uuid\"}", "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeGruposKafka.leer(
                        registro("esto no es json", "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeGruposKafka.leer(registro("{}", "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeGruposKafka.leer(registro(null, "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("pacto · productores (estatico): identidad publica identidad.usuario_registrado con usuarioId y grupos publica"
            + " grupos.grupo_creado con grupoId: los tipos y campos que este servicio consume")
    void losProductoresPublicanLoQueSeConsume() throws IOException {
        String identidad =
                codigoDe("servicios/identidad/src/main/java/bo/aportaya/identidad/aplicacion/CU01RegistrarUsuario.java");
        assertThat(identidad).contains("\"identidad.usuario_registrado\"").contains("\"usuarioId\"");
        String grupos = codigoDe("servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU20CrearGrupo.java");
        assertThat(grupos).contains("\"grupos.grupo_creado\"").contains("\"grupoId\"");
    }
}
