package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.aplicacion.AporteCobrado;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;

/**
 * Lo común de las pruebas del recargo por mora en el mayor (tableros, H1 y H8): el mapeo de {@code RESERVA_RESPALDO},
 * la billetera de sistema del fondo y el evento de un recargo cobrado.
 */
abstract class BaseDeRecargo extends BaseDeAportes {

    protected static String reservaRespaldo;

    @BeforeAll
    static void mapeoDelFondo() {
        reservaRespaldo = "RR.BOB." + UUID.randomUUID().toString().substring(0, 6);
        contable.cuentaDeMovimiento(reservaRespaldo, "PASIVO", "ACREEDORA", "BOB");
        MAPEO.put("RESERVA_RESPALDO.BOB", reservaRespaldo);
    }

    /** La billetera de sistema del fondo en BOB: la sembrada si existe, o una creada (los contenedores se comparten). */
    protected UUID fondo() {
        var existente = dsl.fetchOptional(
                """
                SELECT id FROM nucleo_financiero.cuenta_billetera
                 WHERE tipo = 'FONDO_GARANTIA' AND moneda = 'BOB' AND estado = 'ACTIVA'
                """);
        if (existente.isPresent()) {
            return existente.get().get(0, UUID.class);
        }
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO nucleo_financiero.cuenta_billetera
                    (id, numero_cuenta, tipo, moneda, estado, nivel_debida_diligencia,
                     saldo_disponible, saldo_retenido, permite_saldo_negativo, fecha_apertura, version)
                VALUES (?, ?, 'FONDO_GARANTIA', 'BOB', 'ACTIVA', 'REFORZADA', 0, 0, false, now(), 0)
                """,
                id,
                "FGA-" + id.toString().substring(0, 12));
        return id;
    }

    protected AporteCobrado recargo(Aporte a, String monto) {
        return new AporteCobrado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                a.obligacion(),
                a.grupo(),
                a.usuario(),
                Dinero.de(monto, Moneda.BOB),
                "BILLETERA_MOVIL",
                Optional.of("RECARGO_MORA"));
    }
}
