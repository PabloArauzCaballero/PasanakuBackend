package bo.aportaya.identidad.dominio;

import bo.aportaya.plataforma.dominio.CalendarioHabil;
import bo.aportaya.plataforma.dominio.PlazoHabil;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Plazos del debido proceso de una medida sobre la cuenta (CU-134, D-3). Puro: recibe la politica,
 * la fecha y el calendario; no lee la base ni el reloj.
 *
 * <p>Los plazos se calculan UNA vez y se guardan (R-GAR-01 como criterio): el de descargo al
 * acreditarse la notificacion, el de apelacion al decidir. Un plazo vence al final del dia habil.
 */
public final class PlazosDeMedida {

    private PlazosDeMedida() {}

    /** Los parametros de la politica vigente con la que se abrio el expediente. */
    public record Politica(
            int descargoDiasHabiles, int apelacionDiasHabiles, int prescribeEnMeses, int suspensionMaximaDias) {
        public Politica {
            if (descargoDiasHabiles < 1 || apelacionDiasHabiles < 1 || prescribeEnMeses < 1
                    || suspensionMaximaDias < 1) {
                throw new IllegalArgumentException("Los plazos de la politica de medidas son positivos");
            }
        }
    }

    public static OffsetDateTime limiteDescargo(OffsetDateTime notificadaEn, Politica p, CalendarioHabil cal) {
        return finDelDia(notificadaEn, PlazoHabil.sumar(notificadaEn.toLocalDate(), p.descargoDiasHabiles(), cal));
    }

    public static OffsetDateTime limiteApelacion(OffsetDateTime decididaEn, Politica p, CalendarioHabil cal) {
        return finDelDia(decididaEn, PlazoHabil.sumar(decididaEn.toLocalDate(), p.apelacionDiasHabiles(), cal));
    }

    /** Desde la apertura: sin decision en este plazo el expediente prescribe y no se reabre. */
    public static OffsetDateTime prescripcion(OffsetDateTime propuestaEn, Politica p) {
        return propuestaEn.plusMonths(p.prescribeEnMeses());
    }

    /** Si el fin pedido de una suspension respeta el tope de la politica, contado desde la apertura. */
    public static boolean dentroDelMaximo(OffsetDateTime desde, OffsetDateTime hasta, Politica p) {
        return !hasta.isAfter(desde.plusDays(p.suspensionMaximaDias())) && hasta.isAfter(desde);
    }

    private static OffsetDateTime finDelDia(OffsetDateTime referencia, java.time.LocalDate dia) {
        return OffsetDateTime.of(dia, LocalTime.of(23, 59, 59), referencia.getOffset());
    }
}
