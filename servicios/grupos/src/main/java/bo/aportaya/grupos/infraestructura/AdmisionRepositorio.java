package bo.aportaya.grupos.infraestructura;

import bo.aportaya.grupos.dominio.DecisionDeIngreso;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/** Persistencia del expediente humano, con exclusión por grupo y solicitud. */
@Component
public class AdmisionRepositorio {
    public Solicitud ver(DSLContext dsl, UUID id) {
        var fila = dsl.fetchOne(
                "SELECT id, grupo_id, usuario_id, estado, cupos_solicitados FROM grupos.solicitud_ingreso WHERE id=?",
                id);
        if (fila == null) {
            throw error("La solicitud no existe.");
        }
        return new Solicitud(
                id,
                fila.get("grupo_id", UUID.class),
                fila.get("usuario_id", UUID.class),
                fila.get("estado", String.class),
                fila.get("cupos_solicitados", Integer.class));
    }

    public Solicitud bloquear(DSLContext dsl, UUID id) {
        var solicitud = ver(dsl, id);
        dsl.fetchOne("SELECT id FROM grupos.grupo WHERE id=? FOR UPDATE", solicitud.grupoId());
        dsl.fetchOne("SELECT id FROM grupos.solicitud_ingreso WHERE id=? FOR UPDATE", id);
        return ver(dsl, id);
    }

    public boolean esAdministrador(DSLContext dsl, UUID grupo, UUID usuario) {
        return dsl.fetchOne(
                        "SELECT 1 FROM grupos.participante WHERE grupo_id=? AND usuario_id=? AND es_organizador AND estado IN ('ACTIVO','ACEPTADO_PENDIENTE_FIRMA') LIMIT 1",
                        grupo,
                        usuario)
                != null;
    }

    public Optional<DecisionDeIngreso> porClave(DSLContext dsl, UUID clave) {
        return Optional.ofNullable(
                        dsl.fetchOne("SELECT * FROM grupos.decision_ingreso WHERE clave_idempotencia=?", clave))
                .map(this::mapear);
    }

    public Optional<DecisionDeIngreso> ultima(DSLContext dsl, UUID solicitud) {
        return Optional.ofNullable(dsl.fetchOne(
                        "SELECT * FROM grupos.decision_ingreso WHERE solicitud_id=? ORDER BY revision DESC LIMIT 1",
                        solicitud))
                .map(this::mapear);
    }

    public List<DecisionDeIngreso> historial(DSLContext dsl, UUID solicitud) {
        return dsl.fetch("SELECT * FROM grupos.decision_ingreso WHERE solicitud_id=? ORDER BY revision", solicitud)
                .map(this::mapear);
    }

    public String evidencia(DSLContext dsl, UUID solicitud) {
        var fila = dsl.fetchOne(
                "SELECT payload::text AS evidencia FROM grupos.evento_dominio WHERE agregado_id=? AND tipo='grupos.ingreso_solicitado' ORDER BY ocurrido_en LIMIT 1",
                solicitud);
        if (fila == null) {
            throw error("Falta la evidencia original de postulación; requiere reconstrucción verificada.");
        }
        return fila.get("evidencia", String.class);
    }

    /**
     * Lo que el motor recomendo al postular: version y recomendacion, leidas de la evidencia original. Una
     * evidencia anterior al motor versionado se interpreta por sus alertas y se marca como "heredada".
     */
    public Recomendacion recomendacion(String evidencia) {
        try {
            var nodo = new com.fasterxml.jackson.databind.ObjectMapper().readTree(evidencia);
            String version = nodo.path("versionMotor").asText("heredada");
            String recomendacion = nodo.hasNonNull("recomendacion")
                    ? nodo.get("recomendacion").asText()
                    : evidencia.contains("REVISAR_") || evidencia.contains("SIN_DATOS_")
                            ? "REVISION_HUMANA"
                            : "ACEPTAR";
            return new Recomendacion(version, recomendacion);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw error("La evidencia original de postulación es ilegible; requiere reconstrucción verificada.");
        }
    }

    public record Recomendacion(String version, String recomendacion) {}

