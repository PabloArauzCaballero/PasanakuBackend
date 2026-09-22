package bo.aportaya.plataforma.web.idempotencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H1.S1.M2 — los seis casos que fijan el comportamiento de {@code exigirNueva} caso por
 * caso. Los de recuperacion tras una falla y los 50 hilos en paralelo viven en
 * {@link IdempotenciaConcurrenciaRepositorioTest} (regla tamano-archivo: son dos
 * escenarios distintos, y separados se leen mejor que juntos).
 *
 * <p>La identidad es {@code (usuario_id, operacion, clave_idempotencia)} — nunca solo
 * la clave (ADR-046). Los casos de "otro usuario" y "otra operacion" existen
 * precisamente para que un {@code WHERE} que se olvide de una de las tres columnas se
 * note aca, no en produccion.
 */
class IdempotenciaRepositorioTest extends BaseIdempotenciaRepositorioTest {

    @Test
    @DisplayName("nueva: sin fila previa para esa identidad, exigirNueva reserva sin lanzar")
    void nueva() {
        UUID usuario = usuarioReal();
        var ctx = ctxDe(usuario);
        var clave = claveNueva("recarga");

        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, hash("cuerpo-a"));

        var fila = dsl.select(DSL.field("usuario_id"), DSL.field("operacion"), DSL.field("hash_solicitud"))
                .from(DSL.table(DSL.name(ESQUEMA, TABLA)))
                .where(DSL.field("clave_idempotencia").eq(clave.valor()))
                .fetchOne();
        assertThat(fila).isNotNull();
        assertThat(fila.get(0, UUID.class)).isEqualTo(usuario);
        assertThat(fila.get(1, String.class)).isEqualTo("CU10-recargar");
        assertThat(fila.get(2, String.class)).isEqualTo(hash("cuerpo-a"));
    }

    @Test
    @DisplayName("replay: misma identidad y mismo hash, ya finalizada, devuelve la respuesta guardada — nunca 409")
    void replayMismoHash() {
        UUID usuario = usuarioReal();
        var ctx = ctxDe(usuario);
        var clave = claveNueva("recarga");
        String h = hash("cuerpo-a");

        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h);
        idempotencia.guardarRespuesta(dsl, ctx, "CU10-recargar", clave, 201, "{\"ordenId\": \"abc\"}");

        assertThatThrownBy(() -> idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h))
                .isInstanceOf(OperacionRepetida.class)
                .satisfies(e -> {
                    var repetida = (OperacionRepetida) e;
                    assertThat(repetida.codigoHttp()).isEqualTo(201);
                    assertThat(repetida.cuerpo()).contains("abc");
                });
    }

    @Test
    @DisplayName("cuerpo distinto: misma identidad, otro hash, 409 IdempotenciaConflicto (nunca replay)")
    void cuerpoDistinto409() {
        UUID usuario = usuarioReal();
        var ctx = ctxDe(usuario);
        var clave = claveNueva("recarga");
        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, hash("cuerpo-a"));

        assertThatThrownBy(() -> idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, hash("cuerpo-b")))
                .isInstanceOf(IdempotenciaConflicto.class)
                .satisfies(e ->
                        assertThat(((IdempotenciaConflicto) e).codigo().valor()).isEqualTo("AP-CU00-01"));
    }

    @Test
    @DisplayName("otro usuario: misma clave y operacion, otro usuario_id, es independiente (no es replay ni conflicto)")
    void otroUsuarioEsIndependiente() {
        UUID usuarioA = usuarioReal();
        UUID usuarioB = usuarioReal();
        var clave = claveNueva("recarga");

        idempotencia.exigirNueva(dsl, ctxDe(usuarioA), "CU10-recargar", clave, hash("cuerpo-a"));
        // No debe lanzar: usuarioB no tiene nada reservado con esta clave todavia.
        idempotencia.exigirNueva(dsl, ctxDe(usuarioB), "CU10-recargar", clave, hash("cuerpo-b"));

        assertThat(filasCon(clave)).isEqualTo(2);
    }

    @Test
    @DisplayName("otra operacion: misma clave, mismo usuario, otra operacion; guardarRespuesta no cruza operaciones")
    void otraOperacionEsIndependiente() {
        UUID usuario = usuarioReal();
        var ctx = ctxDe(usuario);
        var clave = claveNueva("cierre-periodo");

        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, hash("op-a"));
        idempotencia.exigirNueva(dsl, ctx, "CU24-cerrar", clave, hash("op-b"));
        idempotencia.guardarRespuesta(dsl, ctx, "CU10-recargar", clave, 201, "{\"marca\": \"recargar\"}");

        var cerrar = dsl.select(DSL.field("codigo_http"))
                .from(DSL.table(DSL.name(ESQUEMA, TABLA)))
                .where(DSL.field("clave_idempotencia").eq(clave.valor()))
                .and(DSL.field("operacion").eq("CU24-cerrar"))
                .fetchOne();
        assertThat(cerrar).isNotNull();
        assertThat(cerrar.get(0, Short.class))
                .as("la reserva de CU24-cerrar no la toco guardarRespuesta de CU10-recargar")
                .isEqualTo((short) 202);
    }

    @Test
    @DisplayName("expirada: una reserva vencida se reemplaza — no cuenta como replay ni como conflicto")
    void expiradaSeReemplaza() {
        UUID usuario = usuarioReal();
        var clave = claveNueva("recarga");
        Instant hace2Dias = Instant.now().minus(2, ChronoUnit.DAYS);
        var idempotenciaDeAntes = new Idempotencia(ESQUEMA, Reloj.fijo(hace2Dias));
        idempotenciaDeAntes.exigirNueva(dsl, ctxDe(usuario), "CU10-recargar", clave, hash("cuerpo-viejo"));

        // Con el reloj de hoy, la reserva de hace 2 dias ya vencio (vigencia 24h): no debe lanzar.
        idempotencia.exigirNueva(dsl, ctxDe(usuario), "CU10-recargar", clave, hash("cuerpo-nuevo"));

        var fila = dsl.select(DSL.field("hash_solicitud"))
                .from(DSL.table(DSL.name(ESQUEMA, TABLA)))
                .where(DSL.field("clave_idempotencia").eq(clave.valor()))
                .fetchOne();
        assertThat(fila.get(0, String.class)).isEqualTo(hash("cuerpo-nuevo"));
        assertThat(filasCon(clave)).isEqualTo(1);
    }
}
