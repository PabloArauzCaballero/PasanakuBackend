package bo.aportaya.plataforma.web.clientes;

import java.util.Optional;

/**
 * La credencial con que un servicio le habla a otro cuando no hay una persona detras: {@code inversiones} al libro para
 * debitar la inversion o acreditar el rescate, {@code entregas} al libro para pagarle al vendedor de un turno.
 *
 * <p>Es el token de acceso de una cuenta de servicio con el rol {@code SERVICIO_INVERSIONES} o {@code SERVICIO_ENTREGAS},
 * emitido por identidad (ADR-024, el unico emisor). Vive minutos: quien la implementa la renueva antes de que venza.
 * {@link #vigente()} es vacio cuando no hay credencial o no se pudo obtener, y quien llama FALLA CERRADO: nunca se mueve
 * plata sin ella. El valor no se registra jamas.
 *
 * <p>Vive en la plataforma (carril G) porque la necesitan dos servicios y ninguno puede importar al otro (invariante 11).
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
