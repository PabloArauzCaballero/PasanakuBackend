package bo.aportaya.aportes.infraestructura;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code aportes.webhook_pasarela}: el aviso firmado de la pasarela, sellado (R-BIL-33, R-AUD-01) y particionado por
 * mes de {@code recibido_en}.
 *
 * <p>El INSERT solo ocurre con la firma YA verificada — nunca se persiste (ni siquiera para auditar) un intento con
 * firma invalida, siguiendo al pie la regla de {@code payments-qr-integration} §4.1: "Firma invalida ⇒ 401 y nada
 * mas". El aviso no se edita ni se borra: la deduplicacion (proveedor, clave) vive en {@code webhook_clave}, unica en
 * todo el tiempo aunque el aviso este particionado, y el cierre del procesamiento agrega una fila a
 * {@code historial_estado_webhook}. El estado vigente es la ultima fila de ese historial; sin filas, RECIBIDO.
 */
@Component
public class WebhookRepositorio {

    /** La MISMA identidad que ampara {@code pk_webhook_clave}: nunca la clave sola. */
    public Optional<Registro> porClaveDeEvento(DSLContext dsl, UUID proveedorId, String clave) {
        Record fila = dsl.fetchOne(
                """
                SELECT w.id, COALESCE(h.estado_nuevo, w.estado) AS estado, COALESCE(h.pago_id, w.pago_id) AS pago_id
                  FROM aportes.webhook_clave k
                  JOIN aportes.webhook_pasarela w ON w.id = k.webhook_id AND w.recibido_en = k.webhook_recibido_en
                  LEFT JOIN LATERAL (
                        SELECT estado_nuevo, pago_id FROM aportes.historial_estado_webhook
                         WHERE webhook_id = w.id AND webhook_recibido_en = w.recibido_en
                         ORDER BY fecha_hora DESC, id DESC LIMIT 1) h ON true
                 WHERE k.proveedor_id = ?::uuid AND k.clave_idempotencia = ?::varchar
                """,
                proveedorId,
                clave);
        return Optional.ofNullable(fila)
                .map(f -> new Registro(
                        f.get("id", UUID.class), f.get("estado", String.class), f.get("pago_id", UUID.class)));
    }

    /**
     * El alta del aviso y de su clave, en la misma transaccion. Un segundo alta con la misma (proveedor, clave) choca
     * con {@code pk_webhook_clave} y deshace las dos filas: el duplicado nunca queda a medias.
     */
    public UUID registrar(
            DSLContext dsl,
            UUID proveedorId,
            String evento,
            String payloadCrudoJson,
            String firma,
            String claveIdempotencia,
            OffsetDateTime recibidoEn) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("aportes", "webhook_pasarela")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("proveedor_id", UUID.class), proveedorId)
                .set(DSL.field("evento", String.class), evento)
                .set(DSL.field("payload_crudo", JSONB.class), JSONB.valueOf(payloadCrudoJson))
                .set(DSL.field("firma", String.class), firma)
                .set(DSL.field("firma_valida", Boolean.class), true)
                .set(DSL.field("recibido_en", OffsetDateTime.class), recibidoEn)
                .set(DSL.field("estado", String.class), "RECIBIDO")
                .set(DSL.field("clave_idempotencia", String.class), claveIdempotencia)
                .execute();
        dsl.insertInto(DSL.table(DSL.name("aportes", "webhook_clave")))
                .set(DSL.field("proveedor_id", UUID.class), proveedorId)
                .set(DSL.field("clave_idempotencia", String.class), claveIdempotencia)
                .set(DSL.field("webhook_id", UUID.class), id)
                .set(DSL.field("webhook_recibido_en", OffsetDateTime.class), recibidoEn)
                .execute();
        return id;
    }

    /**
     * Cierra el procesamiento UNA vez: PROCESADO, DESCARTADO o FALLIDO, como una fila de historial (nunca un UPDATE
     * del aviso). Desde RECIBIDO se sale una sola vez ({@code uq_historial_estado_webhook_salida}): una segunda salida
     * no hace nada.
     */
    public void marcarProcesado(
            DSLContext dsl, UUID webhookId, String estado, String error, UUID pagoId, OffsetDateTime procesadoEn) {
        dsl.execute(
                """
                INSERT INTO aportes.historial_estado_webhook
                       (webhook_id, webhook_recibido_en, estado_anterior, estado_nuevo, pago_id, error_procesamiento,
                        fecha_hora)
                SELECT w.id, w.recibido_en, 'RECIBIDO', ?::varchar, ?::uuid, ?::text, ?::timestamptz
                  FROM aportes.webhook_pasarela w
                 WHERE w.id = ?::uuid
                ON CONFLICT (webhook_id, webhook_recibido_en, estado_anterior) DO NOTHING
                """,
                estado,
                pagoId,
                error,
                procesadoEn,
                webhookId);
    }

    public record Registro(UUID id, String estado, UUID pagoId) {}
}
