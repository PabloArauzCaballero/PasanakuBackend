package bo.aportaya.aportes.dominio;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * La firma del webhook de la pasarela (Q-02, DECIDIDA 2026-09-21): HMAC-SHA256 sobre
 * el cuerpo CRUDO, mas una ventana temporal de +-5 minutos contra el reloj propio.
 *
 * <p>Puro y sin estado a proposito: es lo que permite probarlo con valores fijos, sin
 * PostgreSQL ni Spring, y reusarlo tanto en la verificacion real como en el doble de
 * la pasarela que firma sus peticiones de prueba con el mismo algoritmo.
 */
public final class VerificadorDeFirmaWebhook {

    /** Q-02: +-5 minutos. Ni la skill ni el CU dan otro numero — no se inventa uno mas chico. */
    public static final Duration VENTANA = Duration.ofMinutes(5);

    private static final String ALGORITMO = "HmacSHA256";

    private VerificadorDeFirmaWebhook() {}

    /** El hexadecimal en minuscula de firmar {@code cuerpoCrudo} con {@code secreto}. */
    public static String firmar(String cuerpoCrudo, String secreto) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            byte[] firma = mac.doFinal(cuerpoCrudo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(firma);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // HmacSHA256 es JDK estandar y la clave nunca esta vacia (el puerto de
            // secretos no entrega ""): si esto dispara, es un defecto del entorno, no
            // una entrada de la pasarela.
            throw new IllegalStateException("No se pudo calcular HmacSHA256", e);
        }
    }

    /**
     * Comparacion en TIEMPO CONSTANTE (CWE-208, `secure-code-review` §5): comparar el
     * hexadecimal con {@code String.equals} deja adivinar la firma caracter por
     * caracter midiendo cuanto tarda en fallar cada intento.
     */
    public static boolean firmaValida(String cuerpoCrudo, String firmaRecibida, String secreto) {
        if (firmaRecibida == null || firmaRecibida.isBlank() || cuerpoCrudo == null || secreto == null) {
            return false;
        }
        String esperada = firmar(cuerpoCrudo, secreto);
        return MessageDigest.isEqual(
                esperada.getBytes(StandardCharsets.UTF_8),
                firmaRecibida.trim().toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    /** {@code true} si el instante firmado cae dentro de {@link #VENTANA} de {@code ahora}. */
    public static boolean dentroDeVentana(long timestampEpochSegundos, Instant ahora) {
        Instant firmado = Instant.ofEpochSecond(timestampEpochSegundos);
        Duration diferencia = Duration.between(firmado, ahora).abs();
        return diferencia.compareTo(VENTANA) <= 0;
    }
}
