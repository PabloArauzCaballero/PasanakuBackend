package bo.aportaya.aportes.dominio;

import bo.aportaya.plataforma.dominio.CalendarioHabil;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.PlazoHabil;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Las fechas de una obligacion de aporte: la CONTRACTUAL (la que fijo el reglamento del grupo, el
 * dia de cobro) y la de LIQUIDACION (el primer dia habil desde ella, que es la que se guarda como
 * {@code fecha_vencimiento}).
 *
 * <p>CU-59/CU-21: si el vencimiento cae en un dia no habil declarado antes de generar la obligacion,
 * se corre al siguiente habil, nunca al anterior, y se notifica al grupo. La fecha contractual no
 * se pierde: viaja en el evento y en el calendario para que ninguna se vea como la otra.
 *
 * <p>El fin de gracia conserva su LARGO: se corre los mismos dias que el vencimiento y, si queda en
 * un dia no habil, al siguiente habil (supuesto a favor de quien paga; decision abierta en la ficha
 * de ambiguedades del carril O2). Habil = sin feriado del calendario y sin sabado ni domingo, el
 * mismo criterio de {@link PlazoHabil}.
 *
 * <p>Esta clase es pura: recibe los dias no habiles ya leidos de {@code catalogo.dia_no_habil} y no
 * inventa ninguno.
 */
public record VencimientoDeObligacion(
        LocalDate fechaContractual,
        LocalDate fechaDeLiquidacion,
        LocalDate finDeGraciaContractual,
        LocalDate finDeGracia,
        List<DiaSalteado> diasSalteados) {

    public record DiaSalteado(LocalDate fecha, String motivo) {}

    /** Hasta donde se pide el calendario despues de la fecha mas lejana. */
    public static final int VENTANA_EN_DIAS = 30;

    public VencimientoDeObligacion {
        diasSalteados = List.copyOf(diasSalteados);
    }

    public boolean corridaPorNoHabil() {
        return fechaDeLiquidacion.isAfter(fechaContractual);
    }

    public static CalendarioHabil calendario(Map<LocalDate, String> noHabiles) {
        Objects.requireNonNull(noHabiles, "noHabiles");
        return fecha -> noHabiles.containsKey(fecha) || esFinDeSemana(fecha);
    }

    private static boolean esFinDeSemana(LocalDate fecha) {
        return fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public static VencimientoDeObligacion de(
            LocalDate contractual, LocalDate finDeGraciaContractual, Map<LocalDate, String> noHabiles) {
        if (finDeGraciaContractual.isBefore(contractual)) {
            throw new ErrorDeDominio("El fin de gracia (%s) no puede ser anterior al vencimiento (%s)"
                    .formatted(finDeGraciaContractual, contractual));
        }
        CalendarioHabil habil = calendario(noHabiles);
        LocalDate liquidacion = PlazoHabil.siguienteHabil(contractual, habil);
        long corrimiento = ChronoUnit.DAYS.between(contractual, liquidacion);
        LocalDate gracia = PlazoHabil.siguienteHabil(finDeGraciaContractual.plusDays(corrimiento), habil);
        if (gracia.isAfter(finDeGraciaContractual.plusDays(corrimiento + VENTANA_EN_DIAS))) {
            throw new ErrorDeDominio("El calendario encadena demasiados dias no habiles desde " + gracia);
        }

        List<DiaSalteado> salteados = new ArrayList<>();
        for (LocalDate dia = contractual; dia.isBefore(liquidacion); dia = dia.plusDays(1)) {
            String delCalendario = noHabiles.get(dia);
            salteados.add(new DiaSalteado(
                    dia,
                    delCalendario != null
                            ? delCalendario
                            : (dia.getDayOfWeek() == DayOfWeek.SATURDAY ? "Sabado" : "Domingo")));
        }
        return new VencimientoDeObligacion(contractual, liquidacion, finDeGraciaContractual, gracia, salteados);
    }
}
