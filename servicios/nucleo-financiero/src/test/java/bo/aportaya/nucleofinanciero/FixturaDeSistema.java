package bo.aportaya.nucleofinanciero;

import java.math.BigDecimal;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Las cuentas que no son de nadie (custodia de inversion, ingresos de la plataforma) y una
 * billetera en otra moneda, para las operaciones que llaman otros servicios.
 *
 * <p>Se obtienen o se crean: la base admite UNA custodia de inversion activa por moneda (R-BIL-25) y los
 * contenedores de prueba se comparten entre clases, asi que cada clase reutiliza la que ya existe.
 */
final class FixturaDeSistema {

    private final DSLContext dsl;
    private final FixturaDeBilletera billeteras;

    FixturaDeSistema(DSLContext dsl, FixturaDeBilletera billeteras) {
        this.dsl = dsl;
        this.billeteras = billeteras;
    }

    UUID custodiaDeInversion(String moneda) {
        return cuentaDeSistema("CUSTODIA_INVERSION", moneda);
    }

    UUID ingresosDePlataforma(String moneda) {
        return cuentaDeSistema("PLATAFORMA_INGRESOS", moneda);
    }

    private UUID cuentaDeSistema(String tipo, String moneda) {
        var existente = dsl.fetchOptional(
                "SELECT id FROM nucleo_financiero.cuenta_billetera WHERE tipo = ? AND moneda = ? AND estado = 'ACTIVA'",
                tipo,
                moneda);
        if (existente.isPresent()) {
            return existente.get().get(0, UUID.class);
        }
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO nucleo_financiero.cuenta_billetera
                    (id, numero_cuenta, tipo, politica_billetera_id, moneda, estado,
                     nivel_debida_diligencia, saldo_disponible, saldo_retenido,
                     permite_saldo_negativo, fecha_apertura, version)
                VALUES (?, ?, ?, ?, ?, 'ACTIVA', 'SIMPLIFICADA', 0, 0, false, now(), 0)
                """,
                id,
                "SYS-" + id.toString().substring(0, 12),
                tipo,
                billeteras.politica(),
                moneda);
        return id;
    }

    /** Una billetera de persona en esa moneda, con saldo que entra por el libro. */
    UUID billeteraEn(UUID usuarioId, String moneda, BigDecimal disponible) {
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO nucleo_financiero.cuenta_billetera
                    (id, numero_cuenta, tipo, usuario_id, politica_billetera_id, moneda, estado,
                     nivel_debida_diligencia, saldo_disponible, saldo_retenido,
                     permite_saldo_negativo, fecha_apertura, version)
                VALUES (?, ?, 'USUARIO', ?, ?, ?, 'ACTIVA', 'ESTANDAR', 0, 0, false, now(), 0)
                """,
                id,
                moneda + "-" + id.toString().substring(0, 12),
                usuarioId,
                billeteras.politica(),
                moneda);
        if (disponible.signum() > 0 && "BOB".equals(moneda)) {
            billeteras.acreditar(id, disponible);
        } else if (disponible.signum() > 0) {
            acreditarEn(id, disponible, moneda);
        }
        return id;
    }

    /** Como {@code FixturaDeBilletera.acreditar}, en otra moneda: una transaccion cuadrada contra un puente de esa moneda. */
    private void acreditarEn(UUID cuentaId, BigDecimal monto, String moneda) {
        billeteras.tipoDeCambioDeHoy();
        UUID puente = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO nucleo_financiero.cuenta_billetera
                    (id, numero_cuenta, tipo, politica_billetera_id, moneda, estado,
                     nivel_debida_diligencia, saldo_disponible, saldo_retenido,
                     permite_saldo_negativo, fecha_apertura, version)
                VALUES (?, ?, 'PUENTE_CUSTODIA', ?, ?, 'ACTIVA', 'SIMPLIFICADA', 0, 0, true, now(), 0)
                """,
                puente,
                "PUE-" + moneda + puente.toString().substring(0, 8),
                billeteras.politica(),
                moneda);
        dsl.transaction(config -> {
            var tx = org.jooq.impl.DSL.using(config);
            UUID transaccion = UUID.randomUUID();
            tx.execute(
                    """
                    INSERT INTO nucleo_financiero.transaccion_billetera
                        (id, tipo, estado, moneda, monto_total, origen_tipo, origen_id, canal,
                         clave_idempotencia, hash_registro, ocurrida_en, registrada_en)
                    VALUES (?, 'RECARGA', 'APLICADA', ?, ?, 'ORDEN_RECARGA', gen_random_uuid(), 'API',
                            ?, repeat('a', 64), now(), now())
                    """,
                    transaccion,
                    moneda,
                    monto,
                    "semilla-" + transaccion);
            tx.execute(
                    """
                    INSERT INTO nucleo_financiero.movimiento_billetera
                        (id, transaccion_id, cuenta_billetera_id, orden, sentido, monto,
                         saldo_disponible_posterior, saldo_retenido_posterior, glosa, registrado_en)
                    VALUES (gen_random_uuid(), ?, ?, 1, 'DEBITO', ?, 0, 0, 'Ingreso desde custodia', now())
                    """,
                    transaccion,
                    puente,
                    monto);
            tx.execute(
                    """
                    INSERT INTO nucleo_financiero.movimiento_billetera
                        (id, transaccion_id, cuenta_billetera_id, orden, sentido, monto,
                         saldo_disponible_posterior, saldo_retenido_posterior, glosa, registrado_en)
                    VALUES (gen_random_uuid(), ?, ?, 2, 'CREDITO', ?, ?, 0, 'Saldo de prueba', now())
                    """,
                    transaccion,
                    cuentaId,
                    monto,
                    monto);
        });
    }

    /** El saldo que dice el libro (movimientos menos retenciones vigentes), por la consulta que usa el cuadre. */
    BigDecimal saldoDelLibro(UUID cuentaId) {
        return dsl.fetchOne(
                        """
                        SELECT COALESCE(SUM(CASE WHEN sentido = 'CREDITO' THEN monto ELSE -monto END), 0)
                               - COALESCE((SELECT SUM(monto) FROM nucleo_financiero.retencion_saldo
                                            WHERE cuenta_billetera_id = ? AND estado = 'VIGENTE'), 0)
                          FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id = ?
                        """,
                        cuentaId,
                        cuentaId)
                .get(0, BigDecimal.class);
    }

    /** Debitos y creditos de una transaccion: lo que la base exige que sea igual. */
    BigDecimal[] sumas(UUID transaccionId) {
        var fila = dsl.fetchOne(
                """
                SELECT COALESCE(SUM(monto) FILTER (WHERE sentido = 'DEBITO'), 0),
                       COALESCE(SUM(monto) FILTER (WHERE sentido = 'CREDITO'), 0)
                  FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id = ?
                """,
                transaccionId);
        return new BigDecimal[] {fila.get(0, BigDecimal.class), fila.get(1, BigDecimal.class)};
    }

    /** Imprime el asiento tal como queda en el libro, para la evidencia 91.6.1 (sale en el XML de la corrida). */
    void imprimir(String titulo, UUID transaccionId) {
        System.out.println("ASIENTO | " + titulo + " | tx=" + transaccionId);
        dsl.fetch(
                        """
                        SELECT m.orden, m.sentido, c.tipo, m.monto, t.moneda
                          FROM nucleo_financiero.movimiento_billetera m
                          JOIN nucleo_financiero.cuenta_billetera c ON c.id = m.cuenta_billetera_id
                          JOIN nucleo_financiero.transaccion_billetera t ON t.id = m.transaccion_id
                         WHERE m.transaccion_id = ? ORDER BY m.orden
                        """,
                        transaccionId)
                .forEach(r -> System.out.println(
                        "ASIENTO |   " + r.get(0) + " " + r.get(1) + " " + r.get(2) + " " + r.get(3) + " " + r.get(4)));
        BigDecimal[] s = sumas(transaccionId);
        System.out.println("ASIENTO |   SUMA debitos=" + s[0] + " creditos=" + s[1]);
    }
}
