package bo.aportaya.identidad.web;

import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.web.errores.LimiteDeUsoExcedido;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tope de consultas de titularidad por actor, en ventana deslizante (CU-18).
 *
 * <p>La verificacion contesta solo si/no, pero un si/no repetido contra un documento o un nombre es un oraculo: con la
 * credencial de servicio de {@code entregas} (o el permiso de backoffice) y sin tope, se adivina el titular de una
 * cuenta probando valores (regla 90.1.14). El tope no la vuelve inofensiva, la vuelve cara. El actor es quien firma la
 * llamada: para {@code entregas}, su credencial de servicio.
 *
 * <p>Vive en memoria de la replica, a proposito: es un freno al abuso, no un contador de facturacion. Con N replicas el
 * tope efectivo es N veces el configurado, y eso se declara. Es la misma pieza que {@code notificaciones} usa para la
 * consulta de supresion (carril A3); se repite aca porque vive en la capa web de cada servicio.
 */
final class LimiteDeConsultas {

    private static final int ACTORES_ANTES_DE_BARRER = 10_000;

    private final Reloj reloj;
    private final int maximo;
    private final Duration ventana;
    private final Map<UUID, Deque<Instant>> porActor = new ConcurrentHashMap<>();

    LimiteDeConsultas(Reloj reloj, int maximo, Duration ventana) {
        this.reloj = reloj;
        this.maximo = maximo;
        this.ventana = ventana;
    }

    /** Anota una consulta del actor, o lanza {@link LimiteDeUsoExcedido} con cuanto esperar. */
    void registrar(UUID actor) {
        Instant ahora = reloj.ahora();
        Deque<Instant> consultas = porActor.computeIfAbsent(actor, a -> new ArrayDeque<>());
        synchronized (consultas) {
            while (!consultas.isEmpty() && !consultas.peekFirst().isAfter(ahora.minus(ventana))) {
                consultas.pollFirst();
            }
            if (consultas.size() >= maximo) {
                throw new LimiteDeUsoExcedido(
                        Duration.between(ahora, consultas.peekFirst().plus(ventana)));
            }
            consultas.addLast(ahora);
        }
        if (porActor.size() > ACTORES_ANTES_DE_BARRER) {
            porActor.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().isEmpty() || !e.getValue().peekLast().isAfter(ahora.minus(ventana));
                }
            });
        }
    }
}
