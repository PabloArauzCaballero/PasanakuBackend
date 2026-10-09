package bo.aportaya.identidad.infraestructura;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Nonce persistido y PRF secreta recuperan una respuesta perdida sin guardar el enlace. */
@Component
public class SecretoDeInvitacion {
    private final String clave;
    private final SecureRandom azar = new SecureRandom();

    public SecretoDeInvitacion(@Value("${aportaya.invitaciones.clave:}") String clave) {
        this.clave = clave;
    }

    public String nonce() {
        byte[] bytes = new byte[32];
        azar.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String firmar(String proposito, String valor) {
        if (clave.length() < 32) {
            throw new IllegalStateException("Configurar una clave secreta de invitaciones de al menos 32 caracteres.");
        }
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((proposito + ":" + valor).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC no disponible", e);
        }
    }
}
