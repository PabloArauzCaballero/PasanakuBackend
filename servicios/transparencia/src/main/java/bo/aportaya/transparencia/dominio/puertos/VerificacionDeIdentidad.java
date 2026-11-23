package bo.aportaya.transparencia.dominio.puertos;

import java.util.UUID;

/**
 * Si la identidad de una persona esta verificada. Lo sabe {@code identidad}, no quien pide el certificado (F1.L5.M21):
 * antes era un booleano del cuerpo. Ante cualquier duda —sin respuesta, error, nivel desconocido— la respuesta es
 * {@code false}: falla cerrado.
 */
@FunctionalInterface
public interface VerificacionDeIdentidad {
    boolean verificada(UUID usuarioId);
}
