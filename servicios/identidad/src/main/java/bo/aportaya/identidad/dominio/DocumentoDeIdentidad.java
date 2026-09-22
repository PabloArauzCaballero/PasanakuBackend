package bo.aportaya.identidad.dominio;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * El documento del titular: **cifrado para guardarlo, hasheado para buscarlo**.
 *
 * <p>El numero nunca viaja ni se guarda en claro. El hash con pimienta permite
 * responder «¿este documento ya esta registrado?» sin poder reconstruirlo, que es
 * exactamente lo que hace falta para detectar duplicados sin crear un padron de
 * documentos legible por quien acceda a la base.
 */
public record DocumentoDeIdentidad(Tipo tipo, String hashNumero, String paisEmision, String lugarExpedicion) {

    /** Las nueve extensiones del carnet boliviano: el departamento que lo emitio. */
    public static final java.util.Set<String> EXTENSIONES =
            java.util.Set.of("LP", "SC", "CB", "OR", "PT", "TJ", "CH", "BE", "PD");

    public DocumentoDeIdentidad {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(paisEmision, "pais de emision");
        if (hashNumero == null || hashNumero.length() != 64) {
            throw new ErrorDeDominio("El hash del documento tiene 64 caracteres o no es un hash");
        }
        // El numero de CI se repite entre departamentos: sin la extension, dos
        // personas distintas comparten documento y la segunda no puede abrir cuenta.
        if (tipo == Tipo.CI && lugarExpedicion == null) {
            throw new ErrorDeDominio("Un carnet boliviano necesita su lugar de expedicion");
        }
        if (lugarExpedicion != null && !EXTENSIONES.contains(lugarExpedicion)) {
            throw new ErrorDeDominio("Ese lugar de expedicion no es un departamento de Bolivia");
        }
        // Un pasaporte ya es unico por numero; una extension ahi solo confunde.
        if (tipo != Tipo.CI && lugarExpedicion != null) {
            throw new ErrorDeDominio("Solo el carnet de identidad lleva lugar de expedicion");
        }
    }

    public static DocumentoDeIdentidad de(
            Tipo tipo, String numero, String pimienta, String paisEmision, String lugarExpedicion) {
        return new DocumentoDeIdentidad(tipo, hashear(numero + pimienta), paisEmision, lugarExpedicion);
    }

    /** El hash del numero, sin construir el documento: lo usa el cotejo de titularidad. */
    public static String hashDeNumero(String numero, String pimienta) {
        return hashear(numero + pimienta);
    }

    private static String hashear(String texto) {
        try {
            byte[] digestion = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder(digestion.length * 2);
            for (byte b : digestion) {
                hexadecimal.append("%02x".formatted(b));
            }
            return hexadecimal.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 tiene que existir en cualquier JVM", e);
        }
    }

    /** Los del modelo. `CEX` del caso de uso es `CARNET_EXTRANJERIA` en el `.puml`. */
    public enum Tipo {
        CI,
        CARNET_EXTRANJERIA,
        PASAPORTE
    }
}
