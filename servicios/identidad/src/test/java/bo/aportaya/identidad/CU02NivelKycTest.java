package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.ConsultarNivelKyc;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionTemplate;

/** Contrato de nivel de KYC: correcto (propio y backoffice), límite (inexistente) e inválido (ajeno). */
class CU02NivelKycTest {
    private static DSLContext dsl;
    private static TransactionTemplate tx;
    private static FixturaDeIdentidad fixtura;
    private static ConsultarNivelKyc consulta;

    @BeforeAll
    static void preparar() {
        var pg = BaseDePrueba.contenedor();
        var ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        fixtura = new FixturaDeIdentidad(dsl);
        consulta = new ConsultarNivelKyc(new Datos(dsl));
    }

    private static String telefono() {
        return "+591" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000));
    }

    private ContextoSesion ctx(UUID usuario, String rol) {
        return ContextoSesion.de(usuario, rol, new Traza(UUID.randomUUID().toString()));
    }

    @Test
    void cadaPersonaLeeSuPropioNivel() {
        UUID usuario = fixtura.usuario(telefono());
        assertThat(tx.<String>execute(s -> consulta.ejecutar(usuario, ctx(usuario, "PARTICIPANTE"))))
                .isEqualTo("BASICO");
    }

    @Test
    void unParticipanteNoLeeElNivelDeOtraPersona() {
        UUID usuario = fixtura.usuario(telefono());
        UUID otro = fixtura.usuario(telefono());
        assertThatThrownBy(() -> tx.execute(s -> consulta.ejecutar(usuario, ctx(otro, "PARTICIPANTE"))))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> tx.execute(s -> consulta.ejecutar(usuario, ctx(otro, "ORGANIZADOR"))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void personaInexistenteNoDevuelveUnNivelInventado() {
        UUID fantasma = UUID.randomUUID();
        assertThatThrownBy(() -> tx.execute(s -> consulta.ejecutar(fantasma, ctx(fantasma, "PARTICIPANTE"))))
                .isInstanceOf(ErrorDeNegocio.class);
    }
}
