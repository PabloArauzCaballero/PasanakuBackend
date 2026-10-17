package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.ConsultarMiPerfil;
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
import org.springframework.transaction.support.TransactionTemplate;

/** Mi perfil (CU-07): correcto (correo verificado), límite (sin correo) e inválido (cuenta inexistente). */
class CU07MiPerfilTest {
    private static DSLContext dsl;
    private static TransactionTemplate tx;
    private static FixturaDeIdentidad fixtura;
    private static ConsultarMiPerfil consulta;

    @BeforeAll
    static void preparar() {
        var pg = BaseDePrueba.contenedor();
        var ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        fixtura = new FixturaDeIdentidad(dsl);
        consulta = new ConsultarMiPerfil(new Datos(dsl));
    }

    private static String telefono() {
        return "+591" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000));
    }

    private ContextoSesion ctx(UUID usuario) {
        return ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    @Test
    void devuelveElCorreoDeLaCuentaDeLaSesion() {
        UUID usuario = fixtura.usuario(telefono());
        String correo = "perfil-" + UUID.randomUUID() + "@correo.test";
        dsl.execute(
                "UPDATE identidad.usuario SET correo = ?, correo_verificado_en = now() WHERE id = ?", correo, usuario);

        var perfil = tx.execute(s -> consulta.ejecutar(ctx(usuario)));

        assertThat(perfil.usuarioId()).isEqualTo(usuario);
        assertThat(perfil.correo()).isEqualTo(correo);
        assertThat(perfil.correoVerificado()).isTrue();
    }

    @Test
    void unaCuentaSinCorreoLoDiceSinInventarUno() {
        UUID usuario = fixtura.usuario(telefono());
        dsl.execute("UPDATE identidad.usuario SET correo = NULL, correo_verificado_en = NULL WHERE id = ?", usuario);

        var perfil = tx.execute(s -> consulta.ejecutar(ctx(usuario)));

        assertThat(perfil.correo()).isNull();
        assertThat(perfil.correoVerificado()).isFalse();
    }

    @Test
    void unaSesionSinCuentaNoDevuelveDatos() {
        UUID fantasma = UUID.randomUUID();
        assertThatThrownBy(() -> tx.execute(s -> consulta.ejecutar(ctx(fantasma))))
                .isInstanceOf(ErrorDeNegocio.class);
    }
}
