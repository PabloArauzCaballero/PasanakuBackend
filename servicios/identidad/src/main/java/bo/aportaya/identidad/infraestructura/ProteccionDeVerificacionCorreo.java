package bo.aportaya.identidad.infraestructura;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** HMAC de codigos y destinos: ni el OTP ni el correo en claro quedan en el token. */
@Component
public class ProteccionDeVerificacionCorreo {

    private final byte[] clave;

    public ProteccionDeVerificacionCorreo(@Value("${aportaya.seguridad.pimienta}") String pimienta) {
        this.clave = pimienta.getBytes(StandardCharsets.UTF_8);
    }

    public String normalizar(String correo) {
        return correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
    }

    public String huellaDestino(String correo) {
        return hmac("destino\0" + normalizar(correo));
    }

    public String hashCodigo(String correo, String codigo) {
        return hmac("codigo\0" + normalizar(correo) + "\0" + codigo);
    }

    public boolean coincideDestino(String esperado, String correo) {
        return esperado != null
                && MessageDigest.isEqual(
                        esperado.getBytes(StandardCharsets.US_ASCII),
                        huellaDestino(correo).getBytes(StandardCharsets.US_ASCII));
    }

    public boolean coincide(String esperado, String correo, String codigo) {
        return esperado != null
                && MessageDigest.isEqual(
                        esperado.getBytes(StandardCharsets.US_ASCII),
                        hashCodigo(correo, codigo).getBytes(StandardCharsets.US_ASCII));
    }

    private String hmac(String valor) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("La JVM no ofrece HmacSHA256", e);
        }
    }
}
