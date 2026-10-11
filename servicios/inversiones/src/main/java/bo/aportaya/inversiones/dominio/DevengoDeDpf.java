package bo.aportaya.inversiones.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * El interes simple de un DPF: principal x tasa nominal x dias reales / base de dias.
 *
 * <p>Se redondea UNA vez, a centavos (half-even), sobre el acumulado. El devengo de un
 * dia es la diferencia entre dos acumulados redondeados: la suma de los devengos diarios
 * es exactamente el interes del plazo, sin residuo perdido (el resto de redondeo cae en
 * el dia que lo produce). No convierte una tasa anual dividiendo por doce: el contrato
 * fija la base de dias.
 */
public final class DevengoDeDpf {

    private DevengoDeDpf() {}

    public static BigDecimal interes(BigDecimal principal, BigDecimal tasaNominalAnual, int dias, int baseDias) {
        if (dias < 0 || baseDias <= 0) {
            throw new IllegalArgumentException("Dias y base de dias deben ser positivos");
        }
        return principal
                .multiply(tasaNominalAnual)
                .multiply(BigDecimal.valueOf(dias))
                .divide(BigDecimal.valueOf(baseDias), 2, RoundingMode.HALF_EVEN);
    }

    /** El acumulado a los {@code diasTranscurridos}, sin pasar del plazo. */
    public static BigDecimal acumulado(
            BigDecimal principal, BigDecimal tasaNominalAnual, int diasTranscurridos, int plazoDias, int baseDias) {
        return interes(principal, tasaNominalAnual, Math.min(Math.max(diasTranscurridos, 0), plazoDias), baseDias);
    }

    /** La retencion sobre un interes, a centavos. */
    public static BigDecimal retencion(BigDecimal interes, BigDecimal tasaRetencion) {
        return interes.multiply(tasaRetencion).setScale(2, RoundingMode.HALF_EVEN);
    }
}
