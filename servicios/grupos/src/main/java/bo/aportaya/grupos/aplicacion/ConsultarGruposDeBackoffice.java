package bo.aportaya.grupos.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los grupos vistos desde el backoffice (2026-10-29): el listado y un grupo con sus cupos. Solo lectura.
 *
 * <p>Corre con contexto de sistema, como la bandeja de admisiones: la RLS de {@code grupos} no conoce los roles del
 * backoffice y la puerta es el permiso del controlador ({@code ADMIN_PLATAFORMA} o {@code AUDITORIA_LEER}). Devuelve
 * datos del grupo y, por cupo, solo el alias y el estado: nada de contacto ni importes atribuibles a una persona.
 */
@Service
public class ConsultarGruposDeBackoffice {

    static final int FILAS = 200;

    private final Datos datos;

    public ConsultarGruposDeBackoffice(Datos datos) {
        this.datos = datos;
    }

    /** {@code estado} vacio o nulo trae todos; {@code limite} se acota a 1..200. */
    @Transactional(readOnly = true)
    public List<Grupo> listar(String estado, Integer limite, ContextoSesion ctx) {
        int tope = limite == null ? FILAS : Math.max(1, Math.min(limite, FILAS));
        String filtro = estado == null || estado.isBlank() ? null : estado;
        return datos.conContexto(sistema(ctx), dsl -> dsl.fetch(
                        """
                        SELECT id, codigo_publico, nombre, estado, monto_aporte, moneda, periodicidad,
                               cupos_totales, cupos_ocupados, fecha_inicio
                          FROM grupos.grupo
                         WHERE (?::text IS NULL OR estado = ?::text)
                         ORDER BY fecha_inicio DESC NULLS FIRST, codigo_publico, id
                         LIMIT ?
                        """,
                        filtro,
                        filtro,
                        tope)
                .map(ConsultarGruposDeBackoffice::grupo));
    }

    @Transactional(readOnly = true)
    public Detalle detalle(UUID grupoId, ContextoSesion ctx) {
        return datos.conContexto(sistema(ctx), dsl -> {
            var g = dsl.fetchOne(
                    """
                    SELECT id, codigo_publico, nombre, estado, monto_aporte, moneda, periodicidad,
                           cupos_totales, cupos_ocupados, fecha_inicio
                      FROM grupos.grupo WHERE id = ?
                    """,
                    grupoId);
            if (g == null) {
                throw new ErrorDeNegocio(CodigoError.de(64, 1), "Ese grupo no existe.");
            }
            var cupos = dsl.fetch(
                            """
                            SELECT c.numero, c.estado,
                                   coalesce(p.alias, (SELECT s.nombre_visible FROM grupos.solicitud_ingreso s
                                                       WHERE s.grupo_id = c.grupo_id AND s.usuario_id = p.usuario_id
                                                         AND s.nombre_visible IS NOT NULL
                                                       ORDER BY s.fecha_solicitud DESC LIMIT 1)) AS alias,
                                   p.estado AS estado_participante,
                                   coalesce(p.es_organizador, false) AS es_organizador
                              FROM grupos.cupo c
                              JOIN grupos.grupo g ON g.id = c.grupo_id
                              LEFT JOIN grupos.participante p ON p.id = c.participante_id
                             -- Los cupos que el inicio al 75 % dejo afuera del ciclo (numero > cupos_totales) no se listan.
                             WHERE c.grupo_id = ? AND c.numero <= g.cupos_totales
                             ORDER BY c.numero
                            """,
                            grupoId)
                    .map(r -> new Cupo(
                            r.get("numero", Integer.class),
                            r.get("estado", String.class),
                            r.get("alias", String.class),
                            r.get("estado_participante", String.class),
                            Boolean.TRUE.equals(r.get("es_organizador", Boolean.class))));
            return new Detalle(grupo(g), cupos);
        });
    }

    private static Grupo grupo(org.jooq.Record r) {
        return new Grupo(
                r.get("id", UUID.class),
                r.get("codigo_publico", String.class),
                r.get("nombre", String.class),
                r.get("estado", String.class),
                r.get("monto_aporte", BigDecimal.class),
                r.get("moneda", String.class),
                r.get("periodicidad", String.class),
                r.get("cupos_totales", Integer.class),
                r.get("cupos_ocupados", Integer.class),
                r.get("fecha_inicio", LocalDate.class));
    }

    private static ContextoSesion sistema(ContextoSesion ctx) {
        return ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
    }

    public record Grupo(
            UUID id,
            String codigo,
            String nombre,
            String estado,
            BigDecimal montoAporte,
            String moneda,
            String periodicidad,
            int cuposTotales,
            int cuposOcupados,
            LocalDate fechaInicio) {}

    public record Cupo(int numero, String estado, String alias, String estadoParticipante, boolean esOrganizador) {}

    public record Detalle(Grupo grupo, List<Cupo> cupos) {}
}
