package bo.aportaya.aportes.infraestructura;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Component;

/** Registra avisos ya autenticados y sus transiciones sin modificar los hechos guardados. */
@Component
public class WebhookRepositorio {

    public Optional<Registro> porClaveDeEvento(DSLContext dsl, UUID proveedorId, String clave) {
        return Optional.ofNullable(dsl.fetchOne(
                        """
                        select c.webhook_id, h.pago_id
                          from aportes.webhook_clave c
                         cross join lateral (
                           select h.pago_id
                             from aportes.historial_estado_webhook h
                            where h.webhook_id = c.webhook_id and h.webhook_recibido_en = c.webhook_recibido_en
                            order by (h.estado_anterior is null), h.procesado_en desc nulls last, h.id
                            limit 1
                         ) h
                         where c.proveedor_id = ? and c.clave_idempotencia = ?
                        """,
                        proveedorId,
                        clave))
                .map(r -> new Registro(r.get("webhook_id", UUID.class), r.get("pago_id", UUID.class)));
    }

    /** Reserva la clave global antes de escribir el aviso. Si otra transacción la ganó, no crea filas. */
    public Optional<UUID> registrar(
            DSLContext dsl,
            UUID proveedorId,
            String evento,
            String payloadCrudoJson,
            String firma,
            String claveIdempotencia,
            OffsetDateTime recibidoEn) {
        return Optional.ofNullable(dsl.fetchOne(
                        """
                        with clave as (
                          insert into aportes.webhook_clave
                            (proveedor_id, clave_idempotencia, webhook_id, webhook_recibido_en)
                          values (?, ?, ?, ?::timestamptz)
                          on conflict (proveedor_id, clave_idempotencia) do nothing
                          returning proveedor_id, clave_idempotencia, webhook_id, webhook_recibido_en
                        ), aviso as (
                          insert into aportes.webhook_pasarela
                            (id, proveedor_id, evento, payload_crudo, firma, firma_valida, recibido_en, clave_idempotencia)
                          select webhook_id, proveedor_id, ?, ?::jsonb, ?, true, webhook_recibido_en, clave_idempotencia
                            from clave
                          returning id, recibido_en
                        )
                        insert into aportes.historial_estado_webhook (webhook_id, webhook_recibido_en, estado_nuevo)
                        select id, recibido_en, 'RECIBIDO' from aviso
                        returning webhook_id
                        """,
                        proveedorId,
                        claveIdempotencia,
                        UUID.randomUUID(),
                        recibidoEn,
                        evento,
                        JSONB.valueOf(payloadCrudoJson),
                        firma))
                .map(r -> r.get("webhook_id", UUID.class));
    }

    /** El índice del historial admite una sola salida desde RECIBIDO por aviso. */
    public void marcarProcesado(
            DSLContext dsl,
            UUID webhookId,
            OffsetDateTime recibidoEn,
            String estado,
            String error,
            UUID pagoId,
            OffsetDateTime procesadoEn) {
        int transiciones = dsl.execute(
                """
                insert into aportes.historial_estado_webhook
                  (webhook_id, webhook_recibido_en, estado_anterior, estado_nuevo, error_procesamiento, pago_id,
                   procesado_en, intentos_procesamiento)
                select webhook_id, webhook_recibido_en, estado_nuevo, ?, ?, ?::uuid, ?::timestamptz,
                       intentos_procesamiento + 1
                  from aportes.historial_estado_webhook
                 where webhook_id = ? and webhook_recibido_en = ?::timestamptz and estado_anterior is null
                   and estado_nuevo = 'RECIBIDO'
                """,
                estado,
                error,
                pagoId,
                procesadoEn,
                webhookId,
                recibidoEn);
        if (transiciones != 1) {
            throw new IllegalStateException("El aviso no tiene un estado inicial RECIBIDO");
        }
    }

    public record Registro(UUID id, UUID pagoId) {}
}
