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
    static final int LARGO_MINIMO = 32;

    private final String clave;
    private final SecureRandom azar = new SecureRandom();

    /**
     * F1.L4.M03: la clave se valida <b>al arrancar</b>. Antes se validaba en el primer uso: el
     * servicio quedaba sano (readiness verde) y toda invitacion fallaba con 500.
     */
    public SecretoDeInvitacion(@Value("${aportaya.invitaciones.clave:}") String clave) {
        if (clave == null || clave.length() < LARGO_MINIMO) {
            throw new IllegalStateException(
                    "aportaya.invitaciones.clave (INVITACIONES_CLAVE) es obligatoria y debe tener" + " al menos "
                            + LARGO_MINIMO + " caracteres. Fail closed (F1.L4.M03, regla 90.3.4).");
        }
        this.clave = clave;
    }

    public String nonce() {
        byte[] bytes = new byte[32];
        azar.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String firmar(String proposito, String valor) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((proposito + ":" + valor).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC no disponible", e);
        }
    }
}
