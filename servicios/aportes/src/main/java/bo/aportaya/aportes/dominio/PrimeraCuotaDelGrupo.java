package bo.aportaya.aportes.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Que cupos de un grupo pagaron la cuota 1 y cuales no (R-GRP-20). Es lo que `grupos` necesita para decidir si se puede
 * comprometer el sorteo; solo identificadores de cupo, nunca personas ni importes.
 *
 * @param periodoId el periodo de la cuota 1; vacio si el grupo todavia no tiene obligaciones
 * @param cuposPagados los cupos con la obligacion de la cuota 1 en PAGADO
 * @param cuposPendientes los cupos con la obligacion en cualquier otro estado no anulado
 */
public record PrimeraCuotaDelGrupo(Optional<UUID> periodoId, List<UUID> cuposPagados, List<UUID> cuposPendientes) {}
