package bo.aportaya.nucleofinanciero.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * La huella de una peticion: SHA-256 de lo que la define, en un orden fijo.
 *
 * <p>Sirve para que la misma clave con otro contenido no se confunda con un reintento.
 * Cada parte se escribe con su longitud, asi {@code ("ab","c")} y {@code ("a","bc")} no
 * producen la misma huella.
 */
public final class HuellaDePeticion {

    private HuellaDePeticion() {}

    public static String de(Object... partes) {
        var material = new StringBuilder();
        for (Object parte : partes) {
            String texto = String.valueOf(parte);
            material.append(texto.length()).append(':').append(texto).append('|');
        }
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(material.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("Toda JVM trae SHA-256", imposible);
        }
    }
}
