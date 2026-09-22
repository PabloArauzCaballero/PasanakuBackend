package bo.aportaya.plataforma.web.idempotencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * H1.S1.M2 — los diez casos del helper de idempotencia, contra PostgreSQL real
 * (Testcontainers), esquema {@code nucleo_financiero} (donde vive {@code
 * respuesta_idempotente} hoy; H1.S3.M1 decide que otros esquemas la adoptan).
 *
 * <p>La identidad es {@code (usuario_id, operacion, clave_idempotencia)} — nunca solo
 * la clave (ADR-046). Los casos de "otro usuario" y "otra operacion" existen
 * precisamente para que un {@code WHERE} que se olvide de una de las tres columnas se
 * note aca, no en produccion.
 */
class IdempotenciaRepositorioTest {

    private static final String ESQUEMA = "nucleo_financiero";
    private static final String TABLA = "respuesta_idempotente";

    private static DSLContext dsl;

    private final Idempotencia idempotencia = new Idempotencia(ESQUEMA);

    @BeforeAll
    static void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(fuente, SQLDialect.POSTGRES);
    }

    @AfterEach
    void limpiar() {
        dsl.deleteFrom(DSL.table(DSL.name(ESQUEMA, TABLA))).execute();
        // Los usuarios de fixture no son del interes de otra prueba en el mismo contenedor
        // compartido (BaseDePrueba.contenedor() es un singleton por JVM); se retiran para no
        // dejar basura ajena a la vista de quien depure otra suite despues.
        dsl.execute("DELETE FROM identidad.usuario WHERE codigo_publico LIKE 'IDM-%'");
    }

    private static final java.util.concurrent.atomic.AtomicInteger SECUENCIA_TELEFONO =
            new java.util.concurrent.atomic.AtomicInteger(70_000_000);

    /**
     * Un usuario real en {@code identidad}: {@code respuesta_idempotente.usuario_id} tiene
     * {@code fk_respuesta_idempotente_usuario_id} y no perdona un UUID inventado. Mismo patron
     * que {@code FixturaDeBilletera.usuario()} en nucleo-financiero, copiado y no importado: ese
     * modulo esta en {@code servicios/}, fuera del alcance de {@code plataforma/}.
     */
    private UUID usuarioReal() {
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO identidad.usuario
                    (id, codigo_publico, nombres, apellidos, telefono_e164, fecha_nacimiento,
                     estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
                VALUES (?, ?, 'Idempotencia', 'Prueba', ?, DATE '1990-01-01', 'ACTIVO', 'BASICO',
                        'es', 'America/La_Paz', now())
                """,
                id,
                "IDM-" + id.toString().substring(0, 8),
                "+591" + SECUENCIA_TELEFONO.incrementAndGet());
        return id;
    }

    private ContextoSesion ctxDe(UUID usuarioId) {
        return ContextoSesion.de(
                usuarioId, "aportante", new Traza(UUID.randomUUID().toString()));
    }

    private ClaveIdempotencia claveNueva(String tipo) {
        return new ClaveIdempotencia(tipo + ":" + UUID.randomUUID());
    }

    private String hash(String texto) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] crudo = sha256.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder salida = new StringBuilder(crudo.length * 2);
            for (byte b : crudo) {
                salida.append(String.format("%02x", b));
            }
            return salida.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private long filasCon(ClaveIdempotencia clave) {
        return dsl.selectCount()
                .from(DSL.table(DSL.name(ESQUEMA, TABLA)))
                .where(DSL.field("clave_idempotencia").eq(clave.valor()))
                .fetchOne(0, Long.class);
    }

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
    @DisplayName("50 hilos: misma identidad y mismo hash en paralelo, una sola reserva gana")
    void cincuentaHilosUnaReserva() throws InterruptedException {
        UUID usuario = usuarioReal();
        var ctx = ctxDe(usuario);
        var clave = claveNueva("recarga");
        String h = hash("cuerpo-unico");
        int hilos = 50;

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(hilos);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();
        try {
            List<Future<?>> tareas = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                tareas.add(pool.submit(() -> {
                    try {
                        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h);
                        exitos.incrementAndGet();
                    } catch (RuntimeException e) {
                        conflictos.incrementAndGet();
                    } finally {
                        salida.countDown();
                    }
                }));
            }
            salida.await();
            for (Future<?> tarea : tareas) {
                tarea.get();
            }
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException(e);
        } finally {
            pool.shutdown();
        }

        assertThat(exitos.get()).isEqualTo(1);
        assertThat(conflictos.get()).isEqualTo(hilos - 1);
        assertThat(filasCon(clave)).isEqualTo(1);
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

    @Test
    @DisplayName("rollback: si la transaccion del llamador revierte, no queda reserva y se puede reintentar")
    void rollbackSinReserva() {
        UUID usuario = usuarioReal();
        var ctx = ctxDe(usuario);
        var clave = claveNueva("recarga");

        assertThatThrownBy(() -> dsl.transaction(config -> {
                    DSLContext tx = DSL.using(config);
                    idempotencia.exigirNueva(tx, ctx, "CU10-recargar", clave, hash("cuerpo-a"));
                    throw new IllegalStateException("fallo simulado despues de reservar, antes de comitear");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(filasCon(clave)).as("la reserva se fue con el rollback").isEqualTo(0);

        // Como no quedo nada, un reintento tiene que poder reservar de nuevo sin lanzar.
        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, hash("cuerpo-a"));
        assertThat(filasCon(clave)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "fallo tras reservar: la reserva sigue ahi; el reintento no duplica, y al completarse se puede leer la respuesta real")
    void falloTrasReservarReintentoPosible() {
        UUID usuario = usuarioReal();
        var ctx = ctxDe(usuario);
        var clave = claveNueva("recarga");
        String h = hash("cuerpo-a");

        // Reserva, y el caller "se cae" antes de llamar guardarRespuesta.
        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h);

        // Reintento inmediato: no puede insertar una segunda fila ni tratarse como nueva.
        assertThatThrownBy(() -> idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h))
                .isInstanceOf(IdempotenciaEnProceso.class);
        assertThat(filasCon(clave)).isEqualTo(1);

        // El proceso original (o un worker de recuperacion) termina y guarda la respuesta real.
        idempotencia.guardarRespuesta(dsl, ctx, "CU10-recargar", clave, 201, "{\"ordenId\": \"recuperada\"}");

        assertThatThrownBy(() -> idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h))
                .isInstanceOf(OperacionRepetida.class)
                .satisfies(e -> assertThat(((OperacionRepetida) e).cuerpo()).contains("recuperada"));
    }

    @Test
    @DisplayName(
            "reintento tras error transitorio: una respuesta 5xx guardada no se repite; se permite un nuevo intento")
    void reintentoTrasErrorTransitorio() {
        UUID usuario = usuarioReal();
        var ctx = ctxDe(usuario);
        var clave = claveNueva("recarga");
        String h = hash("cuerpo-a");

        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h);
        idempotencia.guardarRespuesta(dsl, ctx, "CU10-recargar", clave, 503, "{\"codigo\": \"AP-INT-01\"}");

        // Un 5xx no es una respuesta que se deba replayar: el reintento tiene que poder reservar de nuevo.
        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h);
        assertThat(filasCon(clave)).isEqualTo(1);
    }
}
