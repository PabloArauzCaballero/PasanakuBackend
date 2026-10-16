package bo.aportaya.inversiones.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Lo que valen hoy unas cuotas, con la fecha del dato que se uso.
 *
 * <p>No hay promesa: la variacion puede ser negativa y un valor viejo se marca
 * {@code desactualizado} en vez de presentarse como actual. La antiguedad se cuenta
 * contra el dia de hoy, no contra el dia de la ultima sincronizacion.
 */
public record ValoracionDeCuotas(
        BigDecimal valorBruto,
        BigDecimal variacion,
        LocalDate fechaValor,
        long antiguedadDias,
        boolean desactualizado) {

    public static ValoracionDeCuotas de(
            BigDecimal cuotas,
            BigDecimal valorCuota,
            BigDecimal costoBaseVigente,
            LocalDate fechaValor,
            LocalDate hoy,
            int toleranciaDias) {
        BigDecimal bruto = cuotas.multiply(valorCuota).setScale(2, RoundingMode.HALF_EVEN);
        long antiguedad = Math.max(0, ChronoUnit.DAYS.between(fechaValor, hoy));
        return new ValoracionDeCuotas(
                bruto, bruto.subtract(costoBaseVigente), fechaValor, antiguedad, antiguedad > toleranciaDias);
    }

    /**
     * El costo que corresponde a una parte de las cuotas. Cuando se rescata TODO lo que
     * queda, se libera el costo restante entero: el residuo de redondeo de los rescates
     * parciales anteriores se asigna al ultimo, no se pierde.
     */
    public static BigDecimal costoBaseLiberado(
            BigDecimal costoBaseRestante, BigDecimal cuotasRescatadas, BigDecimal cuotasQueQuedaban) {
        if (cuotasRescatadas.signum() <= 0 || cuotasRescatadas.compareTo(cuotasQueQuedaban) > 0) {
            throw new IllegalArgumentException("Las cuotas a rescatar deben estar entre cero y las que quedan");
        }
        if (cuotasRescatadas.compareTo(cuotasQueQuedaban) == 0) {
            return costoBaseRestante;
        }
        return costoBaseRestante.multiply(cuotasRescatadas).divide(cuotasQueQuedaban, 2, RoundingMode.HALF_EVEN);
    }
}
