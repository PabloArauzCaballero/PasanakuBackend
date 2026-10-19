package bo.aportaya.plataforma.datos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
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
 * {@link Datos#comoSistema}, contra PostgreSQL real: el rol de sistema vale SOLO dentro del bloque y la
 * sesion recupera su rol al salir, tambien cuando el trabajo falla. Es lo que impide que un privilegio de
 * sistema quede abierto para el resto de la peticion.
 */
class ComoSistemaRepositorioTest {

    private static final UUID PERSONA = UUID.fromString("00000000-0000-4000-8000-00000000b001");

    private static DSLContext dsl;
    private static TransactionTemplate transaccion;
    private static Datos datos;

    @BeforeAll
    static void prepararLaBase() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        datos = new Datos(dsl);
    }

    private static String rolActual(DSLContext d) {
        return d.fetchOne("select current_setting('app.rol', true)").get(0, String.class);
    }

    private static ContextoSesion participante() {
        return ContextoSesion.de(
                PERSONA, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("dentro del bloque el rol es el de sistema; al salir vuelve el de la sesion")
    void rolDeSistemaSoloDentroDelBloque() {
        String[] roles = transaccion.execute(estado -> datos.conContexto(participante(), d -> {
            String durante = Datos.comoSistema(d, () -> rolActual(d));
            return new String[] {durante, rolActual(d)};
        }));

        assertThat(roles[0]).isEqualTo(ContextoSesion.ROL_SISTEMA);
        assertThat(roles[1]).isEqualTo("PARTICIPANTE");
    }

    @Test
    @DisplayName("si el trabajo falla, el error llega intacto y la sesion igual recupera su rol")
    void restauraElRolCuandoElTrabajoFalla() {
        String despues = transaccion.execute(estado -> datos.conContexto(participante(), d -> {
            assertThatThrownBy(() -> Datos.comoSistema(d, () -> {
                        throw new IllegalStateException("el trabajo fallo");
                    }))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("el trabajo fallo");
            return rolActual(d);
        }));

        assertThat(despues).isEqualTo("PARTICIPANTE");
    }

    @Test
    @DisplayName("sin rol previo en la sesion, al salir el rol queda vacio y no de sistema")
    void sinRolPrevioNoQuedaElDeSistema() {
        String despues = transaccion.execute(estado -> {
            Datos.comoSistema(dsl, () -> null);
            return rolActual(dsl);
        });

        assertThat(despues).isEmpty();
    }

    @Test
    @DisplayName("rechaza argumentos nulos antes de tocar la base")
    void rechazaNulos() {
        assertThatThrownBy(() -> Datos.comoSistema(null, () -> 1)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Datos.comoSistema(dsl, null)).isInstanceOf(NullPointerException.class);
    }
}
