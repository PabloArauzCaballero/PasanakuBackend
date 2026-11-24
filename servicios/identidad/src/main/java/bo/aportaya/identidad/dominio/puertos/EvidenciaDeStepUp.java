package bo.aportaya.identidad.dominio.puertos;

import java.util.UUID;

/**
 * Validar la evidencia step-up que acompana un acto de gobierno de accesos (GU-11, decision P-7).
 *
 * <p>La evidencia es el JWT que emite {@code POST /sesiones/desafios/{id}/verificacion} con proposito
 * {@code ADMIN}. Puerto porque toca firma, reloj y un registro de un solo uso; el caso de uso solo necesita
 * saber si alcanza o no.
 */
public interface EvidenciaDeStepUp {

    /**
     * {@code true} solo si la evidencia es de {@code actorId}, de proposito {@code ADMIN}, vigente, firmada por
     * identidad y no se uso antes. Consumirla es parte de validarla: una segunda llamada con la misma
     * evidencia devuelve {@code false}.
     */
    boolean consumir(UUID actorId, String evidencia);
}
