package bo.aportaya.auditoria.dominio;

import java.util.Optional;

/**
 * operacion/estado · lo que contesto un servicio cuando se le pregunto su readiness. {@code latenciaMs} vacia cuando no
 * contesto: un plazo vencido no es una latencia.
 */
public record SaludDeServicio(String servicio, Estado estado, Optional<Long> latenciaMs) {

    public enum Estado {
        /** Contesto 200. */
        ARRIBA,
        /** Contesto, pero con otro estado (tipicamente 503 OUT_OF_SERVICE). */
        DEGRADADO,
        /** No contesto a tiempo o rechazo la conexion. */
        CAIDO
    }
}
