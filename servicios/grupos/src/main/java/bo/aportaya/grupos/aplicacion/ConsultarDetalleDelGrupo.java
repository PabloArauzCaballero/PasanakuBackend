package bo.aportaya.grupos.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.jooq.Record;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-69 · El grupo visto por uno de sus integrantes (2026-10-20): un resumen y una fila por cupo con el nombre
 * visible de quien lo ocupa, si firmo y su turno. Solo para integrantes; sin telefono ni correo de nadie
 * (decision del dueño). Lo lee el proceso interno porque la politica de fila no deja ver a los demas
 * participantes, y por eso la pertenencia se comprueba aca antes de devolver nada.
 */
@Service
public class ConsultarDetalleDelGrupo {

    private final Datos datos;

    public ConsultarDetalleDelGrupo(Datos datos) {
        this.datos = datos;
    }

    @Transactional(readOnly = true)
    public Detalle consultar(UUID grupoId, ContextoSesion ctx) {
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(interno, dsl -> {
            Record yo = dsl.fetchOne(
                    """
                    SELECT estado, es_organizador FROM grupos.participante
                     WHERE grupo_id = ? AND usuario_id = ?
                       AND estado NOT IN ('RETIRADO', 'EXPULSADO', 'REEMPLAZADO', 'POSTULANTE', 'INVITADO')
                     ORDER BY fecha_ingreso DESC LIMIT 1
                    """,
                    grupoId,
                    ctx.usuarioId());
            if (yo == null) {
                throw new ErrorDeNegocio(CodigoError.de(69, 14), "No sos integrante de este grupo.");
            }
            Record g = dsl.fetchOne(
                    """
                    SELECT nombre, estado, monto_aporte, moneda, periodicidad, cupos_totales, cupos_ocupados,
                           modalidad_turnos
                      FROM grupos.grupo WHERE id = ?
                    """,
                    grupoId);
            Record reglamento = dsl.fetchOne(
                    "SELECT contenido, hash_contenido FROM grupos.reglamento_grupo WHERE grupo_id = ? ORDER BY version DESC LIMIT 1",
                    grupoId);
            Record sorteo = dsl.fetchOne(
                    """
                    SELECT id, estado FROM grupos.sorteo_turnos
                     WHERE grupo_id = ? AND anulado_en IS NULL AND estado IN ('COMPROMETIDO', 'REVELADO')
                     ORDER BY fecha_compromiso DESC LIMIT 1
                    """,
                    grupoId);
            List<Integrante> integrantes = dsl.fetch(
                            """
                            SELECT c.numero, c.id AS cupo_id, c.estado AS estado_cupo, p.alias, p.estado,
                                   coalesce(p.es_organizador, false) AS es_organizador,
                                   coalesce(p.usuario_id = ?, false) AS soy_yo,
                                   (SELECT t.orden_asignado FROM grupos.turno t
                                     WHERE t.cupo_id = c.id AND t.estado <> 'ANULADO'
                                     ORDER BY t.orden_asignado LIMIT 1) AS turno
                              FROM grupos.cupo c
                              LEFT JOIN grupos.participante p ON p.id = c.participante_id
                             WHERE c.grupo_id = ?
                             ORDER BY c.numero
                            """,
                            ctx.usuarioId(),
                            grupoId)
                    .map(f -> new Integrante(
                            f.get("numero", Integer.class),
                            f.get("cupo_id", UUID.class),
                            f.get("alias", String.class),
                            estadoDelCupo(f.get("estado_cupo", String.class), f.get("estado", String.class)),
                            Boolean.TRUE.equals(f.get("es_organizador", Boolean.class)),
                            Boolean.TRUE.equals(f.get("soy_yo", Boolean.class)),
                            f.get("turno", Integer.class)));
            boolean soyOrganizador = Boolean.TRUE.equals(yo.get("es_organizador", Boolean.class));
            String estado = g.get("estado", String.class);
            boolean puedeSortear = soyOrganizador
                    && "CONFORMADO".equals(estado)
                    && "SORTEO_ALEATORIO".equals(g.get("modalidad_turnos", String.class))
                    && sorteo == null;
            return new Detalle(
                    grupoId,
                    g.get("nombre", String.class),
                    estado,
                    g.get("monto_aporte", BigDecimal.class),
                    g.get("moneda", String.class).trim(),
                    g.get("periodicidad", String.class),
                    g.get("cupos_totales", Integer.class),
                    g.get("cupos_ocupados", Integer.class),
                    soyOrganizador,
                    "ACEPTADO_PENDIENTE_FIRMA".equals(yo.get("estado", String.class)),
                    puedeSortear,
                    reglamento == null ? null : reglamento.get("contenido", String.class),
                    reglamento == null ? null : reglamento.get("hash_contenido", String.class),
                    sorteo == null ? null : sorteo.get("id", UUID.class),
                    sorteo == null ? null : sorteo.get("estado", String.class),
                    integrantes);
        });
    }

    /** LIBRE si nadie lo ocupa; FIRMADO si quien lo ocupa ya firmo (cupo OCUPADO); si no, PENDIENTE_FIRMA. */
    static String estadoDelCupo(String estadoCupo, String estadoParticipante) {
        if (estadoParticipante == null || "LIBRE".equals(estadoCupo)) return "LIBRE";
        return "OCUPADO".equals(estadoCupo) ? "FIRMADO" : "PENDIENTE_FIRMA";
    }

    public record Integrante(
            int numero,
            UUID cupoId,
            String nombre,
            String estado,
            boolean esOrganizador,
            boolean soyYo,
            Integer turno) {}

    public record Detalle(
            UUID grupoId,
            String nombre,
            String estado,
            BigDecimal montoAporte,
            String moneda,
            String periodicidad,
            int cuposTotales,
            int cuposOcupados,
            boolean soyOrganizador,
            boolean debeFirmar,
            boolean puedeSortear,
            String reglamentoTexto,
            String reglamentoHash,
            UUID sorteoId,
            String sorteoEstado,
            List<Integrante> integrantes) {}
}
