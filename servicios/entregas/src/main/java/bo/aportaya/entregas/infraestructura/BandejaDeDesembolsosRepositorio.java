package bo.aportaya.entregas.infraestructura;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * La bandeja de desembolsos de Tesoreria (CU-28, backoffice carril A · H8): {@code orden_desembolso}
 * con su ultimo intento. Sin cuenta destino, glosa ni referencia del proveedor: la bandeja dice que
 * mirar, no a quien se le paga.
 */
@Component
public class BandejaDeDesembolsosRepositorio {

    public List<Fila> listar(
            DSLContext dsl,
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite) {
        OffsetDateTime desde = antesDe.orElse(null);
        UUID desdeId = antesDeId.orElse(null);
        return dsl.fetch(
                        """
                SELECT o.id, o.entrega_id, o.proveedor_id, o.monto, o.moneda, o.estado, o.creada_en,
                       o.acreditada_en,
                       (SELECT count(*) FROM entregas.intento_desembolso c WHERE c.orden_desembolso_id = o.id)
                           AS intentos,
                       u.resultado, u.codigo_error, u.reintentable_en
                  FROM entregas.orden_desembolso o
                  LEFT JOIN LATERAL (
                        SELECT i.resultado, i.codigo_error, i.reintentable_en
                          FROM entregas.intento_desembolso i
                         WHERE i.orden_desembolso_id = o.id
                         ORDER BY i.numero_intento DESC
                         LIMIT 1) u ON true
                 WHERE o.estado = ANY(?::varchar[])
                   AND (?::timestamptz IS NULL OR (o.creada_en, o.id) < (?::timestamptz, ?::uuid))
                 ORDER BY o.creada_en DESC, o.id DESC
                 LIMIT ?
                """,
                        estados.toArray(String[]::new),
                        desde,
                        desde,
                        desdeId,
                        limite)
                .map(f -> new Fila(
                        f.get("id", UUID.class),
                        f.get("entrega_id", UUID.class),
                        f.get("proveedor_id", UUID.class),
                        f.get("monto", BigDecimal.class),
                        f.get("moneda", String.class),
                        f.get("estado", String.class),
                        f.get("intentos", Integer.class),
                        f.get("resultado", String.class),
                        f.get("codigo_error", String.class),
                        f.get("reintentable_en", OffsetDateTime.class),
                        f.get("creada_en", OffsetDateTime.class),
                        f.get("acreditada_en", OffsetDateTime.class)));
    }

    public record Fila(
            UUID id,
            UUID entregaId,
            UUID proveedorId,
            BigDecimal monto,
            String moneda,
            String estado,
            int intentos,
            String ultimoResultado,
            String ultimoCodigoError,
            OffsetDateTime reintentableEn,
            OffsetDateTime creadaEn,
            OffsetDateTime acreditadaEn) {}
}
