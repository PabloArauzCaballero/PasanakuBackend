package bo.aportaya.grupos.infraestructura;

import static bo.aportaya.grupos.generado.Tables.GRUPO;
import static bo.aportaya.grupos.generado.Tables.PARTICIPANTE;
import static bo.aportaya.grupos.generado.Tables.TRASPASO_CUPO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/** Lo que el traspaso por subasta lee y escribe en {@code grupos}, aparte del traspaso en si ({@link TraspasoRepositorio}). */
@Component
public class SubastaDeCupoRepositorio {

    /** El aporte del grupo, en su moneda: es lo que el ganador va a tener que seguir pagando. */
    public record Aporte(BigDecimal monto, String moneda) {}

    public Optional<Aporte> aporteDelGrupo(DSLContext dsl, UUID grupoId) {
        return Optional.ofNullable(dsl.select(GRUPO.MONTO_APORTE, GRUPO.MONEDA)
                        .from(GRUPO)
                        .where(GRUPO.ID.eq(grupoId))
                        .fetchOne())
                .map(f -> new Aporte(f.get(GRUPO.MONTO_APORTE), f.get(GRUPO.MONEDA)));
    }

    /** El participante ACTIVO de esa persona en ese grupo, si ya esta dentro. */
    public Optional<UUID> participanteActivo(DSLContext dsl, UUID grupoId, UUID usuarioId) {
        return dsl.select(PARTICIPANTE.ID)
                .from(PARTICIPANTE)
                .where(PARTICIPANTE.GRUPO_ID.eq(grupoId))
                .and(PARTICIPANTE.USUARIO_ID.eq(usuarioId))
                .and(PARTICIPANTE.ESTADO.eq("ACTIVO"))
                .orderBy(PARTICIPANTE.FECHA_INGRESO.desc(), PARTICIPANTE.ID)
                .limit(1)
                .fetchOptional(PARTICIPANTE.ID);
    }

    /**
     * El ganador entra ACTIVO: gano la subasta aceptando el reglamento del grupo al pujar, y su plata ya esta
     * retenida. Entra con historial cero en este grupo, como cualquier otro ingreso.
     */
    public UUID altaDelGanador(DSLContext dsl, UUID grupoId, UUID usuarioId, OffsetDateTime ahora) {
        return dsl.insertInto(PARTICIPANTE)
                .set(PARTICIPANTE.GRUPO_ID, grupoId)
                .set(PARTICIPANTE.USUARIO_ID, usuarioId)
                .set(PARTICIPANTE.ESTADO, "ACTIVO")
                .set(PARTICIPANTE.ES_ORGANIZADOR, false)
                .set(PARTICIPANTE.FECHA_INGRESO, ahora)
                .set(PARTICIPANTE.REPUTACION_AL_INGRESAR, BigDecimal.ZERO)
                .set(PARTICIPANTE.APORTES_REALIZADOS, (short) 0)
                .set(PARTICIPANTE.APORTES_EN_MORA, (short) 0)
                .returning(PARTICIPANTE.ID)
                .fetchOne(PARTICIPANTE.ID);
    }

    /** El ultimo traspaso de ese cupo hacia ese participante: lo que devuelve un reintento. */
    public Optional<UUID> traspasoHacia(DSLContext dsl, UUID cupoId, UUID destino) {
        return dsl.select(TRASPASO_CUPO.ID)
                .from(TRASPASO_CUPO)
                .where(TRASPASO_CUPO.CUPO_ID.eq(cupoId))
                .and(TRASPASO_CUPO.PARTICIPANTE_DESTINO_ID.eq(destino))
                .orderBy(TRASPASO_CUPO.FECHA.desc(), TRASPASO_CUPO.ID)
                .limit(1)
                .fetchOptional(TRASPASO_CUPO.ID);
    }
}
