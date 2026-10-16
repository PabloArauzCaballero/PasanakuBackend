package bo.aportaya.auditoria.infraestructura;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * El catalogo de reportes y su historial de ejecuciones (CU-58, backoffice carril A · A6). Nunca lee
 * {@code consulta_base}, {@code parametros} ni {@code mensaje_error}: la primera es SQL interno y los otros
 * dos pueden llevar datos de una persona.
 */
@Component
public class HistorialDeReportesRepositorio {

    public List<Definicion> definiciones(DSLContext dsl, String despuesDe, UUID despuesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT id, tipo, nombre, descripcion, permiso_requerido, contiene_datos_sensibles
                  FROM auditoria.definicion_reporte
                 WHERE activa
                   AND (?::varchar IS NULL OR (nombre, id) > (?::varchar, ?::uuid))
                 ORDER BY nombre, id
                 LIMIT ?
                """,
                        despuesDe,
                        despuesDe,
                        despuesDeId,
                        limite)
                .map(f -> new Definicion(
                        f.get("id", UUID.class),
                        f.get("tipo", String.class),
                        f.get("nombre", String.class),
                        f.get("descripcion", String.class),
                        f.get("permiso_requerido", String.class),
                        f.get("contiene_datos_sensibles", Boolean.class)));
    }

    public List<Ejecucion> ejecuciones(
            DSLContext dsl, List<String> estados, OffsetDateTime antesDe, UUID antesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT id, definicion_id, solicitado_por, estado, filas_generadas, duracion_ms, hash_resultado,
                       iniciada_en, finalizada_en
                  FROM auditoria.ejecucion_reporte
                 WHERE estado = ANY(?::varchar[])
                   AND (?::timestamptz IS NULL OR (iniciada_en, id) < (?::timestamptz, ?::uuid))
                 ORDER BY iniciada_en DESC, id DESC
                 LIMIT ?
                """,
                        estados.toArray(String[]::new),
                        antesDe,
                        antesDe,
                        antesDeId,
                        limite)
                .map(f -> new Ejecucion(
                        f.get("id", UUID.class),
                        f.get("definicion_id", UUID.class),
                        f.get("solicitado_por", UUID.class),
                        f.get("estado", String.class),
                        f.get("filas_generadas", Integer.class),
                        f.get("duracion_ms", Integer.class),
                        f.get("hash_resultado", String.class),
                        f.get("iniciada_en", OffsetDateTime.class),
                        f.get("finalizada_en", OffsetDateTime.class)));
    }

    public record Definicion(
            UUID id, String tipo, String nombre, String descripcion, String permisoRequerido, boolean sensible) {}

    public record Ejecucion(
            UUID id,
            UUID definicionId,
            UUID solicitadoPor,
            String estado,
            int filas,
            int duracionMs,
            String hash,
            OffsetDateTime iniciadaEn,
            OffsetDateTime finalizadaEn) {}
}
