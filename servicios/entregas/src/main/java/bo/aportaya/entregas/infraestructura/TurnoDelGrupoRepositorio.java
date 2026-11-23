package bo.aportaya.entregas.infraestructura;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code turno_del_grupo}: la copia de lectura de los turnos que publica {@code grupos} (F3.L2.M01).
 *
 * <p>Es dato replicado (regla 98.4): dice que turno cobra en cada periodo y quien era el titular del cupo cuando
 * {@code grupos} lo publico. No decide importes: el dinero sale del recaudo que afirma {@code aportes}.
 *
 * <p>El orden lo decide la base, no un {@code SELECT} previo: el {@code UPDATE} del {@code ON CONFLICT} solo pisa la
 * fila si la version que llega es MAYOR. Dos copias del mismo evento, o uno viejo que llega tarde, no escriben.
 */
@Component
public class TurnoDelGrupoRepositorio {

    public record Turno(
            UUID turnoId, UUID grupoId, UUID periodoId, UUID cupoId, UUID titularParticipanteId, int version) {}

    /** {@code true} si la copia cambio (turno nuevo o version mayor); {@code false} si la version no era mas nueva. */
    public boolean aplicar(DSLContext dsl, Turno t, OffsetDateTime recibidoEn) {
        return dsl.execute(
                        """
                        INSERT INTO entregas.turno_del_grupo
                            (turno_id, grupo_id, periodo_id, cupo_id, titular_participante_id, version, recibido_en)
                        VALUES (?, ?, ?, ?, ?, ?, CAST(? AS timestamptz))
                        ON CONFLICT (turno_id) DO UPDATE
                           SET grupo_id = excluded.grupo_id,
                               periodo_id = excluded.periodo_id,
                               cupo_id = excluded.cupo_id,
                               titular_participante_id = excluded.titular_participante_id,
                               version = excluded.version,
                               recibido_en = excluded.recibido_en
                         WHERE entregas.turno_del_grupo.version < excluded.version
                        """,
                        t.turnoId(),
                        t.grupoId(),
                        t.periodoId(),
                        t.cupoId(),
                        t.titularParticipanteId(),
                        t.version(),
                        recibidoEn)
                == 1;
    }

    public Optional<Turno> delPeriodo(DSLContext dsl, UUID periodoId) {
        return dsl.select(
                        DSL.field("turno_id", UUID.class),
                        DSL.field("grupo_id", UUID.class),
                        DSL.field("periodo_id", UUID.class),
                        DSL.field("cupo_id", UUID.class),
                        DSL.field("titular_participante_id", UUID.class),
                        DSL.field("version", Integer.class))
                .from(DSL.table(DSL.name("entregas", "turno_del_grupo")))
                .where(DSL.field("periodo_id", UUID.class).eq(periodoId))
                .fetchOptional(f -> new Turno(
                        f.get("turno_id", UUID.class),
                        f.get("grupo_id", UUID.class),
                        f.get("periodo_id", UUID.class),
                        f.get("cupo_id", UUID.class),
                        f.get("titular_participante_id", UUID.class),
                        f.get("version", Integer.class)));
    }
}
