package bo.aportaya.auditoria.consumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GU-07 · como se lee un evento de acceso del sobre del outbox. Los mensajes se arman como los arma
 * {@code EnvelopeDeEvento} (doble declarado del productor, regla 65): la prueba contra un broker real queda en el
 * ensayo {@code e2eTest}.
 */
class ConsumidorDeCambiosDeAccesoTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    static ConsumerRecord<String, String> evento(String tipo, UUID agregado, String carga) {
        var r = new ConsumerRecord<>("aportaya." + tipo, 0, 0L, agregado.toString(), carga);
        r.headers().add("event_id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        r.headers().add("type", tipo.getBytes(StandardCharsets.UTF_8));
        r.headers().add("correlation_id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        r.headers().add("occurred_at", "2026-11-24T15:00:00Z".getBytes(StandardCharsets.UTF_8));
        return r;
    }

    @Test
    @DisplayName("correcto: los seis tipos que identidad publica hoy caen en la entidad y la accion del catalogo")
    void tiposActuales() {
        assertThat(ConsumidorDeCambiosDeAccesoKafka.accionDe("identidad.usuario_interno_creado"))
                .isEqualTo("CREACION");
        assertThat(ConsumidorDeCambiosDeAccesoKafka.accionDe("identidad.usuario_interno_actualizado"))
                .isEqualTo("ACTUALIZACION");
        assertThat(ConsumidorDeCambiosDeAccesoKafka.accionDe("identidad.usuario_interno_roles_asignados"))
                .isEqualTo("CAMBIO_PERMISOS");
        assertThat(ConsumidorDeCambiosDeAccesoKafka.accionDe("identidad.usuario_interno_desbloqueado"))
                .isEqualTo("CAMBIO_ESTADO");
        assertThat(ConsumidorDeCambiosDeAccesoKafka.accionDe("identidad.rol_asignado"))
                .isEqualTo("CAMBIO_PERMISOS");
        assertThat(ConsumidorDeCambiosDeAccesoKafka.accionDe("identidad.rol_revocado"))
                .isEqualTo("CAMBIO_PERMISOS");
        assertThat(ConsumidorDeCambiosDeAccesoKafka.entidadDe("identidad.usuario_interno_creado"))
                .isEqualTo("usuario_interno");
        assertThat(ConsumidorDeCambiosDeAccesoKafka.entidadDe("identidad.rol_revocado"))
                .isEqualTo("asignacion_rol");
    }

    @Test
    @DisplayName("correcto: lee motivo, correlacion, fecha del hecho y la carga; sin actor declarado queda nulo")
    void leeElEvento() throws Exception {
        UUID usuario = UUID.randomUUID();
        var mensaje = evento(
                "identidad.usuario_interno_desbloqueado",
                usuario,
                "{\"usuarioId\":\"" + usuario + "\",\"motivo\":\"pidio desbloqueo\"}");

        var cambio = ConsumidorDeCambiosDeAccesoKafka.leer(mensaje);

        assertThat(cambio.entidadId()).isEqualTo(usuario);
        assertThat(cambio.accion()).isEqualTo("CAMBIO_ESTADO");
        assertThat(cambio.motivo()).isEqualTo("pidio desbloqueo");
        assertThat(cambio.actorUsuarioId()).isNull();
        assertThat(cambio.origen()).isEqualTo("PANEL_ADMIN");
        assertThat(cambio.ocurridoEn().toInstant()).hasToString("2026-11-24T15:00:00Z");
        assertThat(JSON.readTree(cambio.valorNuevoJson()).at("/carga/usuarioId").asText())
                .isEqualTo(usuario.toString());
    }

    @Test
    @DisplayName("limite: un tipo nuevo de CU-08 y campos que no conoce entran igual; actor y origen se leen si vienen")
    void tolerante() throws Exception {
        UUID actor = UUID.randomUUID();
        var mensaje = evento(
                "identidad.asignacion_rol_vencida",
                UUID.randomUUID(),
                "{\"actorId\":\"" + actor
                        + "\",\"actorRol\":\"INTERNAL_IDENTITY_ADMIN\",\"origen\":\"TAREA_PROGRAMADA\","
                        + "\"ambito\":\"GLOBAL\",\"campoFuturo\":{\"x\":1},\"motivo\":\"" + "m".repeat(400) + "\"}");

        var cambio = ConsumidorDeCambiosDeAccesoKafka.leer(mensaje);

        assertThat(Pattern.matches(
                        ConsumidorDeCambiosDeAccesoKafka.PATRON, "aportaya.identidad.asignacion_rol_vencida"))
                .isTrue();
        assertThat(cambio.actorUsuarioId()).isEqualTo(actor);
        assertThat(cambio.actorRol()).hasSizeLessThanOrEqualTo(30);
        assertThat(cambio.origen()).isEqualTo("TAREA_PROGRAMADA");
        assertThat(cambio.motivo()).hasSize(300);
        assertThat(JSON.readTree(cambio.valorNuevoJson())
                        .at("/carga/campoFuturo/x")
                        .asInt())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("limite: datos de contacto o secretos que llegaran en la carga no se copian a la bitacora")
    void sinSensibles() throws Exception {
        var cambio = ConsumidorDeCambiosDeAccesoKafka.leer(evento(
                "identidad.usuario_interno_creado",
                UUID.randomUUID(),
                "{\"cargo\":\"Analista\",\"correoInstitucional\":\"x@y\",\"tokenInvitacion\":\"t\"}"));

        var carga = JSON.readTree(cambio.valorNuevoJson()).get("carga");
        assertThat(carga.has("cargo")).isTrue();
        assertThat(carga.has("correoInstitucional")).isFalse();
        assertThat(carga.has("tokenInvitacion")).isFalse();
    }

    @Test
    @DisplayName(
            "GU-16/GU-18: la definicion de un rol es del rol, la cuenta de servicio es suya, y el patron las escucha")
    void rolesYCuentasDeServicio() {
        for (String tipo : new String[] {
            "identidad.rol_creado",
            "identidad.rol_editado",
            "identidad.rol_retirado",
            "identidad.rol_solicitado",
            "identidad.rol_solicitud_rechazada"
        }) {
            assertThat(ConsumidorDeCambiosDeAccesoKafka.entidadDe(tipo)).isEqualTo("rol");
            assertThat(Pattern.matches(ConsumidorDeCambiosDeAccesoKafka.PATRON, "aportaya." + tipo))
                    .isTrue();
        }
        assertThat(ConsumidorDeCambiosDeAccesoKafka.entidadDe("identidad.cuenta_servicio_credencial_rotada"))
                .isEqualTo("cuenta_servicio");
        assertThat(Pattern.matches(
                        ConsumidorDeCambiosDeAccesoKafka.PATRON, "aportaya.identidad.cuenta_servicio_revocada"))
                .isTrue();
        assertThat(ConsumidorDeCambiosDeAccesoKafka.entidadDe("identidad.rol_asignado"))
                .isEqualTo("asignacion_rol");
    }

    @Test
    @DisplayName("el patron no escucha la revocacion de sesiones ni eventos ajenos")
    void patron() {
        assertThat(Pattern.matches(
                        ConsumidorDeCambiosDeAccesoKafka.PATRON, "aportaya.identidad.usuario_interno_creado"))
                .isTrue();
        assertThat(Pattern.matches(ConsumidorDeCambiosDeAccesoKafka.PATRON, "aportaya.identidad.revocacion"))
                .isFalse();
        assertThat(Pattern.matches(ConsumidorDeCambiosDeAccesoKafka.PATRON, "aportaya.identidad.acceso_bloqueado"))
                .isFalse();
    }

    @Test
    @DisplayName(
            "invalido: sin event_id, sin clave, con carga que no es objeto o con un actor que no es UUID se rechaza")
    void invalidos() {
        var sinEvento = new ConsumerRecord<>("t", 0, 0L, UUID.randomUUID().toString(), "{}");
        sinEvento.headers().add("type", "identidad.rol_asignado".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> ConsumidorDeCambiosDeAccesoKafka.leer(sinEvento))
                .isInstanceOf(IllegalArgumentException.class);

        var sinClave = evento("identidad.rol_asignado", UUID.randomUUID(), "{}");
        var sinClaveDeVerdad = new ConsumerRecord<String, String>("t", 0, 0L, null, "{}");
        sinClave.headers().forEach(h -> sinClaveDeVerdad.headers().add(h));
        assertThatThrownBy(() -> ConsumidorDeCambiosDeAccesoKafka.leer(sinClaveDeVerdad))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ConsumidorDeCambiosDeAccesoKafka.leer(
                        evento("identidad.rol_asignado", UUID.randomUUID(), "[1]")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeCambiosDeAccesoKafka.leer(
                        evento("identidad.rol_asignado", UUID.randomUUID(), "{\"actorId\":\"quien\"}")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
