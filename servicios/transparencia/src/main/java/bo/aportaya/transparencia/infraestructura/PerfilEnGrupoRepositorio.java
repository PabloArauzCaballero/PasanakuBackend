package bo.aportaya.transparencia.infraestructura;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * La reputacion de un integrante para el perfil del grupo (2026-10-21): el puntaje vigente con sus factores y las
 * insignias no revocadas. Lo lee el proceso interno, y por eso solo se llama DESPUES de que grupos confirmo que
 * comparten el grupo ({@code ConsultarPerfilEnGrupo}). Sin montos: {@code monto_total_aportado} no se lee.
 */
@Repository
public class PerfilEnGrupoRepositorio {

    private final Datos datos;

    public PerfilEnGrupoRepositorio(Datos datos) {
        this.datos = datos;
    }

    @Transactional(readOnly = true)
    public Perfil de(UUID usuarioId, ContextoSesion ctx) {
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(interno, dsl -> {
            Optional<Factores> factores = dsl.fetchOptional(
                            """
                            SELECT puntaje, nivel_confianza, indice_puntualidad, grupos_completados,
                                   grupos_abandonados, incumplimientos_abiertos, antiguedad_meses
                              FROM transparencia.puntaje_reputacion
                             WHERE usuario_id = ?
                            """,
                            usuarioId)
                    .map(f -> new Factores(
                            f.get("puntaje", BigDecimal.class),
                            f.get("nivel_confianza", String.class),
                            f.get("indice_puntualidad", BigDecimal.class),
                            f.get("grupos_completados", Integer.class),
                            f.get("grupos_abandonados", Integer.class),
                            f.get("incumplimientos_abiertos", Integer.class),
                            f.get("antiguedad_meses", Integer.class)));
            // Una fila por insignia del catalogo (DISTINCT ON): el listado queda acotado por el catalogo, que es corto.
            List<Insignia> insignias = dsl.fetch(
                            """
                            SELECT DISTINCT ON (l.codigo) l.codigo, l.nombre, l.descripcion, o.otorgada_en
                              FROM transparencia.insignia_otorgada o
                              JOIN transparencia.insignia_logro l ON l.id = o.insignia_id
                             WHERE o.usuario_id = ? AND o.revocada_en IS NULL
                             ORDER BY l.codigo, o.otorgada_en DESC
                            """,
                            usuarioId)
                    .map(f -> new Insignia(
                            f.get("codigo", String.class),
                            f.get("nombre", String.class),
                            f.get("descripcion", String.class),
                            f.get("otorgada_en", OffsetDateTime.class)));
            return new Perfil(factores, insignias);
        });
    }

    public record Factores(
            BigDecimal puntaje,
            String nivelConfianza,
            BigDecimal indicePuntualidad,
            int gruposCompletados,
            int gruposAbandonados,
            int incumplimientosAbiertos,
            int antiguedadMeses) {}

    public record Insignia(String codigo, String nombre, String descripcion, OffsetDateTime otorgadaEn) {}

    public record Perfil(Optional<Factores> factores, List<Insignia> insignias) {}
}
