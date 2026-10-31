package bo.aportaya.transparencia.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * En que etapa esta un puntaje y cuanto le falta para la siguiente (etapas de reputacion,
 * 2026-10-30).
 *
 * <p><b>La etapa no se guarda: se deriva.</b> El puntaje sale de eventos y la etapa sale del
 * puntaje; nadie sube de etapa a mano (regla 91.4.2). Por eso esto es una funcion pura sobre
 * el catalogo, y no una columna de {@code puntaje_reputacion}.
 *
 * <p>Cada etapa es {@code [desde, hasta)}, salvo la ultima, que incluye su {@code hasta} (el
 * maximo del modelo). El catalogo no tiene solapes (R-REP-07); un puntaje que cae en un hueco
 * o fuera de la escala se queda sin etapa, y quien pregunta lo ve ausente en vez de recibir
 * una etapa inventada.
 *
 * <p>El progreso va de 0 a 1 con cuatro decimales y redondeo HALF_EVEN, en {@link BigDecimal}:
 * es lo que la app dibuja como barra, y dos clientes tienen que dibujar la misma.
 */
public final class EtapasDeReputacion {

    /** Escala del progreso: cuatro decimales, como dice el contrato ({@code 0.0000}–{@code 1.0000}). */
    private static final int ESCALA_DEL_PROGRESO = 4;

    private EtapasDeReputacion() {}

    /**
     * La etapa de {@code puntaje} dentro de {@code catalogo}, con la siguiente y el progreso.
     *
     * @param catalogo las etapas de UN modelo; el orden de la lista no importa, manda {@code orden}
     */
    public static Optional<Ubicacion> ubicar(List<Etapa> catalogo, BigDecimal puntaje) {
        if (puntaje == null || catalogo.isEmpty()) {
            return Optional.empty();
        }
        List<Etapa> ordenadas =
                catalogo.stream().sorted(Comparator.comparingInt(Etapa::orden)).toList();
        int ultima = ordenadas.size() - 1;
        for (int i = 0; i <= ultima; i++) {
            Etapa etapa = ordenadas.get(i);
            boolean dentro = puntaje.compareTo(etapa.desde()) >= 0
                    && (i == ultima ? puntaje.compareTo(etapa.hasta()) <= 0 : puntaje.compareTo(etapa.hasta()) < 0);
            if (dentro) {
                Etapa siguiente = i == ultima ? null : ordenadas.get(i + 1);
                return Optional.of(new Ubicacion(etapa, siguiente, progreso(etapa, puntaje)));
            }
        }
        return Optional.empty();
    }

    /** {@code (puntaje − desde) / (hasta − desde)}, a cuatro decimales HALF_EVEN. */
    static BigDecimal progreso(Etapa etapa, BigDecimal puntaje) {
        BigDecimal ancho = etapa.hasta().subtract(etapa.desde());
        return puntaje.subtract(etapa.desde()).divide(ancho, ESCALA_DEL_PROGRESO, RoundingMode.HALF_EVEN);
    }

    /**
     * Lo que desbloquea una etapa: cupos totales y aporte por periodo maximos, con su moneda.
     *
     * <p>Solo limita el {@code VIGENTE}. Uno {@code PROPUESTO} se muestra, pero sus valores
     * esperan la confirmacion de negocio y cumplimiento (regla 97.5.4).
     */
    public record Desbloqueo(int cuposMax, BigDecimal aporteMax, String moneda, String estado) {

        public boolean vigente() {
            return "VIGENTE".equals(estado);
        }
    }

    /** Una etapa del catalogo. {@code desbloqueo} es nulo si la etapa no limita nada. */
    public record Etapa(
            String codigo, String nombre, int orden, BigDecimal desde, BigDecimal hasta, Desbloqueo desbloqueo) {}

    /** Donde cae un puntaje: su etapa, la de arriba (nula en la ultima) y cuanto avanzo. */
    public record Ubicacion(Etapa etapa, Etapa siguiente, BigDecimal progreso) {}
}
