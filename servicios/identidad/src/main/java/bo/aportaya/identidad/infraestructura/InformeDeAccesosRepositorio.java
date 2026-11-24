package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.ASIGNACION_ROL;
import static bo.aportaya.identidad.generado.Tables.FACTOR_MFA;
import static bo.aportaya.identidad.generado.Tables.ROL;
import static bo.aportaya.identidad.generado.Tables.USUARIO;

import bo.aportaya.identidad.dominio.InformeDeAccesos.Fila;
import bo.aportaya.identidad.dominio.InformeDeAccesos.Seccion;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * El informe de accesos de CU-08 (GU-08, decision P-6): una fila por asignacion vigente. Solo lee; el orden es
 * determinista (desempate por id) para que la paginacion no repita ni pierda filas (regla 96.6.2).
 */
@Component
public class InformeDeAccesosRepositorio {

    public int contar(DSLContext dsl, Seccion seccion, OffsetDateTime ahora, OffsetDateTime horizonte) {
        return dsl.selectCount()
                .from(ASIGNACION_ROL)
                .join(ROL)
                .on(ROL.ID.eq(ASIGNACION_ROL.ROL_ID))
                .join(USUARIO)
                .on(USUARIO.ID.eq(ASIGNACION_ROL.USUARIO_ID))
                .where(condicion(seccion, ahora, horizonte))
                .fetchOne(0, int.class);
    }

    public List<Fila> pagina(
            DSLContext dsl, Seccion seccion, OffsetDateTime ahora, OffsetDateTime horizonte, int desde, int cuantas) {
        Field<Boolean> totp = DSL.when(totpConfirmado(), DSL.inline(true)).otherwise(DSL.inline(false));
        return dsl.select(
                        ASIGNACION_ROL.ID,
                        ASIGNACION_ROL.USUARIO_ID,
                        USUARIO.NOMBRES,
                        USUARIO.APELLIDOS,
                        USUARIO.ESTADO,
                        ROL.CODIGO,
                        ASIGNACION_ROL.AMBITO,
                        ASIGNACION_ROL.AMBITO_ID,
                        ASIGNACION_ROL.OTORGADA_POR,
                        ASIGNACION_ROL.OTORGADA_EN,
                        ASIGNACION_ROL.VIGENTE_HASTA,
                        totp)
                .from(ASIGNACION_ROL)
                .join(ROL)
                .on(ROL.ID.eq(ASIGNACION_ROL.ROL_ID))
                .join(USUARIO)
                .on(USUARIO.ID.eq(ASIGNACION_ROL.USUARIO_ID))
                .where(condicion(seccion, ahora, horizonte))
                .orderBy(orden(seccion))
                .limit(cuantas)
                .offset(desde)
                .fetch(fila -> aFila(fila, totp));
    }

    private static Condition condicion(Seccion seccion, OffsetDateTime ahora, OffsetDateTime horizonte) {
        Condition vigente = ASIGNACION_ROL
                .REVOCADA_EN
                .isNull()
                .and(ASIGNACION_ROL.OTORGADA_EN.le(ahora))
                .and(ASIGNACION_ROL.VIGENTE_HASTA.isNull().or(ASIGNACION_ROL.VIGENTE_HASTA.gt(ahora)));
        return switch (seccion) {
            case VIVOS -> vigente;
            case POR_VENCER -> vigente.and(ASIGNACION_ROL.VIGENTE_HASTA.le(horizonte));
            case SIN_TOTP -> vigente.and(ROL.AMBITO.eq("GLOBAL")).and(DSL.not(totpConfirmado()));
        };
    }

    private static List<SortField<?>> orden(Seccion seccion) {
        return seccion == Seccion.POR_VENCER
                ? List.of(ASIGNACION_ROL.VIGENTE_HASTA.asc(), ASIGNACION_ROL.ID.asc())
                : List.of(USUARIO.APELLIDOS.asc(), USUARIO.NOMBRES.asc(), ROL.CODIGO.asc(), ASIGNACION_ROL.ID.asc());
    }

    /** R-SEG-10: lo que abre sesion de operador es un TOTP activo y confirmado, nada menos. */
    private static Condition totpConfirmado() {
        return DSL.exists(DSL.selectOne()
                .from(FACTOR_MFA)
                .where(FACTOR_MFA.USUARIO_ID.eq(ASIGNACION_ROL.USUARIO_ID))
                .and(FACTOR_MFA.TIPO.eq("TOTP"))
                .and(FACTOR_MFA.ACTIVO.isTrue())
                .and(FACTOR_MFA.CONFIRMADO_EN.isNotNull()));
    }

    private static Fila aFila(Record fila, Field<Boolean> totp) {
        return new Fila(
                fila.get(ASIGNACION_ROL.ID),
                fila.get(ASIGNACION_ROL.USUARIO_ID),
                (fila.get(USUARIO.NOMBRES) + " " + fila.get(USUARIO.APELLIDOS)).strip(),
                fila.get(USUARIO.ESTADO),
                fila.get(ROL.CODIGO),
                fila.get(ASIGNACION_ROL.AMBITO),
                Optional.ofNullable(fila.get(ASIGNACION_ROL.AMBITO_ID)),
                fila.get(ASIGNACION_ROL.OTORGADA_POR),
                fila.get(ASIGNACION_ROL.OTORGADA_EN),
                Optional.ofNullable(fila.get(ASIGNACION_ROL.VIGENTE_HASTA)),
                Boolean.TRUE.equals(fila.get(totp)));
    }
}
