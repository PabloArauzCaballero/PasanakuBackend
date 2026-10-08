package bo.aportaya.grupos.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-68 · El organizador acepta o rechaza una solicitud de ingreso.
 *
 * <p>Flujo principal 4 del caso de uso: quien decide es el organizador del grupo
 * ({@code revisada_por}, {@code fecha_resolucion}). Aceptada, en la MISMA transaccion se
 * reserva el cupo libre, nace el participante y se emite el evento: o pasa todo o no pasa
 * nada.
 *
 * <p>La firma del reglamento va antes de que el cupo quede firme, asi que la aceptacion
 * deja al participante en {@code ACEPTADO_PENDIENTE_FIRMA} y al cupo en {@code RESERVADO};
 * pasar a ACTIVO/OCUPADO es la firma, un caso posterior (SUPUESTO registrado en el PLAN).
 *
 * <p>Se trabaja con rol de sistema porque la politica de fila reserva a otros las filas de
 * {@code participante} y {@code solicitud_ingreso}; por eso la autorizacion del organizador
 * se comprueba aca, contra el grupo, y no se delega en la base.
 */
@Service
public class CU68AceptarIngreso {
    private final Datos datos;
    private final Outbox outbox;

    public CU68AceptarIngreso(Datos datos, Outbox outbox) {
        this.datos = datos;
        this.outbox = outbox;
    }

    /** La cola del organizador: solo PENDIENTES, la mas antigua primero. */
    @Transactional(readOnly = true)
    public List<Solicitud> pendientes(UUID grupoId, ContextoSesion ctx) {
        return datos.conContexto(sistema(ctx), dsl -> {
            exigirOrganizador(dsl, grupoId, ctx.usuarioId());
            return dsl.fetch(
                            """
                            SELECT id, usuario_id, cupos_solicitados, mensaje, puntaje_compatibilidad,
                                   estado, fecha_solicitud
                              FROM grupos.solicitud_ingreso
                             WHERE grupo_id = ? AND estado = 'PENDIENTE'
                             ORDER BY fecha_solicitud, id
                             LIMIT 200
                            """,
                            grupoId)
                    .map(r -> new Solicitud(
                            r.get("id", UUID.class),
                            r.get("usuario_id", UUID.class),
                            r.get("cupos_solicitados", Integer.class),
                            r.get("mensaje", String.class),
                            r.get("puntaje_compatibilidad", java.math.BigDecimal.class),
                            r.get("estado", String.class),
                            r.get("fecha_solicitud", OffsetDateTime.class)));
        });
    }

