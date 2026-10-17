package bo.aportaya.plataforma.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Un indicador de tablero publicado con honestidad (tableros por rol, H3). Patron tomado de los tableros de Atlas
 * ({@code AtlasDashboardsBackend/src/modules/kpi-catalog/kpi-definition.types.ts}), adaptado a dinero exacto.
 *
 * <p>Tres reglas, y son la razon de existir de esta clase:
 *
 * <ul>
 *   <li><b>{@code null} no es cero.</b> Sin dato se publica {@code valor = null} con un {@link Motivo}. Un SUM sobre
 *       cero filas devuelve 0, y un tablero que dibuja «Bs 0 en mora» cuando en realidad no midio nada es
 *       indistinguible de un panel roto.
 *   <li><b>Toda tasa viaja con su numerador y su denominador.</b> Un 40 % sobre 5 casos y sobre 5.000 son el mismo
 *       numero y decisiones muy distintas.
 *   <li><b>Muestra minima.</b> Debajo del minimo declarado el valor se retiene ({@code MUESTRA_INSUFICIENTE}), con
 *       los conteos intactos.
 * </ul>
 *
 * <p>El dinero nunca viaja sin moneda (regla 91.1.2) y nunca como {@code double} (91.1.1).
 */
public record Indicador(
        String codigo,
        Unidad unidad,
        BigDecimal valor,
        Motivo motivo,
        BigDecimal numerador,
        BigDecimal denominador,
        Moneda moneda,
        long muestra,
        BigDecimal valorAnterior,
        Motivo motivoAnterior) {

    public enum Unidad {
        DINERO,
        CONTEO,
        PORCENTAJE,
        DIAS,
        RAZON,
        /** Latencias del tablero de sistemas (p50, p95, p99). */
        MILISEGUNDOS
    }

    /** Por que un indicador no tiene valor. Nunca se reemplaza por un cero. */
    public enum Motivo {
        SIN_DATOS,
        MUESTRA_INSUFICIENTE,
        NO_IMPLEMENTADO,
        FUENTE_NO_CONFIGURADA,
        /** Un indicador de saldo (stock) no tiene periodo anterior hasta que exista su foto diaria. */
        SIN_HISTORIA
    }

    /**
     * Lo que el catalogo dice del indicador. {@code muestraMinima} 0 = sin minimo. {@code ceroEsReal}: el indicador
     * cuenta sucesos que uno espera que NO ocurran (reversos, fallas): ahi cero no es ausencia de medicion, es la
     * buena noticia, y se publica.
     */
    public record Definicion(String codigo, Unidad unidad, int muestraMinima, boolean ceroEsReal) {
        public Definicion {
            Objects.requireNonNull(codigo, "codigo");
            Objects.requireNonNull(unidad, "unidad");
            if (muestraMinima < 0) {
                throw new ErrorDeDominio("La muestra minima de " + codigo + " no puede ser negativa.");
            }
        }
    }

    /**
     * Lo que se midio en un periodo. {@code denominador} solo para tasas; {@code moneda} solo para dinero.
     * {@code muestra}: cuantos casos sostienen el numero (filas, personas, grupos).
     */
    public record Medicion(BigDecimal numerador, BigDecimal denominador, Moneda moneda, long muestra) {
        public Medicion {
            Objects.requireNonNull(numerador, "numerador");
            if (muestra < 0) {
                throw new ErrorDeDominio("La muestra no puede ser negativa.");
            }
        }

        public static Medicion de(Dinero total, long muestra) {
            return new Medicion(total.monto(), null, total.moneda(), muestra);
        }

        public static Medicion conteo(long cuantos) {
            return new Medicion(BigDecimal.valueOf(cuantos), null, null, cuantos);
        }

        public static Medicion tasa(long numerador, long denominador) {
            return new Medicion(BigDecimal.valueOf(numerador), BigDecimal.valueOf(denominador), null, denominador);
        }
    }

    /** Un indicador que todavia no se puede calcular: se publica con su motivo, no se omite. */
    public static Indicador sinValor(Definicion d, Motivo motivo) {
        return new Indicador(d.codigo(), d.unidad(), null, motivo, null, null, null, 0, null, motivo);
    }

    /** Publica la medicion del periodo; el anterior, si lo hay, sirve para la variacion. */
    public static Indicador publicar(Definicion d, Medicion actual, Medicion anterior) {
        Resuelto hoy = resolver(d, actual);
        Resuelto antes = anterior == null ? new Resuelto(null, Motivo.SIN_HISTORIA) : resolver(d, anterior);
        return new Indicador(
                d.codigo(),
                d.unidad(),
                hoy.valor(),
                hoy.motivo(),
                actual.numerador(),
                actual.denominador(),
                actual.moneda(),
                actual.muestra(),
                antes.valor(),
                antes.motivo());
    }

    private record Resuelto(BigDecimal valor, Motivo motivo) {}

    private static Resuelto resolver(Definicion d, Medicion m) {
        if (d.unidad() == Unidad.DINERO && m.moneda() == null) {
            throw new ErrorDeDominio(d.codigo() + ": un importe sin moneda no se publica (regla 91.1.2).");
        }
        boolean esTasa = d.unidad() == Unidad.PORCENTAJE || d.unidad() == Unidad.RAZON;
        if (esTasa && m.denominador() == null) {
            throw new ErrorDeDominio(d.codigo() + ": una tasa sin denominador no se publica.");
        }
        if (m.muestra() == 0 && !d.ceroEsReal()) {
            return new Resuelto(null, Motivo.SIN_DATOS);
        }
        if (m.muestra() < d.muestraMinima()) {
            return new Resuelto(null, Motivo.MUESTRA_INSUFICIENTE);
        }
        if (!esTasa) {
            return new Resuelto(m.numerador(), null);
        }
        if (m.denominador().signum() == 0) {
            return new Resuelto(null, Motivo.SIN_DATOS);
        }
        // Redondeo declarado: HALF_EVEN, dos decimales en porcentaje y cuatro en razon. Es presentacion, no dinero.
        BigDecimal valor = d.unidad() == Unidad.PORCENTAJE
                ? m.numerador().movePointRight(2).divide(m.denominador(), 2, RoundingMode.HALF_EVEN)
                : m.numerador().divide(m.denominador(), 4, RoundingMode.HALF_EVEN);
        return new Resuelto(valor, null);
    }
}
