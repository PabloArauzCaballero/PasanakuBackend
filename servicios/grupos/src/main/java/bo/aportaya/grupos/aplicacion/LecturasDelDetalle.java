package bo.aportaya.grupos.aplicacion;

import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Las lecturas del detalle del grupo que no son el resumen: los cupos con quien los ocupa, si alguien tiene cupo y las
 * aceptaciones del reglamento vigente. Salieron de {@link ConsultarDetalleDelGrupo} (limite de 300 lineas) sin cambiar
 * lo que hacen; corren dentro de su transaccion, con su contexto.
 */
final class LecturasDelDetalle {

    private LecturasDelDetalle() {}

    /** Una fila por cupo del ciclo, con el nombre visible de quien lo ocupa, si firmo y su turno. */
    static List<ConsultarDetalleDelGrupo.Integrante> integrantes(DSLContext dsl, UUID grupoId, UUID usuarioId) {
        return dsl.fetch(
                        """
                        SELECT c.numero, c.id AS cupo_id, c.estado AS estado_cupo,
                               p.usuario_id,
                               coalesce(p.alias, (SELECT s.nombre_visible FROM grupos.solicitud_ingreso s
                                                   WHERE s.grupo_id = c.grupo_id AND s.usuario_id = p.usuario_id
                                                     AND s.nombre_visible IS NOT NULL
                                                   ORDER BY s.fecha_solicitud DESC LIMIT 1)) AS alias,
                               p.estado,
                               coalesce(p.es_organizador, false) AS es_organizador,
                               coalesce(p.usuario_id = ?, false) AS soy_yo,
                               (SELECT t.orden_asignado FROM grupos.turno t
                                 WHERE t.cupo_id = c.id AND t.estado <> 'ANULADO'
                                 ORDER BY t.orden_asignado LIMIT 1) AS turno
                          FROM grupos.cupo c
                          JOIN grupos.grupo g ON g.id = c.grupo_id
                          LEFT JOIN grupos.participante p ON p.id = c.participante_id
                         -- Iniciado el grupo, los cupos libres quedaron numerados por encima de cupos_totales: no
                         -- son del ciclo y no se muestran (CU69IniciarGrupo).
                         WHERE c.grupo_id = ? AND c.numero <= g.cupos_totales
                         ORDER BY c.numero
                        """,
                        usuarioId,
                        grupoId)
                .map(f -> new ConsultarDetalleDelGrupo.Integrante(
                        f.get("numero", Integer.class),
                        f.get("cupo_id", UUID.class),
                        f.get("usuario_id", UUID.class),
                        f.get("alias", String.class),
                        ConsultarDetalleDelGrupo.estadoDelCupo(
                                f.get("estado_cupo", String.class), f.get("estado", String.class)),
                        Boolean.TRUE.equals(f.get("es_organizador", Boolean.class)),
                        Boolean.TRUE.equals(f.get("soy_yo", Boolean.class)),
                        f.get("turno", Integer.class)));
    }

    /** Si el participante tiene un cupo RESERVADO u OCUPADO. */
    static boolean tieneCupo(DSLContext dsl, UUID participanteId) {
        return Boolean.TRUE.equals(dsl.fetchOne(
                        """
                        SELECT EXISTS (SELECT 1 FROM grupos.cupo
                                        WHERE participante_id = ? AND estado IN ('RESERVADO', 'OCUPADO')) AS si
                        """,
                        participanteId)
                .get("si", Boolean.class));
    }

    /** Si ese participante acepto esa version del reglamento (su firma, o la aceptacion de una version nueva). */
    static boolean acepto(DSLContext dsl, UUID reglamentoId, UUID participanteId) {
        return Boolean.TRUE.equals(dsl.fetchOne(
                        """
                        SELECT EXISTS (SELECT 1 FROM grupos.aceptacion_reglamento
                                        WHERE reglamento_id = ? AND participante_id = ?) AS si
                        """,
                        reglamentoId,
                        participanteId)
                .get("si", Boolean.class));
    }

    /** Cuantos de los que ocupan un cupo del ciclo todavia no aceptaron la version vigente. */
    static int sinAceptarLaVigente(DSLContext dsl, UUID grupoId, UUID reglamentoId) {
        return dsl.fetchOne(
                        """
                        SELECT count(*)::int FROM grupos.cupo c
                          JOIN grupos.grupo g ON g.id = c.grupo_id
                         WHERE c.grupo_id = ? AND c.estado = 'OCUPADO' AND c.numero <= g.cupos_totales
                           AND NOT EXISTS (SELECT 1 FROM grupos.aceptacion_reglamento a
                                            WHERE a.reglamento_id = ? AND a.participante_id = c.participante_id)
                        """,
                        grupoId,
                        reglamentoId)
                .get(0, Integer.class);
    }
}
