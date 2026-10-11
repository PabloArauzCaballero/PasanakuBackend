package bo.aportaya.organizador.infraestructura;

import bo.aportaya.organizador.dominio.DecisionDeHabilitacion;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Escritura única de {@code decision_habilitacion}: la usan la resolución nueva y la ruta heredada de
 * aprobación, de modo que ninguna resolución pueda quedar fuera del historial.
 */
final class BitacoraDeHabilitacion {
    private BitacoraDeHabilitacion() {}

    static void insertar(DSLContext dsl, DecisionDeHabilitacion d) {
        dsl.execute(
                """
            INSERT INTO organizador.decision_habilitacion
            (id,solicitud_id,clave_idempotencia,fase,decision,actor_id,motivo,revision,organizador_id,
             evidencia_requisitos,ocurrida_en,correlacion_id)
            VALUES (?,?,?,?,?,?,?,?,?,?,?::timestamptz,?)
            """,
                d.id(),
                d.solicitudId(),
                d.clave(),
                d.fase(),
                d.decision(),
                d.actorId(),
                d.motivo(),
                d.revision(),
                d.organizadorId(),
                d.evidenciaRequisitos(),
                d.ocurridaEn(),
                d.correlacionId());
    }

    static int siguienteRevision(DSLContext dsl, UUID solicitudId) {
        return dsl.fetchOne(
                        "SELECT COALESCE(MAX(revision),0)+1 FROM organizador.decision_habilitacion WHERE solicitud_id=?",
                        solicitudId)
                .get(0, Integer.class);
    }
}
