package bo.aportaya.auditoria.consumo;

import bo.aportaya.auditoria.aplicacion.RegistroDeCambiosDeAcceso;
import bo.aportaya.auditoria.infraestructura.BitacoraRepositorio.CambioDeAcceso;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * El borde con Kafka de {@link RegistroDeCambiosDeAcceso} (GU-07): escucha por patron los temas de cambios de acceso de
 * identidad —{@code usuario_interno_*}, {@code rol_*} y {@code asignacion_rol_*}—, asi que un evento nuevo de CU-08 entra
 * sin tocar esta clase.
 *
 * <p><b>Tolerante hacia adelante.</b> Lee del sobre del outbox ({@code event_id}, {@code type},
 * {@code correlation_id}, {@code occurred_at}; la clave es el agregado) y, de la carga, solo lo que conoce: el motivo y,
 * si vienen, el actor ({@code actorId}/{@code operadorId}/{@code otorgadoPor}/{@code revocadoPor}), su rol y el
 * origen. Todo lo demas se guarda tal cual en {@code valor_nuevo}, menos los campos que parecen datos de contacto o
 * secretos, que no se copian a la bitacora (regla 90.2).
 *
 * <p>Un mensaje sin sobre completo se RECHAZA: sin {@code event_id} no hay idempotencia posible.
 */
@Component
@ConditionalOnProperty(
        name = "aportaya.auditoria.consumo-de-accesos.habilitado",
        havingValue = "true",
        matchIfMissing = true)
public class ConsumidorDeCambiosDeAccesoKafka {

    static final String PATRON = "aportaya\\.identidad\\.(usuario_interno|rol|asignacion_rol|cuenta_servicio)_[a-z_]+";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> CAMPOS_DE_ACTOR = List.of("actorId", "operadorId", "otorgadoPor", "revocadoPor");
    private static final Set<String> ORIGENES = Set.of(
            "API_PUBLICA",
            "APP_MOVIL",
            "APP_WEB",
            "MIGRACION",
            "ORGANIZADOR_DIGITAL",
            "PANEL_ADMIN",
            "SOPORTE_INTERNO",
            "TAREA_PROGRAMADA",
            "WEBHOOK_ENTRANTE");
    private static final List<String> SENSIBLES =
            List.of("correo", "telefono", "documento", "clave", "contrasena", "token", "secreto");
    private static final Set<String> ROLES_DE_DEFINICION = Set.of(
            "identidad.rol_creado",
            "identidad.rol_editado",
            "identidad.rol_retirado",
            "identidad.rol_solicitado",
            "identidad.rol_solicitud_rechazada");
    private static final int LARGO_DEL_MOTIVO = 300;
    private static final int LARGO_DEL_ROL = 30;

    private final RegistroDeCambiosDeAcceso registro;

    public ConsumidorDeCambiosDeAccesoKafka(RegistroDeCambiosDeAcceso registro) {
        this.registro = registro;
    }

    @KafkaListener(topicPattern = PATRON, groupId = "${spring.application.name}-bitacora-de-accesos")
    public void alRecibir(ConsumerRecord<String, String> mensaje) {
        registro.registrar(UUID.fromString(cabecera(mensaje, "event_id")), leer(mensaje));
    }

    static CambioDeAcceso leer(ConsumerRecord<String, String> mensaje) {
        try {
            String tipo = cabecera(mensaje, "type");
            UUID eventoId = UUID.fromString(cabecera(mensaje, "event_id"));
            UUID correlacion = UUID.fromString(cabecera(mensaje, "correlation_id"));
            if (mensaje.key() == null || mensaje.value() == null) {
                throw new IllegalArgumentException("El evento no trae clave o carga");
            }
            UUID agregado = UUID.fromString(mensaje.key());
            JsonNode carga = JSON.readTree(mensaje.value());
            if (carga == null || !carga.isObject()) {
                throw new IllegalArgumentException("La carga no es un objeto");
            }
            UUID actor = actorDe(carga);
            ObjectNode valor = JSON.createObjectNode();
            valor.put("evento", tipo);
            valor.put("eventoId", eventoId.toString());
            valor.set("carga", sinSensibles((ObjectNode) carga));
            return new CambioDeAcceso(
                    entidadDe(tipo),
                    agregado,
                    accionDe(tipo),
                    actor,
                    recortado(texto(carga, "actorRol"), LARGO_DEL_ROL),
                    origenDe(texto(carga, "origen")),
                    correlacion,
                    JSON.writeValueAsString(valor),
                    recortado(texto(carga, "motivo"), LARGO_DEL_MOTIVO),
                    ocurridoEn(mensaje));
        } catch (JsonProcessingException | IllegalArgumentException ilegible) {
            throw new IllegalArgumentException(
                    "El evento de acceso no se puede leer: " + ilegible.getMessage(), ilegible);
        }
    }

