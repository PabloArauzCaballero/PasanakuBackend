package bo.aportaya.aportes.infraestructura;

import bo.aportaya.aportes.dominio.PrimeraCuotaDelGrupo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Que cupos de un grupo pagaron la cuota 1. Lo pregunta {@code grupos} antes de comprometer el sorteo (CU-60,
 * R-GRP-20), porque no puede mirar {@code aportes.obligacion_aporte} (invariante 11).
 *
 * <p>«La cuota 1» es la obligacion {@code APORTE_PERIODICO} del periodo con el vencimiento mas temprano del grupo: este
 * servicio no lee {@code grupos.periodo}, asi que el numero del periodo no se conoce aca; el orden de vencimiento lo
 * reemplaza. Un cupo cuenta como pagado solo con su obligacion en {@code PAGADO}: ni parcial, ni en verificacion, ni
 * cubierta por garantia. Las anuladas no existen para esta pregunta.
 */
@Component
public class PrimeraCuotaRepositorio {

    private static final String CONSULTA =
            """
            WITH primera AS (
              SELECT o.periodo_id
                FROM aportes.obligacion_aporte o
               WHERE o.grupo_id = ? AND o.tipo = 'APORTE_PERIODICO' AND o.estado <> 'ANULADO'
               ORDER BY o.fecha_vencimiento, o.periodo_id
               LIMIT 1)
            SELECT o.periodo_id, o.cupo_id, o.estado
              FROM aportes.obligacion_aporte o
              JOIN primera p ON p.periodo_id = o.periodo_id
             WHERE o.grupo_id = ? AND o.tipo = 'APORTE_PERIODICO' AND o.estado <> 'ANULADO'
             ORDER BY o.cupo_id
            """;

    public PrimeraCuotaDelGrupo delGrupo(DSLContext dsl, UUID grupoId) {
        List<UUID> pagados = new ArrayList<>();
        List<UUID> pendientes = new ArrayList<>();
        Optional<UUID> periodo = Optional.empty();
        for (var fila : dsl.fetch(CONSULTA, grupoId, grupoId)) {
            periodo = Optional.of(fila.get("periodo_id", UUID.class));
            UUID cupo = fila.get("cupo_id", UUID.class);
            if ("PAGADO".equals(fila.get("estado", String.class))) {
                pagados.add(cupo);
            } else {
                pendientes.add(cupo);
            }
        }
        return new PrimeraCuotaDelGrupo(periodo, List.copyOf(pagados), List.copyOf(pendientes));
    }
}
