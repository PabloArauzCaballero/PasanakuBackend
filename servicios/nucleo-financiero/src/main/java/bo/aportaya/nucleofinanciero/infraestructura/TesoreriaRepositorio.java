package bo.aportaya.nucleofinanciero.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Las lecturas de Tesoreria del backoffice (CU-50/51/14, carril A · H8) y el saldo emitido que la
 * conciliacion calcula del lado del servidor. Solo lectura y solo del esquema {@code nucleo_financiero}.
 */
@Component
public class TesoreriaRepositorio {

    /**
     * CU-50 paso 1: el dinero electronico emitido es la suma de {@code saldo_total} de las billeteras
     * que son pasivo con terceros —USUARIO, GRUPO y FONDO_GARANTIA— en la moneda de la custodia.
     */
    public BigDecimal saldoEmitido(DSLContext dsl, String moneda) {
        return dsl.fetchOne(
                        """
                SELECT COALESCE(sum(saldo_total), 0) FROM nucleo_financiero.cuenta_billetera
                 WHERE tipo IN ('USUARIO', 'GRUPO', 'FONDO_GARANTIA') AND moneda = ?
                """,
                        moneda)
                .get(0, BigDecimal.class);
    }

    public Optional<String> monedaDeLaCustodia(DSLContext dsl, UUID cuentaCustodiaId) {
        return Optional.ofNullable(dsl.fetchOne(
                        "SELECT moneda FROM nucleo_financiero.cuenta_custodia WHERE id = ?", cuentaCustodiaId))
                .map(f -> f.get(0, String.class).trim());
    }

    /** La conciliacion ya registrada de esa cuenta y dia: lo que distingue un reintento de otra conciliacion. */
    public Optional<Conciliacion> conciliacionDelDia(DSLContext dsl, UUID cuentaCustodiaId, LocalDate fecha) {
        return conciliaciones(
                        dsl, Optional.of(cuentaCustodiaId), Optional.of(fecha), Optional.empty(), Optional.empty(), 1)
                .stream()
                .findFirst();
    }

    public List<Conciliacion> conciliaciones(
            DSLContext dsl,
            Optional<UUID> cuenta,
            Optional<LocalDate> delDia,
            Optional<LocalDate> antesDe,
            Optional<UUID> antesDeId,
            int limite) {
        var parametros = new ArrayList<Object>();
        var donde = new StringBuilder(" WHERE true");
        cuenta.ifPresent(c -> {
            donde.append(" AND c.cuenta_custodia_id = ?");
            parametros.add(c);
        });
        delDia.ifPresent(d -> {
            donde.append(" AND c.fecha = ?::date");
            parametros.add(d);
        });
        if (antesDe.isPresent() && antesDeId.isPresent()) {
            donde.append(" AND (c.fecha, c.id) < (?::date, ?::uuid)");
            parametros.add(antesDe.get());
            parametros.add(antesDeId.get());
        }
        parametros.add(limite);
        return dsl.fetch(
                        """
                SELECT c.id, c.cuenta_custodia_id, c.fecha, c.saldo_dinero_electronico, c.saldo_custodia,
                       c.saldo_en_transito, c.diferencia, c.ratio_cobertura, c.cumple_encaje, c.estado,
                       c.ejecutada_por, c.ejecutada_en, cc.moneda,
                       (SELECT count(*) FROM nucleo_financiero.descuadre_custodia d
                         WHERE d.conciliacion_custodia_id = c.id AND d.estado <> 'RESUELTO') AS descuadres_abiertos
                  FROM nucleo_financiero.conciliacion_custodia c
                  JOIN nucleo_financiero.cuenta_custodia cc ON cc.id = c.cuenta_custodia_id"""
                                + donde
                                + " ORDER BY c.fecha DESC, c.id DESC LIMIT ?",
                        parametros.toArray())
                .map(f -> new Conciliacion(
                        f.get("id", UUID.class),
                        f.get("cuenta_custodia_id", UUID.class),
                        f.get("fecha", LocalDate.class),
                        f.get("saldo_dinero_electronico", BigDecimal.class),
                        f.get("saldo_custodia", BigDecimal.class),
                        f.get("saldo_en_transito", BigDecimal.class),
                        f.get("diferencia", BigDecimal.class),
                        f.get("ratio_cobertura", BigDecimal.class),
                        f.get("cumple_encaje", Boolean.class),
                        f.get("estado", String.class),
                        f.get("ejecutada_por", UUID.class),
                        f.get("ejecutada_en", OffsetDateTime.class),
                        f.get("moneda", String.class).trim(),
                        f.get("descuadres_abiertos", Long.class).intValue()));
    }

