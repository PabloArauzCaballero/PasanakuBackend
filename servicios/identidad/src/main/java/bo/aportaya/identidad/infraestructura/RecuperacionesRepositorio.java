package bo.aportaya.identidad.infraestructura;

import bo.aportaya.identidad.web.generado.modelo.SolicitudDeRecuperacion;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/**
 * El expediente de recuperacion de acceso del operador (P-4, ADR-038 §4 y §5).
 *
 * <p>La autoaprobacion la rechaza la base ({@code ck_recuperacion_no_autoaprobada}, R-SEG-13): estas
 * consultas la vuelven a validar por claridad del error, no porque dependan de ello. Toda transicion
 * lleva su precondicion en el {@code UPDATE}.
 */
@Component
public class RecuperacionesRepositorio {

    /** El operador activo con ese correo institucional; no coincide con participantes. */
    public record Operador(UUID usuarioId, String correo) {}

    /** Una solicitud lista para emitir su invitacion. */
    public record Aprobada(UUID id, UUID usuarioId, String alcance) {}

    private static final String SELECCION =
            """
            SELECT s.id, s.usuario_id, u.nombres, u.apellidos, s.alcance, s.estado, s.solicitada_en, s.motivo,
                   s.decidida_por, s.decidida_en, s.motivo_decision, s.enfriamiento_hasta, s.emitida_en
              FROM identidad.solicitud_recuperacion_acceso s
              JOIN identidad.usuario u ON u.id = s.usuario_id
            """;

