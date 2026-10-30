package bo.aportaya.grupos.infraestructura.clientes;

import bo.aportaya.grupos.dominio.Arenas;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La arena de una persona segun {@code transparencia} (2026-10-30): {@code GET /reputacion/arenas} y
 * {@code GET /reputacion/{usuarioId}/puntaje}, por su contrato y nunca por su base (regla 98.1.1).
 *
 * <p>Va por el mismo {@link ClienteConCircuito} hacia transparencia que la reputacion: tiempo maximo de salida
 * (1 s de conexion, 3 s de lectura, {@code ConfiguracionDeClientes}) y circuit breaker propio. Degradacion declarada
 * (regla 98.2.4): si cualquiera de las dos respuestas no llega, llega sin catalogo o llega ilegible, el resultado es
 * VACIO y quien pregunta no deja entrar ni crear. Nunca se supone una arena.
 */
final class ArenasPorHttp {

    private final ClienteConCircuito transparencia;

    ArenasPorHttp(ClienteConCircuito transparencia) {
        this.transparencia = transparencia;
    }

    Optional<Arenas.DeLaPersona> de(UUID usuarioId) {
        Optional<List<Arenas.Arena>> catalogo = catalogo();
        if (catalogo.isEmpty() || catalogo.get().isEmpty()) return Optional.empty();
        try {
            return transparencia
                    .consultar("/reputacion/" + usuarioId + "/puntaje", Puntaje.class)
                    .flatMap(p -> persona(p, catalogo.get()));
        } catch (RuntimeException sinRespuesta) {
            // Un 5xx o un cuerpo ilegible tambien es «no contesto»: vacio, y quien pregunta no deja pasar.
            return Optional.empty();
        }
    }

    private Optional<List<Arenas.Arena>> catalogo() {
        try {
            return transparencia
                    .consultar("/reputacion/arenas", ArenaDelCatalogo[].class)
                    .map(filas -> Arrays.stream(filas)
                            .map(ArenasPorHttp::arena)
                            .sorted(Comparator.comparingInt(Arenas.Arena::orden))
                            .toList());
        } catch (RuntimeException ilegible) {
            // Un importe que no es decimal, un campo obligatorio ausente: no es un catalogo, es «sin dato».
            return Optional.empty();
        }
    }

    private static Optional<Arenas.DeLaPersona> persona(Puntaje p, List<Arenas.Arena> catalogo) {
        try {
            BigDecimal puntaje = p.puntaje() == null ? null : new BigDecimal(p.puntaje());
            Optional<Arenas.Arena> arena = p.arena() != null && p.arena().codigo() != null
                    ? Arenas.porCodigo(catalogo, p.arena().codigo())
                    // Un transparencia sin el campo arena: con historial se ubica el puntaje en el catalogo; sin
                    // historial no se sabe el puntaje base, y no se inventa.
                    : p.tieneHistorial() ? Arenas.contiene(catalogo, puntaje) : Optional.empty();
            return arena.map(a ->
                    new Arenas.DeLaPersona(p.tieneHistorial() && puntaje != null ? puntaje : a.desde(), a, catalogo));
        } catch (NumberFormatException ilegible) {
            return Optional.empty();
        }
    }

    private static Arenas.Arena arena(ArenaDelCatalogo f) {
        Arenas.Desbloqueo desbloqueo = f.desbloqueo() == null
                ? null
                : new Arenas.Desbloqueo(
                        f.desbloqueo().cuposMax(),
                        new BigDecimal(f.desbloqueo().aporteMax()),
                        f.desbloqueo().moneda(),
                        f.desbloqueo().estado());
        return new Arenas.Arena(
                f.codigo(), f.orden(), new BigDecimal(f.desde()), new BigDecimal(f.hasta()), desbloqueo);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Puntaje(boolean tieneHistorial, String puntaje, ArenaDelPuntaje arena) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ArenaDelPuntaje(String codigo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ArenaDelCatalogo(
            String codigo, String nombre, int orden, String desde, String hasta, DesbloqueoDeArena desbloqueo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DesbloqueoDeArena(int cuposMax, String aporteMax, String moneda, String estado) {}
}
