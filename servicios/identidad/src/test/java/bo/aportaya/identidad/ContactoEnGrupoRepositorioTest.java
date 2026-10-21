package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.ConsultarContactoEnGrupo;
import bo.aportaya.identidad.infraestructura.ContactoEnGrupoRepositorio;
import bo.aportaya.identidad.infraestructura.clientes.GruposPorHttp;
import bo.aportaya.plataforma.datos.Datos;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Perfil del integrante (2026-10-21): el telefono de alguien contra PostgreSQL real, detras de la respuesta de grupos.
 * Grupos es un DOBLE declarado (la respuesta de {@code compartenGrupo}); la forma de su contrato la cubre
 * {@code GruposPorHttpPactoContratoTest}. Datos sinteticos.
 */
class ContactoEnGrupoRepositorioTest {

    private static DSLContext dsl;
    private static TransactionTemplate transaccion;
    private static FixturaDeIdentidad fixtura;
    private static ContactoEnGrupoRepositorio repositorio;

    @BeforeAll
    static void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        fixtura = new FixturaDeIdentidad(dsl);
        repositorio = new ContactoEnGrupoRepositorio(new Datos(dsl));
    }

    /** Grupos como doble: responde lo que la prueba diga, sin red. */
    private static final class GruposDoble extends GruposPorHttp {
        private final boolean comparten;

        GruposDoble(boolean comparten) {
            super(org.springframework.web.client.RestClient.builder(), "http://no-se-usa");
            this.comparten = comparten;
        }

        @Override
        public boolean compartenGrupo(UUID grupoId, UUID usuarioId) {
            return comparten;
        }
    }

    private static ContextoSesion de(UUID usuario) {
        return ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    private static String telefono() {
        return "+5917" + String.format("%07d", Math.abs(UUID.randomUUID().hashCode() % 10_000_000));
    }

    private static String consultar(boolean comparten, UUID quienPregunta, UUID otro) {
        var caso = new ConsultarContactoEnGrupo(new GruposDoble(comparten), repositorio);
        return transaccion.execute(s -> caso.ejecutar(UUID.randomUUID(), otro, de(quienPregunta)));
    }

    @Test
    @DisplayName("contacto · si grupos dice que comparten, se lee el telefono de la otra persona (no el propio)")
    void compartenLeeElTelefonoDelOtro() {
        String suyo = telefono();
        UUID beto = fixtura.usuario(suyo);
        UUID ana = fixtura.usuario(telefono());

        assertThat(consultar(true, ana, beto)).isEqualTo(suyo);
    }

    @Test
    @DisplayName("rechaza · si grupos dice que no comparten, es 403 y no se lee nada")
    void noCompartenEs403() {
        UUID beto = fixtura.usuario(telefono());
        UUID carla = fixtura.usuario(telefono());

        assertThatThrownBy(() -> consultar(false, carla, beto)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("rechaza · una persona que no existe es el mismo 403: la respuesta no cuenta quien existe")
    void inexistenteEsElMismo403() {
        UUID ana = fixtura.usuario(telefono());

        assertThatThrownBy(() -> consultar(true, ana, UUID.randomUUID())).isInstanceOf(AccessDeniedException.class);
    }
}
