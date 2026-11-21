package bo.aportaya.identidad.infraestructura;

import java.security.SecureRandom;

/**
 * Semilla nueva de un TOTP: 20 bytes de azar criptografico (160 bits, el largo de HMAC-SHA1
 * que recomienda RFC 4226 §4) en Base32 sin relleno, que es lo que leen las apps de
 * autenticacion.
 */
public final class SemillaTotp {

    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    static final int BYTES = 20;

    private SemillaTotp() {}

    public static String nueva(SecureRandom azar) {
        byte[] bytes = new byte[BYTES];
        azar.nextBytes(bytes);
        return base32(bytes);
    }

    static String base32(byte[] bytes) {
        StringBuilder salida = new StringBuilder((bytes.length * 8 + 4) / 5);
        int acumulado = 0;
        int bits = 0;
        for (byte b : bytes) {
            acumulado = (acumulado << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                salida.append(ALFABETO.charAt((acumulado >> (bits - 5)) & 0x1f));
                bits -= 5;
            }
        }
        if (bits > 0) {
            salida.append(ALFABETO.charAt((acumulado << (5 - bits)) & 0x1f));
        }
        return salida.toString();
    }
}
