package bo.aportaya.garantia.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * La lista de restriccion y el reemplazo.
 *
 * <p>Lo que pasa **despues** de que el fondo cubrio: a quien se restringe y quien ocupa el lugar que quedo vacio.
 * El aval entre participantes y su subrogacion al avalista se retiraron (P-03, F2.L2.M04): el fondo del grupo cubre
 * esos casos (modelo A, F3.L2.M05). Como termina un
 * grupo que ya no puede seguir esta en {@link DisolucionRepositorio}, porque disolver
 * no es gestionar un incumplimiento: es cerrar el grupo entero.
 */
@Component
public class GestionRepositorio {

    // --------------------------------------------------------- restriccion

    /** La restriccion vigente de un usuario, si tiene (R-GAR-05). */
    /**
     * Lo que el deudor tendria que pagar para salir de la lista.
     *
     * <p>Una restriccion sin salida es una condena. Quien consulta necesita poder
     * decirle a la persona cuanto es, no solo que no puede entrar a un grupo.
     */
    public java.math.BigDecimal deudaViva(DSLContext dsl, UUID usuarioId) {
        var fila = dsl.fetchOne(
                """
                SELECT COALESCE(SUM(d.saldo_actual), 0) AS viva
                  FROM garantia.deuda_participante d
                 WHERE d.usuario_id = ? AND d.estado = 'VIGENTE'
                """,
                usuarioId);
        return fila == null ? java.math.BigDecimal.ZERO : fila.get("viva", java.math.BigDecimal.class);
    }

    public Optional<Restriccion> restriccionVigente(DSLContext dsl, UUID usuarioId, OffsetDateTime momento) {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("nivel_restriccion", String.class),
                        DSL.field("incluido_en", OffsetDateTime.class),
                        DSL.field("vigente_hasta", OffsetDateTime.class))
                .from(DSL.table(DSL.name("garantia", "lista_restriccion_interna")))
                .where(DSL.field("usuario_id", UUID.class)
                        .eq(usuarioId)
                        .and(DSL.field("retirado_en", OffsetDateTime.class).isNull())
                        .and(DSL.field("vigente_hasta", OffsetDateTime.class)
                                .isNull()
                                .or(DSL.field("vigente_hasta", OffsetDateTime.class)
                                        .gt(momento))))
                .fetchOptional(f -> new Restriccion(
                        f.get("id", UUID.class),
                        f.get("nivel_restriccion", String.class),
                        f.get("incluido_en", OffsetDateTime.class),
                        f.get("vigente_hasta", OffsetDateTime.class)));
    }

    public UUID restringir(
            DSLContext dsl,
            UUID usuarioId,
            UUID registroOrigenId,
            String motivo,
            String nivel,
            Dinero montoAdeudado,
            OffsetDateTime desde,
            OffsetDateTime hasta) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("garantia", "lista_restriccion_interna")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("usuario_id", UUID.class), usuarioId)
                .set(DSL.field("registro_origen_id", UUID.class), registroOrigenId)
                .set(DSL.field("motivo", String.class), motivo)
                .set(DSL.field("nivel_restriccion", String.class), nivel)
                .set(DSL.field("monto_adeudado", BigDecimal.class), montoAdeudado.monto())
                .set(DSL.field("incluido_en", OffsetDateTime.class), desde)
                .set(DSL.field("vigente_hasta", OffsetDateTime.class), hasta)
                .execute();
        return id;
    }

    /** El levantamiento **se motiva** (R-GAR-05): sin motivo escrito no se levanta. */
    public boolean levantar(DSLContext dsl, UUID id, UUID retiradoPor, String motivo, OffsetDateTime ahora) {
        return dsl.update(DSL.table(DSL.name("garantia", "lista_restriccion_interna")))
                        .set(DSL.field("retirado_en", OffsetDateTime.class), ahora)
                        .set(DSL.field("retirado_por", UUID.class), retiradoPor)
                        .set(DSL.field("motivo_retiro", String.class), motivo)
                        .where(DSL.field("id", UUID.class)
                                .eq(id)
                                .and(DSL.field("retirado_en", OffsetDateTime.class)
                                        .isNull()))
                        .execute()
                == 1;
    }

    // ----------------------------------------------------------- reemplazo

    public UUID proponerReemplazo(
            DSLContext dsl,
            UUID grupoId,
            UUID cupoId,
            UUID registroId,
            UUID salienteId,
            UUID entranteId,
            Dinero deudaAsumida,
            Dinero deudaRetenida,
            boolean conservaTurno,
            String estado,
            OffsetDateTime ahora) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("garantia", "reemplazo_participante")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("grupo_id", UUID.class), grupoId)
                .set(DSL.field("cupo_id", UUID.class), cupoId)
                .set(DSL.field("registro_id", UUID.class), registroId)
                .set(DSL.field("participante_saliente_id", UUID.class), salienteId)
                .set(DSL.field("participante_entrante_id", UUID.class), entranteId)
                .set(DSL.field("deuda_asumida_por_entrante", BigDecimal.class), deudaAsumida.monto())
                .set(DSL.field("deuda_retenida_por_saliente", BigDecimal.class), deudaRetenida.monto())
                .set(DSL.field("conserva_orden_de_turno", Boolean.class), conservaTurno)
                .set(DSL.field("estado", String.class), estado)
                .set(DSL.field("fecha", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    public boolean cambiarEstadoDeReemplazo(DSLContext dsl, UUID id, String desde, String hacia) {
        return dsl.update(DSL.table(DSL.name("garantia", "reemplazo_participante")))
                        .set(DSL.field("estado", String.class), hacia)
                        .where(DSL.field("id", UUID.class)
                                .eq(id)
                                .and(DSL.field("estado", String.class).eq(desde)))
                        .execute()
                == 1;
    }

    public record Aval(
            UUID id,
            UUID avalistaUsuarioId,
            Dinero montoMaximoAvalado,
            BigDecimal porcentajeResponsabilidad,
            String estado) {}

    public record Restriccion(UUID id, String nivel, OffsetDateTime desde, OffsetDateTime hasta) {}
}
