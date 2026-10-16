package bo.aportaya.identidad.dominio.puertos;

import java.util.UUID;

/**
 * Emitir y validar el segundo factor.
 *
 * <p>Puerto porque toca azar y reloj, y porque el codigo viaja por un canal. El
 * adaptador local de desarrollo no manda nada a ningun lado: deja el codigo en la
 * bandeja interna, que es el canal por omision ([[ADR-035]]).
 */
public interface DesafioDeFactor {

    /** Emite el desafio para el factor dado y devuelve el token que lo identifica. */
    UUID emitir(UUID usuarioId, String tipoDeFactor);

    /**
     * Emite el desafio y, si el factor viaja por un canal ({@code CORREO}), lo entrega
     * en {@code destino}. Por omision es el {@link #emitir(UUID, String)} de siempre:
     * un factor que no viaja (TOTP, respaldo) no tiene a donde mandarse.
     */
    default UUID emitir(UUID usuarioId, String tipoDeFactor, String destino) {
        return emitir(usuarioId, tipoDeFactor);
    }

    /** {@code true} si el valor presentado corresponde al desafio vigente. */
    boolean validar(UUID usuarioId, String tipoDeFactor, String valorPresentado);
}
