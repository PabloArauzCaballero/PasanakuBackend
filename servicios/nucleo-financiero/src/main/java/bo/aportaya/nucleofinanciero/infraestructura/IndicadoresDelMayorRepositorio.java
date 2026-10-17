package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Periodo;
import java.math.BigDecimal;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * Tableros · los totales del MAYOR (regla 91.1.5: el saldo nunca es una suma ad-hoc sobre pagos). Cada importe sale de
 * {@code movimiento_contable} de asientos CONFIRMADOS; el tipo de operacion lo da la transaccion de billetera a la que
 * apunta el asiento. El dia es el de La Paz.
 */
@Repository
public class IndicadoresDelMayorRepositorio {

    /** Un total con cuantos casos lo sostienen. */
    public record Total(Dinero total, long casos) {}

    /**
     * Lo movido por las transacciones APLICADAS de un tipo, en el periodo. Una transaccion reversada (CU-14 ejecutado)
     * no cuenta: lo que se devolvio no se cargo, ni se retiro, ni se cobro.
     */
    public Total porTipoDeTransaccion(DSLContext dsl, String tipo, Periodo periodo, Moneda moneda) {
        org.jooq.Record r = dsl.fetchOne(
                """
                        SELECT COALESCE(SUM(m.debe), 0), count(DISTINCT t.id)
                          FROM nucleo_financiero.transaccion_billetera t
                          JOIN nucleo_financiero.asiento_contable a ON a.origen_id = t.id AND a.estado = 'CONFIRMADO'
                          JOIN nucleo_financiero.movimiento_contable m ON m.asiento_id = a.id
                         WHERE t.tipo = ? AND t.estado = 'APLICADA' AND m.moneda = ?
                           AND (a.fecha AT TIME ZONE 'America/La_Paz')::date BETWEEN ? AND ?
                           AND NOT EXISTS (SELECT 1 FROM nucleo_financiero.reverso_transaccion r
                                            WHERE r.transaccion_original_id = t.id AND r.estado = 'EJECUTADO')
                        """,
                tipo,
                moneda.name(),
                periodo.inicio(),
                periodo.fin());
        return new Total(Dinero.de(r.get(0, BigDecimal.class), moneda), r.get(1, Long.class));
    }

    /** Ingresos por comisiones: el HABER neto de las cuentas 4.1 (Ingresos por comisiones) del plan, en el periodo. */
    public Total ingresosPorComision(DSLContext dsl, Periodo periodo, Moneda moneda) {
        org.jooq.Record r = dsl.fetchOne(
                """
                        SELECT COALESCE(SUM(m.haber - m.debe), 0), count(DISTINCT a.id)
                          FROM nucleo_financiero.movimiento_contable m
                          JOIN nucleo_financiero.cuenta_contable c ON c.id = m.cuenta_id
                          JOIN nucleo_financiero.asiento_contable a ON a.id = m.asiento_id AND a.estado = 'CONFIRMADO'
                         WHERE (c.codigo = '4.1' OR c.codigo LIKE '4.1.%') AND m.moneda = ?
                           AND (a.fecha AT TIME ZONE 'America/La_Paz')::date BETWEEN ? AND ?
                        """,
                moneda.name(), periodo.inicio(), periodo.fin());
        return new Total(Dinero.de(r.get(0, BigDecimal.class), moneda), r.get(1, Long.class));
    }
}
