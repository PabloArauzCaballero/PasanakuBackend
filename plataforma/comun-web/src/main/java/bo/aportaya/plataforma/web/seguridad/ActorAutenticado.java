package bo.aportaya.plataforma.web.seguridad;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Quien firma un acto: el usuario y la sesion, los dos tomados del token verificado.
 *
 * <p>Nunca se construye con un identificador que llego en el cuerpo, la query o una
 * cabecera de la peticion. La unica fabrica para el actor de la peticion en curso es
 * {@link CuatroOjos#actor()}; {@link #registrado(UUID, UUID)} existe para rehidratar un
 * actor que el propio servicio guardo cuando ese actor firmo con su token.
 *
 * @param usuarioId el {@code sub} del token.
 * @param sesionId el reclamo {@code sesion_id} del token; puede faltar solo en un actor
 *     rehidratado de una tabla que historicamente no guardaba la sesion.
 */
public record ActorAutenticado(UUID usuarioId, UUID sesionId) {

    public ActorAutenticado {
        Objects.requireNonNull(usuarioId, "usuarioId");
    }

    /**
     * Un actor que el servicio persistio al registrar el primer acto. El valor tiene que
     * haber salido de {@link CuatroOjos#actor()} en su momento, no del cliente.
     */
    public static ActorAutenticado registrado(UUID usuarioId, UUID sesionId) {
        return new ActorAutenticado(usuarioId, sesionId);
    }

    public Optional<UUID> sesion() {
        return Optional.ofNullable(sesionId);
    }
}
