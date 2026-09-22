package bo.aportaya.plataforma.mensajeria;

import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Lo comun a {@link RelevoTest} y a las pruebas de recuperacion: fixture minima de
 * {@code evento_dominio} contra PostgreSQL real (Testcontainers). */
abstract class BaseDePruebaMensajeria {

    static final String ESQUEMA = "nucleo_financiero";
    private static final String TABLA = "evento_dominio";

    static DSLContext dsl;

    @BeforeAll
    static void armarBase() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(fuente, SQLDialect.POSTGRES);
    }

    @AfterEach
    void limpiarEventos() {
        dsl.deleteFrom(DSL.table(DSL.name(ESQUEMA, TABLA))).execute();
    }

    UUID insertarPendiente() {
        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name(ESQUEMA, TABLA)))
                .columns(
                        DSL.field("id"),
                        DSL.field("tipo"),
                        DSL.field("agregado"),
                        DSL.field("agregado_id"),
                        DSL.field("payload"),
                        DSL.field("correlation_id"),
                        DSL.field("estado"),
                        DSL.field("intentos"))
                .values(
                        DSL.val(id),
                        DSL.val("nucleo_financiero.evento_de_prueba"),
                        DSL.val("prueba"),
                        DSL.val(UUID.randomUUID()),
                        DSL.val(JSONB.valueOf("{}")),
                        DSL.val(UUID.randomUUID()),
                        DSL.val("PENDIENTE"),
                        DSL.val((short) 0))
                .execute();
        return id;
    }

    /** Un {@code TOMADO} con {@code tomado_en} en el pasado — simula un relevo que murio. */
    UUID insertarTomadoHace(Duration antiguedad) {
        UUID id = insertarPendiente();
        dsl.update(DSL.table(DSL.name(ESQUEMA, TABLA)))
                .set(DSL.field("estado", String.class), "TOMADO")
                .set(
                        DSL.field("tomado_en", OffsetDateTime.class),
                        OffsetDateTime.now().minus(antiguedad))
                .set(DSL.field("tomado_por", String.class), "relevo-que-murio")
                .where(DSL.field("id").eq(id))
                .execute();
        return id;
    }

    Record filaDe(UUID id) {
        return dsl.select(
                        DSL.field("estado"),
                        DSL.field("publicado_en"),
                        DSL.field("intentos"),
                        DSL.field("ultimo_error"),
                        DSL.field("proximo_intento_en"))
                .from(DSL.table(DSL.name(ESQUEMA, TABLA)))
                .where(DSL.field("id").eq(id))
                .fetchOne();
    }
}
