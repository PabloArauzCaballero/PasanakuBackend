package bo.aportaya.notificaciones.web;

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
 * Tope de consultas de supresion por actor, en ventana deslizante.
 *
 * <p>La consulta contesta «este contacto pidio no recibir»: con permiso de administrar un grupo
 * y sin tope, recorre una agenda y arma la lista de quienes se dieron de baja. El tope no la
 * vuelve inofensiva, la vuelve cara: un administrador legitimo invita a unas decenas de personas
 * por hora; recorrer miles lo frena.
 *
 * <p>Vive en memoria de la replica, a proposito: es un freno al abuso, no un contador de
 * facturacion. Con N replicas el tope efectivo es N veces el configurado, y eso se declara.
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
