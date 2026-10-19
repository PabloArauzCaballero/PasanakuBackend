package bo.aportaya.plataforma.dominio;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Un periodo de tablero (CU-98): {@code AAAA}, {@code AAAA-MM} o {@code AAAA-MM-DD}, con el rango de dias que cubre.
 *
 * <p>Solo se calculan periodos CERRADOS (tableros, ambiguedad A7): la unicidad de {@code indicador_kpi} no deja
 * corregir un valor, asi que el dia de hoy, el mes en curso y el año en curso no se publican hasta terminar.
 */
public record PeriodoDeTablero(String codigo, Periodo rango) {

    public PeriodoDeTablero {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(rango, "rango");
    }

    public static PeriodoDeTablero de(String codigo) {
        try {
            return switch (codigo == null ? -1 : codigo.length()) {
                case 4 -> anio(Integer.parseInt(codigo));
                case 7 -> mes(YearMonth.parse(codigo));
                case 10 -> dia(LocalDate.parse(codigo));
                default ->
                    throw new ErrorDeDominio("El periodo es AAAA, AAAA-MM o AAAA-MM-DD; llego «" + codigo + "».");
            };
        } catch (NumberFormatException | DateTimeParseException mal) {
            throw new ErrorDeDominio("El periodo es AAAA, AAAA-MM o AAAA-MM-DD; llego «" + codigo + "».");
        }
    }

    public static PeriodoDeTablero dia(LocalDate dia) {
        return new PeriodoDeTablero(dia.toString(), Periodo.de(dia, dia));
    }

    public static PeriodoDeTablero mes(YearMonth mes) {
        return new PeriodoDeTablero(mes.toString(), Periodo.de(mes.atDay(1), mes.atEndOfMonth()));
    }

    public static PeriodoDeTablero anio(int anio) {
        return new PeriodoDeTablero(
                String.valueOf(anio), Periodo.de(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31)));
    }

    /** Los periodos que CIERRAN con el dia de ayer: siempre ayer; el mes, si ayer fue fin de mes; el año, si fin de año. */
    public static List<PeriodoDeTablero> cerradosAl(LocalDate hoy) {
        LocalDate ayer = hoy.minusDays(1);
        var cerrados = new ArrayList<PeriodoDeTablero>();
        cerrados.add(dia(ayer));
        if (hoy.getDayOfMonth() == 1) {
            cerrados.add(mes(YearMonth.from(ayer)));
        }
        if (hoy.getDayOfYear() == 1) {
            cerrados.add(anio(ayer.getYear()));
        }
        return List.copyOf(cerrados);
    }

    /**
     * El relleno: los ultimos {@code dias} dias cerrados, los ultimos {@code meses} meses cerrados y los años cerrados
     * que esos meses tocan. Recalcular un periodo ya guardado no lo pisa, asi que el relleno es idempotente.
     */
    public static List<PeriodoDeTablero> relleno(LocalDate hoy, int meses, int dias) {
        if (meses < 0 || dias < 0) {
            throw new ErrorDeDominio("El relleno no va hacia el futuro.");
        }
        var periodos = new ArrayList<PeriodoDeTablero>();
        for (int d = dias; d >= 1; d--) {
            periodos.add(dia(hoy.minusDays(d)));
        }
        YearMonth actual = YearMonth.from(hoy);
        for (int m = meses; m >= 1; m--) {
            periodos.add(mes(actual.minusMonths(m)));
        }
        if (meses > 0) {
            for (int a = actual.minusMonths(meses).getYear(); a < hoy.getYear(); a++) {
                periodos.add(anio(a));
            }
        }
        return List.copyOf(periodos);
    }
}
