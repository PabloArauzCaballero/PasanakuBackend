package bo.aportaya.erp.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Las lecturas de listado del backoffice (carril A · A6) que giran alrededor de un tercero comercial:
 * ordenes de compra, facturas de proveedor y cuentas por cobrar. Solo lectura y solo del esquema
 * {@code erp}. Mismas reglas que {@link ListadosDeErpRepositorio}: texto fijo y keyset descendente.
 */
@Component
public class ListadosDeTercerosRepositorio {

    public List<Orden> ordenes(
            DSLContext dsl, List<String> estados, OffsetDateTime antesDe, UUID antesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT o.id, o.numero, o.tercero_comercial_id, t.razon_social, o.centro_costo_id,
                       cc.nombre AS centro_nombre, o.estado, o.monto_total, o.moneda, o.creada_en
                  FROM erp.orden_compra o
                  JOIN erp.tercero_comercial t ON t.id = o.tercero_comercial_id
                  LEFT JOIN erp.centro_costo cc ON cc.id = o.centro_costo_id
                 WHERE o.estado = ANY (?::varchar[])
                   AND (?::timestamptz IS NULL OR (o.creada_en, o.id) < (?::timestamptz, ?::uuid))
                 ORDER BY o.creada_en DESC, o.id DESC
                 LIMIT ?
                """,
                        ListadosDeErpRepositorio.arreglo(estados),
                        antesDe,
                        antesDe,
                        antesDeId,
                        limite)
                .map(f -> new Orden(
                        f.get("id", UUID.class),
                        f.get("numero", String.class),
                        f.get("tercero_comercial_id", UUID.class),
                        f.get("razon_social", String.class),
                        f.get("centro_costo_id", UUID.class),
                        f.get("centro_nombre", String.class),
                        f.get("estado", String.class),
                        f.get("monto_total", BigDecimal.class),
                        ListadosDeErpRepositorio.moneda(f),
                        f.get("creada_en", OffsetDateTime.class)));
    }

    /**
     * {@code aprobada_por_la_sesion} se resuelve contra el sujeto del token, nunca en el cliente (R-CTB-05).
     *
     * <p>Lo pagado, el saldo y el estado se DERIVAN de {@code pago_a_proveedor}, igual que en CU-103: la
     * factura es append-only, asi que {@code monto_pagado} y {@code estado} se quedan como se insertaron y
     * {@code saldo_pendiente} no es una columna generada en el DDL. Leerlos tal cual mentiria.
     */
    public List<Factura> facturas(
            DSLContext dsl, UUID sesion, List<String> estados, LocalDate antesDe, UUID antesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT f.id, f.numero_factura, f.tercero_comercial_id, t.razon_social, f.orden_compra_id,
                       f.fecha_emision, f.fecha_vencimiento, e.estado, f.monto, x.pagado AS monto_pagado,
                       f.monto - x.pagado AS saldo_pendiente, f.moneda, f.asiento_contable_id,
                       COALESCE(f.aprobada_por = ?::uuid, false) AS aprobada_por_la_sesion
                  FROM erp.factura_proveedor f
                  JOIN erp.tercero_comercial t ON t.id = f.tercero_comercial_id
                 CROSS JOIN LATERAL (SELECT COALESCE(sum(p.monto), 0) AS pagado FROM erp.pago_a_proveedor p
                                      WHERE p.factura_proveedor_id = f.id) x
                 CROSS JOIN LATERAL (SELECT CASE WHEN f.estado = 'ANULADA' THEN f.estado
                                                 WHEN x.pagado >= f.monto THEN 'PAGADA'
                                                 WHEN x.pagado > 0 THEN 'PAGADA_PARCIAL'
                                                 ELSE f.estado END AS estado) e
                 WHERE e.estado = ANY (?::varchar[])
                   AND (?::date IS NULL OR (f.fecha_emision, f.id) < (?::date, ?::uuid))
                 ORDER BY f.fecha_emision DESC, f.id DESC
                 LIMIT ?
                """,
                        sesion,
                        ListadosDeErpRepositorio.arreglo(estados),
                        antesDe,
                        antesDe,
                        antesDeId,
                        limite)
                .map(f -> new Factura(
                        f.get("id", UUID.class),
                        f.get("numero_factura", String.class),
                        f.get("tercero_comercial_id", UUID.class),
                        f.get("razon_social", String.class),
                        f.get("orden_compra_id", UUID.class),
                        f.get("fecha_emision", LocalDate.class),
                        f.get("fecha_vencimiento", LocalDate.class),
                        f.get("estado", String.class),
                        f.get("monto", BigDecimal.class),
                        f.get("monto_pagado", BigDecimal.class),
                        f.get("saldo_pendiente", BigDecimal.class),
                        ListadosDeErpRepositorio.moneda(f),
                        f.get("asiento_contable_id", UUID.class),
                        f.get("aprobada_por_la_sesion", Boolean.class)));
    }

    /** Lo cobrado, el saldo y el estado se derivan de {@code cobro_cuenta_por_cobrar}, igual que en CU-104. */
    public List<CuentaPorCobrar> cuentasPorCobrar(
            DSLContext dsl, List<String> estados, LocalDate antesDe, UUID antesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT c.id, c.origen_tipo, c.origen_id, c.tercero_comercial_id, t.razon_social,
                       c.fecha_vencimiento, e.estado, c.monto, x.cobrado AS monto_cobrado,
                       c.monto - x.cobrado AS saldo_pendiente, c.moneda
                  FROM erp.cuenta_por_cobrar c
                  LEFT JOIN erp.tercero_comercial t ON t.id = c.tercero_comercial_id
                 CROSS JOIN LATERAL (SELECT COALESCE(sum(k.monto), 0) AS cobrado FROM erp.cobro_cuenta_por_cobrar k
                                      WHERE k.cuenta_por_cobrar_id = c.id) x
                 CROSS JOIN LATERAL (SELECT CASE WHEN c.estado = 'INCOBRABLE' THEN c.estado
                                                 WHEN x.cobrado >= c.monto THEN 'COBRADA'
                                                 WHEN x.cobrado > 0 THEN 'COBRADA_PARCIAL'
                                                 ELSE c.estado END AS estado) e
                 WHERE e.estado = ANY (?::varchar[])
                   AND (?::date IS NULL OR (c.fecha_vencimiento, c.id) < (?::date, ?::uuid))
                 ORDER BY c.fecha_vencimiento DESC, c.id DESC
                 LIMIT ?
                """,
                        ListadosDeErpRepositorio.arreglo(estados),
                        antesDe,
                        antesDe,
                        antesDeId,
                        limite)
                .map(f -> new CuentaPorCobrar(
                        f.get("id", UUID.class),
                        f.get("origen_tipo", String.class),
                        f.get("origen_id", UUID.class),
                        f.get("tercero_comercial_id", UUID.class),
                        f.get("razon_social", String.class),
                        f.get("fecha_vencimiento", LocalDate.class),
                        f.get("estado", String.class),
                        f.get("monto", BigDecimal.class),
                        f.get("monto_cobrado", BigDecimal.class),
                        f.get("saldo_pendiente", BigDecimal.class),
                        ListadosDeErpRepositorio.moneda(f)));
    }

    public record Orden(
            UUID id,
            String numero,
            UUID terceroId,
            String razonSocial,
            UUID centroCostoId,
            String centroCostoNombre,
            String estado,
            BigDecimal monto,
            String moneda,
            OffsetDateTime creadaEn) {}

    public record Factura(
            UUID id,
            String numero,
            UUID terceroId,
            String razonSocial,
            UUID ordenCompraId,
            LocalDate emision,
            LocalDate vencimiento,
            String estado,
            BigDecimal monto,
            BigDecimal pagado,
            BigDecimal saldo,
            String moneda,
            UUID asientoContableId,
            boolean aprobadaPorLaSesion) {}

    public record CuentaPorCobrar(
            UUID id,
            String origenTipo,
            UUID origenId,
            UUID terceroId,
            String razonSocial,
            LocalDate vencimiento,
            String estado,
            BigDecimal monto,
            BigDecimal cobrado,
            BigDecimal saldo,
            String moneda) {}
}
