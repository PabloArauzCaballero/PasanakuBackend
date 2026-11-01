package bo.aportaya.grupos.dominio.puertos;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Lo que `grupos` necesita saber de `aportes` para sortear: que cupos ya pagaron la cuota 1 (R-GRP-20). Se pregunta por
 * contrato; este servicio no lee el esquema de aportes (invariante 11).
 *
 * <p>El productor real es `aportes`: {@code GET /aportes/grupos/{grupoId}/primera-cuota} ({@code consultarPrimeraCuota},
 * permiso {@code BILLETERA_VER}, esquema {@code PrimeraCuota} de {@code aportes.yaml}). La implementacion de produccion
 * es {@code PrimerAportePorHttp}, con tiempo maximo, circuit breaker y fallo cerrado: {@code empty} si aportes no
 * contesto, y quien pregunta (CU-60) deniega, porque lo unico que no se puede suponer es que la plata esta.
 */
public interface PrimerAporteDelCupo {

    /** Los cupos con la cuota 1 en PAGADO; vacio (el conjunto) si el grupo no tiene obligaciones todavia. */
    Optional<Set<UUID>> cuposConPrimeraCuotaPagada(UUID grupoId);
}
