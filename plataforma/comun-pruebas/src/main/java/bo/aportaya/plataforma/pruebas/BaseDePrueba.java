package bo.aportaya.plataforma.pruebas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * La PostgreSQL real de las pruebas de integracion, con el esquema de {@code sql/}
 * aplicado.
 *
 * <p>El contenedor se levanta UNA vez por corrida y se reutiliza: levantarlo por
 * clase multiplica por veinte el tiempo de la suite y termina en pruebas que nadie
 * corre en local (ADR-026).
 *
 * <p>El esquema se aplica con el {@code psql} del propio contenedor y no por JDBC,
 * porque {@code sql/aplicar.sql} usa {@code \\ir}: es un guion de psql, no un lote
 * de sentencias.
 */
public final class BaseDePrueba {

    private static final String IMAGEN = "postgres:16";
    private static final String NOMBRE = "pasanaku";

    // H2.S2.M2: version fijada, no "latest" — la clase org.testcontainers.kafka.KafkaContainer
    // (no la org.testcontainers.containers.KafkaContainer vieja, basada en Confluent) se
    // verifico con javap contra el jar real de testcontainers:kafka:1.21.3 antes de escribir,
    // por indicacion explicita del encargo. La imagen es la oficial de Apache (no Confluent):
    // publicada por el propio proyecto Kafka desde la serie 3.7.
    //
    // F-Leo-06 (2026-09-24): 3.8.1 y no 3.9.0. Kafka 3.9.0 exige (observado, ver abajo) que el listener
    // CONTROLLER este anunciado cuando el nodo es broker+controller; KafkaContainer de
    // testcontainers 1.21.x solo anuncia PLAINTEXT y BROKER, asi que 3.9.0 aborta al
    // formatear ("advertised.listeners cannot use the nonroutable meta-address 0.0.0.0").
    // Reproducido en Windows/Docker Desktop y en el runner Linux del CI (run 36042014583),
    // y aislado con `docker run` a mano: la misma configuracion arranca en cuanto se
    // anuncia CONTROLLER. No era un problema de la maquina.
    private static final String IMAGEN_KAFKA = "apache/kafka:3.8.1";

    private static PostgreSQLContainer<?> contenedor;
    private static KafkaContainer kafka;

    private BaseDePrueba() {}

    /** El contenedor compartido, ya con esquema, roles, permisos y catalogos. */
    public static synchronized PostgreSQLContainer<?> contenedor() {
        if (contenedor == null) {
            contenedor = arrancar();
        }
        return contenedor;
    }

    /**
     * El broker de Kafka compartido, real (no mockeado): H2 exige apagarlo a proposito para
     * probar backoff/reinicio, y eso solo se puede hacer contra un broker de verdad
     * (microservices-testing). Un solo contenedor por JVM, igual que {@link #contenedor()}.
     */
    public static synchronized KafkaContainer kafka() {
        if (kafka == null) {
            kafka = new KafkaContainer(IMAGEN_KAFKA);
            kafka.start();
        }
        return kafka;
    }

    /** Una conexion como el rol dueno de la base. */
    public static Connection conexion() throws SQLException {
        PostgreSQLContainer<?> c = contenedor();
        return DriverManager.getConnection(c.getJdbcUrl(), c.getUsername(), c.getPassword());
    }

    private static PostgreSQLContainer<?> arrancar() {
        Path repositorio = raizDelRepositorio();
        PostgreSQLContainer<?> nuevo = new PostgreSQLContainer<>(IMAGEN)
                .withDatabaseName(NOMBRE)
                .withUsername(NOMBRE)
                .withPassword(NOMBRE)
                // CU12 prueba 50 reintentos simultaneos con conexiones directas; los
                // contextos Spring del mismo corredor tambien mantienen conexiones.
                // Es un maximo, no conexiones abiertas de antemano.
                .withCommand("postgres", "-c", "max_connections=200")
                .withFileSystemBind(repositorio.resolve("sql").toString(), "/repo/sql", BindMode.READ_ONLY)
                // Cinco minutos para estar listo, no el minuto por omision. No es
                // tolerancia a un contenedor lento: es que la maquina de desarrollo
                // corre otros stacks y con carga alta un PostgreSQL tarda mas en
                // aceptar conexiones. Con el valor por omision la prueba falla por
                // «no arranco» y manda a buscar un defecto donde no lo hay.
                .withStartupTimeout(java.time.Duration.ofMinutes(5));
        nuevo.start();

        // El DDL califica cada tabla con su esquema; el SQL escrito a mano que viene
        // despues la referencia por nombre simple. Los 307 nombres son unicos.
        ejecutar(nuevo, "-c", "ALTER DATABASE " + NOMBRE + " SET search_path TO " + esquemas());
        ejecutar(nuevo, "-f", "/repo/sql/aplicar.sql");
        return nuevo;
    }

    private static String esquemas() {
        return "aportes, auditoria, cumplimiento, entregas, erp, garantia, grupos, identidad,"
                + " notificaciones, nucleo_financiero, organizador, publicidad, tarifas,"
                + " transparencia, catalogo, comun, public";
    }

    private static void ejecutar(PostgreSQLContainer<?> destino, String bandera, String valor) {
        try {
            var resultado = destino.execInContainer(
                    "psql", "-v", "ON_ERROR_STOP=1", "-U", NOMBRE, "-d", NOMBRE, "-q", bandera, valor);
            if (resultado.getExitCode() != 0) {
                throw new IllegalStateException("psql " + bandera + " " + valor + ":\n" + resultado.getStderr());
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("No se pudo preparar la base de prueba", e);
        }
    }

    /** Sube desde el directorio del modulo hasta el que tiene {@code sql/}. */
    private static Path raizDelRepositorio() {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null && !Files.isDirectory(actual.resolve("sql"))) {
            actual = actual.getParent();
        }
        if (actual == null) {
            throw new IllegalStateException(
                    "No encontre la raiz del repositorio desde " + Path.of("").toAbsolutePath());
        }
        return actual;
    }
}
