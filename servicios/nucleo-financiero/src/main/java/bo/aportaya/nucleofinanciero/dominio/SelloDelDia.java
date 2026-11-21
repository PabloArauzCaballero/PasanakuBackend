package bo.aportaya.nucleofinanciero.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * El sello del saldo del dia de una cuenta, encadenado con el del dia anterior (F1.L1.M13, ANF-DINERO-006).
 *
 * <p>El sello viejo era {@code SHA-256(cuentaId | fecha)}: no cubria lo que decia proteger, y cambiar el
 * saldo de una fila no cambiaba su sello. Ahora el material es
 * {@code cuentaId | fecha | moneda | disponible | retenido | sello anterior}, con los importes en formato
 * canonico (escala 2, sin notacion cientifica). Alterar el saldo, la moneda o el eslabon previo cambia el
 * sello, y como cada dia firma al anterior, la alteracion rompe todos los dias que siguen.
 */
public final class SelloDelDia {

    /** Lo que firma el primer dia de una cuenta, que no tiene anterior. */
    public static final String SIN_ANTERIOR = "-";

    private SelloDelDia() {}

    public static String material(
            UUID cuentaId,
            LocalDate fecha,
            String moneda,
            BigDecimal disponible,
            BigDecimal retenido,
            String selloAnterior) {
        return cuentaId + "|" + fecha + "|" + moneda + "|" + canonico(disponible) + "|" + canonico(retenido) + "|"
                + (selloAnterior == null ? SIN_ANTERIOR : selloAnterior);
    }

    public static String sellar(
            UUID cuentaId,
            LocalDate fecha,
            String moneda,
            BigDecimal disponible,
            BigDecimal retenido,
            String selloAnterior) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(material(cuentaId, fecha, moneda, disponible, retenido, selloAnterior)
                            .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("Toda JVM trae SHA-256", imposible);
        }
    }

    /**
     * Recalcula la cadena de una cuenta, del dia mas viejo al mas nuevo, y devuelve la primera fila cuyo
     * sello no es el que tendria que ser. Una fila cuyo {@code hashAnterior} no es el sello de la previa
     * tambien es una ruptura: alguien reemplazo un eslabon.
     */
    public static Optional<Eslabon> primeraRuptura(List<Eslabon> cadenaPorFecha) {
        String anterior = null;
        for (Eslabon e : cadenaPorFecha) {
            String esperado = sellar(e.cuentaId(), e.fecha(), e.moneda(), e.disponible(), e.retenido(), anterior);
            boolean enlazada = anterior == null ? e.selloAnterior() == null : anterior.equals(e.selloAnterior());
            if (!enlazada || !esperado.equals(e.sello())) {
                return Optional.of(e);
            }
            anterior = e.sello();
        }
        return Optional.empty();
    }

    private static String canonico(BigDecimal importe) {
        return importe.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    /** Una fila de {@code saldo_diario_billetera} con la moneda de su cuenta. */
    public record Eslabon(
            UUID cuentaId,
            LocalDate fecha,
            String moneda,
            BigDecimal disponible,
            BigDecimal retenido,
            String sello,
            String selloAnterior) {}
}
