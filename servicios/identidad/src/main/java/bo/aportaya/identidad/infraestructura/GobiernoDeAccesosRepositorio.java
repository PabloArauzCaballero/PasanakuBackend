package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.ASIGNACION_ROL;
import static bo.aportaya.identidad.generado.Tables.ROL;
import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.USUARIO_INTERNO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectOnConditionStep;
import org.springframework.stereotype.Component;

/**
 * Las consultas del gobierno de accesos de CU-08 (GU-05, GU-08, GU-11, GU-14). Sin logica: deciden los
 * casos de uso. Separado de {@link AccesosRepositorio} por el limite de tamano de archivo.
 */
@Component
public class GobiernoDeAccesosRepositorio {

    /** Una asignacion tal como la necesita el gobierno: quien, que rol, en que ambito y si sigue viva. */
    public record Asignacion(
            UUID id,
            UUID usuarioId,
            UUID rolId,
            String rolCodigo,
            String ambito,
            Optional<UUID> ambitoId,
            Optional<OffsetDateTime> vigenteHasta,
            Optional<OffsetDateTime> revocadaEn) {

        /** Viva y no vencida a la fecha: la que cuenta en los permisos efectivos. */
        public boolean vigenteEn(OffsetDateTime fecha) {
            return revocadaEn.isEmpty() && vigenteHasta.map(fecha::isBefore).orElse(true);
        }
    }

    public Optional<Asignacion> asignacion(DSLContext dsl, UUID asignacionId) {
        return seleccion(dsl).where(ASIGNACION_ROL.ID.eq(asignacionId)).fetchOptional(this::aAsignacion);
    }

    /** La asignacion sin revocar con la misma clave que {@code uq_asignacion_vigente}, vencida o no. */
    public Optional<Asignacion> sinRevocarIgual(
            DSLContext dsl, UUID usuarioId, UUID rolId, String ambito, Optional<UUID> ambitoId) {
        return seleccion(dsl)
                .where(ASIGNACION_ROL.USUARIO_ID.eq(usuarioId))
                .and(ASIGNACION_ROL.ROL_ID.eq(rolId))
                .and(ASIGNACION_ROL.AMBITO.eq(ambito))
                .and(ambitoId.<Condition>map(id -> ASIGNACION_ROL.AMBITO_ID.eq(id))
                        .orElse(ASIGNACION_ROL.AMBITO_ID.isNull()))
                .and(ASIGNACION_ROL.REVOCADA_EN.isNull())
                .fetchOptional(this::aAsignacion);
    }

    /** Todas las asignaciones sin revocar del usuario, vencidas incluidas, en orden estable. */
    public List<Asignacion> sinRevocarDe(DSLContext dsl, UUID usuarioId) {
        return seleccion(dsl)
                .where(ASIGNACION_ROL.USUARIO_ID.eq(usuarioId))
                .and(ASIGNACION_ROL.REVOCADA_EN.isNull())
                .orderBy(ASIGNACION_ROL.OTORGADA_EN, ASIGNACION_ROL.ID)
                .fetch(this::aAsignacion);
    }

    /** {@code ESTADO} de la cuenta si el usuario tiene perfil interno; vacio si no es un operador. */
    public Optional<String> estadoDelOperador(DSLContext dsl, UUID usuarioId) {
        return dsl.select(USUARIO.ESTADO)
                .from(USUARIO)
                .join(USUARIO_INTERNO)
                .on(USUARIO_INTERNO.USUARIO_ID.eq(USUARIO.ID))
                .where(USUARIO.ID.eq(usuarioId))
                .fetchOptional(USUARIO.ESTADO);
    }

    /** El usuarioId a partir del id del usuario o del de su perfil interno (los dos viajan en la URL). */
    public Optional<UUID> operador(DSLContext dsl, UUID id) {
        return dsl.select(USUARIO.ID)
                .from(USUARIO)
                .join(USUARIO_INTERNO)
                .on(USUARIO_INTERNO.USUARIO_ID.eq(USUARIO.ID))
                .where(USUARIO.ID.eq(id).or(USUARIO_INTERNO.ID.eq(id)))
                .fetchOptional(USUARIO.ID);
    }

    public int cambiarEstado(DSLContext dsl, UUID usuarioId, String estado) {
        return dsl.update(USUARIO)
                .set(USUARIO.ESTADO, estado)
                .where(USUARIO.ID.eq(usuarioId))
                .execute();
    }

