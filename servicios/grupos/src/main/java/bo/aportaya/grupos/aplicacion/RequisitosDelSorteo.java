package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.SnapshotDeSorteo;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * CU-60 · Lo que el grupo tiene que tener ANTES de comprometer su sorteo: la firma de la version vigente del
 * reglamento (AP-CU60-05) y la cuota 1 cobrada en cada cupo (R-GRP-20: AP-CU60-11 y AP-CU60-12).
 *
 * <p>Solo decide. Las consultas las hace {@link CU60Sortear} dentro de su transaccion, y la pregunta a
 * {@code aportes} la hace ANTES de abrirla (ninguna llamada remota adentro).
 */
final class RequisitosDelSorteo {

    private RequisitosDelSorteo() {}

    /**
     * AP-CU60-05: cada ocupante con la version VIGENTE del reglamento aceptada. Al iniciar el grupo se publica una
     * version nueva con el plantel real: la firma de la anterior es la firma de otro contrato.
     */
    static void exigirFirmaVigente(List<Integer> cuposSinFirma) {
        if (!cuposSinFirma.isEmpty()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(60, 5),
                    "Falta que alguien acepte el reglamento del grupo.",
                    Map.of("cuposSinFirma", cuposSinFirma));
        }
    }

    /**
     * R-GRP-20: el sorteo se hace con plata cobrada, no prometida. Si {@code aportes} no contesto (vacio) se
     * deniega con AP-CU60-12: lo unico que no se puede suponer es que todos pagaron. Con respuesta, cada cupo del
     * plantel tiene que estar entre los que pagaron la cuota 1, o AP-CU60-11 nombra a los que faltan.
     */
    static void exigirPrimeraCuota(Optional<Set<UUID>> conCuotaPagada, List<SnapshotDeSorteo.CupoCongelado> plantel) {
        Set<UUID> pagados = conCuotaPagada.orElseThrow(() -> new ErrorDeNegocio(
                CodigoError.de(60, 12), "No pudimos confirmar los aportes del grupo. Proba de nuevo en unos minutos."));
        List<Integer> sinCuota = plantel.stream()
                .filter(c -> !pagados.contains(c.cupoId()))
                .map(SnapshotDeSorteo.CupoCongelado::numero)
                .toList();
        if (!sinCuota.isEmpty()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(60, 11),
                    "Falta que algunos cupos paguen su primera cuota antes del sorteo.",
                    Map.of("cuposPendientes", sinCuota));
        }
    }
}
