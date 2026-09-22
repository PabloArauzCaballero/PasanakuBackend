package bo.aportaya.plataforma.web.idempotencia;

import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.impl.DSL;

/**
 * Reintentar tiene que ser seguro. La clave se valida ANTES de escribir.
 *
 * <p>El orden importa y es el error mas comun de esta capa: validar despues del
 * {@code INSERT} significa que el segundo intento ya escribio antes de descubrir que
 * era el segundo.
 *
 * <p>La deteccion no se apoya SOLO en un {@code SELECT} previo sino en la clave unica
 * de la tabla (ADR-046): entre dos peticiones simultaneas para la misma identidad, el
 * {@code SELECT} de las dos dice «no existe» y las dos intentan escribir; solo el
 * indice unico {@code (usuario_id, clave_idempotencia, operacion)} decide cual gana.
 *
 * <p>La identidad completa es esas tres columnas, nunca solo la clave (H1.S1.M3): dos
 * usuarios pueden mandar la misma clave sin pisarse, y la misma clave sirve para dos
 * operaciones distintas del mismo hecho.
 */
public final class Idempotencia {

    /**
     * Vigencia por omision, para quien construye sin pasar la propia (pruebas, sobre todo).
     * En produccion la fija {@code ConfiguracionComunWeb} desde {@code
     * aportaya.idempotencia.vigencia} (default {@code PT24H} en la plantilla) — H1.S2.M1.
     */
    private static final Duration VIGENCIA_POR_DEFECTO = Duration.ofDays(1);

    /** El valor de reserva, antes de que {@link #guardarRespuesta} escriba la respuesta real. */
    private static final short CODIGO_RESERVA = 202;

    private static final String CUERPO_RESERVA = "{}";

    /** Una respuesta guardada con este codigo o mas no se repite: no es un exito que replayar. */
    private static final short UMBRAL_TRANSITORIO = 500;

    private final String esquema;
    private final Reloj reloj;
    private final Duration vigencia;

    public Idempotencia(String esquema) {
        this(esquema, Reloj.delSistema(), VIGENCIA_POR_DEFECTO);
    }

    public Idempotencia(String esquema, Reloj reloj) {
        this(esquema, reloj, VIGENCIA_POR_DEFECTO);
    }

    public Idempotencia(String esquema, Reloj reloj, Duration vigencia) {
        this.esquema = Objects.requireNonNull(esquema, "esquema");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.vigencia = Objects.requireNonNull(vigencia, "vigencia");
    }

    /**
     * Reserva la clave para {@code (usuario, operacion, clave)}. Si ya habia una reserva
     * vigente con el MISMO cuerpo, lanza {@link OperacionRepetida} con la respuesta
     * original (si ya esta finalizada) o {@link IdempotenciaEnProceso} (si todavia no).
     * Si habia una con OTRO cuerpo, lanza {@link IdempotenciaConflicto}. Si la reserva
     * previa vencio, o si guardaba una respuesta transitoria ({@code >= 500}, que no
     * cuenta como exito para replayar), se reutiliza la fila para esta reserva nueva.
     */
    public void exigirNueva(
            DSLContext dsl, ContextoSesion ctx, String operacion, ClaveIdempotencia clave, String hashSolicitud) {
        var tabla = DSL.table(DSL.name(esquema, "respuesta_idempotente"));
        Instant ahora = reloj.ahora();

        Record previa = dsl.select(
                        DSL.field("hash_solicitud"),
                        DSL.field("codigo_http"),
                        DSL.field("cuerpo_respuesta"),
                        DSL.field("expira_en"))
                .from(tabla)
                .where(identidad(ctx, operacion, clave))
                .fetchAny();

        if (previa != null) {
            resolverExistente(dsl, tabla, ctx, operacion, clave, hashSolicitud, ahora, previa);
            return;
        }

        int filas = dsl.insertInto(tabla)
                .columns(
                        DSL.field("id"),
                        DSL.field("usuario_id"),
                        DSL.field("operacion"),
                        DSL.field("clave_idempotencia"),
                        DSL.field("hash_solicitud"),
                        DSL.field("codigo_http"),
                        DSL.field("cuerpo_respuesta"),
                        DSL.field("registrada_en"),
                        DSL.field("expira_en"))
                .values(
                        DSL.field("gen_random_uuid()"),
                        DSL.val(ctx.usuarioId()),
                        DSL.val(operacion),
                        DSL.val(clave.valor()),
                        DSL.val(hashSolicitud),
                        DSL.val(CODIGO_RESERVA),
                        DSL.val(JSONB.valueOf(CUERPO_RESERVA)),
                        DSL.val(registradaDesde(ahora)),
                        DSL.val(vencimientoDesde(ahora)))
                .onConflictDoNothing()
                .execute();

        if (filas == 0) {
            // Otra peticion gano la carrera entre el SELECT y este INSERT: exactamente la
            // misma situacion que encontrar la reserva en el SELECT de arriba.
            throw new IdempotenciaEnProceso();
        }
    }

