package bo.aportaya.nucleofinanciero.dominio;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * El texto de un QR interno: {@code PSNK1.<id>.<firma>}.
 *
 * <p>Lleva el identificador del cobro y una firma HMAC-SHA256 truncada a 16 bytes, y nada mas:
 * ni cuenta, ni importe, ni vencimiento. Eso vive en la base, de modo que alterar el texto solo
 * puede invalidarlo; nunca cambiar a quien se paga o cuanto. La firma se compara en tiempo
 * constante.
 */
public final class ContenidoDeQr {

    private static final Pattern FORMA = Pattern.compile(
            "^" + Pattern.quote(ClasificadorDeQr.PREFIJO_INTERNO) + "([A-Za-z0-9_-]{22})\\.([A-Za-z0-9_-]{22})$");
    private static final int BYTES_DE_FIRMA = 16;
    private static final int BYTES_MINIMOS_DE_CLAVE = 32;

    private final byte[] clave;

    public ContenidoDeQr(String clave) {
        byte[] bytes = clave == null ? new byte[0] : clave.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < BYTES_MINIMOS_DE_CLAVE) {
            throw new IllegalArgumentException("La clave de los QR internos necesita al menos 32 bytes.");
        }
        this.clave = bytes;
    }

    public String de(UUID qrId) {
        return ClasificadorDeQr.PREFIJO_INTERNO + codificar(qrId) + "." + firmaDe(qrId);
    }

    /** El identificador del cobro, o vacio si el texto esta mal formado o la firma no coincide. */
    public Optional<UUID> verificar(String contenido) {
        if (contenido == null) {
            return Optional.empty();
        }
        String limpio = contenido.strip();
        var forma = FORMA.matcher(limpio);
        if (!forma.matches()) {
            return Optional.empty();
        }
        try {
            UUID id = decodificar(forma.group(1));
            // Se compara contra el texto canonico: una codificacion distinta del mismo identificador
            // (bits sobrantes en el ultimo caracter) no puede validar como si fuera el mismo QR.
            boolean igual = MessageDigest.isEqual(
                    de(id).getBytes(StandardCharsets.UTF_8), limpio.getBytes(StandardCharsets.UTF_8));
            return igual ? Optional.of(id) : Optional.empty();
        } catch (IllegalArgumentException ilegible) {
            return Optional.empty();
        }
    }

    private String firmaDe(UUID id) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(firma(id));
    }

    private byte[] firma(UUID id) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            byte[] completa = mac.doFinal(("qr-interno-v1|" + id).getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(completa, BYTES_DE_FIRMA);
        } catch (GeneralSecurityException imposible) {
            throw new IllegalStateException("Toda JVM trae HmacSHA256", imposible);
        }
    }

    private static String codificar(UUID id) {
        ByteBuffer bytes = ByteBuffer.allocate(16);
        bytes.putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.array());
    }

    private static UUID decodificar(String texto) {
        ByteBuffer bytes = ByteBuffer.wrap(Base64.getUrlDecoder().decode(texto));
        return new UUID(bytes.getLong(), bytes.getLong());
    }
}
