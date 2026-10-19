package bo.aportaya.notificaciones.infraestructura;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Lo que Auditoria mira de la mensajeria (backoffice carril A · A6): las versiones de plantilla y los
 * intentos de envio con su ultimo acuse.
 *
 * <p>Las columnas se nombran una por una y a proposito <b>no</b> se leen {@code destinatario},
 * {@code contenido_enviado}, {@code id_mensaje_proveedor}, {@code clave_idempotencia}, {@code cuerpo},
 * {@code asunto} ni {@code payload_crudo}: telefono, correo y texto del mensaje son datos personales o
 * contenido (regla 90.2), y lo que no se lee no se puede filtrar despues.
 */
@Component
public class AuditoriaDeMensajeriaRepositorio {

    public List<Plantilla> plantillas(
            DSLContext dsl,
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite) {
        OffsetDateTime desde = antesDe.orElse(null);
        UUID desdeId = antesDeId.orElse(null);
        return dsl.fetch(
                        """
                SELECT p.id, p.codigo, e.tipo AS evento, p.canal, p.categoria_proveedor, p.estado_aprobacion,
                       p.activa, v.id AS version_id, v.version, v.idioma, v.vigente_desde, v.vigente_hasta
                  FROM notificaciones.version_plantilla v
                  JOIN notificaciones.plantilla_mensaje p ON p.id = v.plantilla_id
                  JOIN notificaciones.evento_notificable e ON e.id = p.evento_id
                 WHERE p.estado_aprobacion = ANY(?::varchar[])
                   AND (?::timestamptz IS NULL OR (v.vigente_desde, v.id) < (?::timestamptz, ?::uuid))
                 ORDER BY v.vigente_desde DESC, v.id DESC
                 LIMIT ?
                """,
                        estados.toArray(String[]::new),
                        desde,
                        desde,
                        desdeId,
                        limite)
                .map(f -> new Plantilla(
                        f.get("id", UUID.class),
                        f.get("codigo", String.class),
                        f.get("evento", String.class),
                        f.get("canal", String.class),
                        f.get("categoria_proveedor", String.class),
                        f.get("estado_aprobacion", String.class),
                        f.get("activa", Boolean.class),
                        f.get("version_id", UUID.class),
                        f.get("version", Integer.class),
                        f.get("idioma", String.class),
                        f.get("vigente_desde", OffsetDateTime.class),
                        f.get("vigente_hasta", OffsetDateTime.class)));
    }

    public List<Envio> envios(
            DSLContext dsl,
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite) {
        OffsetDateTime desde = antesDe.orElse(null);
        UUID desdeId = antesDeId.orElse(null);
        return dsl.fetch(
                        """
                SELECT s.id, s.notificacion_id, s.proveedor_id, pr.codigo AS proveedor, s.version_plantilla_id,
                       s.canal, s.estado, s.orden, s.intentos, s.max_intentos, s.costo, s.moneda,
                       s.codigo_error, s.encolado_en, s.enviado_en, s.entregado_en, s.leido_en,
                       s.proximo_reintento_en,
                       a.tipo_evento, a.fecha_hora_proveedor, a.recibido_en, a.codigo_error AS acuse_codigo_error
                  FROM notificaciones.envio_notificacion s
                  JOIN notificaciones.proveedor_mensajeria pr ON pr.id = s.proveedor_id
                  LEFT JOIN LATERAL (
                        SELECT m.tipo_evento, m.fecha_hora_proveedor, m.recibido_en, m.codigo_error
                          FROM notificaciones.evento_entrega_mensaje m
                         WHERE m.envio_id = s.id
                         ORDER BY m.fecha_hora_proveedor DESC, m.id DESC
                         LIMIT 1) a ON true
                 WHERE s.estado = ANY(?::varchar[])
                   AND (?::timestamptz IS NULL OR (s.encolado_en, s.id) < (?::timestamptz, ?::uuid))
                 ORDER BY s.encolado_en DESC, s.id DESC
                 LIMIT ?
                """,
                        estados.toArray(String[]::new),
                        desde,
                        desde,
                        desdeId,
                        limite)
                .map(f -> new Envio(
                        f.get("id", UUID.class),
                        f.get("notificacion_id", UUID.class),
                        f.get("proveedor_id", UUID.class),
                        f.get("proveedor", String.class),
                        f.get("version_plantilla_id", UUID.class),
                        f.get("canal", String.class),
                        f.get("estado", String.class),
                        f.get("orden", Integer.class),
                        f.get("intentos", Integer.class),
                        f.get("max_intentos", Integer.class),
                        f.get("costo", BigDecimal.class),
                        f.get("moneda", String.class),
                        f.get("codigo_error", String.class),
                        f.get("encolado_en", OffsetDateTime.class),
                        f.get("enviado_en", OffsetDateTime.class),
                        f.get("entregado_en", OffsetDateTime.class),
                        f.get("leido_en", OffsetDateTime.class),
                        f.get("proximo_reintento_en", OffsetDateTime.class),
                        f.get("tipo_evento", String.class),
                        f.get("fecha_hora_proveedor", OffsetDateTime.class),
                        f.get("recibido_en", OffsetDateTime.class),
                        f.get("acuse_codigo_error", String.class)));
    }

    public record Plantilla(
            UUID id,
            String codigo,
            String evento,
            String canal,
            String categoriaProveedor,
            String estadoAprobacion,
            boolean activa,
            UUID versionId,
            int version,
            String idioma,
            OffsetDateTime vigenteDesde,
            OffsetDateTime vigenteHasta) {}

    /** Un intento de envio; las columnas del acuse vienen nulas si el proveedor todavia no contesto. */
    public record Envio(
            UUID id,
            UUID notificacionId,
            UUID proveedorId,
            String proveedor,
            UUID versionPlantillaId,
            String canal,
            String estado,
            int orden,
            int intentos,
            int maxIntentos,
            BigDecimal costo,
            String moneda,
            String codigoError,
            OffsetDateTime encoladoEn,
            OffsetDateTime enviadoEn,
            OffsetDateTime entregadoEn,
            OffsetDateTime leidoEn,
            OffsetDateTime proximoReintentoEn,
            String acuseTipo,
            OffsetDateTime acuseFechaHoraProveedor,
            OffsetDateTime acuseRecibidoEn,
            String acuseCodigoError) {}
}
