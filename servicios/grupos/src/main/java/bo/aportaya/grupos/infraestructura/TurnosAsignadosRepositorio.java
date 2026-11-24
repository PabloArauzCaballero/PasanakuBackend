package bo.aportaya.grupos.infraestructura;

import static bo.aportaya.grupos.generado.Tables.CUPO;
import static bo.aportaya.grupos.generado.Tables.TURNO;

import bo.aportaya.grupos.dominio.TurnoAsignado;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/** Los turnos del grupo con el titular actual de su cupo, en el orden del sorteo. Solo lectura. */
@Component
public class TurnosAsignadosRepositorio {

    /** Primera publicación: todavía nadie cambió de titular, así que cada turno sale con versión 1. */
    public static final long VERSION_INICIAL = 1;

    public List<TurnoAsignado> delGrupo(DSLContext dsl, UUID grupoId) {
        return dsl.select(TURNO.ID, TURNO.ORDEN_ASIGNADO, TURNO.PERIODO_ID, TURNO.CUPO_ID, CUPO.PARTICIPANTE_ID)
                .from(TURNO)
                .join(CUPO)
                .on(CUPO.ID.eq(TURNO.CUPO_ID))
                .where(TURNO.GRUPO_ID.eq(grupoId))
                .orderBy(TURNO.ORDEN_ASIGNADO)
                .fetch(r -> {
                    UUID titular = r.get(CUPO.PARTICIPANTE_ID);
                    if (titular == null) {
                        // El sorteo solo baraja cupos OCUPADOS: un turno sin titular es un estado roto, no un dato.
                        throw new IllegalStateException("El turno " + r.get(TURNO.ID) + " no tiene titular");
                    }
                    return new TurnoAsignado(
                            r.get(TURNO.ID),
                            r.get(TURNO.ORDEN_ASIGNADO),
                            r.get(TURNO.PERIODO_ID),
                            r.get(TURNO.CUPO_ID),
                            titular,
                            VERSION_INICIAL);
                });
    }
}
