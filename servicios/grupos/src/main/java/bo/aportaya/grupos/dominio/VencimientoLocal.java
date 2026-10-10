package bo.aportaya.grupos.dominio;

import bo.aportaya.plataforma.dominio.CalendarioHabil;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.PlazoHabil;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * El vencimiento de una obligacion, con las TRES fechas a la vista: la que ve el reloj en UTC, la
 * contractual (el dia calendario en la zona del contrato) y la de liquidacion (el primer dia habil
 * desde la contractual).
 *
 * <p>Por que tres. Un instante en UTC puede caer en un dia distinto del que vive la persona:
 * las 22:00 del 1 de mayo en La Paz son las 02:00 del 2 de mayo en UTC. Si el calendario se
 * consultara con la fecha UTC, un feriado del 1 de mayo no correria nada y un sabado UTC correria
 * un viernes laboral. La fecha contractual es siempre la del dia en la zona del contrato; la de
 * liquidacion nunca es anterior (a favor del cliente, nunca hacia atras).
 *
 * <p>Que dias son no habiles lo dice el calendario que se le pasa, MAS sabados y domingos. Esa
 * ultima regla es la misma que usa {@link PlazoHabil} para sumar plazos; si el contrato o la
 * regulacion trataran el sabado como habil, es una decision abierta (ver la ficha de ambiguedades
 * del carril O2) y se cambia aca, en un solo lugar. Esta clase no inventa feriados: solo los lee.
 */
public record VencimientoLocal(
        Instant instante,
        ZoneId zona,
        LocalDate fechaEnUtc,
        LocalDate fechaContractual,
        LocalDate fechaDeLiquidacion,
        List<DiaNoHabil> diasSalteados) {

    /** Un dia que no conto, y por que. */
    public record DiaNoHabil(LocalDate fecha, String motivo) {}

    /** Hasta donde hay que pedirle feriados al calendario: nadie encadena mas dias no habiles que esto. */
    public static final int VENTANA_DE_BUSQUEDA_EN_DIAS = 30;

    public VencimientoLocal {
        diasSalteados = List.copyOf(diasSalteados);
    }

    /** ¿La liquidacion cae despues de la fecha contractual? Entonces hay que explicarlo, no solo mostrarlo. */
    public boolean corridaPorNoHabil() {
        return fechaDeLiquidacion.isAfter(fechaContractual);
    }

    /** El dia calendario del instante en la zona del contrato. */
    public static LocalDate diaEn(Instant instante, ZoneId zona) {
        return LocalDate.ofInstant(Objects.requireNonNull(instante, "instante"), Objects.requireNonNull(zona, "zona"));
    }

    /** Sabados, domingos y los dias del calendario: el criterio unico de «no habil» de este servicio. */
    public static CalendarioHabil calendario(Map<LocalDate, String> noHabiles) {
        Objects.requireNonNull(noHabiles, "noHabiles");
        return fecha -> noHabiles.containsKey(fecha) || esFinDeSemana(fecha);
    }

    public static boolean esFinDeSemana(LocalDate fecha) {
        return fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /**
     * Calcula las tres fechas. {@code noHabiles} debe traer los dias del calendario de
     * {@code fechaContractual} en adelante (al menos {@link #VENTANA_DE_BUSQUEDA_EN_DIAS}): un dia que
     * no esta en el mapa se considera habil si no es fin de semana.
     */
    public static VencimientoLocal de(Instant instante, ZoneId zona, Map<LocalDate, String> noHabiles) {
        LocalDate contractual = diaEn(instante, zona);
        LocalDate enUtc = diaEn(instante, ZoneId.of("UTC"));
        CalendarioHabil calendario = calendario(noHabiles);

        LocalDate liquidacion = PlazoHabil.siguienteHabil(contractual, calendario);
        if (liquidacion.isAfter(contractual.plusDays(VENTANA_DE_BUSQUEDA_EN_DIAS))) {
            throw new ErrorDeDominio(
                    "El calendario encadena mas de %d dias no habiles desde %s: revisa los feriados cargados"
                            .formatted(VENTANA_DE_BUSQUEDA_EN_DIAS, contractual));
        }

        List<DiaNoHabil> salteados = new ArrayList<>();
        for (LocalDate dia = contractual; dia.isBefore(liquidacion); dia = dia.plusDays(1)) {
            salteados.add(new DiaNoHabil(dia, motivoDe(dia, noHabiles)));
        }
        return new VencimientoLocal(instante, zona, enUtc, contractual, liquidacion, salteados);
    }

    private static String motivoDe(LocalDate dia, Map<LocalDate, String> noHabiles) {
        String delCalendario = noHabiles.get(dia);
        if (delCalendario != null) {
            return delCalendario;
        }
        return dia.getDayOfWeek() == DayOfWeek.SATURDAY ? "Sabado" : "Domingo";
    }
}
