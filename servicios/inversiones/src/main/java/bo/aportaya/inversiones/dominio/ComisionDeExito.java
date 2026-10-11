package bo.aportaya.inversiones.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Comision por resultado con marca maxima previa NETA de comision (high-water mark).
 *
 * <p>Solo grava lo que supera la marca previa: si el valor cae y despues se recupera sin
 * pasar la marca, no hay comision. Cada suscripcion es su propio lote con su propia marca
 * (la que parte del valor de entrada), asi que no se cobra por ganancias anteriores a la
 * entrada de la persona.
 *
 * <p>Ejemplo sintetico del plan (H11): 100 cuotas, marca 110, valor 112 y tasa del 10 %
 * dan base elegible 200,00, comision 20,00 y cuota neta 111,80. Es un ejemplo de calculo,
 * NO una tarifa comercial: la tasa sale de las condiciones de cada producto y mientras no
 * tenga fuente aprobada es sintetica ({@code DR-INV-04}).
 *
 * <p>Redondeo: la base y la comision a centavos (half-even); la cuota neta a seis
 * decimales (half-even), partiendo del valor total sin redondear antes. La marca nueva
 * nunca baja de la previa.
 */
public record ComisionDeExito(
        BigDecimal baseElegible, BigDecimal comision, BigDecimal cuotaNeta, BigDecimal marcaNueva) {

    public static final int ESCALA_CUOTA = 6;

    public static ComisionDeExito calcular(
            BigDecimal cuotas, BigDecimal valorCuota, BigDecimal marcaPrevia, BigDecimal tasa) {
        if (cuotas.signum() <= 0 || valorCuota.signum() <= 0 || marcaPrevia.signum() <= 0 || tasa.signum() < 0) {
            throw new IllegalArgumentException("Cuotas, valor y marca deben ser positivos y la tasa no negativa");
        }
        if (valorCuota.compareTo(marcaPrevia) <= 0 || tasa.signum() == 0) {
            return new ComisionDeExito(
                    BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), valorCuota, marcaPrevia);
        }
        BigDecimal base = valorCuota.subtract(marcaPrevia).multiply(cuotas).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal comision = base.multiply(tasa).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal cuotaNeta =
                valorCuota.multiply(cuotas).subtract(comision).divide(cuotas, ESCALA_CUOTA, RoundingMode.HALF_EVEN);
        return new ComisionDeExito(base, comision, cuotaNeta, marcaPrevia.max(cuotaNeta));
    }
}
