package bo.aportaya.plataforma.pruebas;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;

/**
 * Poll acotado, sin {@code Thread.sleep} fijo — Awaitility no esta en el catalogo de
 * versiones, y agregarlo para un solo uso no vale el micro-PR al troncal.
 *
 * <p>Un {@code sleep(5000)} fijo es dos cosas malas a la vez: en la maquina rapida
 * hace esperar 5s a una prueba que en 200ms ya tenia su respuesta, y en la maquina
 * lenta (Testcontainers bajo carga, esta misma sesion) 5s no alcanzan y la prueba
 * es flaky. El poll corto y acotado no tiene ninguno de los dos problemas.
 */
public final class Espera {

    private Espera() {}

    /**
     * Reintenta {@code condicion} cada {@code intervalo} hasta que sea verdadera o pase
     * {@code maximo}. Lanza {@link IllegalStateException} con el motivo si se agota el
     * tiempo — nunca deja un {@code assert} silencioso pasar por un timeout mudo.
     */
    public static void hasta(Duration maximo, Duration intervalo, String motivo, BooleanSupplier condicion) {
        Instant limite = Instant.now().plus(maximo);
        while (Instant.now().isBefore(limite)) {
            if (condicion.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(intervalo.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("espera interrumpida: " + motivo, e);
            }
        }
        if (!condicion.getAsBoolean()) {
            throw new IllegalStateException("timeout esperando: " + motivo + " (maximo " + maximo + ")");
        }
    }
}
