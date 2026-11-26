package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.ASIGNACION_ROL;
import static bo.aportaya.identidad.generado.Tables.BLOQUEO_CUENTA;
import static bo.aportaya.identidad.generado.Tables.FACTOR_MFA;
import static bo.aportaya.identidad.generado.Tables.ROL;
import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.USUARIO_INTERNO;

import bo.aportaya.identidad.dominio.PermisosEfectivos;
import bo.aportaya.identidad.infraestructura.UsuarioInternoMapeador.BloqueoVigente;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoDetalle;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoResumen;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectField;
import org.jooq.impl.DSL;

/**
 * Las lecturas de personal interno: listado paginado y ficha.
 *
 * <p>El bloqueo que se muestra es el de {@code bloqueo_cuenta}, el mismo que lee el ingreso (GU-10): la columna
 * vieja {@code usuario_interno.bloqueado_hasta} no la escribe nadie y mostraba «sin bloqueo» a quien no podia
 * entrar. El segundo factor cuenta solo si esta activo y confirmado, como en {@code AccesoRepositorio.factorActivo}
 * (GU-17): un enrolamiento a medias no abre sesion y no puede figurar como hecho.
 */
final class LecturaDeUsuariosInternos {

    private static final List<SelectField<?>> COLUMNAS = List.of(
            USUARIO_INTERNO.ID,
            USUARIO_INTERNO.USUARIO_ID,
            USUARIO.CODIGO_PUBLICO,
            USUARIO.NOMBRES,
            USUARIO.APELLIDOS,
            USUARIO.CORREO,
            USUARIO.TELEFONO_E164,
            USUARIO_INTERNO.DEPARTAMENTO,
            USUARIO_INTERNO.CARGO,
            USUARIO.ESTADO,
            USUARIO_INTERNO.DEBE_CAMBIAR_CONTRASENA,
            USUARIO.FECHA_REGISTRO,
            USUARIO.FECHA_NACIMIENTO);

    private final AccesosRepositorio accesos;

    LecturaDeUsuariosInternos(AccesosRepositorio accesos) {
        this.accesos = accesos;
    }

    /** Una pagina, en orden {@code (fecha_registro, id)} descendente: determinista aunque dos altas coincidan. */
    List<UsuarioInternoResumen> listar(DSLContext dsl, FiltroDeUsuariosInternos filtro, OffsetDateTime ahora) {
        var filas = dsl.select(COLUMNAS)
                .from(USUARIO_INTERNO)
                .join(USUARIO)
                .on(USUARIO.ID.eq(USUARIO_INTERNO.USUARIO_ID))
                .where(condicion(filtro, ahora))
                .orderBy(USUARIO.FECHA_REGISTRO.desc(), USUARIO_INTERNO.ID.desc())
                .limit(filtro.limite())
                .fetch();

        List<UsuarioInternoResumen> resultado = new ArrayList<>();
        for (Record r : filas) {
            UUID usuarioId = r.get(USUARIO_INTERNO.USUARIO_ID);
            resultado.add(UsuarioInternoMapeador.aResumen(
                    r,
                    usuarioId,
                    accesos.codigosDeRolesVigentes(dsl, usuarioId, ahora),
                    tieneSegundoFactor(dsl, usuarioId),
                    bloqueoVigente(dsl, usuarioId, ahora)));
        }
        return resultado;
    }

    Optional<UsuarioInternoDetalle> consultar(DSLContext dsl, UUID id, OffsetDateTime ahora) {
        Record r = dsl.select(COLUMNAS)
                .from(USUARIO_INTERNO)
                .join(USUARIO)
                .on(USUARIO.ID.eq(USUARIO_INTERNO.USUARIO_ID))
                .where(USUARIO.ID.eq(id).or(USUARIO_INTERNO.ID.eq(id)))
                .fetchOne();
        if (r == null) {
            return Optional.empty();
        }

        UUID usuarioId = r.get(USUARIO_INTERNO.USUARIO_ID);
        UsuarioInternoDetalle detalle = UsuarioInternoMapeador.aDetalle(
                r,
                usuarioId,
                accesos.codigosDeRolesVigentes(dsl, usuarioId, ahora),
                PermisosEfectivos.de(accesos.asignacionesDe(dsl, usuarioId), ahora),
                tieneSegundoFactor(dsl, usuarioId),
                bloqueoVigente(dsl, usuarioId, ahora));
        detalle.setActualizadoEn(ahora);
        return Optional.of(detalle);
    }

