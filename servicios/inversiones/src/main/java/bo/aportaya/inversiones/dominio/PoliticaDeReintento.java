package bo.aportaya.inversiones.dominio;

import java.time.Duration;

/**
 * Cuando se vuelve a intentar una instruccion al libro que quedo pendiente, y cuando se deja
 * de insistir y se la muestra como atascada.
 *
 * <p>Espera exponencial con tope, medida desde que la instruccion nacio (no hay otro reloj
 * guardado): el intento numero {@code n} no ocurre antes de que pase la suma de
 * {@code min(base x 2^k, tope)} para {@code k = 0..n}. El primer intento tambien espera
 * {@code base}: da tiempo a que termine el pedido que la creo, que sigue su camino por su
 * cuenta. Repetir es seguro (misma clave de idempotencia), asi que insistir de mas cuesta
 * una llamada, no plata; por eso hay un tope de intentos solo para que lo roto se vea y no
 * se pierda entre reintentos eternos.
 *
 * <p>Pasados {@code maxIntentos} la instruccion NO se descarta: sigue pendiente, el
 * trabajo programado deja de tocarla y queda contada como atascada hasta que una persona
 * la reintente a mano o el libro vuelva.
 */
public record PoliticaDeReintento(Duration base, Duration tope, int maxIntentos) {

    public PoliticaDeReintento {
        if (base.isNegative() || base.isZero() || tope.compareTo(base) < 0 || maxIntentos < 1) {
            throw new IllegalArgumentException("Base positiva, tope no menor que la base y al menos un intento");
        }
    }

    /** La edad minima de la instruccion para que le toque el intento numero {@code intentos}. */
    public Duration esperaAcumulada(int intentos) {
        Duration total = Duration.ZERO;
        Duration paso = base;
        for (int k = 0; k <= intentos; k++) {
            total = total.plus(paso.compareTo(tope) > 0 ? tope : paso);
            paso = paso.multipliedBy(2);
            if (paso.compareTo(tope) > 0) {
                paso = tope;
            }
        }
        return total;
    }

    public boolean atascada(int intentos) {
        return intentos >= maxIntentos;
    }

    public boolean leToca(int intentos, Duration edad) {
        return !atascada(intentos) && edad.compareTo(esperaAcumulada(intentos)) >= 0;
    }
}
