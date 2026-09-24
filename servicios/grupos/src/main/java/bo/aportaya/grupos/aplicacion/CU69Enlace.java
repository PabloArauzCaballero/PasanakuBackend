package bo.aportaya.grupos.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta y acepta un enlace después de que identidad validó el secreto y el teléfono. */
@Service
public class CU69Enlace {
    private final Datos datos;
    private final Outbox outbox;

    public CU69Enlace(Datos datos, Outbox outbox) {
        this.datos = datos;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public Detalle datosDe(UUID tokenId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> detalle(dsl, tokenId));
    }

    @Transactional
    public Aceptada aceptar(UUID tokenId, String hashReglamento, String ip, BigDecimal reputacion, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            // Todas las aceptaciones del mismo grupo toman primero esta fila. Dos
            // invitaciones distintas no pueden ocupar el último cupo a la vez.
            Record grupo = dsl.fetchOne(
                    """
                    SELECT g.id FROM grupos.grupo g
                    JOIN grupos.invitacion i ON i.grupo_id = g.id
                    WHERE i.token_id = ? FOR UPDATE OF g
                    """,
                    tokenId);
            if (grupo == null) throw enlaceInvalido();

            Detalle vigente = detalle(dsl, tokenId);
            if (!vigente.hashReglamento().equals(hashReglamento)) {
                throw new ErrorDeNegocio(CodigoError.de(69, 5), "El reglamento cambió. Revisalo antes de aceptar.");
            }
            if (Boolean.TRUE.equals(dsl.fetchOne(
                            """
                    SELECT EXISTS (SELECT 1 FROM grupos.participante
                     WHERE grupo_id = ? AND usuario_id = ?
                       AND estado NOT IN ('RETIRADO','EXPULSADO','REEMPLAZADO')) AS existe
                    """,
                            vigente.grupoId(),
                            ctx.usuarioId())
                    .get("existe", Boolean.class))) {
                throw new ErrorDeNegocio(CodigoError.de(69, 3), "Ya sos participante de este grupo.");
            }

            Record cupo = dsl.fetchOne(
                    """
                    SELECT id FROM grupos.cupo
                     WHERE grupo_id = ? AND estado = 'LIBRE'
                     ORDER BY numero LIMIT 1 FOR UPDATE
                    """,
                    vigente.grupoId());
            if (cupo == null) {
                throw new ErrorDeNegocio(CodigoError.de(69, 1), "Ya no hay cupos libres.");
            }

            Record aceptada = dsl.fetchOne(
                    """
                    UPDATE grupos.invitacion SET estado = 'ACEPTADA', fecha_respuesta = now()
                     WHERE token_id = ? AND estado = 'ENVIADA' AND fecha_expiracion > now()
                    RETURNING id
                    """,
                    tokenId);
            if (aceptada == null) throw enlaceInvalido();

            UUID invitacionId = aceptada.get("id", UUID.class);
            UUID participanteId = UUID.randomUUID();
            dsl.execute(
                    """
                    INSERT INTO grupos.participante
                      (id, grupo_id, usuario_id, estado, es_organizador, invitado_por_id,
                       fecha_ingreso, reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
                    VALUES (?, ?, ?, 'ACTIVO', false,
                      (SELECT id FROM grupos.participante WHERE grupo_id = ? AND usuario_id = ?
                       AND estado = 'ACTIVO' LIMIT 1), now(), ?, 0, 0)
                    """,
                    participanteId,
                    vigente.grupoId(),
                    ctx.usuarioId(),
                    vigente.grupoId(),
                    vigente.emisorId(),
                    reputacion);
            dsl.execute(
                    """
                    UPDATE grupos.cupo SET participante_id = ?, estado = 'OCUPADO', asignado_en = now()
                     WHERE id = ? AND estado = 'LIBRE'
                    """,
                    participanteId,
                    cupo.get("id", UUID.class));
            dsl.execute("UPDATE grupos.grupo SET cupos_ocupados = cupos_ocupados + 1 WHERE id = ?", vigente.grupoId());
            dsl.execute(
                    """
                    INSERT INTO grupos.aceptacion_reglamento
                      (reglamento_id, participante_id, aceptado_en, hash_firmado, ip_origen)
                    VALUES (?, ?, now(), ?, ?::inet)
                    """,
                    vigente.reglamentoId(),
                    participanteId,
                    hashReglamento,
                    ip);
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "grupos.invitacion_aceptada",
                            "invitacion",
                            invitacionId,
                            Map.of(
                                    "invitacionId",
                                    invitacionId.toString(),
                                    "participanteId",
                                    participanteId.toString()),
                            UUID.fromString(ctx.traza().id())));
            return new Aceptada(vigente.grupoId(), participanteId);
        });
    }

    private Detalle detalle(DSLContext dsl, UUID tokenId) {
        Record fila = dsl.fetchOne(
                """
                SELECT i.id, i.grupo_id, i.telefono_invitado, i.emisor_id,
                       g.nombre, g.monto_aporte, g.moneda, g.periodicidad,
                       g.requiere_kyc_minimo, g.reputacion_minima,
                       r.id AS reglamento_id, r.contenido, r.hash_contenido
                  FROM grupos.invitacion i
                  JOIN grupos.grupo g ON g.id = i.grupo_id
                  JOIN LATERAL (
                    SELECT id, contenido, hash_contenido
                      FROM grupos.reglamento_grupo
                     WHERE grupo_id = g.id AND vigente_hasta IS NULL
                     ORDER BY version DESC LIMIT 1
                  ) r ON true
                 WHERE i.token_id = ? AND i.estado = 'ENVIADA'
                   AND i.fecha_expiracion > now()
                   AND g.estado IN ('CONFORMADO','ABIERTO_A_INSCRIPCION')
                """,
                tokenId);
        if (fila == null) throw enlaceInvalido();
        return new Detalle(
                fila.get("grupo_id", UUID.class),
                fila.get("telefono_invitado", String.class),
                fila.get("emisor_id", UUID.class),
                fila.get("nombre", String.class),
                fila.get("monto_aporte", BigDecimal.class),
                fila.get("moneda", String.class).trim(),
                fila.get("periodicidad", String.class),
                fila.get("requiere_kyc_minimo", String.class),
                fila.get("reputacion_minima", BigDecimal.class),
                fila.get("reglamento_id", UUID.class),
                fila.get("contenido", String.class),
                fila.get("hash_contenido", String.class));
    }

    private ErrorDeNegocio enlaceInvalido() {
        return new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion ya no es valida.");
    }

    public record Detalle(
            UUID grupoId,
            String telefono,
            UUID emisorId,
            String nombre,
            BigDecimal montoAporte,
            String moneda,
            String periodicidad,
            String kycMinimo,
            BigDecimal reputacionMinima,
            UUID reglamentoId,
            String reglamento,
            String hashReglamento) {}

    public record Aceptada(UUID grupoId, UUID participanteId) {}
}
