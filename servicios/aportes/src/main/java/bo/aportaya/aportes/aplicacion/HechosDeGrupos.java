package bo.aportaya.aportes.aplicacion;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo que `aportes` necesita saber de `grupos` antes de registrar un pago (B15/B5): de quien es la
 * obligacion y si el periodo sigue abierto. Se pregunta por su contrato; este servicio no lee el
 * esquema de grupos (invariante 11).
 */
public interface HechosDeGrupos {

    /** {@code empty} si grupos no contesto: quien pregunta decide que significa (para dinero, rechazar). */
    Optional<Admisibilidad> admisibilidad(UUID participanteId, UUID periodoId);

    record Admisibilidad(boolean esDelUsuario, boolean periodoAbierto) {}
}
