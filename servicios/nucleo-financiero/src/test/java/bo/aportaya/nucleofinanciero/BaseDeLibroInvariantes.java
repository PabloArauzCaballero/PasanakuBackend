package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia;
import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.SalidaTransferencia;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;

abstract class BaseDeLibroInvariantes extends BaseDeBilletera {
    protected static final String ESTANDAR = "ESTANDAR";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    protected Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    protected record Par(UUID origen, UUID destino, ContextoSesion ctx) {}

    protected Par par(String saldoOrigen) {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID quienPaga = fixtura.usuario();
        UUID origen = fixtura.billetera(quienPaga, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal(saldoOrigen));
        UUID destino = fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
        return new Par(origen, destino, contextoDe(quienPaga));
    }

    protected SalidaTransferencia transferir(Par p, String monto, String clave) {
        return transaccion.execute(t -> transferenciaCU.ejecutar(
                new EntradaTransferencia(
                        clave, p.origen(), p.destino(), bob(monto), "prueba", Optional.empty(), Optional.empty()),
                p.ctx()));
    }

    /** El cuadre que TODO escenario tiene que cumplir: nunca positivo, nunca negativo. */
    protected int descuadreDe(UUID transaccionId) {
        return contar(
                """
                SELECT COALESCE(SUM(CASE WHEN sentido = 'CREDITO' THEN monto ELSE -monto END), 0)::int
                  FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id = ?
                """,
                transaccionId);
    }

    protected int saldoTotalDelSistema() {
        return contar("SELECT COALESCE(SUM(saldo_total),0)::int FROM nucleo_financiero.cuenta_billetera");
    }
}
