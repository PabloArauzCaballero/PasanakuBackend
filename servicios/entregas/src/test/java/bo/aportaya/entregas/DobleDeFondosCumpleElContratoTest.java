package bo.aportaya.entregas;

import bo.aportaya.entregas.dominio.puertos.FondosDelComprador;
import bo.aportaya.plataforma.dominio.Dinero;
import java.util.UUID;

/**
 * El doble de nucleo-financiero ({@link DoblesDelMercado.Fondos}) tiene que cumplir {@link ContratoDeFondosDelComprador}:
 * si el doble miente, todas las pruebas del mercado que lo usan estarian probando contra un invento.
 *
 * <p>El adaptador real ({@code FondosPorHttp}, carril G) no pasa ESTAS pruebas: su contrato HTTP es distinto del del
 * doble (el libro deriva a quien pagar y cuanto desde la oferta, el cuerpo del pago va vacio), asi que tiene las
 * suyas en {@code FondosPorHttpTest} y el recorrido contra nucleo-financiero y entregas REALES vive en
 * {@code despliegue/ensayos/integracion}.
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
