package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.infraestructura.EvidenciaStepUpConsumidaRepositorio;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * El {@code jti} de la evidencia step-up de gobierno se consume una sola vez en la BASE (GU-11, decision de Pablo
 * 2026-11-24), no en memoria: dos repositorios —como dos replicas o un reinicio— comparten la misma barrera.
 */
class EvidenciaStepUpConsumidaRepositorioTest {

    private static DSLContext dsl;
    private static TransactionTemplate transaccion;
    private static FixturaDeIdentidad fixtura;

    @BeforeAll
    static void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        fixtura = new FixturaDeIdentidad(dsl);
    }

    @Test
    @DisplayName("doble uso: la misma evidencia en dos replicas se acepta una vez y la tabla tiene una sola fila")
    void dobleUsoEntreReplicas() {
        UUID actor = fixtura.usuario("+5917" + ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999));
        UUID jti = UUID.randomUUID();
        OffsetDateTime ahora = OffsetDateTime.now();
        var replicaA = new EvidenciaStepUpConsumidaRepositorio(dsl);
        var replicaB = new EvidenciaStepUpConsumidaRepositorio(dsl);

        Boolean primera = transaccion.execute(e -> replicaA.consumir(jti, actor, "ADMIN", ahora, ahora.plusMinutes(5)));
        Boolean segunda = transaccion.execute(e -> replicaB.consumir(jti, actor, "ADMIN", ahora, ahora.plusMinutes(5)));

        assertThat(primera).isTrue();
        assertThat(segunda).isFalse();
        assertThat(dsl.fetchValue("SELECT count(*)::int FROM identidad.evidencia_step_up_consumida WHERE jti = ?", jti))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza: la evidencia consumida no se borra ni se renueva (append-only)")
    void noSeRenueva() {
        UUID actor = fixtura.usuario("+5917" + ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999));
        UUID jti = UUID.randomUUID();
        OffsetDateTime ahora = OffsetDateTime.now();
        transaccion.execute(e -> new EvidenciaStepUpConsumidaRepositorio(dsl)
                .consumir(jti, actor, "ADMIN", ahora, ahora.plusMinutes(5)));

        String error = "";
        try {
            transaccion.execute(
                    e -> dsl.execute("DELETE FROM identidad.evidencia_step_up_consumida WHERE jti = ?", jti));
        } catch (RuntimeException rechazo) {
            Throwable raiz = rechazo;
            while (raiz.getCause() != null && raiz.getCause() != raiz) {
                raiz = raiz.getCause();
            }
            error = String.valueOf(raiz.getMessage());
        }

        assertThat(error).isNotEmpty();
        assertThat(dsl.fetchValue("SELECT count(*)::int FROM identidad.evidencia_step_up_consumida WHERE jti = ?", jti))
                .isEqualTo(1);
    }
}
