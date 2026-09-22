package bo.aportaya.nucleofinanciero.infraestructura;

import java.util.Objects;
import org.jooq.DSLContext;

/** Exclusión transaccional del mismo scope que un índice de idempotencia. */
final class BloqueoDeIdempotencia {

    private BloqueoDeIdempotencia() {}

    /** Serializa reintentos iguales hasta el commit o rollback. */
    static void tomar(DSLContext dsl, String recurso, String... partes) {
        StringBuilder alcance = new StringBuilder(Objects.requireNonNull(recurso, "recurso"));
        for (String parte : partes) {
            String noNula = Objects.requireNonNull(parte, "parte del scope");
            alcance.append('|').append(noNula.length()).append(':').append(noNula);
        }
        // Una colisión del hash solo agrega espera: el resultado siempre se relee por scope real.
        dsl.execute("SELECT pg_advisory_xact_lock(hashtextextended(?, 0::bigint))", alcance.toString());
    }
}
