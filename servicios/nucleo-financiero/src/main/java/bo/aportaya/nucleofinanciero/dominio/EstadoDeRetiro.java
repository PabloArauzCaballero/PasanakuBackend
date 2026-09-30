package bo.aportaya.nucleofinanciero.dominio;

import java.util.Map;
import java.util.Set;

/**
 * El estado del retiro, con las transiciones permitidas explicitas en Java.
 *
 * <p>{@code ck_orden_retiro_estado} (`sql/10_tablas/10_billetera_custodia/orden_retiro.sql:28`)
 * fija los valores posibles pero no dice QUE puede llegar a QUE. La base tiene su
 * propia guardia sobre el camino critico —{@code ck_retiro_doble_aprobacion} y el
 * {@code WHERE estado = <desde>} de cada `UPDATE`— pero esa guardia solo protege contra
 * lo que la base modela con un CHECK o una precondicion puntual: no hay una sola tabla
 * en PostgreSQL que diga «de EN_PROCESO no se vuelve a PENDIENTE». Esta clase es esa
 * tabla, en Java, y la unica fuente de verdad para quien lee el codigo sin tener
 * abiertas las 1800 lineas de `restricciones.sql`.
 *
 * <p>{@code BORRADOR} existe en el CHECK de la base para una fase que ningun caso de
 * uso escribe todavia (regla 00 — nada se modela por si acaso): no aparece aqui hasta
 * que exista un CU que lo use.
 */
public enum EstadoDeRetiro {
    PENDIENTE,
    EN_REVISION,
    AUTORIZADA,
    EN_PROCESO,
    PAGADA,
    RECHAZADA,
    REVERSADA;

    private static final Map<EstadoDeRetiro, Set<EstadoDeRetiro>> TRANSICIONES = Map.of(
            PENDIENTE, Set.of(EN_REVISION, AUTORIZADA, RECHAZADA),
            EN_REVISION, Set.of(AUTORIZADA, RECHAZADA),
            AUTORIZADA, Set.of(EN_PROCESO, RECHAZADA),
            EN_PROCESO, Set.of(PAGADA, RECHAZADA),
            PAGADA, Set.of(REVERSADA),
            RECHAZADA, Set.of(),
            REVERSADA, Set.of());

    /** ¿Puede este estado pasar directamente al destino? Nunca implicito, siempre la tabla. */
    public boolean puedePasarA(EstadoDeRetiro destino) {
        return TRANSICIONES.getOrDefault(this, Set.of()).contains(destino);
    }
}
