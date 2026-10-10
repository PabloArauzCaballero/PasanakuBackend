package bo.aportaya.inversiones.infraestructura.clientes;

import java.util.Optional;

/**
 * La credencial con que ESTE servicio le habla al libro (nucleo-financiero) cuando no hay una persona detras:
 * debitar la inversion, acreditar el rescate y los reintentos del reloj.
 *
 * <p>Es el token de acceso de una cuenta de servicio con el rol {@code SERVICIO_INVERSIONES}, emitido por identidad
 * (ADR-024, el unico emisor). Vive minutos: quien la implementa la renueva antes de que venza. {@link #vigente()} es
 * vacio cuando no hay credencial o no se pudo obtener, y quien llama FALLA CERRADO: nunca se mueve plata sin ella.
 * El valor no se registra jamas.
 */
public interface CredencialDeServicio {

    Optional<String> vigente();

    /** Un token ya emitido (pruebas manuales o un entorno donde otro proceso lo renueva). No se renueva solo. */
    static CredencialDeServicio fija(String token) {
        String limpio = token == null ? "" : token.strip();
        return () -> limpio.isEmpty() ? Optional.empty() : Optional.of(limpio);
    }

    static CredencialDeServicio ninguna() {
        return Optional::empty;
    }
}
