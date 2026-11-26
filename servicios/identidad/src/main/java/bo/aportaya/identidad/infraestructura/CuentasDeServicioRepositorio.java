package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.CREDENCIAL_ACCESO;
import static bo.aportaya.identidad.generado.Tables.ROL;
import static bo.aportaya.identidad.generado.Tables.USUARIO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

/**
 * {@code identidad.cuenta_servicio}: el gobierno de las identidades de servicio (GU-18, CU-136).
 *
 * <p>Solo sentencias. Nunca toca ni devuelve una credencial en claro: de la clave solo sabe el hash que ya guarda
 * {@code credencial_acceso}, y eso lo escribe {@code CredencialRepositorio}.
 */
@Component
public class CuentasDeServicioRepositorio {

    private static final Table<?> CUENTA = DSL.table(DSL.name("identidad", "cuenta_servicio"));
    private static final Field<UUID> ID = DSL.field(DSL.name("id"), SQLDataType.UUID);
    private static final Field<UUID> USUARIO_ID = DSL.field(DSL.name("usuario_id"), SQLDataType.UUID);
    private static final Field<UUID> ROL_ID = DSL.field(DSL.name("rol_id"), SQLDataType.UUID);
    private static final Field<String> NOMBRE = DSL.field(DSL.name("nombre"), SQLDataType.VARCHAR);
    private static final Field<String> ESTADO = DSL.field(DSL.name("estado"), SQLDataType.VARCHAR);
    private static final Field<OffsetDateTime> VENCE_EN =
            DSL.field(DSL.name("vence_en"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<UUID> CREADA_POR = DSL.field(DSL.name("creada_por"), SQLDataType.UUID);
    private static final Field<OffsetDateTime> CREADA_EN =
            DSL.field(DSL.name("creada_en"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<OffsetDateTime> ROTADA_EN =
            DSL.field(DSL.name("credencial_rotada_en"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<UUID> REVOCADA_POR = DSL.field(DSL.name("revocada_por"), SQLDataType.UUID);
    private static final Field<OffsetDateTime> REVOCADA_EN =
            DSL.field(DSL.name("revocada_en"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<String> MOTIVO_REVOCACION =
            DSL.field(DSL.name("motivo_revocacion"), SQLDataType.VARCHAR);

    public record Fila(
            UUID id,
            UUID usuarioId,
            String rolCodigo,
            String nombre,
            String estado,
            OffsetDateTime venceEn,
            OffsetDateTime creadaEn,
            OffsetDateTime credencialRotadaEn,
            Optional<OffsetDateTime> revocadaEn) {}

    public Optional<UUID> rolIdDe(DSLContext dsl, String codigo) {
        return dsl.select(ROL.ID)
                .from(ROL)
                .where(ROL.CODIGO.eq(codigo))
                .and(ROL.RETIRADO_EN.isNull())
                .fetchOptional(ROL.ID);
    }

    public boolean existeUsuario(DSLContext dsl, UUID usuarioId) {
        return dsl.fetchExists(dsl.selectOne().from(USUARIO).where(USUARIO.ID.eq(usuarioId)));
    }

    public boolean registradaPara(DSLContext dsl, UUID usuarioId) {
        return dsl.fetchExists(dsl.selectOne().from(CUENTA).where(USUARIO_ID.eq(usuarioId)));
    }

    public UUID insertar(
            DSLContext dsl,
            UUID usuarioId,
            UUID rolId,
            String nombre,
            OffsetDateTime venceEn,
            UUID creadaPor,
            OffsetDateTime ahora) {
        return dsl.insertInto(CUENTA)
                .set(USUARIO_ID, usuarioId)
                .set(ROL_ID, rolId)
                .set(NOMBRE, nombre)
                .set(ESTADO, "ACTIVA")
                .set(VENCE_EN, venceEn)
                .set(CREADA_POR, creadaPor)
                .set(CREADA_EN, ahora)
                .set(ROTADA_EN, ahora)
                .returning(ID)
                .fetchOne()
                .get(ID);
    }

    public Optional<Fila> porId(DSLContext dsl, UUID id) {
        return consulta(dsl)
                .where(c("id", SQLDataType.UUID).eq(id))
                .fetchOptional()
                .map(CuentasDeServicioRepositorio::aFila);
    }

    public List<Fila> listar(DSLContext dsl, int limite) {
        return consulta(dsl)
                .orderBy(
                        c("creada_en", SQLDataType.TIMESTAMPWITHTIMEZONE).desc(),
                        c("id", SQLDataType.UUID).asc())
                .limit(limite)
                .fetch()
                .map(CuentasDeServicioRepositorio::aFila);
    }

    /** {@code true} solo si seguia ACTIVA: la precondicion va en la escritura. */
    public boolean rotar(DSLContext dsl, UUID id, OffsetDateTime venceEn, OffsetDateTime ahora) {
        return dsl.update(CUENTA)
                        .set(ROTADA_EN, ahora)
                        .set(VENCE_EN, venceEn)
                        .where(ID.eq(id))
                        .and(ESTADO.eq("ACTIVA"))
                        .execute()
                == 1;
    }

    public boolean revocar(DSLContext dsl, UUID id, UUID por, String motivo, OffsetDateTime ahora) {
        return dsl.update(CUENTA)
                        .set(ESTADO, "REVOCADA")
                        .set(REVOCADA_POR, por)
                        .set(REVOCADA_EN, ahora)
                        .set(MOTIVO_REVOCACION, motivo)
                        .where(ID.eq(id))
                        .and(ESTADO.eq("ACTIVA"))
                        .execute()
                == 1;
    }

    /** El hash vigente de la credencial, para pasarlo al historial antes de reemplazarlo. */
    public Optional<String> hashVigente(DSLContext dsl, UUID usuarioId) {
        return dsl.select(CREDENCIAL_ACCESO.HASH_CONTRASENA)
                .from(CREDENCIAL_ACCESO)
                .where(CREDENCIAL_ACCESO.USUARIO_ID.eq(usuarioId))
                .fetchOptional(CREDENCIAL_ACCESO.HASH_CONTRASENA);
    }

    /** Una cuenta revocada no vuelve a abrir sesion: el estado del usuario lo corta con el rechazo de CU-04. */
    public void suspenderUsuario(DSLContext dsl, UUID usuarioId) {
        dsl.update(USUARIO)
                .set(USUARIO.ESTADO, "SUSPENDIDO")
                .where(USUARIO.ID.eq(usuarioId))
                .and(USUARIO.ESTADO.eq("ACTIVO"))
                .execute();
    }

    public void vencerCredencial(DSLContext dsl, UUID usuarioId, OffsetDateTime ahora) {
        dsl.update(CREDENCIAL_ACCESO)
                .set(CREDENCIAL_ACCESO.EXPIRA_EN, ahora)
                .where(CREDENCIAL_ACCESO.USUARIO_ID.eq(usuarioId))
                .execute();
    }

    private static final Table<?> C = CUENTA.as("c");

    private static <T> Field<T> c(String columna, org.jooq.DataType<T> tipo) {
        return DSL.field(DSL.name("c", columna), tipo);
    }

    private static org.jooq.SelectOnConditionStep<? extends Record> consulta(DSLContext dsl) {
        return dsl.select(
                        c("id", SQLDataType.UUID),
                        c("usuario_id", SQLDataType.UUID),
                        ROL.CODIGO,
                        c("nombre", SQLDataType.VARCHAR),
                        c("estado", SQLDataType.VARCHAR),
                        c("vence_en", SQLDataType.TIMESTAMPWITHTIMEZONE),
                        c("creada_en", SQLDataType.TIMESTAMPWITHTIMEZONE),
                        c("credencial_rotada_en", SQLDataType.TIMESTAMPWITHTIMEZONE),
                        c("revocada_en", SQLDataType.TIMESTAMPWITHTIMEZONE))
                .from(C)
                .join(ROL)
                .on(c("rol_id", SQLDataType.UUID).eq(ROL.ID));
    }

    private static Fila aFila(Record r) {
        return new Fila(
                r.get(c("id", SQLDataType.UUID)),
                r.get(c("usuario_id", SQLDataType.UUID)),
                r.get(ROL.CODIGO),
                r.get(c("nombre", SQLDataType.VARCHAR)),
                r.get(c("estado", SQLDataType.VARCHAR)),
                r.get(c("vence_en", SQLDataType.TIMESTAMPWITHTIMEZONE)),
                r.get(c("creada_en", SQLDataType.TIMESTAMPWITHTIMEZONE)),
                r.get(c("credencial_rotada_en", SQLDataType.TIMESTAMPWITHTIMEZONE)),
                Optional.ofNullable(r.get(c("revocada_en", SQLDataType.TIMESTAMPWITHTIMEZONE))));
    }
}
