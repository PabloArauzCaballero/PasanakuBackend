package bo.aportaya.identidad.aplicacion;

import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Deja cada paso de la cuenta del operador en la bitacora y pide el aviso (decision P-5), en la MISMA
 * transaccion que el paso: outbox, nunca una publicacion aparte (regla 98.3.3).
 *
 * <p>Misma forma v1 que los actos de gobierno de CU-08: el hecho ({@code identidad.usuario_interno_*}),
 * que {@code auditoria} escribe en {@code bitacora_evento}, y {@code identidad.aviso_de_acceso_solicitado}
 * para la persona afectada y los oficiales de cumplimiento vigentes. Solo identificadores: el correo lo
 * resuelve {@code notificaciones}; nunca viaja correo, telefono, clave ni token (regla 90.2.1).
 */
@Component
public class BitacoraDeCuenta {

    static final String VERSION = "1";
    static final String EVENTO_NOTIFICABLE = "ACCESO_DE_OPERADOR_CAMBIADO";
    /** El NOMBRE del canal, no una direccion: la direccion la resuelve notificaciones. */
    static final String CANAL_DEL_AVISO = "CORREO";

    static final String ROL_QUE_SE_ENTERA = "OFICIAL_CUMPLIMIENTO";

    /** Quien actua cuando es la propia persona, sin sesion (el enlace del correo). */
    public static final String ROL_DEL_TITULAR = "TITULAR_DE_LA_CUENTA";

    /** Origen del catalogo de {@code bitacora_evento} para lo que llega por un endpoint sin sesion. */
    public static final String ORIGEN_PUBLICO = "API_PUBLICA";

    public static final String ORIGEN_PANEL = "PANEL_ADMIN";
    public static final String ORIGEN_TAREA = "TAREA_PROGRAMADA";

    public enum Accion {
        INVITACION_EMITIDA("identidad.usuario_interno_invitacion_emitida"),
        INVITACION_ACEPTADA("identidad.usuario_interno_invitacion_aceptada"),
        RECUPERACION_SOLICITADA("identidad.usuario_interno_recuperacion_solicitada"),
        RECUPERACION_APROBADA("identidad.usuario_interno_recuperacion_aprobada"),
        RECUPERACION_RECHAZADA("identidad.usuario_interno_recuperacion_rechazada"),
        RECUPERACION_EMITIDA("identidad.usuario_interno_recuperacion_emitida"),
        CLAVE_RESTABLECIDA("identidad.usuario_interno_clave_restablecida"),
        FACTOR_REINSCRITO("identidad.usuario_interno_factor_reinscrito"),
        CLAVE_CAMBIADA("identidad.usuario_interno_clave_cambiada");

        private final String tipoDeEvento;

        Accion(String tipoDeEvento) {
            this.tipoDeEvento = tipoDeEvento;
        }

        public String tipoDeEvento() {
            return tipoDeEvento;
        }
    }

    /** Quien actuo y desde donde: lo que el consumidor de auditoria lee de la carga. */
    public record Actor(UUID id, String rol, String origen) {}

    private final Outbox outbox;

    public BitacoraDeCuenta(Outbox outbox) {
        this.outbox = outbox;
    }

    public void registrar(
            DSLContext dsl,
            Accion accion,
            UUID afectadoId,
            Actor actor,
            String motivo,
            Map<String, Object> detalle,
            UUID correlacion,
            OffsetDateTime ahora) {
        Map<String, Object> carga = new LinkedHashMap<>();
        carga.put("version", VERSION);
        carga.put("accion", accion.name());
        carga.put("actorId", actor.id().toString());
        carga.put("actorRol", actor.rol());
        carga.put("origen", actor.origen());
        carga.put("afectadoId", afectadoId.toString());
        carga.put("motivo", motivo == null ? "" : motivo);
        carga.put("ocurridoEn", ahora.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        carga.putAll(detalle);
        outbox.emitir(dsl, new EventoDominio(accion.tipoDeEvento(), "usuario", afectadoId, carga, correlacion));

        Set<String> destinatarios = new LinkedHashSet<>();
        destinatarios.add(afectadoId.toString());
        oficialesVigentes(dsl, ahora).forEach(oficial -> destinatarios.add(oficial.toString()));
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "identidad.aviso_de_acceso_solicitado",
                        "usuario",
                        afectadoId,
                        Map.of(
                                "version", VERSION,
                                "eventoNotificable", EVENTO_NOTIFICABLE,
                                "eventoOrigen", accion.tipoDeEvento(),
                                "accion", accion.name(),
                                "afectadoId", afectadoId.toString(),
                                "canal", CANAL_DEL_AVISO,
                                "destinatarios", List.copyOf(destinatarios)),
                        correlacion));
    }

    private List<UUID> oficialesVigentes(DSLContext dsl, OffsetDateTime ahora) {
        return dsl.fetch(
                        """
                        SELECT DISTINCT a.usuario_id FROM identidad.asignacion_rol a
                        JOIN identidad.rol r ON r.id = a.rol_id
                        JOIN identidad.usuario u ON u.id = a.usuario_id
                        WHERE r.codigo = ? AND a.revocada_en IS NULL
                          AND (a.vigente_hasta IS NULL OR a.vigente_hasta > ?::timestamptz)
                          AND u.estado = 'ACTIVO'
                        """,
                        ROL_QUE_SE_ENTERA,
                        ahora)
                .map(fila -> fila.get(0, UUID.class));
    }
}
