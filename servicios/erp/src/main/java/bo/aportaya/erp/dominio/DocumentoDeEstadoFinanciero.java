package bo.aportaya.erp.dominio;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * El texto sellado de un estado financiero y su huella (CU-106).
 *
 * <p>Una sola definicion para los dos lados: la generacion sella con {@link #canonico} y
 * {@link #sha256}, y la descarga reconstruye el MISMO texto desde lo guardado y lo vuelve a hashear
 * antes de entregarlo. Si cada lado serializara a su manera, el hash dejaria de probar nada.
 *
 * <p>Atomo puro: sin Spring ni jOOQ.
 */
public final class DocumentoDeEstadoFinanciero {

    private DocumentoDeEstadoFinanciero() {}

    /** Un renglon del estado: el concepto y su importe exacto. */
    public record Renglon(String concepto, BigDecimal monto) {}

    /**
     * El JSON del estado, en forma canonica: los renglones en el orden en que se calcularon y los
     * importes como cadena (un {@code double} daria otro hash en otra maquina).
     */
    public static String canonico(String tipo, List<Renglon> renglones) {
        var sb = new StringBuilder("{\"tipo\":\"").append(tipo).append("\",\"renglones\":[");
        for (int i = 0; i < renglones.size(); i++) {
            var r = renglones.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"concepto\":\"")
                    .append(r.concepto())
                    .append("\",\"monto\":\"")
                    .append(r.monto().toPlainString())
                    .append("\"}");
        }
        return sb.append("]}").toString();
    }

    public static String sha256(String contenido) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(contenido.getBytes(StandardCharsets.UTF_8));
            var texto = new StringBuilder(resumen.length * 2);
            for (byte b : resumen) {
                texto.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return texto.toString();
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("Toda JVM trae SHA-256", imposible);
        }
    }
}
