package bo.aportaya.nucleofinanciero.dominio;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CU-24 · el átomo del cuadre: {@code SUM(debe) = SUM(haber)} en UNA moneda.
 *
 * <p>La base lo hace cumplir con {@code tg_asiento_cuadrado} (R-AUD-05) y
 * {@code tg_asiento_una_moneda} (R-AUD-12), diferidos al {@code COMMIT}. Esta
 * verificación no los reemplaza — los adelanta, para devolver {@code AP-CU24-01} o
 * {@code AP-CU24-04} en vez de esperar a que la transacción entera falle.
 *
 * <p>Un asiento no mezcla monedas: sumar BOB con USD sin un tipo de cambio registrado
 * es inventar plata (91.1.2). Una operación en dos monedas son dos asientos.
 */
public final class CuadrarPartidas {

    private CuadrarPartidas() {}

    /** Verifica el cuadre y devuelve el total de cada lado, ya normalizado a dos decimales. */
    public static Totales verificar(List<Partida> partidas) {
        if (partidas == null || partidas.size() < 2) {
            throw new ErrorDeNegocio(
                    CodigoError.de(24, 1), "Un asiento necesita al menos dos partidas para poder cuadrar.");
        }

        Set<Moneda> monedas = partidas.stream().map(Partida::moneda).collect(Collectors.toSet());
        if (monedas.size() > 1) {
            throw new ErrorDeNegocio(
                    CodigoError.de(24, 4),
                    "Un asiento no mezcla monedas: llegaron " + monedas + ".",
                    Map.of("monedas", monedas.toString()));
        }
        Moneda moneda = monedas.iterator().next();

        Dinero totalDebe = Dinero.cero(moneda);
        Dinero totalHaber = Dinero.cero(moneda);
        for (Partida p : partidas) {
            totalDebe = totalDebe.mas(Dinero.de(p.debe(), moneda));
            totalHaber = totalHaber.mas(Dinero.de(p.haber(), moneda));
        }

        if (!totalDebe.equals(totalHaber)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(24, 1),
                    "El asiento no cuadra: el debe y el haber no coinciden.",
                    Map.of("totalDebe", totalDebe.toString(), "totalHaber", totalHaber.toString()));
        }

        return new Totales(totalDebe, totalHaber);
    }

    public record Totales(Dinero debe, Dinero haber) {}
}
