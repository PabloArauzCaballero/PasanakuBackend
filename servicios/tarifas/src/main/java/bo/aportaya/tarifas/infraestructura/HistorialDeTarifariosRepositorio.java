package bo.aportaya.tarifas.infraestructura;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * El historial de tarifarios que mira el backoffice (CU-34, backoffice carril A · A6):
 * {@code catalogo.tarifario} con el expediente del cambio que publico cada version y su ultima
 * simulacion. Sin el escenario ni el resultado JSON de la simulacion: la lista dice que version rige
 * y por que, no reproduce el calculo.
 */
@Component
public class HistorialDeTarifariosRepositorio {

    public List<Fila> listar(
            DSLContext dsl,
            Optional<String> codigo,
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite) {
        OffsetDateTime desde = antesDe.orElse(null);
        UUID desdeId = antesDeId.orElse(null);
        return dsl.fetch(
                        """
                SELECT t.id, t.codigo, t.version, t.nombre, t.estado, t.moneda_base, t.vigente_desde,
                       t.vigente_hasta, t.dias_preaviso, t.publicado_en, t.tarifario_anterior_id,
                       t.aprobado_por, t.acta_aprobacion,
                       c.id AS cambio_id, c.tipo_cambio, c.requiere_preaviso,
                       c.dias_preaviso AS cambio_dias_preaviso, c.fecha_aviso, c.canal_aviso,
                       c.usuarios_notificados, c.permite_rescision_sin_costo,
                       s.delta_ingreso_estimado, s.usuarios_impactados, s.ejecutada_en
                  FROM catalogo.tarifario t
                  LEFT JOIN tarifas.cambio_tarifario c ON c.tarifario_nuevo_id = t.id
                  LEFT JOIN LATERAL (
                        SELECT x.delta_ingreso_estimado, x.usuarios_impactados, x.ejecutada_en
                          FROM tarifas.simulacion_tarifa x
                         WHERE x.tarifario_id = t.id
                         ORDER BY x.ejecutada_en DESC, x.id DESC
                         LIMIT 1) s ON true
                 WHERE (?::varchar IS NULL OR t.codigo = ?::varchar)
                   AND t.estado = ANY(?::varchar[])
                   AND (?::timestamptz IS NULL OR (t.vigente_desde, t.id) < (?::timestamptz, ?::uuid))
                 ORDER BY t.vigente_desde DESC, t.id DESC
                 LIMIT ?
                """,
                        codigo.orElse(null),
                        codigo.orElse(null),
                        estados.toArray(String[]::new),
                        desde,
                        desde,
                        desdeId,
                        limite)
                .map(f -> new Fila(
                        f.get("id", UUID.class),
                        f.get("codigo", String.class),
                        f.get("version", Integer.class),
                        f.get("nombre", String.class),
                        f.get("estado", String.class),
                        f.get("moneda_base", String.class),
                        f.get("vigente_desde", OffsetDateTime.class),
                        f.get("vigente_hasta", OffsetDateTime.class),
                        f.get("dias_preaviso", Integer.class),
                        f.get("publicado_en", OffsetDateTime.class),
                        f.get("tarifario_anterior_id", UUID.class),
                        f.get("aprobado_por", UUID.class),
                        f.get("acta_aprobacion", String.class),
                        f.get("cambio_id", UUID.class),
                        f.get("tipo_cambio", String.class),
                        f.get("requiere_preaviso", Boolean.class),
                        f.get("cambio_dias_preaviso", Integer.class),
                        f.get("fecha_aviso", OffsetDateTime.class),
                        f.get("canal_aviso", String.class),
                        f.get("usuarios_notificados", Integer.class),
                        f.get("permite_rescision_sin_costo", Boolean.class),
                        f.get("delta_ingreso_estimado", BigDecimal.class),
                        f.get("usuarios_impactados", Integer.class),
                        f.get("ejecutada_en", OffsetDateTime.class)));
    }

    /** Una version del tarifario; las columnas del cambio y de la simulacion vienen nulas si no hay. */
    public record Fila(
            UUID id,
            String codigo,
            int version,
            String nombre,
            String estado,
            String monedaBase,
            OffsetDateTime vigenteDesde,
            OffsetDateTime vigenteHasta,
            int diasPreaviso,
            OffsetDateTime publicadoEn,
            UUID tarifarioAnteriorId,
            UUID aprobadoPor,
            String actaAprobacion,
            UUID cambioId,
            String tipoCambio,
            Boolean requierePreaviso,
            Integer cambioDiasPreaviso,
            OffsetDateTime fechaAviso,
            String canalAviso,
            Integer usuariosNotificados,
            Boolean permiteRescisionSinCosto,
            BigDecimal deltaIngresoEstimado,
            Integer usuariosImpactados,
            OffsetDateTime simuladaEn) {}
}
