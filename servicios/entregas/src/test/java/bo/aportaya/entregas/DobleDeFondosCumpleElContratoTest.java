package bo.aportaya.entregas;

import bo.aportaya.entregas.dominio.puertos.FondosDelComprador;
import bo.aportaya.plataforma.dominio.Dinero;
import java.util.UUID;

/**
 * El doble de nucleo-financiero ({@link DoblesDelMercado.Fondos}) tiene que cumplir {@link ContratoDeFondosDelComprador}:
 * si el doble miente, todas las pruebas del mercado que lo usan estarian probando contra un invento.
 *
 * <p>QUEDA PENDIENTE contra el productor real: cuando nucleo-financiero publique {@code pagarAlVendedor}
 * (retencion de un tercero), se agrega una subclase que arma el adaptador HTTP contra un nucleo levantado y
 * pasa EXACTAMENTE estas mismas pruebas. Hasta entonces H9.S1.M5 esta cerrada contra doble, no contra nucleo.
 */
class DobleDeFondosCumpleElContratoTest extends ContratoDeFondosDelComprador {

    private DoblesDelMercado.Fondos doble;

    @Override
    protected FondosDelComprador implementacion() {
        doble = new DoblesDelMercado.Fondos();
        return doble;
    }

    @Override
    protected void sembrarSaldo(UUID usuario, Dinero monto) {
        doble.saldo(usuario, monto);
    }

    @Override
    protected Dinero disponible(UUID usuario) {
        return doble.disponible(usuario);
    }

    @Override
    protected Dinero plataforma() {
        return doble.plataforma();
    }

    @Override
    protected Dinero totalDelSistema() {
        return doble.totalDelSistema();
    }

    @Override
    protected void caer(boolean caido) {
        doble.seCae(caido);
    }
}
