package bo.aportaya.grupos.infraestructura;

import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.PaqueteDeSorteo;
import bo.aportaya.grupos.dominio.PoliticaDelGrupo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Component;

/**
 * Lo que este servicio le contesta a los otros.
 *
 * <p>Tres preguntas que no puede responder nadie mas: a que cuenta apunta un alias, si
 * alguien participa de un grupo activo y como fue un sorteo. Las tres viven en este
 * esquema, y publicarlas es lo que evita que {@code nucleo-financiero} y
 * {@code transparencia} tengan que leerlo (invariante 11).
 */
@Component
public class ConsultasRepositorio {

    /**
     * La cuenta de billetera detras de un alias.
     *
     * <p>Devuelve la cuenta y **nada mas**: ni el nombre ni el telefono de nadie. Quien
     * transfiere ya sabe a quien le manda; esta consulta no es un directorio.
     */
    public Optional<UUID> usuarioDelAlias(DSLContext dsl, String alias) {
        // Busca entre los participantes de OTROS: la politica de fila deja al titular ver solo
        // lo suyo. La consulta devuelve unicamente un id de usuario, no la fila, y por eso es
        // el unico lugar donde se lee con rol de sistema.
        var fila = bo.aportaya.plataforma.datos.Datos.comoSistema(
                dsl,
                () -> dsl.fetchOne(
                        """
                SELECT p.usuario_id AS usuario
                  FROM grupos.participante p
                 WHERE p.alias = ? AND p.estado = 'ACTIVO'
                 LIMIT 1
                """,
                        alias));
        return fila == null ? Optional.empty() : Optional.ofNullable(fila.get("usuario", UUID.class));
    }

    /** En cuantos grupos vivos participa hoy. Cero significa que se puede ir. */
    public int gruposActivosDe(DSLContext dsl, UUID usuarioId) {
        var fila = dsl.fetchOne(
                """
                SELECT COUNT(*)::int AS activos
                  FROM grupos.participante p
                  JOIN grupos.grupo g ON g.id = p.grupo_id
                 WHERE p.usuario_id = ? AND p.estado = 'ACTIVO'
                   AND g.estado NOT IN ('CERRADO', 'CANCELADO', 'DISUELTO')
                """,
                usuarioId);
        return fila == null ? 0 : fila.get("activos", Integer.class);
    }

    /**
     * El paquete publicado de un sorteo, para que se pueda rehacer desde afuera.
     *
     * <p>Antes del revelado la semilla viaja nula, y ese es el punto: comprometer y
     * revelar existen para que nadie —nosotros incluidos— elija el resultado despues de
     * conocerlo.
     */
    public Optional<PaqueteDeSorteo> paqueteDelSorteo(DSLContext dsl, UUID sorteoId) {
        var fila = dsl.fetchOne(
                """
                SELECT s.hash_semilla_previo, s.semilla_publica, s.algoritmo,
                       s.aportes_entropia, s.resultado
                  FROM grupos.sorteo_turnos s
                 WHERE s.id = ?
                """,
                sorteoId);
        if (fila == null) {
            return Optional.empty();
        }
        return Optional.of(new PaqueteDeSorteo(
                fila.get("hash_semilla_previo", String.class),
                fila.get("semilla_publica", String.class),
                fila.get("algoritmo", String.class),
                comoLista(fila.get("aportes_entropia", JSONB.class)),
                comoLista(fila.get("resultado", JSONB.class))));
    }

    /** Los periodos del grupo, en orden. El sorteo reparte turnos contra ellos. */
    public List<UUID> periodosDe(DSLContext dsl, UUID grupoId) {
        return dsl.fetch(
                        """
                        SELECT id FROM grupos.periodo WHERE grupo_id = ? ORDER BY numero
                        """,
                        grupoId)
                .map(f -> f.get("id", UUID.class));
    }

    /**
     * Lo que se estima que cobra cada turno: el aporte por la cantidad de cupos.
     *
     * <p>Es una estimacion y se dice: la bolsa real depende de quien pague, y eso recien
     * se sabe al cerrar el periodo.
     */
    public java.math.BigDecimal montoEstimado(DSLContext dsl, UUID grupoId) {
        var fila = dsl.fetchOne(
                """
                SELECT g.monto_aporte * g.cupos_totales AS estimado
                  FROM grupos.grupo g WHERE g.id = ?
                """,
                grupoId);
        return fila == null ? java.math.BigDecimal.ZERO : fila.get("estimado", java.math.BigDecimal.class);
    }

    /** El grupo de un acuerdo. */
    public Optional<UUID> grupoDelAcuerdo(DSLContext dsl, UUID acuerdoId) {
        var fila = dsl.fetchOne("SELECT grupo_id FROM grupos.acuerdo WHERE id = ?", acuerdoId);
        return fila == null ? Optional.empty() : Optional.ofNullable(fila.get("grupo_id", UUID.class));
    }

    /** El grupo al que pertenece un turno. */
    public Optional<UUID> grupoDelTurno(DSLContext dsl, UUID turnoId) {
        var fila = dsl.fetchOne("SELECT grupo_id FROM grupos.turno WHERE id = ?", turnoId);
        return fila == null ? Optional.empty() : Optional.ofNullable(fila.get("grupo_id", UUID.class));
    }

