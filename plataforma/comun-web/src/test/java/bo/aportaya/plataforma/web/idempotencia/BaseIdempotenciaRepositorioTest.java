package bo.aportaya.plataforma.web.idempotencia;

import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Lo comun a los casos de {@code IdempotenciaRepositorioTest} e
 * {@code IdempotenciaConcurrenciaRepositorioTest} — separadas por la regla de las 300
 * lineas (tamano-archivo), igual que {@code CU10Test}/{@code CU10ConcurrenciaTest} en
 * nucleo-financiero: aquella fija el comportamiento caso por caso, esta corre varias
 * reservas a la vez y prueba la recuperacion tras una falla.
 *
 * <p>Contra PostgreSQL real (Testcontainers), esquema {@code nucleo_financiero} (donde
 * vive {@code respuesta_idempotente} hoy; H1.S2.M3 decide que otros esquemas la
 * adoptan).
 */
abstract class BaseIdempotenciaRepositorioTest {

    static final String ESQUEMA = "nucleo_financiero";
    static final String TABLA = "respuesta_idempotente";

    static DSLContext dsl;

    final Idempotencia idempotencia = new Idempotencia(ESQUEMA);

    private static final AtomicInteger SECUENCIA_TELEFONO = new AtomicInteger(70_000_000);

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

    /**
     * Un usuario real en {@code identidad}: {@code respuesta_idempotente.usuario_id} tiene
     * {@code fk_respuesta_idempotente_usuario_id} y no perdona un UUID inventado. Mismo patron
     * que {@code FixturaDeBilletera.usuario()} en nucleo-financiero, copiado y no importado: ese
     * modulo esta en {@code servicios/}, fuera del alcance de {@code plataforma/}.
     */
    UUID usuarioReal() {
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

    ContextoSesion ctxDe(UUID usuarioId) {
        return ContextoSesion.de(
                usuarioId, "aportante", new Traza(UUID.randomUUID().toString()));
    }

    ClaveIdempotencia claveNueva(String tipo) {
        return new ClaveIdempotencia(tipo + ":" + UUID.randomUUID());
    }

    String hash(String texto) {
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

    long filasCon(ClaveIdempotencia clave) {
        return dsl.selectCount()
                .from(DSL.table(DSL.name(ESQUEMA, TABLA)))
                .where(DSL.field("clave_idempotencia").eq(clave.valor()))
                .fetchOne(0, Long.class);
    }
}
