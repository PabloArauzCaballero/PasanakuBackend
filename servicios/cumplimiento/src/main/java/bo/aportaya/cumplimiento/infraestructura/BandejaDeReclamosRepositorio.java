package bo.aportaya.cumplimiento.infraestructura;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/**
 * La bandeja y la ficha de {@code reclamo_cliente}: solo lectura (CU-52, backoffice carril A · H3).
 *
 * <p>El orden es el limite vigente —la prorroga si la hay, si no el plazo de respuesta— y el id
 * desempata: dos reclamos que vencen en el mismo instante no se reordenan entre paginas. Los plazos
 * son los guardados (R-CON-01): aca no se calcula ninguno.
 */
@Component
public class BandejaDeReclamosRepositorio {

    private static final String COLUMNAS =
            """
        SELECT r.id, r.codigo, r.categoria, r.producto, r.canal_ingreso, r.monto_reclamado, r.estado,
               r.fecha_ingreso, r.plazo_respuesta, r.plazo_prorrogado_hasta, r.dias_habiles_plazo,
               COALESCE(r.plazo_prorrogado_hasta, r.plazo_respuesta) AS limite_vigente, r.responsable_id
        """;

    public List<EnBandeja> pagina(
            DSLContext dsl,
            List<String> estados,
            Optional<OffsetDateTime> despuesDeLimite,
            Optional<UUID> despuesDeId,
            int limite) {
        var parametros = new ArrayList<Object>();
        parametros.add(estados.toArray(String[]::new));
        String cursor = "";
        if (despuesDeLimite.isPresent() && despuesDeId.isPresent()) {
            cursor = " AND (COALESCE(r.plazo_prorrogado_hasta, r.plazo_respuesta), r.id) > (?::timestamptz, ?::uuid)";
            parametros.add(despuesDeLimite.get());
            parametros.add(despuesDeId.get());
        }
        parametros.add(limite);
        return dsl.fetch(
                        COLUMNAS + " FROM cumplimiento.reclamo_cliente r WHERE r.estado = ANY(?::varchar[])" + cursor
                                + " ORDER BY limite_vigente, r.id LIMIT ?",
                        parametros.toArray())
                .map(BandejaDeReclamosRepositorio::enBandeja);
    }

    public Optional<Ficha> ficha(DSLContext dsl, UUID reclamoId) {
        return Optional.ofNullable(dsl.fetchOne(
                        COLUMNAS
                                + ", r.descripcion, r.resultado, r.respuesta, r.fecha_respuesta, r.devolucion_comision_id"
                                + " FROM cumplimiento.reclamo_cliente r WHERE r.id = ?",
                        reclamoId))
                .map(f -> new Ficha(
                        enBandeja(f),
                        f.get("descripcion", String.class),
                        f.get("resultado", String.class),
                        f.get("respuesta", String.class),
                        f.get("fecha_respuesta", OffsetDateTime.class),
                        f.get("devolucion_comision_id", UUID.class)));
    }

    private static EnBandeja enBandeja(Record f) {
        return new EnBandeja(
                f.get("id", UUID.class),
                f.get("codigo", String.class),
                f.get("categoria", String.class),
                f.get("producto", String.class),
                f.get("canal_ingreso", String.class),
                f.get("monto_reclamado", BigDecimal.class),
                f.get("estado", String.class),
                f.get("fecha_ingreso", OffsetDateTime.class),
                f.get("plazo_respuesta", OffsetDateTime.class),
                f.get("plazo_prorrogado_hasta", OffsetDateTime.class),
                f.get("dias_habiles_plazo", Integer.class),
                f.get("limite_vigente", OffsetDateTime.class),
                f.get("responsable_id", UUID.class));
    }

    /** Una fila de la bandeja: sin texto del reclamo ni persona que reclama (respuesta minima). */
    public record EnBandeja(
            UUID id,
            String codigo,
            String categoria,
            String producto,
            String canalIngreso,
            BigDecimal montoReclamado,
            String estado,
            OffsetDateTime fechaIngreso,
            OffsetDateTime plazoRespuesta,
            OffsetDateTime plazoProrrogadoHasta,
            int diasHabilesPlazo,
            OffsetDateTime limiteVigente,
            UUID responsableId) {}

    public record Ficha(
            EnBandeja reclamo,
            String descripcion,
            String resultado,
            String respuesta,
            OffsetDateTime fechaRespuesta,
            UUID devolucionId) {}
}
