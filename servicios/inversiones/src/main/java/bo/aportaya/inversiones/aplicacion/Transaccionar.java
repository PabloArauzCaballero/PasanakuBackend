package bo.aportaya.inversiones.aplicacion;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Abre UNA transaccion local para un paso de una saga.
 *
 * <p>Las llamadas a otros servicios van siempre FUERA de estas transacciones (invariante
 * 6 y regla 98.3): esperar a un tercero con una transaccion abierta es retener bloqueos
 * mientras alguien mas decide cuanto tarda. Por eso los casos de uso que coordinan no
 * llevan {@code @Transactional}: llaman a esta clase, paso por paso, y se comportan igual
 * con o sin el proxy de Spring.
 */
@Component
public class Transaccionar {

    private final TransactionTemplate plantilla;

    public Transaccionar(PlatformTransactionManager gestor) {
        this.plantilla = new TransactionTemplate(gestor);
    }

    public <T> T en(Supplier<T> trabajo) {
        return plantilla.execute(estado -> trabajo.get());
    }

    public void en(Runnable trabajo) {
        plantilla.executeWithoutResult(estado -> trabajo.run());
    }
}
