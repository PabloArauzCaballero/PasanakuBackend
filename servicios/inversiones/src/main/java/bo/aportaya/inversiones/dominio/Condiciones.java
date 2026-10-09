package bo.aportaya.inversiones.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Las condiciones de un producto tal como estaban cuando se cotizaron.
 *
 * <p>Inmutable a proposito: es lo que la persona acepto. Una cotizacion nueva es una
 * version nueva, nunca una edicion de esta.
 *
 * @param plazoDias solo DPF
 * @param tasaNominalAnual solo DPF; un fondo no declara tasa fija (no hay rentabilidad prometida)
 * @param diasRescate solo fondo: dias habiles entre el corte y la liquidacion
 * @param horaCorte solo fondo, hora de La Paz
 */
public record Condiciones(
        UUID versionId,
        int numero,
        TipoProducto tipo,
        Optional<Integer> plazoDias,
        Optional<Integer> baseDias,
        Optional<BigDecimal> tasaNominalAnual,
        boolean permiteRescateAnticipado,
        Optional<BigDecimal> penalizacionAnticipo,
        Optional<Integer> diasRescate,
        Optional<LocalTime> horaCorte,
        BigDecimal montoMinimo,
        Optional<BigDecimal> tasaRetencion,
        Optional<BigDecimal> tasaComisionExito,
        String costos,
        String texto,
        String textoHash,
        String fuente,
        Instant fechaCotizacion,
        OrigenDatos origen,
        boolean aptoProduccion) {

    public Condiciones {
        if (origen == OrigenDatos.SINTETICO && aptoProduccion) {
            throw new IllegalArgumentException("Un dato sintetico no puede ser apto para produccion");
        }
    }

    public boolean esSintetico() {
        return origen == OrigenDatos.SINTETICO;
    }
}
