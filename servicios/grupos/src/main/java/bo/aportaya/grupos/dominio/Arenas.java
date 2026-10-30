package bo.aportaya.grupos.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Arenas de reputacion (2026-10-30): tramos CONTINUOS del puntaje que publica {@code transparencia}
 * ({@code GET /reputacion/arenas}). Grupos no las guarda ni las calcula: las recibe por contrato y aca solo compara.
 *
 * <p>Dos reglas viven sobre ellas. La arena minima del grupo: entra quien tiene un puntaje mayor o igual al
 * {@code desde} de esa arena (el grupo guarda ese {@code desde} en {@code reputacion_minima}). Y el desbloqueo de cada
 * arena: cuantos cupos y cuanto aporte por periodo puede tener un grupo que crea o integra quien esta en ella. Solo se
 * aplica el desbloqueo {@code VIGENTE}; uno {@code PROPUESTO} todavia no limita nada (sus valores esperan la
 * confirmacion de negocio, regla 97.5.4).
 *
 * <p>Atomo puro: sin red ni base. Quien llama ya resolvio los hechos afuera y decide que codigo de error corresponde.
 */
public final class Arenas {

    private static final String VIGENTE = "VIGENTE";

    private Arenas() {}

    /** Una arena del catalogo. {@code desbloqueo} es nulo si la arena no limita nada. */
    public record Arena(String codigo, int orden, BigDecimal desde, BigDecimal hasta, Desbloqueo desbloqueo) {}

    /** Lo que desbloquea una arena: cupos totales y aporte por periodo maximos, con su moneda. */
    public record Desbloqueo(int cuposMax, BigDecimal aporteMax, String moneda, String estado) {
        public boolean vigente() {
            return VIGENTE.equals(estado);
        }
    }

    /**
     * Lo que se sabe de una persona: el puntaje con el que se la compara, su arena y el catalogo entero (para
     * nombrar la arena minima de un grupo). Sin historial, el puntaje es el {@code desde} de la arena que le asigna
     * {@code transparencia} (la del puntaje base del modelo): «no se sabe» no es «malo», pero tampoco se le supone
     * mas de lo que el piso de esa arena garantiza.
     */
    public record DeLaPersona(BigDecimal puntaje, Arena arena, List<Arena> catalogo) {}

    /** La arena que contiene ese puntaje: {@code desde} incluido y {@code hasta} excluido; la ultima incluye su hasta. */
    public static Optional<Arena> contiene(List<Arena> catalogo, BigDecimal puntaje) {
        if (puntaje == null || catalogo.isEmpty()) return Optional.empty();
        Arena ultima = catalogo.get(catalogo.size() - 1);
        for (Arena arena : catalogo) {
            boolean dentro = puntaje.compareTo(arena.desde()) >= 0
                    && (puntaje.compareTo(arena.hasta()) < 0
                            || (arena == ultima && puntaje.compareTo(arena.hasta()) == 0));
            if (dentro) return Optional.of(arena);
        }
        return Optional.empty();
    }

    public static Optional<Arena> porCodigo(List<Arena> catalogo, String codigo) {
        return catalogo.stream().filter(a -> a.codigo().equals(codigo)).findFirst();
    }

    /** Si la persona llega a la arena minima del grupo. Un grupo sin minimo (nulo o cero) no pide nada. */
    public static boolean alcanza(DeLaPersona persona, BigDecimal reputacionMinima) {
        return reputacionMinima == null
                || reputacionMinima.signum() <= 0
                || persona.puntaje().compareTo(reputacionMinima) >= 0;
    }

    /**
     * El desbloqueo VIGENTE de la arena, si no cubre un grupo de esos cupos y ese aporte. Un aporte en otra moneda que
     * la del desbloqueo tampoco queda cubierto: comparar importes de monedas distintas sin tipo de cambio esta
     * prohibido (regla 91.1.2), asi que se deniega por omision. Sin desbloqueo, o con uno PROPUESTO, no limita.
     */
    public static Optional<Desbloqueo> excedido(Arena arena, int cupos, BigDecimal aporte, String moneda) {
        Desbloqueo d = arena.desbloqueo();
        if (d == null || !d.vigente()) return Optional.empty();
        boolean cubre = cupos <= d.cuposMax() && d.moneda().equals(moneda) && aporte.compareTo(d.aporteMax()) <= 0;
        return cubre ? Optional.empty() : Optional.of(d);
    }

    /** El codigo de la arena minima que pide un grupo, para mostrarlo; vacio si no pide ninguna. */
    public static Optional<String> codigoDeLaMinima(List<Arena> catalogo, BigDecimal reputacionMinima) {
        if (reputacionMinima == null || reputacionMinima.signum() <= 0) return Optional.empty();
        return contiene(catalogo, reputacionMinima).map(Arena::codigo);
    }
}
