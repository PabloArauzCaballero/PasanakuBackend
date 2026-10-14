package bo.aportaya.identidad.web;

import bo.aportaya.plataforma.dominio.Reloj;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * El permiso de la persona que acaba de darse de alta para completar SU expediente KYC.
 *
 * <p>Las fotos del documento se suben justo despues del alta. El acceso limitado a la sesion
 * puede comenzar mientras la cuenta sigue {@code PENDIENTE_VERIFICACION}; este token permite
 * completar el expediente sin ampliar los permisos de la sesion. Esa ruta era
 * publica y confiaba en el {@code usuarioId} de la URL: quien conociera el UUID de otra persona
 * escribia en su expediente. Ahora el alta devuelve este token, atado a ESE usuario, con un unico
 * proposito —subir las fotos del expediente— y vencimiento corto (regla 90.1.11).
 *
 * <p><b>Sin estado.</b> {@code <vence>.<HMAC-SHA256(clave, proposito|usuarioId|vence)>}: no hay
 * tabla que migrar ni nada que guardar, y no sirve para nada mas porque el proposito entra en la
 * firma (con la misma clave se firman otras cosas). Se verifica en tiempo constante. La clave es
 * la pimienta del servicio, que ya es obligatoria para arrancar.
 *
 * <p>Que un token no sea de ese usuario, que haya vencido, que este mal formado o que no exista
 * dan la misma respuesta: no hay forma de usar esta ruta para saber si un UUID es de alguien.
 */
final class TokenDeAlta {

    static final String PROPOSITO = "alta-expediente";

    private final byte[] clave;
    private final Reloj reloj;
    private final Duration vigencia;

    TokenDeAlta(String clave, Reloj reloj, Duration vigencia) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException("Hace falta la clave del servicio para firmar el token de alta.");
        }
        this.clave = clave.getBytes(StandardCharsets.UTF_8);
        this.reloj = reloj;
        this.vigencia = vigencia;
    }

    Emitido emitir(UUID usuarioId) {
        Instant vence = reloj.ahora().plus(vigencia);
        return new Emitido(vence.getEpochSecond() + "." + firma(usuarioId, vence.getEpochSecond()), vence);
    }

    /** {@code true} solo si lo firmamos para ESE usuario, para ESTE proposito y no vencio. */
    boolean valido(String token, UUID usuarioId) {
        if (token == null || usuarioId == null) {
            return false;
        }
        int punto = token.indexOf('.');
        if (punto <= 0 || punto == token.length() - 1) {
            return false;
        }
        long vence;
        try {
            vence = Long.parseLong(token.substring(0, punto));
        } catch (NumberFormatException noEsNumero) {
            return false;
        }
        // La firma se calcula SIEMPRE, aunque ya haya vencido: no se distingue por tiempo.
        boolean firmaCorrecta = MessageDigest.isEqual(
                firma(usuarioId, vence).getBytes(StandardCharsets.UTF_8),
                token.substring(punto + 1).getBytes(StandardCharsets.UTF_8));
        return firmaCorrecta && reloj.ahora().getEpochSecond() <= vence;
    }

    private String firma(UUID usuarioId, long vence) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return HexFormat.of()
                    .formatHex(
                            mac.doFinal((PROPOSITO + "|" + usuarioId + "|" + vence).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException imposible) {
            throw new IllegalStateException("HMAC-SHA256 no disponible", imposible);
        }
    }

    record Emitido(String token, Instant venceEn) {}
}
