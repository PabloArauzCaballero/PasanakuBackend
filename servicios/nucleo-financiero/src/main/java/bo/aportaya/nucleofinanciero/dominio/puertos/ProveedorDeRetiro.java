package bo.aportaya.nucleofinanciero.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.UUID;

/**
 * Quien de verdad manda la plata fuera de la plataforma (H4.S2).
 *
 * <p>{@code instruir} se llama **fuera de la transaccion** (invariante 6): una llamada
 * de red adentro mantiene la orden con el saldo retenido bloqueado tanto como tarde el
 * proveedor. El resultado de {@code instruir} nunca es "fallo" en el sentido de
 * excepcion no controlada — es uno de tres estados explicitos, y {@code TIMEOUT} NO es
 * {@code RECHAZADO}: un timeout no dice que la plata no salio, dice que no se sabe
 * todavia. Confundirlos es la diferencia entre reconciliar despues y pagarle dos veces
 * a alguien (o no pagarle nunca) porque se asumio un rechazo que nunca paso.
 *
 * <p>Introducido en H3.S1.M3 para que {@code CU11.instruirPago} tenga a que llamar —
 * {@code CU11.instruirPago}/{@code confirmarPago}/{@code rechazar} viven en H3, pero el
 * proveedor real, su doble de tres niveles completo (acepta / responde tarde antes-y-
 * despues del timeout / rechaza) y la reconciliacion son H4.S2. Este puerto y su doble
 * minimo de este momento son la base que H4.S2 EXTIENDE, no reemplaza.
 */
public interface ProveedorDeRetiro {

    /** Instruye el pago. Nunca lanza por un rechazo de negocio: ESO es un {@link Resultado}. */
    Resultado instruir(UUID ordenRetiroId, Dinero monto);

    /**
     * Pregunta por una instruccion ya enviada (H4.S2.M3): lo que
     * {@code ReconciliacionDeRetiros} usa para resolver una orden que quedo
     * {@code EN_PROCESO} con un {@link Estado#TIMEOUT}. Puede seguir devolviendo
     * {@code TIMEOUT} — significa que el proveedor tampoco lo sabe todavia, no que la
     * pregunta fallo.
     */
    Estado consultar(String referencia);

    /** Los tres estados posibles de una instruccion ya enviada. */
    enum Estado {
        ACEPTADO,
        RECHAZADO,
        TIMEOUT
    }

    /**
     * @param referencia la referencia del proveedor — siempre presente, incluso en
     *     {@code TIMEOUT}: es lo que el job de reconciliacion (H4.S2.M3) usa para
     *     preguntar mas tarde.
     */
    record Resultado(Estado estado, String referencia) {}
}
