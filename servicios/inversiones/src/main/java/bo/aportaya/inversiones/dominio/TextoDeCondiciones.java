package bo.aportaya.inversiones.dominio;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * El texto que la persona lee y acepta. Se arma a partir de las condiciones, de forma
 * deterministica: el mismo producto con las mismas condiciones da el mismo texto y el
 * mismo hash, que es lo que prueba QUE acepto.
 *
 * <p>Nunca habla de rentabilidad como algo seguro. Un dato sintetico lo dice en la
 * primera linea.
 */
public final class TextoDeCondiciones {

    public static final String ADVERTENCIA =
            "Invertir puede generar perdidas. El rendimiento no esta garantizado ni por Pasanaku ni por el aliado,"
                    + " y la garantia del pozo de un grupo NO alcanza a esta inversion.";

    private TextoDeCondiciones() {}

    public static String redactar(String nombre, String emisor, Condiciones c, String titularidad) {
        StringBuilder t = new StringBuilder();
        if (c.esSintetico()) {
            t.append("DATOS SINTETICOS DE DEMOSTRACION: no son una oferta ni una cotizacion real.\n");
        }
        t.append("Producto: ")
                .append(nombre)
                .append(" (")
                .append(c.tipo())
                .append(") de ")
                .append(emisor)
                .append(".\n");
        t.append("Titularidad: ").append(titularidad).append('\n');
        t.append("Monto minimo: ").append(c.montoMinimo().toPlainString()).append(" BOB.\n");
        if (c.tipo() == TipoProducto.DPF) {
            t.append("Plazo: ")
                    .append(c.plazoDias().orElseThrow())
                    .append(" dias. Tasa nominal anual: ")
                    .append(plano(c.tasaNominalAnual().orElseThrow()))
                    .append(", base de ")
                    .append(c.baseDias().orElseThrow())
                    .append(" dias, fijada al constituir.\n");
            t.append(
                    c.permiteRescateAnticipado()
                            ? "Cancelacion anticipada: permitida con penalizacion de "
                                    + plano(c.penalizacionAnticipo().orElse(BigDecimal.ZERO)) + " sobre el interes.\n"
                            : "Cancelacion anticipada: NO permitida; el deposito se cobra a su vencimiento.\n");
            t.append("Retencion sobre el interes: ")
                    .append(plano(c.tasaRetencion().orElseThrow()))
                    .append(".\n");
        } else {
            t.append("Fondo abierto: el valor de la cuota sube y baja y puede ser menor al que pagaste.\n");
            t.append("Corte de ordenes: ")
                    .append(c.horaCorte().orElseThrow())
                    .append(" (hora de La Paz); el rescate se liquida a los ")
                    .append(c.diasRescate().orElseThrow())
                    .append(" dias habiles del corte. Solicitar un rescate no significa tener el dinero disponible.\n");
            c.tasaComisionExito().ifPresent(tasa -> t.append("Comision por resultado: ")
                    .append(plano(tasa))
                    .append(" sobre lo que supere la marca maxima previa neta de comision.\n"));
        }
        t.append("Costos: ").append(c.costos()).append('\n');
        t.append(ADVERTENCIA);
        return t.toString();
    }

    public static String hash(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String plano(BigDecimal x) {
        return x.toPlainString();
    }
}
