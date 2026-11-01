package bo.aportaya.notificaciones.infraestructura;

import bo.aportaya.notificaciones.dominio.PlataformaDeMensajeria.EventoDeEntrega;
import bo.aportaya.notificaciones.dominio.PlataformaDeMensajeria.Proveedor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/** Lecturas para Sistemas › Proveedores y Webhooks. Solo columnas no sensibles; orden determinista. */
@Component
public class PlataformaDeMensajeriaRepositorio {

    public List<Proveedor> proveedores(DSLContext dsl) {
        return dsl.fetch(
                        """
                        select id, codigo, nombre, canales_soportados, activo, prioridad, salud_porcentaje
                          from notificaciones.proveedor_mensajeria
                         order by prioridad, codigo
                         limit 200
                        """)
                .map(r -> new Proveedor(
                        r.get("id", UUID.class),
                        r.get("codigo", String.class),
                        r.get("nombre", String.class),
                        r.get("canales_soportados", String.class),
                        r.get("activo", Boolean.class),
                        r.get("prioridad", Integer.class),
                        r.get("salud_porcentaje", BigDecimal.class)));
    }

    public List<EventoDeEntrega> eventos(DSLContext dsl, int limite) {
        return dsl.fetch(
                        """
                        select e.id, p.codigo, n.canal, e.tipo_evento, e.recibido_en, e.fecha_hora_proveedor,
                               e.codigo_error
                          from notificaciones.evento_entrega_mensaje e
                          join notificaciones.envio_notificacion n on n.id = e.envio_id
                          join notificaciones.proveedor_mensajeria p on p.id = n.proveedor_id
                         order by e.recibido_en desc, e.id
                         limit ?
                        """,
                        limite)
                .map(r -> new EventoDeEntrega(
                        r.get("id", UUID.class),
                        r.get("codigo", String.class),
                        r.get("canal", String.class),
                        r.get("tipo_evento", String.class),
                        r.get("recibido_en", OffsetDateTime.class),
                        r.get("fecha_hora_proveedor", OffsetDateTime.class),
                        Optional.ofNullable(r.get("codigo_error", String.class))));
    }
}
