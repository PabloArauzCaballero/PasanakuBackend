package bo.aportaya.entregas.infraestructura;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * La saga de la entrega automatica en {@code entregas.estado_saga} (ADR-028), una fila por periodo.
 *
 * <ul>
 *   <li>{@code INICIADA}: hay aviso de recaudo y falta fondear (o falta el turno, o {@code aportes} no respondio).
 *   <li>{@code EN_CURSO}: se fondeo con faltante; espera otro aviso del mismo periodo para completar el MISMO fondeo.
 *   <li>{@code COMPLETADA}: el pozo quedo fondeado completo. Otro aviso ya no hace nada.
 *   <li>{@code FALLIDA}: el aviso no cierra con el recaudo de {@code aportes} (otro grupo): pide una persona.
 * </ul>
 *
 * <p>La unicidad {@code (tipo_saga, clave_negocio)} es la de la base: dos avisos simultaneos del mismo periodo
 * terminan en UNA fila.
 */
@Component
public class SagaDeEntregaAutomaticaRepositorio {

    public static final String TIPO = "entrega-automatica";

    public record Saga(UUID periodoId, UUID grupoId, String estado, String correlationId) {}

    /**
     * Registra el aviso: crea la saga o la reabre si estaba esperando otro aviso ({@code EN_CURSO}). Una saga
     * {@code COMPLETADA} o {@code FALLIDA} no se toca.
     */
    public void avisar(DSLContext dsl, UUID periodoId, UUID grupoId, String motivo, UUID correlationId) {
        dsl.execute(
                """
                INSERT INTO entregas.estado_saga (tipo_saga, clave_negocio, paso, estado, datos)
                VALUES (?, ?, 0, 'INICIADA',
                        jsonb_build_object('grupoId', ?::text, 'motivo', ?::text, 'correlationId', ?::text))
                ON CONFLICT (tipo_saga, clave_negocio) DO UPDATE
                   SET estado = 'INICIADA', datos = excluded.datos, actualizado_en = now()
                 WHERE entregas.estado_saga.estado = 'EN_CURSO'
                """,
                TIPO,
                periodoId.toString(),
                grupoId.toString(),
                motivo,
                correlationId.toString());
    }

    public Optional<Saga> delPeriodo(DSLContext dsl, UUID periodoId) {
        return dsl.resultQuery(
                        """
                        SELECT estado, datos->>'grupoId' AS grupo, datos->>'correlationId' AS correlacion
                          FROM entregas.estado_saga
                         WHERE tipo_saga = ? AND clave_negocio = ?
                        """,
                        TIPO,
                        periodoId.toString())
                .fetchOptional(f -> new Saga(
                        periodoId,
                        UUID.fromString(f.get("grupo", String.class)),
                        f.get("estado", String.class),
                        f.get("correlacion", String.class)));
    }

    /** Pasa de {@code INICIADA} al estado dado; {@code false} si otro proceso ya la movio. */
    public boolean cerrar(DSLContext dsl, UUID periodoId, String estado, int paso) {
        return dsl.execute(
                        """
                        UPDATE entregas.estado_saga
                           SET estado = ?, paso = ?, actualizado_en = now()
                         WHERE tipo_saga = ? AND clave_negocio = ? AND estado = 'INICIADA'
                        """,
                        estado,
                        paso,
                        TIPO,
                        periodoId.toString())
                == 1;
    }

    /** Marca el intento para que el barrido no la tome de nuevo enseguida. */
    public void tocar(DSLContext dsl, UUID periodoId) {
        dsl.execute(
                """
                UPDATE entregas.estado_saga SET actualizado_en = now()
                 WHERE tipo_saga = ? AND clave_negocio = ? AND estado = 'INICIADA'
                """,
                TIPO,
                periodoId.toString());
    }

    /** Las iniciadas que nadie toco desde {@code antesDe}, de la mas vieja a la mas nueva. */
    public List<UUID> iniciadas(DSLContext dsl, OffsetDateTime antesDe, int limite) {
        return dsl.resultQuery(
                        """
                        SELECT clave_negocio FROM entregas.estado_saga
                         WHERE tipo_saga = ? AND estado = 'INICIADA' AND actualizado_en < CAST(? AS timestamptz)
                         ORDER BY actualizado_en, clave_negocio
                         LIMIT ?
                        """,
                        TIPO,
                        antesDe,
                        limite)
                .fetch(f -> UUID.fromString(f.get(0, String.class)));
    }
}