    /**
     * Vence en un paso las asignaciones cuyo {@code vigente_hasta} ya paso: escribe {@code revocada_en =
     * vigente_hasta} (el permiso efectivo a cualquier fecha pasada no cambia) y el motivo. {@code SKIP
     * LOCKED} reparte el lote entre replicas y la precondicion {@code revocada_en IS NULL} hace que una
     * segunda corrida no encuentre nada: la idempotencia es de la escritura, no de un lock.
     */
    public List<Asignacion> vencer(DSLContext dsl, String motivo, OffsetDateTime ahora, int lote) {
        List<UUID> ids = dsl.update(ASIGNACION_ROL)
                .set(ASIGNACION_ROL.REVOCADA_EN, ASIGNACION_ROL.VIGENTE_HASTA)
                .set(ASIGNACION_ROL.MOTIVO_REVOCACION, motivo)
                .where(ASIGNACION_ROL.ID.in(dsl.select(ASIGNACION_ROL.ID)
                        .from(ASIGNACION_ROL)
                        .where(ASIGNACION_ROL.REVOCADA_EN.isNull())
                        .and(ASIGNACION_ROL.VIGENTE_HASTA.le(ahora))
                        .orderBy(ASIGNACION_ROL.VIGENTE_HASTA, ASIGNACION_ROL.ID)
                        .limit(lote)
                        .forUpdate()
                        .skipLocked()))
                .and(ASIGNACION_ROL.REVOCADA_EN.isNull())
                .returning(ASIGNACION_ROL.ID)
                .fetch(ASIGNACION_ROL.ID);
        return ids.isEmpty()
                ? List.of()
                : seleccion(dsl)
                        .where(ASIGNACION_ROL.ID.in(ids))
                        .orderBy(ASIGNACION_ROL.VIGENTE_HASTA, ASIGNACION_ROL.ID)
                        .fetch(this::aAsignacion);
    }

    /** Los usuarios ACTIVOS con una asignacion vigente del rol: a quienes se avisa (P-5). */
    public List<UUID> titularesVigentesDelRol(DSLContext dsl, String codigoDeRol, OffsetDateTime ahora) {
        return dsl.selectDistinct(ASIGNACION_ROL.USUARIO_ID)
                .from(ASIGNACION_ROL)
                .join(ROL)
                .on(ROL.ID.eq(ASIGNACION_ROL.ROL_ID))
                .join(USUARIO)
                .on(USUARIO.ID.eq(ASIGNACION_ROL.USUARIO_ID))
                .where(ROL.CODIGO.eq(codigoDeRol))
                .and(USUARIO.ESTADO.eq("ACTIVO"))
                .and(ASIGNACION_ROL.REVOCADA_EN.isNull())
                .and(ASIGNACION_ROL.OTORGADA_EN.le(ahora))
                .and(ASIGNACION_ROL.VIGENTE_HASTA.isNull().or(ASIGNACION_ROL.VIGENTE_HASTA.gt(ahora)))
                .orderBy(ASIGNACION_ROL.USUARIO_ID)
                .fetch(ASIGNACION_ROL.USUARIO_ID);
    }

    private SelectOnConditionStep<? extends Record> seleccion(DSLContext dsl) {
        return dsl.select(
                        ASIGNACION_ROL.ID,
                        ASIGNACION_ROL.USUARIO_ID,
                        ASIGNACION_ROL.ROL_ID,
                        ROL.CODIGO,
                        ASIGNACION_ROL.AMBITO,
                        ASIGNACION_ROL.AMBITO_ID,
                        ASIGNACION_ROL.VIGENTE_HASTA,
                        ASIGNACION_ROL.REVOCADA_EN)
                .from(ASIGNACION_ROL)
                .join(ROL)
                .on(ROL.ID.eq(ASIGNACION_ROL.ROL_ID));
    }

    private Asignacion aAsignacion(Record fila) {
        return new Asignacion(
                fila.get(ASIGNACION_ROL.ID),
                fila.get(ASIGNACION_ROL.USUARIO_ID),
                fila.get(ASIGNACION_ROL.ROL_ID),
                fila.get(ROL.CODIGO),
                fila.get(ASIGNACION_ROL.AMBITO),
                Optional.ofNullable(fila.get(ASIGNACION_ROL.AMBITO_ID)),
                Optional.ofNullable(fila.get(ASIGNACION_ROL.VIGENTE_HASTA)),
                Optional.ofNullable(fila.get(ASIGNACION_ROL.REVOCADA_EN)));
    }
}
