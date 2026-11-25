package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.BLOQUEO_CUENTA;
import static bo.aportaya.identidad.generado.Tables.EXPEDIENTE_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.RESTRICCION_USUARIO;
import static bo.aportaya.identidad.generado.Tables.USUARIO;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Directorio y ficha del participante (CU-134), y el rastro de cada lectura en
 * {@code comun.registro_acceso_datos} (R-SEG-02). Sin logica: enmascarar es de {@code Enmascarado}.
 */
@Component
public class ParticipantesRepositorio {

    /** Una fila del directorio: lo minimo para elegir a quien abrir, sin contacto. */
    public record Fila(
            UUID usuarioId,
            String codigoPublico,
            String nombres,
            String apellidos,
            String estado,
            String nivelKyc,
            OffsetDateTime fechaRegistro,
            boolean tieneRestriccion,
            boolean tieneBloqueo,
            boolean tieneMedidaEnCurso) {}

    /** Los datos de la ficha antes de enmascarar. El valor plano no sale del organismo. */
    public record Ficha(Fila resumen, OffsetDateTime ultimoAcceso, String telefonoE164, String correo) {}

    public List<Fila> buscar(
            DSLContext dsl,
            Optional<String> codigoPublico,
            Optional<String> telefonoE164,
            Optional<String> estado,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite,
            OffsetDateTime ahora) {
        Condition donde = DSL.noCondition();
        if (codigoPublico.isPresent()) {
            donde = donde.and(USUARIO.CODIGO_PUBLICO.eq(codigoPublico.get()));
        }
        if (telefonoE164.isPresent()) {
            donde = donde.and(USUARIO.TELEFONO_E164.eq(telefonoE164.get()));
        }
        if (estado.isPresent()) {
            donde = donde.and(USUARIO.ESTADO.eq(estado.get()));
        }
        if (antesDe.isPresent() && antesDeId.isPresent()) {
            donde = donde.and(DSL.row(USUARIO.FECHA_REGISTRO, USUARIO.ID).lt(antesDe.get(), antesDeId.get()));
        }
        List<Fila> filas = new ArrayList<>();
        for (Record r : dsl.select(columnas(ahora))
                .from(USUARIO)
                .where(donde)
                .and(USUARIO.ELIMINADO_EN.isNull())
                .orderBy(USUARIO.FECHA_REGISTRO.desc(), USUARIO.ID.desc())
                .limit(limite)
                .fetch()) {
            filas.add(fila(r));
        }
        return filas;
    }

    public Optional<Ficha> ficha(DSLContext dsl, UUID usuarioId, OffsetDateTime ahora) {
        var campos = new ArrayList<>(List.of(columnas(ahora)));
        campos.add(USUARIO.ULTIMO_ACCESO_EN);
        campos.add(USUARIO.TELEFONO_E164);
        campos.add(USUARIO.CORREO);
        return dsl.select(campos)
                .from(USUARIO)
                .where(USUARIO.ID.eq(usuarioId))
                .fetchOptional()
                .map(r -> new Ficha(
                        fila(r), r.get(USUARIO.ULTIMO_ACCESO_EN), r.get(USUARIO.TELEFONO_E164), r.get(USUARIO.CORREO)));
    }

    /**
     * Rastro de lectura (R-SEG-02, regla 90.2.7). Va en la misma transaccion que la lectura: si no se
     * puede escribir, la lectura falla y no se muestra nada.
     */
    public void registrarLectura(
            DSLContext dsl,
            UUID consultor,
            UUID afectado,
            String tipoDato,
            String operacion,
            String justificacion,
            String ticketSoporteId,
            int cantidad,
            String ip,
            OffsetDateTime ahora) {
        dsl.insertInto(DSL.table(DSL.name("comun", "registro_acceso_datos")))
                .set(DSL.field(DSL.name("usuario_consultor_id"), UUID.class), consultor)
                .set(DSL.field(DSL.name("usuario_afectado_id"), UUID.class), afectado)
                .set(DSL.field(DSL.name("tipo_dato"), String.class), tipoDato)
                .set(DSL.field(DSL.name("operacion"), String.class), operacion)
                .set(DSL.field(DSL.name("justificacion"), String.class), justificacion)
                .set(DSL.field(DSL.name("ticket_soporte_id"), String.class), ticketSoporteId)
                .set(DSL.field(DSL.name("cantidad_registros"), Integer.class), cantidad)
                .set(
                        DSL.field(DSL.name("ip_origen"), String.class),
                        DSL.field("cast({0} as inet)", String.class, DSL.val(ip)))
                .set(DSL.field(DSL.name("fecha_hora"), OffsetDateTime.class), ahora)
                .execute();
    }

    private static org.jooq.SelectField<?>[] columnas(OffsetDateTime ahora) {
        return new org.jooq.SelectField<?>[] {
            USUARIO.ID,
            USUARIO.CODIGO_PUBLICO,
            USUARIO.NOMBRES,
            USUARIO.APELLIDOS,
            USUARIO.ESTADO,
            USUARIO.NIVEL_KYC,
            USUARIO.FECHA_REGISTRO,
            DSL.exists(DSL.selectOne()
                            .from(RESTRICCION_USUARIO)
                            .where(RESTRICCION_USUARIO.USUARIO_ID.eq(USUARIO.ID))
                            .and(RESTRICCION_USUARIO.VIGENTE_DESDE.le(ahora))
                            .and(RESTRICCION_USUARIO
                                    .VIGENTE_HASTA
                                    .isNull()
                                    .or(RESTRICCION_USUARIO.VIGENTE_HASTA.gt(ahora))))
                    .as("tiene_restriccion"),
            DSL.exists(DSL.selectOne()
                            .from(BLOQUEO_CUENTA)
                            .where(BLOQUEO_CUENTA.USUARIO_ID.eq(USUARIO.ID))
                            .and(BLOQUEO_CUENTA.LIBERADA_EN.isNull()))
                    .as("tiene_bloqueo"),
            DSL.exists(DSL.selectOne()
                            .from(EXPEDIENTE_MEDIDA_CUENTA)
                            .where(EXPEDIENTE_MEDIDA_CUENTA.USUARIO_ID.eq(USUARIO.ID))
                            .and(EXPEDIENTE_MEDIDA_CUENTA.CERRADA_EN.isNull()))
                    .as("tiene_medida")
        };
    }

    private static Fila fila(Record r) {
        return new Fila(
                r.get(USUARIO.ID),
                r.get(USUARIO.CODIGO_PUBLICO),
                r.get(USUARIO.NOMBRES),
                r.get(USUARIO.APELLIDOS),
                r.get(USUARIO.ESTADO),
                r.get(USUARIO.NIVEL_KYC),
                r.get(USUARIO.FECHA_REGISTRO),
                Boolean.TRUE.equals(r.get("tiene_restriccion", Boolean.class)),
                Boolean.TRUE.equals(r.get("tiene_bloqueo", Boolean.class)),
                Boolean.TRUE.equals(r.get("tiene_medida", Boolean.class)));
    }
}
