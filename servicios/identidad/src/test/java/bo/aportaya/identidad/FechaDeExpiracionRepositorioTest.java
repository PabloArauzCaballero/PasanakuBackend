package bo.aportaya.identidad;

import static bo.aportaya.identidad.ExpedientesDePrueba.expediente;
import static bo.aportaya.identidad.ExpedientesDePrueba.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.infraestructura.FechaDeExpiracionRepositorio;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.LocalDate;
import java.util.UUID;
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
 * CU-02 · La fecha de vencimiento del documento solo se completa mientras el expediente sigue abierto
 * (PENDIENTE o EN_REVISION), contra PostgreSQL real. Cubre {@link FechaDeExpiracionRepositorio}.
 */
class FechaDeExpiracionRepositorioTest {

    private static final LocalDate VENCE = LocalDate.of(2031, 5, 20);

    private static DSLContext dsl;
    private static TransactionTemplate transaccion;
    private static FechaDeExpiracionRepositorio repositorio;

    @BeforeAll
    static void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        repositorio = new FechaDeExpiracionRepositorio();
        ExpedientesDePrueba.usar(dsl);
    }

    private static void actualizar(UUID usuario) {
        transaccion.execute(s -> {
            repositorio.actualizarFechaExpiracion(dsl, usuario, VENCE);
            return null;
        });
    }

    private static LocalDate fechaGuardada(UUID usuario) {
        return dsl.fetchOne("SELECT fecha_expiracion FROM identidad.documento_identidad WHERE usuario_id = ?", usuario)
                .get(0, LocalDate.class);
    }

    @Test
    @DisplayName("actualiza · con el expediente PENDIENTE o EN_REVISION la fecha queda en el documento")
    void expedienteAbiertoGuarda() {
        UUID pendiente = usuario();
        expediente(pendiente, "PENDIENTE", true);
        UUID enRevision = usuario();
        expediente(enRevision, "EN_REVISION", true);

        actualizar(pendiente);
        actualizar(enRevision);

        assertThat(fechaGuardada(pendiente)).isEqualTo(VENCE);
        assertThat(fechaGuardada(enRevision)).isEqualTo(VENCE);
    }

    @Test
    @DisplayName("rechaza · un expediente ya decidido (APROBADA o RECHAZADA) no admite cambios")
    void expedienteDecididoRechaza() {
        for (String estado : new String[] {"APROBADA", "RECHAZADA"}) {
            UUID persona = usuario();
            expediente(persona, estado, true);

            assertThatThrownBy(() -> actualizar(persona)).isInstanceOf(ErrorDeNegocio.class);
            assertThat(fechaGuardada(persona)).isNull();
        }
    }

    @Test
    @DisplayName("rechaza · sin expediente abierto, o con expediente pero sin documento cargado")
    void sinExpedienteOSinDocumentoRechaza() {
        UUID sinExpediente = usuario();
        UUID sinDocumento = usuario();
        expediente(sinDocumento, "PENDIENTE", false);

        assertThatThrownBy(() -> actualizar(sinExpediente)).isExactlyInstanceOf(ErrorDeDominio.class);
        assertThatThrownBy(() -> actualizar(sinDocumento)).isExactlyInstanceOf(ErrorDeDominio.class);
    }
}
