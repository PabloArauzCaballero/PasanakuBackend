package bo.aportaya.erp.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/**
 * Las lecturas de listado del backoffice (carril A · A6) de presupuesto y activos fijos. Solo lectura y
 * solo del esquema {@code erp} (invariante 11).
 *
 * <p>Cada consulta es UN texto fijo: los filtros opcionales viajan como parametros nulos
 * ({@code ?::uuid IS NULL OR ...}) y no como SQL armado. El cursor es keyset {@code (columna, id)}
 * descendente: el id desempata, asi que dos filas del mismo dia no se repiten ni se pierden entre paginas.
 */
@Component
public class ListadosDeErpRepositorio {

    public List<Partida> partidas(
            DSLContext dsl, UUID ejercicioId, List<String> estados, LocalDate antesDe, UUID antesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT pa.id, pa.presupuesto_id, pr.nombre AS presupuesto_nombre, pr.estado,
                       pr.centro_costo_id, cc.nombre AS centro_nombre, pa.cuenta_contable_id,
                       pa.periodo_contable_id, ej.anio, pe.mes, pe.fecha_inicio,
                       pa.monto_presupuestado, pa.monto_ejecutado, pa.moneda
                  FROM erp.partida_presupuestaria pa
                  JOIN erp.presupuesto pr ON pr.id = pa.presupuesto_id
                  JOIN erp.centro_costo cc ON cc.id = pr.centro_costo_id
                  JOIN erp.periodo_contable pe ON pe.id = pa.periodo_contable_id
                  JOIN erp.ejercicio_fiscal ej ON ej.id = pe.ejercicio_fiscal_id
                 WHERE (?::uuid IS NULL OR pr.ejercicio_fiscal_id = ?::uuid)
                   AND pr.estado = ANY (?::varchar[])
                   AND (?::date IS NULL OR (pe.fecha_inicio, pa.id) < (?::date, ?::uuid))
                 ORDER BY pe.fecha_inicio DESC, pa.id DESC
                 LIMIT ?
                """,
                        ejercicioId,
                        ejercicioId,
                        arreglo(estados),
                        antesDe,
                        antesDe,
                        antesDeId,
                        limite)
                .map(f -> new Partida(
                        f.get("id", UUID.class),
                        f.get("presupuesto_id", UUID.class),
                        f.get("presupuesto_nombre", String.class),
                        f.get("estado", String.class),
                        f.get("centro_costo_id", UUID.class),
                        f.get("centro_nombre", String.class),
                        f.get("cuenta_contable_id", UUID.class),
                        f.get("periodo_contable_id", UUID.class),
                        f.get("anio", Integer.class),
                        f.get("mes", Integer.class),
                        f.get("fecha_inicio", LocalDate.class),
                        f.get("monto_presupuestado", BigDecimal.class),
                        f.get("monto_ejecutado", BigDecimal.class),
                        moneda(f)));
    }

    /** {@code hoy} es la fecha de America/La_Paz: el periodo vigente es el que la contiene. */
    public List<Activo> activos(
            DSLContext dsl, LocalDate hoy, List<String> estados, LocalDate antesDe, UUID antesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT a.id, a.codigo_inventario, a.descripcion, ca.nombre AS categoria_nombre,
                       ca.vida_util_meses, a.fecha_adquisicion, a.estado, a.costo_adquisicion,
                       a.valor_residual, a.depreciacion_acumulada, a.moneda,
                       EXISTS (SELECT 1 FROM erp.depreciacion_activo d
                                 JOIN erp.periodo_contable p ON p.id = d.periodo_contable_id
                                WHERE d.activo_fijo_id = a.id
                                  AND ?::date BETWEEN p.fecha_inicio AND p.fecha_fin) AS depreciado_en_vigente
                  FROM erp.activo_fijo a
                  JOIN erp.categoria_activo_fijo ca ON ca.id = a.categoria_activo_fijo_id
                 WHERE a.estado = ANY (?::varchar[])
                   AND (?::date IS NULL OR (a.fecha_adquisicion, a.id) < (?::date, ?::uuid))
                 ORDER BY a.fecha_adquisicion DESC, a.id DESC
                 LIMIT ?
                """,
                        hoy,
                        arreglo(estados),
                        antesDe,
                        antesDe,
                        antesDeId,
                        limite)
                .map(f -> new Activo(
                        f.get("id", UUID.class),
                        f.get("codigo_inventario", String.class),
                        f.get("descripcion", String.class),
                        f.get("categoria_nombre", String.class),
                        f.get("vida_util_meses", Integer.class),
                        f.get("fecha_adquisicion", LocalDate.class),
                        f.get("estado", String.class),
                        f.get("costo_adquisicion", BigDecimal.class),
                        f.get("valor_residual", BigDecimal.class),
                        f.get("depreciacion_acumulada", BigDecimal.class),
                        moneda(f),
                        f.get("depreciado_en_vigente", Boolean.class)));
    }

    static String[] arreglo(List<String> estados) {
        return estados.toArray(String[]::new);
    }

    static String moneda(Record f) {
        return f.get("moneda", String.class).trim();
    }

    public record Partida(
            UUID id,
            UUID presupuestoId,
            String presupuestoNombre,
            String estado,
            UUID centroCostoId,
            String centroCostoNombre,
            UUID cuentaContableId,
            UUID periodoContableId,
            int anio,
            int mes,
            LocalDate fechaInicio,
            BigDecimal presupuestado,
            BigDecimal ejecutado,
            String moneda) {}

    public record Activo(
            UUID id,
            String codigo,
            String descripcion,
            String categoriaNombre,
            int vidaUtilMeses,
            LocalDate adquisicion,
            String estado,
            BigDecimal costo,
            BigDecimal residual,
            BigDecimal acumulada,
            String moneda,
            boolean depreciadoEnElPeriodoVigente) {}
}
