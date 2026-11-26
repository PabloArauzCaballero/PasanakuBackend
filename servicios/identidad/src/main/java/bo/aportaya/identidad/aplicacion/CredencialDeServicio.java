package bo.aportaya.identidad.aplicacion;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** La credencial de una cuenta de servicio: azar del generador seguro; el valor solo existe en memoria hasta entregarse. */
final class CredencialDeServicio {

    private static final int BYTES = 32;
    private static final SecureRandom AZAR = new SecureRandom();

    private CredencialDeServicio() {}

    /** 32 bytes del generador seguro, en base64 sin relleno: 43 caracteres, sin nada que escapar en una URL. */
    static char[] nueva() {
        byte[] bytes = new byte[BYTES];
        AZAR.nextBytes(bytes);
        char[] valor =
                Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).toCharArray();
        Arrays.fill(bytes, (byte) 0);
        return valor;
    }
}