    public UUID reservarAdmision(DSLContext dsl, Solicitud solicitud) {
        var grupo = dsl.fetchOne("SELECT estado FROM grupos.grupo WHERE id=?", solicitud.grupoId());
        if (!"ABIERTO_A_INSCRIPCION".equals(grupo.get("estado", String.class))) {
            throw error("El grupo no está abierto a inscripciones.");
        }
        if (solicitud.cupos() < 1
                || dsl.fetchOne(
                                "SELECT 1 FROM grupos.participante WHERE grupo_id=? AND usuario_id=? AND estado NOT IN ('RETIRADO','EXPULSADO','REEMPLAZADO') LIMIT 1",
                                solicitud.grupoId(),
                                solicitud.usuarioId())
                        != null) {
            throw error("La persona ya tiene una participación o la cantidad de cupos es inválida.");
        }
        var config = dsl.fetchOne(
                "SELECT permite_cupos_multiples, max_cupos_por_persona FROM grupos.configuracion_grupo WHERE grupo_id=?",
                solicitud.grupoId());
        if (config == null
                || solicitud.cupos() > config.get("max_cupos_por_persona", Integer.class)
                || (solicitud.cupos() > 1 && !config.get("permite_cupos_multiples", Boolean.class))) {
            throw error("La solicitud supera los cupos permitidos por persona.");
        }
        var cupos = dsl.fetch(
                "SELECT id FROM grupos.cupo WHERE grupo_id=? AND estado='LIBRE' ORDER BY numero LIMIT ? FOR UPDATE",
                solicitud.grupoId(),
                solicitud.cupos());
        if (cupos.size() != solicitud.cupos()) {
            throw error("Ya no hay suficientes cupos libres.");
        }
        UUID participante = UUID.randomUUID();
        dsl.execute(
                "INSERT INTO grupos.participante (id, grupo_id, usuario_id, estado, es_organizador, fecha_ingreso, reputacion_al_ingresar, aportes_realizados, aportes_en_mora) VALUES (?, ?, ?, 'ACEPTADO_PENDIENTE_FIRMA', false, now(), 0, 0, 0)",
                participante,
                solicitud.grupoId(),
                solicitud.usuarioId());
        for (var cupo : cupos) {
            dsl.execute(
                    "UPDATE grupos.cupo SET participante_id=?, estado='RESERVADO', asignado_en=now() WHERE id=? AND estado='LIBRE'",
                    participante,
                    cupo.get("id", UUID.class));
        }
        dsl.execute(
                "UPDATE grupos.grupo SET cupos_ocupados=(SELECT count(*) FROM grupos.cupo WHERE grupo_id=? AND estado IN ('OCUPADO','RESERVADO')) WHERE id=?",
                solicitud.grupoId(),
                solicitud.grupoId());
        return participante;
    }

    public void guardar(DSLContext dsl, DecisionDeIngreso decision) {
        dsl.execute(
                "INSERT INTO grupos.decision_ingreso (id, solicitud_id, clave_idempotencia, fase, decision, actor_id, motivo, propuesta_id, participante_id, revision, evidencia_algoritmo, version_motor, recomendacion_algoritmo, apartamiento, ocurrida_en, correlacion_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::timestamptz,?)",
                decision.id(),
                decision.solicitudId(),
                decision.clave(),
                decision.fase(),
                decision.decision(),
                decision.actorId(),
                decision.motivo(),
                decision.propuestaId(),
                decision.participanteId(),
                decision.revision(),
                decision.evidenciaAlgoritmo(),
                decision.versionMotor(),
                decision.recomendacionAlgoritmo(),
                decision.apartamiento(),
                decision.ocurridaEn(),
                decision.correlacionId());
    }

    public void resolver(DSLContext dsl, UUID solicitud, DecisionDeIngreso decision) {
        dsl.execute(
                "UPDATE grupos.solicitud_ingreso SET estado=?, revisada_por=?, fecha_resolucion=?::timestamptz WHERE id=? AND estado='PENDIENTE'",
                "ACEPTAR".equals(decision.decision()) ? "APROBADA" : "RECHAZADA",
                decision.actorId(),
                decision.ocurridaEn(),
                solicitud);
    }

    private DecisionDeIngreso mapear(Record f) {
        return new DecisionDeIngreso(
                f.get("id", UUID.class),
                f.get("solicitud_id", UUID.class),
                f.get("clave_idempotencia", UUID.class),
                f.get("fase", String.class),
                f.get("decision", String.class),
                f.get("actor_id", UUID.class),
                f.get("motivo", String.class),
                f.get("propuesta_id", UUID.class),
                f.get("participante_id", UUID.class),
                f.get("revision", Integer.class),
                f.get("evidencia_algoritmo", String.class),
                f.get("version_motor", String.class),
                f.get("recomendacion_algoritmo", String.class),
                f.get("apartamiento", Boolean.class),
                f.get("ocurrida_en", OffsetDateTime.class).withOffsetSameInstant(java.time.ZoneOffset.UTC),
                f.get("correlacion_id", UUID.class));
    }

    private static ErrorDeNegocio error(String mensaje) {
        return new ErrorDeNegocio(CodigoError.de(68, 6), mensaje);
    }

    public record Solicitud(UUID id, UUID grupoId, UUID usuarioId, String estado, int cupos) {}
}