    /** Si el reglamento del grupo de ese turno admite permutas. Sin dato, no admite. */
    public boolean permitePermuta(DSLContext dsl, UUID turnoId) {
        var fila = dsl.fetchOne(
                """
                SELECT COALESCE(c.permite_permuta_turnos, false) AS permite
                  FROM grupos.turno t
                  LEFT JOIN grupos.configuracion_grupo c ON c.grupo_id = t.grupo_id
                 WHERE t.id = ?
                """,
                turnoId);
        return fila != null && Boolean.TRUE.equals(fila.get("permite", Boolean.class));
    }

    /** Lo que el grupo le exige a quien quiere entrar, y el quorum de sus decisiones. */
    public Optional<PoliticaDelGrupo> politicaDelGrupo(DSLContext dsl, UUID grupoId) {
        var fila = dsl.fetchOne(
                """
                SELECT g.requiere_kyc_minimo, g.reputacion_minima, g.quorum_decisiones,
                       g.cupos_totales - g.cupos_ocupados AS cupos_libres
                  FROM grupos.grupo g WHERE g.id = ?
                """,
                grupoId);
        if (fila == null) {
            return Optional.empty();
        }
        return Optional.of(new PoliticaDelGrupo(
                fila.get("requiere_kyc_minimo", String.class),
                fila.get("reputacion_minima", Integer.class),
                fila.get("quorum_decisiones", java.math.BigDecimal.class),
                fila.get("cupos_libres", Integer.class)));
    }

    /** Si ya cobro su turno. Cambia de que lado queda al retirarse. */
    public boolean yaCobroSuTurno(DSLContext dsl, UUID participanteId) {
        var fila = dsl.fetchOne(
                """
                SELECT COUNT(*)::int AS cobrados
                  FROM grupos.turno t
                  JOIN grupos.cupo c ON c.id = t.cupo_id
                 WHERE c.participante_id = ? AND t.estado = 'COBRADO'
                """,
                participanteId);
        return fila != null && fila.get("cobrados", Integer.class) > 0;
    }

    /** Si ese usuario ya esta en el grupo. Invitar a quien ya entro es ruido. */
    public boolean yaEsParticipante(DSLContext dsl, UUID grupoId, UUID usuarioId) {
        return dsl.fetchExists(org.jooq
                .impl
                .DSL
                .selectOne()
                .from(org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("grupos", "participante")))
                .where(org.jooq.impl.DSL.field("grupo_id", UUID.class).eq(grupoId))
                .and(org.jooq.impl.DSL.field("usuario_id", UUID.class).eq(usuarioId))
                .and(org.jooq.impl.DSL.field("estado", String.class).eq("ACTIVO")));
    }

    /** El participante que es este usuario en ese grupo. */
    public Optional<UUID> participanteDe(DSLContext dsl, UUID grupoId, UUID usuarioId) {
        var fila = dsl.fetchOne(
                """
                SELECT id FROM grupos.participante
                 WHERE grupo_id = ? AND usuario_id = ? AND estado = 'ACTIVO'
                """,
                grupoId,
                usuarioId);
        return fila == null ? Optional.empty() : Optional.ofNullable(fila.get("id", UUID.class));
    }

    public List<Consultas.Participacion> participacionesDe(DSLContext dsl, UUID usuarioId) {
        return dsl.fetch(
                        """
                        SELECT grupo_id, id, estado FROM grupos.participante
                         WHERE usuario_id = ?
                           AND estado NOT IN ('RETIRADO','EXPULSADO','REEMPLAZADO')
                         ORDER BY fecha_ingreso DESC, id
                         LIMIT 50
                        """,
                        usuarioId)
                .map(r -> new Consultas.Participacion(
                        r.get("grupo_id", UUID.class), r.get("id", UUID.class), r.get("estado", String.class)));
    }

    /** Si ese participante es la persona de la sesion. */
    public boolean esElParticipanteDe(DSLContext dsl, UUID participanteId, UUID usuarioId) {
        return Boolean.TRUE.equals(dsl.fetchOne(
                        "SELECT EXISTS (SELECT 1 FROM grupos.participante WHERE id = ? AND usuario_id = ?) AS si",
                        participanteId,
                        usuarioId)
                .get("si", Boolean.class));
    }

    /** Un periodo admite pago salvo que este CERRADO, LIQUIDADO o CANCELADO (o no exista). */
    public boolean periodoAdmitePago(DSLContext dsl, UUID periodoId) {
        return Boolean.TRUE.equals(dsl.fetchOne(
                        "SELECT EXISTS (SELECT 1 FROM grupos.periodo WHERE id = ? AND estado IN ('PROGRAMADO','ABIERTO','EN_GRACIA')) AS si",
                        periodoId)
                .get("si", Boolean.class));
    }

    public record Politica(String kycMinimo, int reputacionMinima, java.math.BigDecimal quorum, int cuposLibres) {}

    /** Un jsonb de una sola dimension, como lista de textos. Vacio si no hay nada. */
    private static List<String> comoLista(JSONB json) {
        if (json == null) {
            return List.of();
        }
        String crudo = json.data().trim();
        if (crudo.length() <= 2) {
            return List.of();
        }
        return List.of(crudo.substring(1, crudo.length() - 1).replace("\"", "").split(","));
    }
}
