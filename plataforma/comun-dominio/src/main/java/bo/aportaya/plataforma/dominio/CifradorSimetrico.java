package bo.aportaya.plataforma.dominio;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifrado simétrico AES-256-GCM para datos financieros y sensibles en reposo.
 *
 * <p>Cumple con R-SEG-01 y las directivas de seguridad bancaria ASFI / NIST SP 800-38D:
 * <ul>
 *   <li>Algoritmo: {@code AES/GCM/NoPadding} (autenticado, con verificación de integridad de etiqueta).</li>
 *   <li>Longitud de clave: 256 bits (derivada vía SHA-256 sobre el secreto/pimienta).</li>
 *   <li>Vector de inicialización (IV / Nonce): 96 bits (12 bytes) aleatorio criptográfico por operación.</li>
 *   <li>Etiqueta de autenticación (Tag): 128 bits (16 bytes).</li>
 *   <li>Formato serializado: {@code enc:v1:<base64url(iv + ciphertext + tag)>}.</li>
 * </ul>
 */
public final class CifradorSimetrico {

    public static final String PREFIJO = "enc:v1:";
    private static final String ALGORITMO_TRANSFORMACION = "AES/GCM/NoPadding";
    private static final String ALGORITMO_CLAVE = "AES";
    private static final int LONGITUD_IV_BYTES = 12; // 96 bits
    private static final int LONGITUD_TAG_BITS = 128; // 16 bytes
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CifradorSimetrico() {
        // Clase de utilidades criptográficas
    }

    /**
     * Cifra un texto en claro usando AES-256-GCM y devuelve el texto con prefijo y codificación Base64 URL-safe.
     */
    public static String cifrar(String textoEnClaro, String secreto) {
        if (textoEnClaro == null) {
            return null;
        }
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalArgumentException("El secreto de cifrado no puede ser nulo ni vacío");
        }

        try {
            byte[] iv = new byte[LONGITUD_IV_BYTES];
            ALEATORIO.nextBytes(iv);

            SecretKey clave = derivarClave(secreto);
            Cipher cipher = Cipher.getInstance(ALGORITMO_TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(LONGITUD_TAG_BITS, iv));

            byte[] textoBytes = textoEnClaro.getBytes(StandardCharsets.UTF_8);
            byte[] textoCifradoConTag = cipher.doFinal(textoBytes);

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + textoCifradoConTag.length);
            buffer.put(iv);
            buffer.put(textoCifradoConTag);

            String cargaBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
            return PREFIJO + cargaBase64;
        } catch (GeneralSecurityException e) {
            throw new SecurityException("Fallo criptográfico al cifrar dato sensible", e);
        }
    }

    /**
     * Descifra un texto previamente cifrado con {@link #cifrar(String, String)}.
     */
    public static String descifrar(String textoCifrado, String secreto) {
        if (textoCifrado == null) {
            return null;
        }
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalArgumentException("El secreto de cifrado no puede ser nulo ni vacío");
        }
        if (!textoCifrado.startsWith(PREFIJO)) {
            throw new IllegalArgumentException("El formato del texto cifrado no es reconocido: debe iniciar con " + PREFIJO);
        }

        String cargaBase64 = textoCifrado.substring(PREFIJO.length());
        byte[] cargaBytes;
        try {
            cargaBytes = Base64.getUrlDecoder().decode(cargaBase64);
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Carga base64 ilegible o corrupta", e);
        }

        if (cargaBytes.length < LONGITUD_IV_BYTES + (LONGITUD_TAG_BITS / 8)) {
            throw new SecurityException("Carga cifrada demasiado corta para contener IV y autenticación GCM");
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(cargaBytes);
            byte[] iv = new byte[LONGITUD_IV_BYTES];
            buffer.get(iv);

            byte[] ciphertextConTag = new byte[buffer.remaining()];
            buffer.get(ciphertextConTag);

            SecretKey clave = derivarClave(secreto);
            Cipher cipher = Cipher.getInstance(ALGORITMO_TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(LONGITUD_TAG_BITS, iv));

            byte[] descifrado = cipher.doFinal(ciphertextConTag);
            return new String(descifrado, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new SecurityException("Error de autenticación o integridad al descifrar: clave incorrecta o dato manipulado", e);
        }
    }

    /**
     * Deriva una clave AES de 256 bits (32 bytes) aplicando SHA-256 sobre el secreto/pimienta.
     */
    private static SecretKey derivarClave(String secreto) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(secreto.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, ALGORITMO_CLAVE);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 debe estar disponible en toda JVM estándar", e);
        }
    }
}
