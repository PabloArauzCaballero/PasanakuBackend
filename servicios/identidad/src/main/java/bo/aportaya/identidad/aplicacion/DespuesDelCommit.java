package bo.aportaya.identidad.aplicacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Los efectos hacia afuera (el correo) corren DESPUES del commit, no dentro de la transaccion: si el
 * commit revierte, el correo no sale; si el correo falla, lo ya escrito no se pierde (regla 96.3.4).
 */
final class DespuesDelCommit {

    private static final Logger BITACORA = LoggerFactory.getLogger(DespuesDelCommit.class);

    private DespuesDelCommit() {}

    static void ejecutar(String descripcion, Runnable tarea) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    correr(descripcion, tarea);
                }
            });
        } else {
            correr(descripcion, tarea);
        }
    }

    private static void correr(String descripcion, Runnable tarea) {
        try {
            tarea.run();
        } catch (RuntimeException fallo) {
            // Solo el tipo del fallo: el mensaje de un cliente de correo puede traer la direccion.
            BITACORA.warn(
                    "no se pudo completar el envio ({}): {}",
                    descripcion,
                    fallo.getClass().getSimpleName());
        }
    }
}