    private void resolverExistente(
            DSLContext dsl,
            org.jooq.Table<?> tabla,
            ContextoSesion ctx,
            String operacion,
            ClaveIdempotencia clave,
            String hashSolicitud,
            Instant ahora,
            Record previa) {
        boolean expirada = previa.get(3, OffsetDateTime.class).toInstant().isBefore(ahora);
        short codigoPrevio = previa.get(1, Short.class);
        boolean transitoria = codigoPrevio >= UMBRAL_TRANSITORIO;

        if (expirada || transitoria) {
            // Se reutiliza la fila: misma identidad, nueva vigencia. No es un INSERT nuevo
            // porque el indice unico no lo permitiria mientras la fila vieja siga ahi.
            dsl.update(tabla)
                    .set(DSL.field("hash_solicitud"), hashSolicitud)
                    .set(DSL.field("codigo_http"), DSL.val(CODIGO_RESERVA))
                    .set(DSL.field("cuerpo_respuesta"), DSL.val(JSONB.valueOf(CUERPO_RESERVA)))
                    .set(DSL.field("registrada_en", OffsetDateTime.class), registradaDesde(ahora))
                    .set(DSL.field("expira_en"), DSL.val(vencimientoDesde(ahora)))
                    .where(identidad(ctx, operacion, clave))
                    .execute();
            return;
        }

        boolean mismoHash = hashSolicitud.equals(previa.get(0, String.class));
        if (!mismoHash) {
            throw new IdempotenciaConflicto();
        }

        boolean finalizada = codigoPrevio != CODIGO_RESERVA || !CUERPO_RESERVA.equals(String.valueOf(previa.get(2)));
        if (!finalizada) {
            throw new IdempotenciaEnProceso();
        }
        throw new OperacionRepetida(codigoPrevio, String.valueOf(previa.get(2)));
    }

    /** Guarda la respuesta real, para que el reintento devuelva exactamente eso. */
    public void guardarRespuesta(
            DSLContext dsl,
            ContextoSesion ctx,
            String operacion,
            ClaveIdempotencia clave,
            int codigoHttp,
            String cuerpoJson) {
        dsl.update(DSL.table(DSL.name(esquema, "respuesta_idempotente")))
                .set(DSL.field("codigo_http"), DSL.val((short) codigoHttp))
                .set(DSL.field("cuerpo_respuesta"), DSL.val(JSONB.valueOf(cuerpoJson)))
                .where(identidad(ctx, operacion, clave))
                .execute();
    }

    private org.jooq.Condition identidad(ContextoSesion ctx, String operacion, ClaveIdempotencia clave) {
        return DSL.field("usuario_id")
                .eq(ctx.usuarioId())
                .and(DSL.field("operacion").eq(operacion))
                .and(DSL.field("clave_idempotencia").eq(clave.valor()));
    }

    private OffsetDateTime vencimientoDesde(Instant ahora) {
        return OffsetDateTime.ofInstant(ahora.plus(vigencia), Reloj.ZONA);
    }

    /**
     * {@code registrada_en} sale del MISMO reloj que {@code expira_en} — nunca de {@code now()}
     * de la base. Mezclarlos viola {@code ck_respuesta_idem_expira} (expira_en > registrada_en)
     * en cuanto una prueba (o una recuperacion de reloj desfasado) usa un {@link Reloj} que no es
     * el del sistema: encontrado corriendo el caso "expirada" con {@code Reloj.fijo(...)}.
     */
    private OffsetDateTime registradaDesde(Instant ahora) {
        return OffsetDateTime.ofInstant(ahora, Reloj.ZONA);
    }
}
