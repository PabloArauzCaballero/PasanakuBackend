package bo.aportaya.plataforma.web.seguridad;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Las sesiones revocadas que este proceso conoce, en memoria (ADR-030 §2).
 *
 * <p><b>No es una fuente de verdad: es una copia reconstruible.</b> La verdad es {@code identidad.sesion} y su eco es
 * el tema compactado. Al reiniciar, la lista nace vacia y <b>no al dia</b>; {@link LectorDeRevocaciones} la vuelve a
 * llenar leyendo el tema desde el principio y recien entonces la declara al dia. Mientras no lo esta, el validador
 * falla del lado seguro (ADR-030 §3).
 *
 * <p>Cada entrada se recuerda hasta su {@code venceEn}: despues ningun token de esa sesion puede estar vivo, y
 * olvidarla no abre nada. Por eso el tamaño queda acotado por las revocaciones de la ultima media hora, no por la
 * historia.
 */
public class ListaDeRevocaciones {

    private final Map<UUID, Instant> revocadas = new ConcurrentHashMap<>();
    private final AtomicBoolean alDia = new AtomicBoolean(false);

    /** Agrega o renueva la revocacion. Si llega dos veces, gana el vencimiento mas lejano. */
    public void registrar(RevocacionDeSesion revocacion) {
        revocadas.merge(
                revocacion.sesionId(), revocacion.venceEn(), (antes, ahora) -> ahora.isAfter(antes) ? ahora : antes);
    }

    /** {@code true} si la sesion esta revocada y todavia hay que recordarlo. */
    public boolean estaRevocada(UUID sesionId, Instant ahora) {
        Instant vence = revocadas.get(sesionId);
        return vence != null && ahora.isBefore(vence);
    }

    /** Olvida lo que ya vencio. Devuelve cuantas entradas quito. */
    public int purgar(Instant ahora) {
        int antes = revocadas.size();
        revocadas.values().removeIf(vence -> !ahora.isBefore(vence));
        return antes - revocadas.size();
    }

    public int tamano() {
        return revocadas.size();
    }

    /** La lista alcanzo el final del tema al arrancar: lo que no esta, no fue revocado. */
    public void marcarAlDia() {
        alDia.set(true);
    }

    /** El tema no esta disponible. Conserva lo conocido, pero no se puede validar el resto de las sesiones. */
    void marcarNoDisponible() {
        alDia.set(false);
    }

    public boolean estaAlDia() {
        return alDia.get();
    }
}
