package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code respuesta_idempotente}: la clave, la huella de la peticion y lo que se respondio.
 *
 * <p>Sirve a las operaciones que no tienen una fila propia donde guardar su clave (una
 * retencion). La clave se ampara en el titular: el espacio de claves es de cada persona.
 *
 * <p>Dos peticiones simultaneas con la misma clave se turnan con un candado de
 * transaccion sobre (titular, operacion, clave): la segunda espera, lee lo que la primera
 * dejo escrito y devuelve eso. Sin el candado las dos pasan por «no existe» y las dos
 * ejecutan; el indice unico solo las separaria al final, con un error y trabajo hecho.
 */
@Component
public class RegistroDeIdempotencia {

    private static final long VIGENCIA_HORAS = 24;

    /**
     * @return lo que se respondio la primera vez, o vacio si la clave es nueva.
     * @throws ErrorDeNegocio si la clave ya se uso con una peticion distinta.
     */
    public Optional<String> buscar(DSLContext dsl, UUID usuarioId, String operacion, String clave, String huella) {
        turnarse(dsl, usuarioId, operacion, clave);
        var previa = dsl.select(
                        DSL.field("hash_solicitud", String.class),
                        DSL.field("cast(cuerpo_respuesta as text)", String.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "respuesta_idempotente")))
                .where(DSL.field("usuario_id", UUID.class).eq(usuarioId))
                .and(DSL.field("operacion", String.class).eq(operacion))
                .and(DSL.field("clave_idempotencia", String.class).eq(clave))
                .fetchOne();
        if (previa == null) {
            return Optional.empty();
        }
        if (!huella.equals(previa.get(0, String.class))) {
            throw new ErrorDeNegocio(CodigoError.de(13, 4), "La clave de idempotencia ya identifica otra peticion.");
        }
        return Optional.of(previa.get(1, String.class));
    }

    /**
     * Las peticiones simultaneas con la misma clave se ponen en fila hasta el fin de la
     * transaccion. Quien llega segundo espera, y al despertar ve lo que el primero dejo escrito.
     */
    public void turnarse(DSLContext dsl, UUID usuarioId, String operacion, String clave) {
        dsl.execute("select pg_advisory_xact_lock(hashtextextended(?, 0))", usuarioId + "|" + operacion + "|" + clave);
    }

    public void guardar(
            DSLContext dsl,
            UUID usuarioId,
            String operacion,
            String clave,
            String huella,
            int codigoHttp,
            String cuerpoJson,
            OffsetDateTime ahora) {
        dsl.insertInto(DSL.table(DSL.name("nucleo_financiero", "respuesta_idempotente")))
                .set(DSL.field("id", UUID.class), UUID.randomUUID())
                .set(DSL.field("usuario_id", UUID.class), usuarioId)
                .set(DSL.field("operacion", String.class), operacion)
                .set(DSL.field("clave_idempotencia", String.class), clave)
                .set(DSL.field("hash_solicitud", String.class), huella)
                .set(DSL.field("codigo_http", Short.class), (short) codigoHttp)
                .set(DSL.field("cuerpo_respuesta", JSONB.class), JSONB.valueOf(cuerpoJson))
                .set(DSL.field("registrada_en", OffsetDateTime.class), ahora)
                .set(DSL.field("expira_en", OffsetDateTime.class), ahora.plusHours(VIGENCIA_HORAS))
                .execute();
    }
}
