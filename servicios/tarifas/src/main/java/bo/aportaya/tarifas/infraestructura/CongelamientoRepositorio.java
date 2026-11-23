package bo.aportaya.tarifas.infraestructura;

import static bo.aportaya.tarifas.generado.Tables.PARAMETRO_CONGELADO_GRUPO;
import static bo.aportaya.tarifas.generado.Tables.PARAMETRO_FINANCIERO;
import static bo.aportaya.tarifas.generado.Tables.VERSION_PARAMETRO;

import bo.aportaya.tarifas.dominio.VersionDelParametro;
import bo.aportaya.tarifas.dominio.VigenciaDeParametros.ParametroCongelado;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * La copia de los parámetros de un grupo ({@code parametro_congelado_grupo}, R-TAR-17). Solo se inserta: la tabla es
 * de solo agregado y la unicidad por grupo, parámetro y nivel la decide la base.
 */
@Component
public class CongelamientoRepositorio {

    private static final Table<?> NIVELES_CONGELADOS = DSL.table(DSL.name("tarifas", "nivel_aplicable_congelado_grupo"));
    private static final Field<UUID> NIVEL_GRUPO_ID = DSL.field(DSL.name("grupo_id"), UUID.class);
    private static final Field<String> NIVEL_CODIGO = DSL.field(DSL.name("nivel_codigo"), String.class);
    private static final Field<Short> NIVEL_ORDEN = DSL.field(DSL.name("orden"), Short.class);
    private static final Field<OffsetDateTime> NIVEL_CONGELADO_EN = DSL.field(DSL.name("congelado_en"), OffsetDateTime.class);

    /**
     * Fija una única lista de niveles por grupo. El lock transaccional evita que dos entregas simultáneas
     * mezclen catálogos distintos; una reemisión posterior conserva la primera lista.
     */
    public List<String> fijarNivelesAplicables(DSLContext dsl, UUID grupoId, List<String> propuestos, OffsetDateTime en) {
        dsl.fetch("select pg_advisory_xact_lock(hashtextextended(cast(? as text), 0))", grupoId);
        boolean yaExiste = dsl.fetchExists(dsl.selectOne().from(NIVELES_CONGELADOS)
                .where(NIVEL_GRUPO_ID.eq(grupoId)));
        if (!yaExiste) {
            for (int i = 0; i < propuestos.size(); i++) {
                dsl.insertInto(NIVELES_CONGELADOS)
                        .set(NIVEL_GRUPO_ID, grupoId)
                        .set(NIVEL_CODIGO, propuestos.get(i))
                        .set(NIVEL_ORDEN, (short) i)
                        .set(NIVEL_CONGELADO_EN, en)
                        .onConflict(NIVEL_GRUPO_ID, NIVEL_CODIGO)
                        .doNothing()
                        .execute();
            }
        }
        return dsl.select(NIVEL_CODIGO).from(NIVELES_CONGELADOS)
                .where(NIVEL_GRUPO_ID.eq(grupoId)).orderBy(NIVEL_ORDEN)
                .fetch(r -> r.get(NIVEL_CODIGO));
    }

    public List<String> nivelesAplicablesDelGrupo(DSLContext dsl, UUID grupoId) {
        return dsl.select(NIVEL_CODIGO).from(NIVELES_CONGELADOS)
                .where(NIVEL_GRUPO_ID.eq(grupoId)).orderBy(NIVEL_ORDEN)
                .fetch(r -> r.get(NIVEL_CODIGO));
    }

    /** Congela cada versión para el grupo; lo que ya estaba congelado no se toca. Devuelve cuántas filas nuevas. */
    public int congelar(DSLContext dsl, UUID grupoId, List<VersionDelParametro> vigentes, OffsetDateTime en) {
        int nuevas = 0;
        for (VersionDelParametro v : vigentes) {
            nuevas += dsl.insertInto(PARAMETRO_CONGELADO_GRUPO)
                    .set(PARAMETRO_CONGELADO_GRUPO.GRUPO_ID, grupoId)
                    .set(PARAMETRO_CONGELADO_GRUPO.PARAMETRO_ID, v.parametroId())
                    .set(PARAMETRO_CONGELADO_GRUPO.VERSION_PARAMETRO_ID, v.id())
                    .set(PARAMETRO_CONGELADO_GRUPO.NIVEL_CODIGO, v.nivel().orElse(null))
                    .set(PARAMETRO_CONGELADO_GRUPO.CONGELADO_EN, en)
                    // Sin objetivo: el índice único es de expresión (COALESCE del nivel).
                    .onConflictDoNothing()
                    .execute();
        }
        return nuevas;
    }

    /** Lo congelado del grupo, ordenado por código y nivel. Vacío si el grupo no tiene congelamiento. */
    public List<ParametroCongelado> delGrupo(DSLContext dsl, UUID grupoId) {
        return dsl.select(
                        PARAMETRO_CONGELADO_GRUPO.PARAMETRO_ID,
                        PARAMETRO_FINANCIERO.CODIGO,
                        PARAMETRO_CONGELADO_GRUPO.NIVEL_CODIGO,
                        VERSION_PARAMETRO.VALOR,
                        PARAMETRO_FINANCIERO.UNIDAD,
                        VERSION_PARAMETRO.MONEDA,
                        VERSION_PARAMETRO.ID,
                        PARAMETRO_CONGELADO_GRUPO.CONGELADO_EN)
                .from(PARAMETRO_CONGELADO_GRUPO)
                .join(VERSION_PARAMETRO)
                .on(VERSION_PARAMETRO.ID.eq(PARAMETRO_CONGELADO_GRUPO.VERSION_PARAMETRO_ID))
                .join(PARAMETRO_FINANCIERO)
                .on(PARAMETRO_FINANCIERO.ID.eq(PARAMETRO_CONGELADO_GRUPO.PARAMETRO_ID))
                .where(PARAMETRO_CONGELADO_GRUPO.GRUPO_ID.eq(grupoId))
                .orderBy(
                        PARAMETRO_FINANCIERO.CODIGO,
                        PARAMETRO_CONGELADO_GRUPO.NIVEL_CODIGO.asc().nullsFirst())
                .fetch(r -> new ParametroCongelado(
                        r.get(PARAMETRO_CONGELADO_GRUPO.PARAMETRO_ID),
                        r.get(PARAMETRO_FINANCIERO.CODIGO),
                        Optional.ofNullable(r.get(PARAMETRO_CONGELADO_GRUPO.NIVEL_CODIGO)),
                        r.get(VERSION_PARAMETRO.VALOR),
                        Optional.ofNullable(r.get(PARAMETRO_FINANCIERO.UNIDAD)),
                        Optional.ofNullable(r.get(VERSION_PARAMETRO.MONEDA)),
                        r.get(VERSION_PARAMETRO.ID),
                        r.get(PARAMETRO_CONGELADO_GRUPO.CONGELADO_EN)));
    }
}