    public Optional<Operador> operadorActivoPorCorreo(DSLContext dsl, String correo) {
        var filas = dsl.fetch(
                """
                        SELECT u.id, u.correo FROM identidad.usuario u
                        JOIN identidad.usuario_interno i ON i.usuario_id = u.id
                        WHERE lower(u.correo) = lower(?) AND u.estado = 'ACTIVO' AND u.eliminado_en IS NULL
                        LIMIT 2
                        """,
                correo.strip());
        if (filas.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(new Operador(filas.get(0).get("id", UUID.class), filas.get(0).get("correo", String.class)));
    }

    /** {@code true} si creo el pedido; {@code false} si ya habia uno vivo del mismo alcance. */
    public boolean abrir(
            DSLContext dsl,
            UUID solicitudId,
            UUID usuarioId,
            String alcance,
            UUID solicitadaPor,
            String motivo,
            OffsetDateTime ahora) {
        return dsl.execute(
                        """
                                INSERT INTO identidad.solicitud_recuperacion_acceso (id, usuario_id, alcance, estado,
                                solicitada_por, solicitada_en, motivo, clave_idempotencia)
                                VALUES (?, ?, ?, 'PENDIENTE', ?, ?::timestamptz, ?, ?) ON CONFLICT DO NOTHING
                                """,
                        solicitudId,
                        usuarioId,
                        alcance,
                        solicitadaPor,
                        ahora,
                        motivo,
                        "REC:" + solicitudId)
                == 1;
    }

    /** La fila con su candado, para decidirla. */
    public Optional<Record> paraDecidir(DSLContext dsl, UUID id) {
        return Optional.ofNullable(dsl.fetchOne(
                """
                        SELECT id, usuario_id, estado, decidida_por FROM identidad.solicitud_recuperacion_acceso
                        WHERE id = ? FOR UPDATE
                        """,
                id));
    }

    public boolean aprobar(
            DSLContext dsl, UUID id, UUID aprobador, String motivo, OffsetDateTime ahora, OffsetDateTime enfriaHasta) {
        return dsl.execute(
                        """
                                UPDATE identidad.solicitud_recuperacion_acceso SET estado = 'APROBADA', decidida_por = ?,
                                decidida_en = ?::timestamptz, motivo_decision = ?,
                                enfriamiento_hasta = ?::timestamptz
                                WHERE id = ? AND estado = 'PENDIENTE'
                                """,
                        aprobador,
                        ahora,
                        motivo,
                        enfriaHasta,
                        id)
                == 1;
    }

    public boolean rechazar(DSLContext dsl, UUID id, UUID quien, String motivo, OffsetDateTime ahora) {
        return dsl.execute(
                        """
                                UPDATE identidad.solicitud_recuperacion_acceso SET estado = 'RECHAZADA', decidida_por = ?,
                                decidida_en = ?::timestamptz, motivo_decision = ?
                                WHERE id = ? AND estado = 'PENDIENTE'
                                """,
                        quien,
                        ahora,
                        motivo,
                        id)
                == 1;
    }

    public Optional<SolicitudDeRecuperacion> porId(DSLContext dsl, UUID id) {
        return Optional.ofNullable(
                dsl.fetchOne(SELECCION + " WHERE s.id = ?", id))
                .map(RecuperacionesRepositorio::aSolicitud);
    }

    /** Pagina por cursor ({@code solicitada_en}, {@code id}), de la mas nueva a la mas vieja, con desempate estable. */
    public List<SolicitudDeRecuperacion> listar(
            DSLContext dsl, String estado, int limite, OffsetDateTime antesDe, UUID antesDeId) {
        StringBuilder sql = new StringBuilder(SELECCION).append(" WHERE 1 = 1");
        java.util.ArrayList<Object> valores = new java.util.ArrayList<>();
        if (estado != null) {
            sql.append(" AND s.estado = ?");
            valores.add(estado);
        }
        if (antesDe != null) {
            sql.append(" AND (s.solicitada_en, s.id) < (?::timestamptz, ?::uuid)");
            valores.add(antesDe);
            valores.add(antesDeId != null ? antesDeId : new UUID(-1L, -1L));
        }
        sql.append(" ORDER BY s.solicitada_en DESC, s.id DESC LIMIT ?");
        valores.add(limite);
        return dsl.fetch(sql.toString(), valores.toArray()).stream()
                .map(RecuperacionesRepositorio::aSolicitud)
                .toList();
    }

    /** Las aprobadas cuyo enfriamiento ya paso, con candado y sin esperar a quien las tenga tomadas. */
    public List<Aprobada> listasParaEmitir(DSLContext dsl, OffsetDateTime ahora, int tanda) {
        return dsl.fetch(
                        """
                                SELECT id, usuario_id, alcance FROM identidad.solicitud_recuperacion_acceso
                                WHERE estado = 'APROBADA' AND enfriamiento_hasta <= ?::timestamptz
                                ORDER BY enfriamiento_hasta, id LIMIT ? FOR UPDATE SKIP LOCKED
                                """,
                        ahora,
                        tanda)
                .stream()
                .map(fila -> new Aprobada(
                        fila.get("id", UUID.class),
                        fila.get("usuario_id", UUID.class),
                        fila.get("alcance", String.class)))
                .toList();
    }

    public boolean marcarEmitida(DSLContext dsl, UUID id, UUID tokenId, OffsetDateTime ahora) {
        return dsl.execute(
                        """
                                UPDATE identidad.solicitud_recuperacion_acceso SET estado = 'EMITIDA', token_id = ?,
                                emitida_en = ?::timestamptz WHERE id = ? AND estado = 'APROBADA'
                                """,
                        tokenId,
                        ahora,
                        id)
                == 1;
    }

    /** La invitacion se uso: el expediente se cierra. */
    public void marcarCompletadaPorToken(DSLContext dsl, UUID tokenId, OffsetDateTime ahora) {
        dsl.execute(
                """
                        UPDATE identidad.solicitud_recuperacion_acceso SET estado = 'COMPLETADA',
                        completada_en = ?::timestamptz WHERE token_id = ? AND estado = 'EMITIDA'
                        """,
                ahora,
                tokenId);
    }

    /**
     * Cierra lo que quedo sin salida: un pedido que nadie decidio en {@code vigencia}, y una
     * invitacion emitida cuyo token ya vencio sin usarse. Sin esto la unicidad de «una viva por
     * operador» lo dejaria sin poder pedir de nuevo.
     */
    public int vencerLoAbandonado(DSLContext dsl, OffsetDateTime ahora, OffsetDateTime pedidosAnterioresA) {
        int pedidos = dsl.execute(
                """
                        UPDATE identidad.solicitud_recuperacion_acceso SET estado = 'VENCIDA'
                        WHERE estado = 'PENDIENTE' AND solicitada_en < ?::timestamptz
                        """,
                pedidosAnterioresA);
        int invitaciones = dsl.execute(
                """
                        UPDATE identidad.solicitud_recuperacion_acceso s SET estado = 'VENCIDA'
                        FROM identidad.token_verificacion t
                        WHERE s.token_id = t.id AND s.estado = 'EMITIDA' AND t.estado <> 'CONSUMIDO'
                        AND t.expira_en < ?::timestamptz
                        """,
                ahora);
        return pedidos + invitaciones;
    }

    public void vencer(DSLContext dsl, UUID id) {
        dsl.execute(
                """
                        UPDATE identidad.solicitud_recuperacion_acceso SET estado = 'VENCIDA'
                        WHERE id = ? AND estado = 'APROBADA'
                        """,
                id);
    }

    private static SolicitudDeRecuperacion aSolicitud(Record fila) {
        SolicitudDeRecuperacion s = new SolicitudDeRecuperacion();
        s.setId(fila.get("id", UUID.class));
        s.setUsuarioId(fila.get("usuario_id", UUID.class));
        s.setNombres(fila.get("nombres", String.class));
        s.setApellidos(fila.get("apellidos", String.class));
        s.setAlcance(fila.get("alcance", String.class));
        s.setEstado(fila.get("estado", String.class));
        s.setSolicitadaEn(fila.get("solicitada_en", OffsetDateTime.class));
        s.setMotivo(fila.get("motivo", String.class));
        s.setDecididaPor(fila.get("decidida_por", UUID.class));
        s.setDecididaEn(fila.get("decidida_en", OffsetDateTime.class));
        s.setMotivoDecision(fila.get("motivo_decision", String.class));
        s.setEnfriamientoHasta(fila.get("enfriamiento_hasta", OffsetDateTime.class));
        s.setEmitidaEn(fila.get("emitida_en", OffsetDateTime.class));
        return s;
    }
}
