package bo.aportaya.auditoria.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

/**
 * sistemas/servicios · lo que se midio de un servicio en la ventana del SLO: cuantos minutos se le pregunto el
 * readiness y en cuantos contesto 200. Los minutos sin medicion (auditoria caido, despliegue) no cuentan ni a favor
 * ni en contra: no se sabe que paso, y contarlos como caida o como arriba seria inventar.
 *
 * <p>Los porcentajes se truncan hacia abajo: una disponibilidad redondeada hacia arriba promete mas de lo medido.
 */
public record DisponibilidadDelServicio(
        String servicio, long minutosMedidos, long minutosArriba, Optional<Instant> ultimaInterrupcion) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public DisponibilidadDelServicio {
        if (minutosMedidos < 0 || minutosArriba < 0 || minutosArriba > minutosMedidos) {
            throw new IllegalArgumentException("minutos incoherentes para " + servicio);
        }
    }

    /** Porcentaje de minutos ARRIBA sobre los medidos, con dos decimales; vacio si no hay medicion. */
    public Optional<BigDecimal> disponibilidad() {
        if (minutosMedidos == 0) {
            return Optional.empty();
        }
        return Optional.of(BigDecimal.valueOf(minutosArriba)
                .multiply(CIEN)
                .divide(BigDecimal.valueOf(minutosMedidos), 2, RoundingMode.DOWN));
    }

    /**
     * Cuanto del presupuesto de error queda, en porcentaje con un decimal: 100 sin caidas, 0 al agotarlo, negativo
     * cuando el SLO ya no se cumple (se dice, no se esconde en un cero). Vacio si no hay medicion.
     *
     * @param objetivo el SLO en porcentaje, por ejemplo 99.5
     */
    public Optional<BigDecimal> presupuestoRestante(BigDecimal objetivo) {
        if (objetivo.signum() <= 0 || objetivo.compareTo(CIEN) >= 0) {
            // Un SLO de 100 % no tiene presupuesto: dividir por cero no es una respuesta.
            throw new IllegalArgumentException("objetivo de SLO fuera de (0, 100): " + objetivo);
        }
        if (minutosMedidos == 0) {
            return Optional.empty();
        }
        // minutos permitidos sin estar ARRIBA = medidos × (100 − objetivo) / 100
        BigDecimal permitidos = BigDecimal.valueOf(minutosMedidos).multiply(CIEN.subtract(objetivo));
        BigDecimal gastados = BigDecimal.valueOf(minutosMedidos - minutosArriba)
                .multiply(CIEN)
                .multiply(CIEN);
        return Optional.of(CIEN.subtract(gastados.divide(permitidos, 1, RoundingMode.CEILING)));
    }
}
