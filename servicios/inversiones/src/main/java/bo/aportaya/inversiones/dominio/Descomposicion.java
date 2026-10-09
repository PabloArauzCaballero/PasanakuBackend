package bo.aportaya.inversiones.dominio;

import java.math.BigDecimal;

/**
 * Lo que se paga o se invierte, separado en las cosas que el titular y el fisco
 * necesitan ver por separado: principal, interes, impuesto y comision.
 *
 * <p>{@code neto = principal + interes - impuesto - comision} y
 * {@code costoBase = principal + perdidaRealizada}. Las dos identidades se verifican al
 * construir y la base las repite ({@code R-INV-03}): un comprobante que no descompone
 * sin residuo no existe.
 *
 * <p>Una perdida y una ganancia no conviven en la misma liquidacion.
 */
public record Descomposicion(
        BigDecimal principal,
        BigDecimal interes,
        BigDecimal impuesto,
        BigDecimal comision,
        BigDecimal perdidaRealizada,
        BigDecimal neto,
        BigDecimal costoBase) {

    public Descomposicion {
        if (principal.signum() < 0
                || interes.signum() < 0
                || impuesto.signum() < 0
                || comision.signum() < 0
                || perdidaRealizada.signum() < 0) {
            throw new IllegalArgumentException("Ningun componente de una liquidacion es negativo");
        }
        if (perdidaRealizada.signum() > 0 && interes.signum() > 0) {
            throw new IllegalArgumentException("Una liquidacion no tiene ganancia y perdida a la vez");
        }
        if (neto.compareTo(principal.add(interes).subtract(impuesto).subtract(comision)) != 0 || neto.signum() < 0) {
            throw new IllegalArgumentException("El neto no es principal + interes - impuesto - comision");
        }
        if (costoBase.compareTo(principal.add(perdidaRealizada)) != 0) {
            throw new IllegalArgumentException("El costo base no es principal + perdida");
        }
    }

    /** La suscripcion: todo es principal. */
    public static Descomposicion suscripcion(BigDecimal monto) {
        return new Descomposicion(
                monto, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, monto, monto);
    }

    /** El vencimiento (o cancelacion) de un DPF: devuelve el principal y paga el interes menos la retencion. */
    public static Descomposicion dpf(BigDecimal principal, BigDecimal interesBruto, BigDecimal retencion) {
        BigDecimal neto = principal.add(interesBruto).subtract(retencion);
        return new Descomposicion(
                principal, interesBruto, retencion, BigDecimal.ZERO, BigDecimal.ZERO, neto, principal);
    }

    /**
     * El rescate de cuotas: lo que valen hoy contra el costo que se libera. Si valen
     * menos, la diferencia es perdida realizada y no se disfraza de interes negativo.
     *
     * @param costoBaseLiberado la parte del costo que corresponde a las cuotas rescatadas
     * @param producido lo que valen las cuotas rescatadas (cuotas x valor, a centavos)
     * @param comision comision de exito ya calculada; nunca supera la ganancia
     */
    public static Descomposicion fondo(BigDecimal costoBaseLiberado, BigDecimal producido, BigDecimal comision) {
        if (producido.compareTo(costoBaseLiberado) >= 0) {
            BigDecimal ganancia = producido.subtract(costoBaseLiberado);
            return new Descomposicion(
                    costoBaseLiberado,
                    ganancia,
                    BigDecimal.ZERO,
                    comision,
                    BigDecimal.ZERO,
                    producido.subtract(comision),
                    costoBaseLiberado);
        }
        if (comision.signum() != 0) {
            throw new IllegalArgumentException("No hay comision de exito sobre una perdida");
        }
        return new Descomposicion(
                producido,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                costoBaseLiberado.subtract(producido),
                producido,
                costoBaseLiberado);
    }
}
