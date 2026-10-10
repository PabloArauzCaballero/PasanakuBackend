package bo.aportaya.inversiones.infraestructura.clientes;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** TOTP de RFC 6238 (HMAC-SHA1, 30 s, 6 digitos): el segundo factor de una cuenta de servicio que entra como operador. */
final class CodigoTotp {

    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int PASO = 30;

    private CodigoTotp() {}

    static String de(String semillaBase32, Instant ahora) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decodificar(semillaBase32), "HmacSHA1"));
            byte[] h = mac.doFinal(ByteBuffer.allocate(8)
                    .putLong(ahora.getEpochSecond() / PASO)
                    .array());
            int desde = h[h.length - 1] & 0x0f;
            int binario = ((h[desde] & 0x7f) << 24)
                    | ((h[desde + 1] & 0xff) << 16)
                    | ((h[desde + 2] & 0xff) << 8)
                    | (h[desde + 3] & 0xff);
            return "%06d".formatted(binario % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el codigo TOTP", e);
        }
    }

    private static byte[] decodificar(String base32) {
        String limpio = base32.replace("=", "").replace(" ", "").toUpperCase();
        ByteBuffer salida = ByteBuffer.allocate(limpio.length() * 5 / 8);
        int acumulado = 0;
        int bits = 0;
        for (char c : limpio.toCharArray()) {
            int v = ALFABETO.indexOf(c);
            if (v < 0) {
                throw new IllegalArgumentException("La semilla TOTP no es Base32");
            }
            acumulado = (acumulado << 5) | v;
            bits += 5;
            if (bits >= 8) {
                salida.put((byte) (acumulado >> (bits - 8)));
                bits -= 8;
                acumulado &= (1 << bits) - 1;
            }
        }
        return salida.array();
    }
}