    /** La tabla a la que se refiere el cambio. El id es la clave del mensaje (el agregado del outbox). */
    static String entidadDe(String tipo) {
        if (tipo.startsWith("identidad.usuario_interno_")) {
            return "usuario_interno";
        }
        if (tipo.startsWith("identidad.cuenta_servicio_")) {
            return "cuenta_servicio";
        }
        // GU-16: la definicion de un rol (creado, editado, retirado, solicitado, solicitud rechazada) es sobre el rol;
        // las
        // asignaciones (rol_asignado, rol_revocado, rol_vencido) siguen siendo de asignacion_rol.
        return ROLES_DE_DEFINICION.contains(tipo) ? "rol" : "asignacion_rol";
    }

    /**
     * La accion del catalogo {@code ck_bitacora_evento_accion}. Un hecho que no se reconoce cae en ACTUALIZACION: se
     * registra igual; perderlo seria peor que clasificarlo de forma generica.
     */
    static String accionDe(String tipo) {
        String hecho = tipo.substring(tipo.lastIndexOf('.') + 1);
        if (hecho.contains("rol") || hecho.contains("asign") || hecho.contains("revoc") || hecho.contains("permiso")) {
            return "CAMBIO_PERMISOS";
        }
        if (hecho.endsWith("_creado") || hecho.contains("alta")) {
            return "CREACION";
        }
        if (hecho.contains("suspend")
                || hecho.contains("reactiv")
                || hecho.contains("desbloq")
                || hecho.contains("bloque")
                || hecho.contains("baja")) {
            return "CAMBIO_ESTADO";
        }
        return "ACTUALIZACION";
    }

    private static UUID actorDe(JsonNode carga) {
        for (String campo : CAMPOS_DE_ACTOR) {
            String valor = texto(carga, campo);
            if (valor != null) {
                return UUID.fromString(valor);
            }
        }
        return null;
    }

    private static String origenDe(String declarado) {
        return declarado != null && ORIGENES.contains(declarado) ? declarado : "PANEL_ADMIN";
    }

    private static ObjectNode sinSensibles(ObjectNode carga) {
        ObjectNode limpia = carga.deepCopy();
        List<String> nombres = new ArrayList<>();
        limpia.fieldNames().forEachRemaining(nombres::add);
        for (String nombre : nombres) {
            String minuscula = nombre.toLowerCase(Locale.ROOT);
            if (SENSIBLES.stream().anyMatch(minuscula::contains)) {
                limpia.remove(nombre);
            }
        }
        return limpia;
    }

    private static OffsetDateTime ocurridoEn(ConsumerRecord<String, String> mensaje) {
        Header h = mensaje.headers().lastHeader("occurred_at");
        if (h == null || h.value() == null) {
            return Instant.ofEpochMilli(mensaje.timestamp()).atOffset(ZoneOffset.UTC);
        }
        try {
            return Instant.parse(new String(h.value(), StandardCharsets.UTF_8)).atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException mal) {
            throw new IllegalArgumentException("occurred_at invalido", mal);
        }
    }

    private static String texto(JsonNode carga, String campo) {
        JsonNode nodo = carga.get(campo);
        return nodo == null || nodo.isNull() || nodo.asText().isBlank() ? null : nodo.asText();
    }

    private static String recortado(String valor, int largo) {
        return valor == null || valor.length() <= largo ? valor : valor.substring(0, largo - 1) + "…";
    }

    private static String cabecera(ConsumerRecord<String, String> mensaje, String nombre) {
        Header h = mensaje.headers().lastHeader(nombre);
        if (h == null || h.value() == null) {
            throw new IllegalArgumentException("Falta la cabecera " + nombre);
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