    private static Condition condicion(FiltroDeUsuariosInternos filtro, OffsetDateTime ahora) {
        Condition condicion = DSL.noCondition();
        if (filtro.estado() != null && !filtro.estado().isBlank()) {
            condicion = condicion.and(USUARIO.ESTADO.eq(filtro.estado()));
        }
        if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
            String patron = "%" + filtro.busqueda().trim().toLowerCase(Locale.ROOT) + "%";
            condicion = condicion.and(DSL.lower(USUARIO.NOMBRES)
                    .like(patron)
                    .or(DSL.lower(USUARIO.APELLIDOS).like(patron))
                    .or(DSL.lower(USUARIO.CORREO).like(patron)));
        }
        // El filtro de rol va en la consulta y no despues: filtrar en memoria una pagina ya cortada devuelve
        // paginas cortas en el medio y el cliente las toma por la ultima.
        if (filtro.rol() != null && !filtro.rol().isBlank()) {
            condicion = condicion.and(DSL.exists(DSL.selectOne()
                    .from(ASIGNACION_ROL)
                    .join(ROL)
                    .on(ROL.ID.eq(ASIGNACION_ROL.ROL_ID))
                    .where(ASIGNACION_ROL.USUARIO_ID.eq(USUARIO_INTERNO.USUARIO_ID))
                    .and(ROL.CODIGO.eq(filtro.rol()))
                    .and(ASIGNACION_ROL.OTORGADA_EN.le(ahora))
                    .and(ASIGNACION_ROL.REVOCADA_EN.isNull())
                    .and(ASIGNACION_ROL.VIGENTE_HASTA.isNull().or(ASIGNACION_ROL.VIGENTE_HASTA.ge(ahora)))));
        }
        if (filtro.antesDe() != null) {
            condicion = condicion.and(
                    DSL.row(USUARIO.FECHA_REGISTRO, USUARIO_INTERNO.ID).lt(filtro.antesDe(), filtro.antesDeId()));
        }
        return condicion;
    }

    private static boolean tieneSegundoFactor(DSLContext dsl, UUID usuarioId) {
        return dsl.fetchExists(dsl.selectOne()
                .from(FACTOR_MFA)
                .where(FACTOR_MFA.USUARIO_ID.eq(usuarioId))
                .and(FACTOR_MFA.ACTIVO.isTrue())
                .and(FACTOR_MFA.CONFIRMADO_EN.isNotNull()));
    }

    /**
     * El bloqueo vigente con la misma condicion que el ingreso ({@code UsuarioRepositorio}): sin liberar y sin
     * vencer. Si hay varios, manda el indefinido y despues el que termina mas tarde.
     */
    private static Optional<BloqueoVigente> bloqueoVigente(DSLContext dsl, UUID usuarioId, OffsetDateTime ahora) {
        return dsl.select(BLOQUEO_CUENTA.DESBLOQUEA_EN, BLOQUEO_CUENTA.MOTIVO)
                .from(BLOQUEO_CUENTA)
                .where(BLOQUEO_CUENTA.USUARIO_ID.eq(usuarioId))
                .and(BLOQUEO_CUENTA.LIBERADA_EN.isNull())
                .and(BLOQUEO_CUENTA.DESBLOQUEA_EN.isNull().or(BLOQUEO_CUENTA.DESBLOQUEA_EN.gt(ahora)))
                .orderBy(BLOQUEO_CUENTA.DESBLOQUEA_EN.desc().nullsFirst(), BLOQUEO_CUENTA.BLOQUEADA_EN.desc())
                .limit(1)
                .fetchOptional(r -> new BloqueoVigente(r.value1(), r.value2()));
    }
}
