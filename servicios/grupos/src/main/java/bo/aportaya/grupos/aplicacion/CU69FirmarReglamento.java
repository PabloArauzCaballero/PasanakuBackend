package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.infraestructura.CalendarioDelGrupoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-69 · Firmar el reglamento despues de ser aceptado (2026-10-20). Quien entro por el enlace del grupo —y el
 * organizador, que ocupa el cupo 1— nace {@code ACEPTADO_PENDIENTE_FIRMA} con el cupo RESERVADO; hasta ahora no
 * habia como salir de ahi, y sin firmas ningun grupo llegaba al sorteo (CU-60 exige todas, AP-CU60-05).
 *
 * <p>Se firma el texto EXACTO: el hash que manda la app tiene que ser el del reglamento vigente. Firmar deja al
 * participante ACTIVO y su cupo OCUPADO. La ultima firma, con todos los cupos ocupados, conforma el grupo y arma
 * su calendario. La fila del grupo se toma primero: dos firmas simultaneas no pueden creerse las dos ultimas.
 */
@Service
public class CU69FirmarReglamento {

    private static final String MOTIVO_CONFORMADO = "Todos los cupos ocupados y con el reglamento firmado.";

    private final Datos datos;
    private final CalendarioDelGrupoRepositorio calendario;
    private final Reloj reloj;

    public CU69FirmarReglamento(Datos datos, CalendarioDelGrupoRepositorio calendario, Reloj reloj) {
        this.datos = datos;
        this.calendario = calendario;
        this.reloj = reloj;
    }

    @Transactional
    public Firmado firmar(UUID grupoId, String hashReglamento, String ip, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        // `participante` y `cupo` son del proceso interno (politica de fila): la sesion no se escribe a si misma.
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(interno, dsl -> {
            Record grupo = dsl.fetchOne("SELECT estado FROM grupos.grupo WHERE id = ? FOR UPDATE", grupoId);
            if (grupo == null) throw sinFirmaPendiente();
            Record yo = dsl.fetchOne(
                    """
                    SELECT id, estado FROM grupos.participante
                     WHERE grupo_id = ? AND usuario_id = ? AND estado IN ('ACEPTADO_PENDIENTE_FIRMA', 'ACTIVO')
                     ORDER BY fecha_ingreso DESC LIMIT 1
                    """,
                    grupoId,
                    ctx.usuarioId());
            if (yo == null) throw sinFirmaPendiente();
            UUID participante = yo.get("id", UUID.class);
            // Firmar dos veces es un reintento: no se repite la firma ni se mueve nada.
            if ("ACTIVO".equals(yo.get("estado", String.class))) {
                return new Firmado(participante, grupo.get("estado", String.class), false);
            }
            Record reglamento = dsl.fetchOne(
                    "SELECT id, hash_contenido FROM grupos.reglamento_grupo WHERE grupo_id = ? ORDER BY version DESC LIMIT 1",
                    grupoId);
            if (reglamento == null
                    || !reglamento.get("hash_contenido", String.class).equals(hashReglamento)) {
                throw new ErrorDeNegocio(CodigoError.de(69, 5), "El reglamento cambio. Revisalo antes de firmar.");
            }
            dsl.execute(
                    """
                    INSERT INTO grupos.aceptacion_reglamento
                      (reglamento_id, participante_id, aceptado_en, hash_firmado, ip_origen)
                    VALUES (?, ?, CAST(? AS timestamptz), ?, ?::inet)
                    """,
                    reglamento.get("id", UUID.class),
                    participante,
                    ahora,
                    hashReglamento,
                    ip);
            dsl.execute(
                    "UPDATE grupos.participante SET estado = 'ACTIVO' WHERE id = ? AND estado = 'ACEPTADO_PENDIENTE_FIRMA'",
                    participante);
            dsl.execute(
                    "UPDATE grupos.cupo SET estado = 'OCUPADO' WHERE participante_id = ? AND estado = 'RESERVADO'",
                    participante);
            boolean conformado = conformarSiCorresponde(dsl, grupoId, grupo.get("estado", String.class), ctx, ahora);
            return new Firmado(participante, conformado ? "CONFORMADO" : grupo.get("estado", String.class), conformado);
        });
    }

    /** Con todos los cupos ocupados (y por lo tanto firmados) el grupo deja de recibir y se arma su calendario. */
    private boolean conformarSiCorresponde(
            DSLContext dsl, UUID grupoId, String estado, ContextoSesion ctx, OffsetDateTime ahora) {
        int sinOcupar = dsl.fetchOne(
                        "SELECT count(*)::int FROM grupos.cupo WHERE grupo_id = ? AND estado <> 'OCUPADO'", grupoId)
                .get(0, Integer.class);
        if (sinOcupar > 0 || !"ABIERTO_A_INSCRIPCION".equals(estado)) return false;
        int cambio = dsl.execute(
                "UPDATE grupos.grupo SET estado = 'CONFORMADO' WHERE id = ? AND estado = 'ABIERTO_A_INSCRIPCION'",
                grupoId);
        if (cambio == 0) return false;
        dsl.execute(
                """
                INSERT INTO grupos.historial_estado_grupo
                  (grupo_id, estado_anterior, estado_nuevo, motivo, ejecutado_por, fecha_hora)
                VALUES (?, 'ABIERTO_A_INSCRIPCION', 'CONFORMADO', ?, ?, CAST(? AS timestamptz))
                """,
                grupoId,
                MOTIVO_CONFORMADO,
                ctx.usuarioId(),
                ahora);
        calendario.generar(dsl, grupoId, ahora.toLocalDate());
        return true;
    }

    private static ErrorDeNegocio sinFirmaPendiente() {
        return new ErrorDeNegocio(CodigoError.de(69, 13), "No tenes una firma pendiente en este grupo.");
    }

    public record Firmado(UUID participanteId, String estadoDelGrupo, boolean grupoConformado) {}
}
