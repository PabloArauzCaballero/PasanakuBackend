package bo.aportaya.inversiones.dominio;

import bo.aportaya.plataforma.dominio.CalendarioHabil;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * El corte de un fondo: una orden que llega despues de la hora de corte, o un dia no
 * habil, toma el valor del proximo dia habil.
 *
 * <p>Esto es lo que se le INFORMA a la persona antes de enviar. El dato que manda es el
 * que devuelve el aliado en su confirmacion; si difieren, gana el aliado y la diferencia
 * queda a la vista.
 */
public final class CorteDeOrden {

    private CorteDeOrden() {}

    public static LocalDate fechaValor(Instant ahora, LocalTime horaCorte, CalendarioHabil calendario) {
        ZonedDateTime local = ahora.atZone(Reloj.ZONA);
        if (calendario.esHabil(local.toLocalDate()) && local.toLocalTime().isBefore(horaCorte)) {
            return local.toLocalDate();
        }
        return habilPosterior(local.toLocalDate(), 1, calendario);
    }

    /** El n-esimo dia habil posterior a {@code desde}. */
    public static LocalDate habilPosterior(LocalDate desde, int n, CalendarioHabil calendario) {
        LocalDate dia = desde;
        int faltan = n;
        while (faltan > 0) {
            dia = dia.plusDays(1);
            if (calendario.esHabil(dia)) {
                faltan--;
            }
        }
        return dia;
    }
}
