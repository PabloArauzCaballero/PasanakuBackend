package bo.aportaya.garantia.infraestructura;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

/**
 * {@code plan_contingencia}: la contencion de una falta extraordinaria de caja.
 *
 * <p>Una contingencia VIVA (PROPUESTO o APROBADO) por grupo y disparador la exige la base (R-GAR-13,
 * {@code uq_plan_contingencia_vivo}): un segundo intento no abre un segundo plan ni escala dos veces.
 * Aca la contencion en vigor se guarda como APROBADO (la aprueba la politica, no una persona) y al
 * cubrirse el faltante pasa a EJECUTADO.
 */
@Component
public class PlanDeContingenciaRepositorio {

    private static final String DISPARADOR = "FONDO_AGOTADO";

    /** El id del plan nuevo, o vacio si ese grupo ya tenia uno vivo. */
    public Optional<UUID> abrir(
            DSLContext dsl, UUID grupoId, String descripcion, BigDecimal impactoEstimado, OffsetDateTime ahora) {
        return dsl.fetchOptional(
                        """
                        INSERT INTO garantia.plan_contingencia
                            (id, grupo_id, disparador, tipo, descripcion, impacto_estimado,
                             requiere_acuerdo, estado, propuesto_en)
                        VALUES (?::uuid, ?::uuid, 'FONDO_AGOTADO', 'EXTENSION_DE_PLAZO', ?::text, ?::numeric, false, 'APROBADO', ?::timestamptz)
                        ON CONFLICT DO NOTHING
                        RETURNING id
                        """,
                        DSL.val(UUID.randomUUID(), SQLDataType.UUID),
                        DSL.val(grupoId, SQLDataType.UUID),
                        DSL.val(descripcion, SQLDataType.CLOB),
                        DSL.val(impactoEstimado, SQLDataType.NUMERIC(14, 2)),
                        DSL.val(ahora, SQLDataType.TIMESTAMPWITHTIMEZONE))
                .map(r -> r.get(0, UUID.class));
    }

    /** Cierra la contingencia viva del grupo. Devuelve su id, o vacio si no habia ninguna. */
    public Optional<UUID> resolver(DSLContext dsl, UUID grupoId, OffsetDateTime ahora) {
        return dsl.fetchOptional(
                        """
                        UPDATE garantia.plan_contingencia
                           SET estado = 'EJECUTADO', ejecutado_en = ?::timestamptz
                         WHERE grupo_id = ?::uuid AND disparador = 'FONDO_AGOTADO' AND estado IN ('PROPUESTO', 'APROBADO')
                        RETURNING id
                        """,
                        DSL.val(ahora, SQLDataType.TIMESTAMPWITHTIMEZONE),
                        DSL.val(grupoId, SQLDataType.UUID))
                .map(r -> r.get(0, UUID.class));
    }

    /** ¿Hay alguna contingencia de caja viva, de cualquier grupo? Mientras la haya no se prometen ciclos nuevos. */
    public boolean hayAlguna(DSLContext dsl) {
        return dsl.fetchExists(
                DSL.table(DSL.name("garantia", "plan_contingencia")),
                DSL.field("disparador").eq(DISPARADOR).and(DSL.field("estado").in("PROPUESTO", "APROBADO")));
    }
}