    public List<Cierre> cierres(DSLContext dsl, Optional<LocalDate> antesDe, Optional<UUID> antesDeId, int limite) {
        var parametros = new ArrayList<Object>();
        String cursor = "";
        if (antesDe.isPresent() && antesDeId.isPresent()) {
            cursor = " WHERE (fecha, id) < (?::date, ?::uuid)";
            parametros.add(antesDe.get());
            parametros.add(antesDeId.get());
        }
        parametros.add(limite);
        return dsl.fetch(
                        """
                SELECT id, fecha, total_recaudado, total_conciliado, total_excepciones, cantidad_pagos, cuadrado,
                       cerrado_por, cerrado_en, reabierto_en
                  FROM nucleo_financiero.cierre_diario"""
                                + cursor
                                + " ORDER BY fecha DESC, id DESC LIMIT ?",
                        parametros.toArray())
                .map(f -> new Cierre(
                        f.get("id", UUID.class),
                        f.get("fecha", LocalDate.class),
                        f.get("total_recaudado", BigDecimal.class),
                        f.get("total_conciliado", BigDecimal.class),
                        f.get("total_excepciones", BigDecimal.class),
                        f.get("cantidad_pagos", Integer.class),
                        f.get("cuadrado", Boolean.class),
                        f.get("cerrado_por", UUID.class),
                        f.get("cerrado_en", OffsetDateTime.class),
                        f.get("reabierto_en", OffsetDateTime.class)));
    }

    public List<Reverso> reversos(
            DSLContext dsl,
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite) {
        var parametros = new ArrayList<Object>();
        parametros.add(estados.toArray(String[]::new));
        String cursor = "";
        if (antesDe.isPresent() && antesDeId.isPresent()) {
            cursor = " AND (solicitada_en, id) < (?::timestamptz, ?::uuid)";
            parametros.add(antesDe.get());
            parametros.add(antesDeId.get());
        }
        parametros.add(limite);
        return dsl.fetch(
                        """
                SELECT id, transaccion_original_id, transaccion_reverso_id, autorizada_por, tipo, motivo,
                       monto_reversado, estado, solicitada_en, ejecutada_en
                  FROM nucleo_financiero.reverso_transaccion
                 WHERE estado = ANY(?::varchar[])"""
                                + cursor
                                + " ORDER BY solicitada_en DESC, id DESC LIMIT ?",
                        parametros.toArray())
                .map(f -> new Reverso(
                        f.get("id", UUID.class),
                        f.get("transaccion_original_id", UUID.class),
                        f.get("transaccion_reverso_id", UUID.class),
                        f.get("autorizada_por", UUID.class),
                        f.get("tipo", String.class),
                        f.get("motivo", String.class),
                        f.get("monto_reversado", BigDecimal.class),
                        f.get("estado", String.class),
                        f.get("solicitada_en", OffsetDateTime.class),
                        f.get("ejecutada_en", OffsetDateTime.class)));
    }

    public record Conciliacion(
            UUID id,
            UUID cuentaCustodiaId,
            LocalDate fecha,
            BigDecimal saldoEmitido,
            BigDecimal saldoCustodia,
            BigDecimal saldoEnTransito,
            BigDecimal diferencia,
            BigDecimal ratio,
            boolean cumpleEncaje,
            String estado,
            UUID ejecutadaPor,
            OffsetDateTime ejecutadaEn,
            String moneda,
            int descuadresAbiertos) {}

    public record Cierre(
            UUID id,
            LocalDate fecha,
            BigDecimal totalRecaudado,
            BigDecimal totalConciliado,
            BigDecimal totalExcepciones,
            int cantidadPagos,
            boolean cuadrado,
            UUID cerradoPor,
            OffsetDateTime cerradoEn,
            OffsetDateTime reabiertoEn) {}

    public record Reverso(
            UUID id,
            UUID transaccionOriginalId,
            UUID transaccionReversoId,
            UUID autorizadaPor,
            String tipo,
            String motivo,
            BigDecimal montoReversado,
            String estado,
            OffsetDateTime solicitadaEn,
            OffsetDateTime ejecutadaEn) {}
}
