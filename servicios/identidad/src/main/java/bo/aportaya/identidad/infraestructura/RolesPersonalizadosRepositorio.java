package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.ASIGNACION_ROL;
import static bo.aportaya.identidad.generado.Tables.PERMISO;
import static bo.aportaya.identidad.generado.Tables.ROL;
import static bo.aportaya.identidad.generado.Tables.ROL_PERMISO;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

/**
 * Escritura de los roles personalizados y de sus solicitudes a cuatro ojos (GU-16, CU-135).
 *
 * <p>No decide nada: cada metodo es una sentencia. Las reglas viven en {@code ReglasDeRolPersonalizado} y en el caso
 * de uso. Un rol {@code es_sistema} no se toca desde aca: el caso de uso lo corta antes con {@code AP-CU135-06}, y los
 * {@code UPDATE} llevan {@code es_sistema = false} en el {@code WHERE} por si alguien se olvida (precondicion en la
 * escritura, regla 96.3.2).
 */
@Component
public class RolesPersonalizadosRepositorio {

    private static final Table<?> SOLICITUD = DSL.table(DSL.name("identidad", "solicitud_cambio_rol"));
    private static final Field<UUID> S_ID = DSL.field(DSL.name("id"), SQLDataType.UUID);
    private static final Field<String> S_TIPO = DSL.field(DSL.name("tipo"), SQLDataType.VARCHAR);
    private static final Field<UUID> S_ROL = DSL.field(DSL.name("rol_id"), SQLDataType.UUID);
    private static final Field<JSONB> S_PROPUESTA = DSL.field(DSL.name("propuesta"), SQLDataType.JSONB);
    private static final Field<String> S_MOTIVO = DSL.field(DSL.name("motivo"), SQLDataType.VARCHAR);
    private static final Field<String> S_ESTADO = DSL.field(DSL.name("estado"), SQLDataType.VARCHAR);
    private static final Field<UUID> S_PROPUESTA_POR = DSL.field(DSL.name("propuesta_por"), SQLDataType.UUID);
    private static final Field<OffsetDateTime> S_PROPUESTA_EN =
            DSL.field(DSL.name("propuesta_en"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<UUID> S_RESUELTA_POR = DSL.field(DSL.name("resuelta_por"), SQLDataType.UUID);
    private static final Field<OffsetDateTime> S_RESUELTA_EN =
            DSL.field(DSL.name("resuelta_en"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<String> S_MOTIVO_RESOLUCION =
            DSL.field(DSL.name("motivo_resolucion"), SQLDataType.VARCHAR);

    public record FilaDeRol(
            UUID id, String codigo, String nombre, String descripcion, boolean esSistema, boolean retirado) {}

    public record FilaDeSolicitud(
            UUID id,
            String tipo,
            Optional<UUID> rolId,
            String propuestaJson,
            String motivo,
            String estado,
            UUID propuestaPor,
            OffsetDateTime propuestaEn) {}

    public Optional<FilaDeRol> rol(DSLContext dsl, UUID id) {
        return dsl.select(ROL.ID, ROL.CODIGO, ROL.NOMBRE, ROL.DESCRIPCION, ROL.ES_SISTEMA, ROL.RETIRADO_EN)
                .from(ROL)
                .where(ROL.ID.eq(id))
                .fetchOptional()
                .map(r -> new FilaDeRol(
                        r.get(ROL.ID),
                        r.get(ROL.CODIGO),
                        r.get(ROL.NOMBRE),
                        r.get(ROL.DESCRIPCION),
                        Boolean.TRUE.equals(r.get(ROL.ES_SISTEMA)),
                        r.get(ROL.RETIRADO_EN) != null));
    }

    public boolean existeCodigo(DSLContext dsl, String codigo) {
        return dsl.fetchExists(dsl.selectOne().from(ROL).where(ROL.CODIGO.eq(codigo)));
    }

    /** Codigo del permiso → nivel de riesgo, solo de los que existen: los que faltan no aparecen. */
    public Map<String, String> riesgoDe(DSLContext dsl, Collection<String> codigos) {
        Map<String, String> riesgo = new HashMap<>();
        dsl.select(PERMISO.CODIGO, PERMISO.NIVEL_RIESGO)
                .from(PERMISO)
                .where(PERMISO.CODIGO.in(codigos))
                .forEach(r -> riesgo.put(r.value1(), r.value2()));
        return riesgo;
    }

    public UUID insertarRol(
            DSLContext dsl, String codigo, String nombre, String descripcion, UUID creadoPor, OffsetDateTime ahora) {
        return dsl.insertInto(ROL)
                .set(ROL.CODIGO, codigo)
                .set(ROL.NOMBRE, nombre)
                .set(ROL.AMBITO, "GLOBAL")
                .set(ROL.DESCRIPCION, descripcion)
                .set(ROL.ES_SISTEMA, false)
                .set(ROL.CREADO_POR, creadoPor)
                .set(ROL.CREADO_EN, ahora)
                .returning(ROL.ID)
                .fetchOne()
                .getId();
    }

    /** {@code true} si cambio una fila: un rol de sistema o retirado no se edita. */
    public boolean actualizarRol(DSLContext dsl, UUID id, String nombre, String descripcion) {
        return dsl.update(ROL)
                        .set(ROL.NOMBRE, nombre)
                        .set(ROL.DESCRIPCION, descripcion)
                        .where(ROL.ID.eq(id))
                        .and(ROL.ES_SISTEMA.isFalse())
                        .and(ROL.RETIRADO_EN.isNull())
                        .execute()
                == 1;
    }

    public boolean retirar(DSLContext dsl, UUID id, OffsetDateTime ahora) {
        return dsl.update(ROL)
                        .set(ROL.RETIRADO_EN, ahora)
                        .where(ROL.ID.eq(id))
                        .and(ROL.ES_SISTEMA.isFalse())
                        .and(ROL.RETIRADO_EN.isNull())
                        .execute()
                == 1;
    }

    public void agregarPermisos(DSLContext dsl, UUID rolId, Collection<String> codigos) {
        if (codigos.isEmpty()) {
            return;
        }
        dsl.insertInto(ROL_PERMISO, ROL_PERMISO.ROL_ID, ROL_PERMISO.PERMISO_ID)
                .select(dsl.select(DSL.val(rolId), PERMISO.ID).from(PERMISO).where(PERMISO.CODIGO.in(codigos)))
                .onConflictDoNothing()
                .execute();
    }

    public void quitarPermisos(DSLContext dsl, UUID rolId, Collection<String> codigos) {
        if (codigos.isEmpty()) {
            return;
        }
        dsl.deleteFrom(ROL_PERMISO)
                .where(ROL_PERMISO.ROL_ID.eq(rolId))
                .and(ROL_PERMISO.PERMISO_ID.in(
                        dsl.select(PERMISO.ID).from(PERMISO).where(PERMISO.CODIGO.in(codigos))))
                .execute();
    }

    /** Quienes tienen hoy una asignacion vigente de este rol. */
    public List<UUID> titularesVigentes(DSLContext dsl, UUID rolId, OffsetDateTime ahora) {
        return dsl.selectDistinct(ASIGNACION_ROL.USUARIO_ID)
                .from(ASIGNACION_ROL)
                .where(ASIGNACION_ROL.ROL_ID.eq(rolId))
                .and(ASIGNACION_ROL.REVOCADA_EN.isNull())
                .and(ASIGNACION_ROL.VIGENTE_HASTA.isNull().or(ASIGNACION_ROL.VIGENTE_HASTA.gt(ahora)))
                .fetch(ASIGNACION_ROL.USUARIO_ID);
    }

    // ---- solicitudes a cuatro ojos ----

    public UUID insertarSolicitud(
            DSLContext dsl,
            String tipo,
            UUID rolId,
            String propuestaJson,
            String motivo,
            UUID propuestaPor,
            OffsetDateTime ahora) {
        return dsl.insertInto(SOLICITUD)
                .set(S_TIPO, tipo)
                .set(S_ROL, rolId)
                .set(S_PROPUESTA, JSONB.valueOf(propuestaJson))
                .set(S_MOTIVO, motivo)
                .set(S_ESTADO, "PENDIENTE")
                .set(S_PROPUESTA_POR, propuestaPor)
                .set(S_PROPUESTA_EN, ahora)
                .returning(S_ID)
                .fetchOne()
                .get(S_ID);
    }

    public Optional<FilaDeSolicitud> solicitud(DSLContext dsl, UUID id) {
        return dsl.select(S_ID, S_TIPO, S_ROL, S_PROPUESTA, S_MOTIVO, S_ESTADO, S_PROPUESTA_POR, S_PROPUESTA_EN)
                .from(SOLICITUD)
                .where(S_ID.eq(id))
                .fetchOptional()
                .map(r -> new FilaDeSolicitud(
                        r.get(S_ID),
                        r.get(S_TIPO),
                        Optional.ofNullable(r.get(S_ROL)),
                        r.get(S_PROPUESTA).data(),
                        r.get(S_MOTIVO),
                        r.get(S_ESTADO),
                        r.get(S_PROPUESTA_POR),
                        r.get(S_PROPUESTA_EN)));
    }

    public List<FilaDeSolicitud> solicitudesPendientes(DSLContext dsl, int limite) {
        return dsl.select(S_ID, S_TIPO, S_ROL, S_PROPUESTA, S_MOTIVO, S_ESTADO, S_PROPUESTA_POR, S_PROPUESTA_EN)
                .from(SOLICITUD)
                .where(S_ESTADO.eq("PENDIENTE"))
                .orderBy(S_PROPUESTA_EN.asc(), S_ID.asc())
                .limit(limite)
                .fetch()
                .map(r -> new FilaDeSolicitud(
                        r.get(S_ID),
                        r.get(S_TIPO),
                        Optional.ofNullable(r.get(S_ROL)),
                        r.get(S_PROPUESTA).data(),
                        r.get(S_MOTIVO),
                        r.get(S_ESTADO),
                        r.get(S_PROPUESTA_POR),
                        r.get(S_PROPUESTA_EN)));
    }

    /** {@code true} solo si seguia PENDIENTE: la precondicion va en la escritura (dos resolutores, uno gana). */
    public boolean resolver(
            DSLContext dsl, UUID id, String estado, UUID resueltaPor, String motivo, OffsetDateTime ahora) {
        return dsl.update(SOLICITUD)
                        .set(S_ESTADO, estado)
                        .set(S_RESUELTA_POR, resueltaPor)
                        .set(S_RESUELTA_EN, ahora)
                        .set(S_MOTIVO_RESOLUCION, motivo)
                        .where(S_ID.eq(id))
                        .and(S_ESTADO.eq("PENDIENTE"))
                        .execute()
                == 1;
    }

    /** Los permisos del conjunto que no existen en el catalogo. */
    public Set<String> inexistentes(DSLContext dsl, Collection<String> codigos) {
        Set<String> faltan = new java.util.TreeSet<>(codigos);
        faltan.removeAll(riesgoDe(dsl, codigos).keySet());
        return faltan;
    }
}
