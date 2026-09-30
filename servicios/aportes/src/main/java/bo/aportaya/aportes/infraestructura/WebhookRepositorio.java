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
 * {@code aportes.webhook_pasarela}: append-only una vez firmado (invariante 7 +
 * {@code uq_webhook_idem (proveedor_id, clave_idempotencia)}).
 *
 * <p>El INSERT solo ocurre con la firma YA verificada — nunca se persiste (ni
 * siquiera para auditar) un intento con firma invalida, siguiendo al pie la regla de
 * {@code payments-qr-integration} §4.1: "Firma invalida ⇒ 401 y nada mas". El
 * registro no se borra nunca; {@code estado}/{@code procesado_en}/{@code pago_id} se
 * actualizan una sola vez, al terminar de procesar ese mismo evento.
 */
@Component
public class WebhookRepositorio {

    /** La MISMA identidad que ampara {@code uq_webhook_idem}: nunca la clave sola. */
    public Optional<Registro> porClaveDeEvento(DSLContext dsl, UUID proveedorId, String clave) {
        Record fila = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("estado", String.class),
                        DSL.field("pago_id", UUID.class))
                .from(DSL.table(DSL.name("aportes", "webhook_pasarela")))
                .where(DSL.field("proveedor_id", UUID.class).eq(proveedorId))
                .and(DSL.field("clave_idempotencia").eq(clave))
                .fetchOne();
        return Optional.ofNullable(fila)
                .map(f -> new Registro(
                        f.get("id", UUID.class), f.get("estado", String.class), f.get("pago_id", UUID.class)));
    }

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
        return id;
    }

    /** Cierra el procesamiento UNA vez: PROCESADO, DESCARTADO o FALLIDO. */
    public void marcarProcesado(
            DSLContext dsl, UUID webhookId, String estado, String error, UUID pagoId, OffsetDateTime procesadoEn) {
        dsl.update(DSL.table(DSL.name("aportes", "webhook_pasarela")))
                .set(DSL.field("estado", String.class), estado)
                .set(DSL.field("error_procesamiento", String.class), error)
                .set(DSL.field("pago_id", UUID.class), pagoId)
                .set(DSL.field("procesado_en", OffsetDateTime.class), procesadoEn)
                .set(
                        DSL.field("intentos_procesamiento", Short.class),
                        DSL.field("intentos_procesamiento", Short.class).plus((short) 1))
                .where(DSL.field("id", UUID.class).eq(webhookId))
                .execute();
    }

    public record Registro(UUID id, String estado, UUID pagoId) {}
}
