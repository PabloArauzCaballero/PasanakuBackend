package bo.aportaya.organizador.infraestructura;

import bo.aportaya.organizador.dominio.DecisionDeHabilitacion;
import bo.aportaya.organizador.dominio.ExpedienteDeHabilitacion;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/** Expediente de habilitación: bandeja del backoffice, solicitud con candado e historial íntegro de decisiones. */
@Component
public class HabilitacionRepositorio {

    private static final String EXPEDIENTE =
            """
        SELECT s.id, s.usuario_id, s.estado, s.puntaje_reputacion_al_solicitar, s.kyc_reforzado_id,
               s.motivo_rechazo, s.fecha_solicitud, s.fecha_resolucion,
               (SELECT COALESCE(MAX(d.revision),0) FROM organizador.decision_habilitacion d
                 WHERE d.solicitud_id=s.id) AS revision
        FROM organizador.solicitud_organizador s
        """;

    /** La solicitud con candado de fila: dos resoluciones simultáneas se serializan aquí. */
    public Optional<ExpedienteDeHabilitacion> bloquear(DSLContext dsl, UUID id) {
        dsl.fetch("SELECT id FROM organizador.solicitud_organizador WHERE id=? FOR UPDATE", id);
        return ver(dsl, id);
    }

    public Optional<ExpedienteDeHabilitacion> ver(DSLContext dsl, UUID id) {
        return Optional.ofNullable(dsl.fetchOne(EXPEDIENTE + " WHERE s.id=?", id))
                .map(this::expediente);
    }

    public Optional<ExpedienteDeHabilitacion> ultimaDelUsuario(DSLContext dsl, UUID usuarioId) {
        return dsl
                .fetch(
                        EXPEDIENTE + " WHERE s.usuario_id=? ORDER BY s.fecha_solicitud DESC, s.id DESC LIMIT 1",
                        usuarioId)
                .stream()
                .findFirst()
                .map(this::expediente);
    }

    /**
     * Cola de revisión, de la más antigua a la más nueva, con paginación por cursor (fecha, id): el
     * desempate por id hace estable el orden aunque dos solicitudes compartan el instante.
     */
    public List<ExpedienteDeHabilitacion> bandeja(
            DSLContext dsl, List<String> estados, OffsetDateTime despuesDeFecha, UUID despuesDeId, int limite) {
        String cursor = despuesDeFecha == null ? "" : " AND (s.fecha_solicitud, s.id) > (?::timestamptz, ?::uuid)";
        var parametros = new ArrayList<Object>();
        parametros.add(estados.toArray(String[]::new));
        if (despuesDeFecha != null) {
            parametros.add(despuesDeFecha);
            parametros.add(despuesDeId);
        }
        parametros.add(limite);
        return dsl.fetch(
                        EXPEDIENTE + " WHERE s.estado = ANY(?::varchar[])" + cursor
                                + " ORDER BY s.fecha_solicitud, s.id LIMIT ?",
                        parametros.toArray())
                .map(this::expediente);
    }

    /** Cierra la solicitud solo desde un estado abierto: el segundo revisor se entera y no pisa al primero. */
    public boolean cerrar(
            DSLContext dsl, UUID id, String estadoNuevo, UUID revisadaPor, String motivoRechazo, OffsetDateTime ahora) {
        return dsl.execute(
                        """
            UPDATE organizador.solicitud_organizador SET estado=?, revisada_por=?, motivo_rechazo=?,
            fecha_resolucion=?::timestamptz WHERE id=? AND estado IN ('PENDIENTE','EN_REVISION')
            """,
                        estadoNuevo,
                        revisadaPor,
                        motivoRechazo,
                        ahora,
                        id)
                == 1;
    }

    public Optional<DecisionDeHabilitacion> porClave(DSLContext dsl, UUID clave) {
        return Optional.ofNullable(dsl.fetchOne(
                        "SELECT * FROM organizador.decision_habilitacion WHERE clave_idempotencia=?", clave))
                .map(this::decision);
    }

    public Optional<DecisionDeHabilitacion> ultima(DSLContext dsl, UUID solicitudId) {
        return dsl
                .fetch(
                        "SELECT * FROM organizador.decision_habilitacion WHERE solicitud_id=? ORDER BY revision DESC LIMIT 1",
                        solicitudId)
                .stream()
                .findFirst()
                .map(this::decision);
    }

    public List<DecisionDeHabilitacion> historial(DSLContext dsl, UUID solicitudId) {
        return dsl.fetch(
                        "SELECT * FROM organizador.decision_habilitacion WHERE solicitud_id=? ORDER BY revision",
                        solicitudId)
                .map(this::decision);
    }

    public void guardar(DSLContext dsl, DecisionDeHabilitacion decision) {
        BitacoraDeHabilitacion.insertar(dsl, decision);
    }

    /** El organizador que nació de la solicitud de ese usuario, si ya existe. */
    public Optional<UUID> organizadorDelUsuario(DSLContext dsl, UUID usuarioId) {
        return Optional.ofNullable(dsl.fetchOne("SELECT id FROM organizador.organizador WHERE usuario_id=?", usuarioId))
                .map(f -> f.get(0, UUID.class));
    }

    private ExpedienteDeHabilitacion expediente(Record f) {
        OffsetDateTime resolucion = f.get("fecha_resolucion", OffsetDateTime.class);
        return new ExpedienteDeHabilitacion(
                f.get("id", UUID.class),
                f.get("usuario_id", UUID.class),
                f.get("estado", String.class),
                f.get("puntaje_reputacion_al_solicitar", BigDecimal.class),
                f.get("kyc_reforzado_id", UUID.class),
                f.get("motivo_rechazo", String.class),
                f.get("fecha_solicitud", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                resolucion == null ? null : resolucion.withOffsetSameInstant(ZoneOffset.UTC),
                f.get("revision", Integer.class));
    }

    private DecisionDeHabilitacion decision(Record f) {
        return new DecisionDeHabilitacion(
                f.get("id", UUID.class),
                f.get("solicitud_id", UUID.class),
                f.get("clave_idempotencia", UUID.class),
                f.get("fase", String.class),
                f.get("decision", String.class),
                f.get("actor_id", UUID.class),
                f.get("motivo", String.class),
                f.get("revision", Integer.class),
                f.get("organizador_id", UUID.class),
                f.get("evidencia_requisitos", String.class),
                f.get("ocurrida_en", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                f.get("correlacion_id", UUID.class));
    }
}
