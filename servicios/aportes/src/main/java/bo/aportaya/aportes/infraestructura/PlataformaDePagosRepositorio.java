package bo.aportaya.aportes.infraestructura;

import bo.aportaya.aportes.dominio.PlataformaDePagos.Proveedor;
import bo.aportaya.aportes.dominio.PlataformaDePagos.Webhook;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/** Lecturas para Sistemas › Proveedores y Webhooks. Solo columnas no sensibles; orden determinista. */
@Component
public class PlataformaDePagosRepositorio {

    public List<Proveedor> proveedores(DSLContext dsl) {
        return dsl.fetch(
                        """
                        select id, codigo, nombre, tipo, activo, soporta_webhook, prioridad
                          from aportes.proveedor_pago
                         order by prioridad, codigo
                         limit 200
                        """)
                .map(r -> new Proveedor(
                        r.get("id", UUID.class),
                        r.get("codigo", String.class),
                        r.get("nombre", String.class),
                        r.get("tipo", String.class),
                        r.get("activo", Boolean.class),
                        r.get("soporta_webhook", Boolean.class),
                        r.get("prioridad", Integer.class)));
    }

    public List<Webhook> webhooks(DSLContext dsl, int limite) {
        return dsl.fetch(
                        """
                        select w.id, p.codigo, w.evento, w.estado, w.firma_valida, w.recibido_en, w.procesado_en,
                               w.intentos_procesamiento
                          from aportes.webhook_pasarela w
                          join aportes.proveedor_pago p on p.id = w.proveedor_id
                         order by w.recibido_en desc, w.id
                         limit ?
                        """,
                        limite)
                .map(r -> new Webhook(
                        r.get("id", UUID.class),
                        r.get("codigo", String.class),
                        r.get("evento", String.class),
                        r.get("estado", String.class),
                        r.get("firma_valida", Boolean.class),
                        r.get("recibido_en", OffsetDateTime.class),
                        Optional.ofNullable(r.get("procesado_en", OffsetDateTime.class)),
                        r.get("intentos_procesamiento", Integer.class)));
    }
}
