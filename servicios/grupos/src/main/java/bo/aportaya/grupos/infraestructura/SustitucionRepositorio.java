package bo.aportaya.grupos.infraestructura;

import bo.aportaya.grupos.dominio.SustitucionDeAdministrador;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/** Cambio de administrador de un grupo: quién administra, qué obligaciones conserva y el registro íntegro. */
@Component
public class SustitucionRepositorio {

    public void bloquearGrupo(DSLContext dsl, UUID grupoId) {
        dsl.fetch("SELECT id FROM grupos.grupo WHERE id=? FOR UPDATE", grupoId);
    }

    public List<Participante> administradoresVigentes(DSLContext dsl, UUID grupoId) {
        return dsl.fetch(
                        "SELECT id, usuario_id FROM grupos.participante WHERE grupo_id=? AND es_organizador AND estado IN ('ACTIVO','ACEPTADO_PENDIENTE_FIRMA') FOR UPDATE",
                        grupoId)
                .map(f -> new Participante(f.get("id", UUID.class), f.get("usuario_id", UUID.class)));
    }

    public Optional<Participante> participanteActivo(DSLContext dsl, UUID grupoId, UUID usuarioId) {
        return Optional.ofNullable(dsl.fetchOne(
                        "SELECT id, usuario_id FROM grupos.participante WHERE grupo_id=? AND usuario_id=? AND estado='ACTIVO' FOR UPDATE",
                        grupoId,
                        usuarioId))
                .map(f -> new Participante(f.get("id", UUID.class), f.get("usuario_id", UUID.class)));
    }

    /** Las obligaciones que el saliente CONSERVA: su participación y sus cupos, en texto para el registro. */
    public String obligacionesDe(DSLContext dsl, UUID participanteId) {
        var p = dsl.fetchOne(
                "SELECT estado, aportes_realizados, aportes_en_mora FROM grupos.participante WHERE id=?",
                participanteId);
        var cupos = dsl.fetch(
                "SELECT numero, estado FROM grupos.cupo WHERE participante_id=? ORDER BY numero", participanteId);
        return "participante=" + participanteId
                + ";estado=" + p.get("estado")
                + ";aportesRealizados=" + p.get("aportes_realizados")
                + ";aportesEnMora=" + p.get("aportes_en_mora")
                + ";cupos="
                + String.join(
                        ",",
                        cupos.map(c -> c.get("numero") + ":" + c.get("estado")).stream()
                                .map(Object::toString)
                                .toList());
    }

    public void marcarAdministrador(DSLContext dsl, UUID participanteId, boolean valor) {
        dsl.execute("UPDATE grupos.participante SET es_organizador=? WHERE id=?", valor, participanteId);
    }

    public Optional<SustitucionDeAdministrador> porClave(DSLContext dsl, UUID clave) {
        return Optional.ofNullable(dsl.fetchOne(
                        "SELECT * FROM grupos.sustitucion_administrador WHERE clave_idempotencia=?", clave))
                .map(f -> new SustitucionDeAdministrador(
                        f.get("id", UUID.class),
                        f.get("grupo_id", UUID.class),
                        f.get("saliente_participante_id", UUID.class),
                        f.get("entrante_participante_id", UUID.class),
                        f.get("clave_idempotencia", UUID.class),
                        f.get("motivo", String.class),
                        f.get("actor_id", UUID.class),
                        f.get("obligaciones_conservadas", String.class),
                        f.get("ocurrida_en", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                        f.get("correlacion_id", UUID.class)));
    }

    public void guardar(DSLContext dsl, SustitucionDeAdministrador s) {
        dsl.execute(
                """
            INSERT INTO grupos.sustitucion_administrador
            (id,grupo_id,saliente_participante_id,entrante_participante_id,clave_idempotencia,motivo,actor_id,
             obligaciones_conservadas,ocurrida_en,correlacion_id)
            VALUES (?,?,?,?,?,?,?,?,?::timestamptz,?)
            """,
                s.id(),
                s.grupoId(),
                s.salienteParticipanteId(),
                s.entranteParticipanteId(),
                s.clave(),
                s.motivo(),
                s.actorId(),
                s.obligacionesConservadas(),
                s.ocurridaEn(),
                s.correlacionId());
    }

    public record Participante(UUID id, UUID usuarioId) {}
}
