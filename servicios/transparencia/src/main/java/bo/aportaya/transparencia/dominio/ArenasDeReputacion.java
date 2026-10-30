package bo.aportaya.transparencia.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * En que arena esta un puntaje y cuanto le falta para la siguiente (arenas de reputacion,
 * 2026-10-30).
 *
 * <p><b>La arena no se guarda: se deriva.</b> El puntaje sale de eventos y la arena sale del
 * puntaje; nadie sube de arena a mano (regla 91.4.2). Por eso esto es una funcion pura sobre
 * el catalogo, y no una columna de {@code puntaje_reputacion}.
 *
 * <p>Cada arena es {@code [desde, hasta)}, salvo la ultima, que incluye su {@code hasta} (el
 * maximo del modelo). El catalogo no tiene solapes (R-REP-07); un puntaje que cae en un hueco
 * o fuera de la escala se queda sin arena, y quien pregunta lo ve ausente en vez de recibir
 * una arena inventada.
 *
 * <p>El progreso va de 0 a 1 con cuatro decimales y redondeo HALF_EVEN, en {@link BigDecimal}:
 * es lo que la app dibuja como barra, y dos clientes tienen que dibujar la misma.
 */
public final class ArenasDeReputacion {

    /** Escala del progreso: cuatro decimales, como dice el contrato ({@code 0.0000}–{@code 1.0000}). */
    private static final int ESCALA_DEL_PROGRESO = 4;

    private ArenasDeReputacion() {}

    /**
     * La arena de {@code puntaje} dentro de {@code catalogo}, con la siguiente y el progreso.
     *
     * @param catalogo las arenas de UN modelo; el orden de la lista no importa, manda {@code orden}
     */
    public static Optional<Ubicacion> ubicar(List<Arena> catalogo, BigDecimal puntaje) {
        if (puntaje == null || catalogo.isEmpty()) {
            return Optional.empty();
        }
        List<Arena> ordenadas =
                catalogo.stream().sorted(Comparator.comparingInt(Arena::orden)).toList();
        int ultima = ordenadas.size() - 1;
        for (int i = 0; i <= ultima; i++) {
            Arena arena = ordenadas.get(i);
            boolean dentro = puntaje.compareTo(arena.desde()) >= 0
                    && (i == ultima ? puntaje.compareTo(arena.hasta()) <= 0 : puntaje.compareTo(arena.hasta()) < 0);
            if (dentro) {
                Arena siguiente = i == ultima ? null : ordenadas.get(i + 1);
                return Optional.of(new Ubicacion(arena, siguiente, progreso(arena, puntaje)));
            }
        }
        return Optional.empty();
    }

    /** {@code (puntaje − desde) / (hasta − desde)}, a cuatro decimales HALF_EVEN. */
    static BigDecimal progreso(Arena arena, BigDecimal puntaje) {
        BigDecimal ancho = arena.hasta().subtract(arena.desde());
        return puntaje.subtract(arena.desde()).divide(ancho, ESCALA_DEL_PROGRESO, RoundingMode.HALF_EVEN);
    }

    /**
     * Lo que desbloquea una arena: cupos totales y aporte por periodo maximos, con su moneda.
     *
     * <p>Solo limita el {@code VIGENTE}. Uno {@code PROPUESTO} se muestra, pero sus valores
     * esperan la confirmacion de negocio y cumplimiento (regla 97.5.4).
     */
    public record Desbloqueo(int cuposMax, BigDecimal aporteMax, String moneda, String estado) {

        public boolean vigente() {
            return "VIGENTE".equals(estado);
        }
    }

    /** Una arena del catalogo. {@code desbloqueo} es nulo si la arena no limita nada. */
    public record Arena(
            String codigo, String nombre, int orden, BigDecimal desde, BigDecimal hasta, Desbloqueo desbloqueo) {}

    /** Donde cae un puntaje: su arena, la de arriba (nula en la ultima) y cuanto avanzo. */
    public record Ubicacion(Arena arena, Arena siguiente, BigDecimal progreso) {}
}