    @Transactional
    public Resultado decidir(
            UUID solicitudId, boolean aceptar, String motivo, java.math.BigDecimal reputacion, ContextoSesion ctx) {
        return datos.conContexto(sistema(ctx), dsl -> {
            Record s = dsl.fetchOne(
                    "SELECT id, grupo_id, usuario_id, estado FROM grupos.solicitud_ingreso WHERE id = ?", solicitudId);
            if (s == null) {
                throw new ErrorDeNegocio(CodigoError.de(68, 8), "Esa solicitud no existe.");
            }
            UUID grupoId = s.get("grupo_id", UUID.class);
            exigirOrganizador(dsl, grupoId, ctx.usuarioId());

            // Todas las decisiones del mismo grupo toman primero esta fila: dos aceptaciones no pueden
            // ocupar el ultimo cupo a la vez.
            dsl.fetchOne("SELECT id FROM grupos.grupo WHERE id = ? FOR UPDATE", grupoId);
            Record vigente =
                    dsl.fetchOne("SELECT estado FROM grupos.solicitud_ingreso WHERE id = ? FOR UPDATE", solicitudId);
            String actual = vigente.get("estado", String.class);
            if (!"PENDIENTE".equals(actual)) {
                // Repetir la MISMA decision es un reintento: se devuelve lo ya resuelto sin mover nada.
                if (("APROBADA".equals(actual) && aceptar) || ("RECHAZADA".equals(actual) && !aceptar)) {
                    Record previo = dsl.fetchOne(
                            """
                            SELECT p.id AS participante_id, c.id AS cupo_id
                              FROM grupos.participante p
                              LEFT JOIN grupos.cupo c ON c.participante_id = p.id
                             WHERE p.grupo_id = ? AND p.usuario_id = ?
                             ORDER BY p.fecha_ingreso DESC LIMIT 1
                            """,
                            grupoId,
                            s.get("usuario_id", UUID.class));
                    return new Resultado(
                            solicitudId,
                            actual,
                            aceptar && previo != null ? previo.get("participante_id", UUID.class) : null,
                            aceptar && previo != null ? previo.get("cupo_id", UUID.class) : null);
                }
                throw new ErrorDeNegocio(CodigoError.de(68, 9), "Esa solicitud ya esta resuelta.");
            }
            if (!aceptar && (motivo == null || motivo.isBlank())) {
                throw new ErrorDeNegocio(CodigoError.de(68, 10), "Para rechazar hay que decir por que.");
            }

            UUID participanteId = null;
            UUID cupoId = null;
            if (aceptar) {
                Record cupo = dsl.fetchOne(
                        """
                        SELECT id FROM grupos.cupo
                         WHERE grupo_id = ? AND estado = 'LIBRE'
                         ORDER BY numero LIMIT 1 FOR UPDATE
                        """,
                        grupoId);
                if (cupo == null) {
                    throw new ErrorDeNegocio(CodigoError.de(68, 4), "Ya no hay cupos libres.");
                }
                cupoId = cupo.get("id", UUID.class);
                participanteId = UUID.randomUUID();
                dsl.execute(
                        """
                        INSERT INTO grupos.participante
                          (id, grupo_id, usuario_id, estado, es_organizador, invitado_por_id,
                           fecha_ingreso, reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
                        VALUES (?, ?, ?, 'ACEPTADO_PENDIENTE_FIRMA', false,
                          (SELECT id FROM grupos.participante WHERE grupo_id = ? AND usuario_id = ?
                              AND es_organizador LIMIT 1), now(), ?, 0, 0)
                        """,
                        participanteId,
                        grupoId,
                        s.get("usuario_id", UUID.class),
                        grupoId,
                        ctx.usuarioId(),
                        reputacion);
                dsl.execute(
                        """
                        UPDATE grupos.cupo SET participante_id = ?, estado = 'RESERVADO', asignado_en = now()
                         WHERE id = ? AND estado = 'LIBRE'
                        """,
                        participanteId,
                        cupoId);
                dsl.execute("UPDATE grupos.grupo SET cupos_ocupados = cupos_ocupados + 1 WHERE id = ?", grupoId);
            }

            String estado = aceptar ? "APROBADA" : "RECHAZADA";
            dsl.execute(
                    """
                    UPDATE grupos.solicitud_ingreso
                       SET estado = ?, revisada_por = ?, fecha_resolucion = now()
                     WHERE id = ? AND estado = 'PENDIENTE'
                    """,
                    estado,
                    ctx.usuarioId(),
                    solicitudId);

            Map<String, Object> carga = new java.util.HashMap<>();
            carga.put("solicitudId", solicitudId.toString());
            carga.put("grupoId", grupoId.toString());
            carga.put("decision", estado);
            if (participanteId != null) {
                carga.put("participanteId", participanteId.toString());
            }
            if (motivo != null && !motivo.isBlank()) {
                carga.put("motivo", motivo);
            }
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            aceptar ? "grupos.ingreso_aceptado" : "grupos.ingreso_rechazado",
                            "solicitud_ingreso",
                            solicitudId,
                            carga,
                            UUID.fromString(ctx.traza().id())));
            return new Resultado(solicitudId, estado, participanteId, cupoId);
        });
    }

    /** Quien pidio entrar, para consultar su reputacion FUERA de la transaccion de decidir (invariante 6). */
    @Transactional(readOnly = true)
    public UUID solicitante(UUID solicitudId, ContextoSesion ctx) {
        return datos.conContexto(sistema(ctx), dsl -> {
            Record s =
                    dsl.fetchOne("SELECT grupo_id, usuario_id FROM grupos.solicitud_ingreso WHERE id = ?", solicitudId);
            if (s == null) {
                throw new ErrorDeNegocio(CodigoError.de(68, 8), "Esa solicitud no existe.");
            }
            exigirOrganizador(dsl, s.get("grupo_id", UUID.class), ctx.usuarioId());
            return s.get("usuario_id", UUID.class);
        });
    }

    /** Solo el organizador del grupo decide: el permiso global no alcanza (BOLA). */
    private static void exigirOrganizador(DSLContext dsl, UUID grupoId, UUID usuarioId) {
        boolean esOrganizador = Boolean.TRUE.equals(dsl.fetchOne(
                        """
                        SELECT EXISTS (SELECT 1 FROM grupos.participante
                                        WHERE grupo_id = ? AND usuario_id = ? AND es_organizador
                                          AND estado NOT IN ('RETIRADO','EXPULSADO','REEMPLAZADO')) AS ok
                        """,
                        grupoId,
                        usuarioId)
                .get("ok", Boolean.class));
        if (!esOrganizador) {
            throw new ErrorDeNegocio(CodigoError.de(68, 8), "Esa solicitud no existe.");
        }
    }

    private static ContextoSesion sistema(ContextoSesion ctx) {
        return ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
    }

    public record Solicitud(
            UUID id,
            UUID usuarioId,
            int cuposSolicitados,
            String mensaje,
            java.math.BigDecimal puntaje,
            String estado,
            OffsetDateTime fecha) {}

    public record Resultado(UUID solicitudId, String estado, UUID participanteId, UUID cupoId) {}
}
