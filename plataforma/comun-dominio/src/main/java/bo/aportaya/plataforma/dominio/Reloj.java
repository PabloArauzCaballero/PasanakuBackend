package bo.aportaya.plataforma.dominio;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * El reloj se inyecta. Un {@code Instant.now()} dentro de un calculo es una prueba no
 * determinista esperando fecha: el dia que el plazo caiga en el borde, la suite falla
 * en la maquina de otro y nadie sabe por que.
 *
 * <p>Todo instante que sale de aca viene truncado a <b>microsegundos</b>, que es la
 * precision de {@code timestamptz}. No es un detalle cosmetico: sin truncar, el valor
 * que la aplicacion tiene en memoria y el que la base guarda dejan de ser el mismo, y
 * cualquier comparacion entre «lo que devolvi» y «lo que quedo escrito» falla por menos
 * de un microsegundo. Pasa solo en algunas maquinas —de ahi lo peor del asunto—: en
 * Linux este reloj da nanosegundos y en macOS ya da microsegundos, asi que la misma
 * prueba pasa en la laptop y falla en el CI. Se midio: {@code getNano()} da 654936304
 * en Linux y 50458000 en macOS.
 */
public interface Reloj {

    /** Bolivia no tiene horario de verano, pero el plazo legal se cuenta aca. */
    ZoneId ZONA = ZoneId.of("America/La_Paz");

    Instant ahora();

    default LocalDate hoy() {
        return LocalDate.ofInstant(ahora(), ZONA);
    }

    static Reloj delSistema() {
        Clock reloj = Clock.system(ZONA);
        return () -> reloj.instant().truncatedTo(ChronoUnit.MICROS);
    }

    /** Para las pruebas: el tiempo se para donde uno lo deja, con la misma precision. */
    static Reloj fijo(Instant momento) {
        Instant truncado = momento.truncatedTo(ChronoUnit.MICROS);
        return () -> truncado;
    }
}
