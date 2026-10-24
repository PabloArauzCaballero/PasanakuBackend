package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.dominio.puertos.HasheadorDeCredencial;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.Argon2Hasheador;
import bo.aportaya.identidad.infraestructura.RolesYPermisosRepositorio;
import bo.aportaya.identidad.infraestructura.UsuariosInternosRepositorio;
import bo.aportaya.identidad.web.generado.modelo.EntradaAltaUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoDetalle;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.OffsetDateTime;
import java.util.UUID;
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
 * El alta de personal interno contra la base real (Testcontainers): cubre
 * {@link UsuariosInternosRepositorio#crear}.
 *
 * <p>Existe porque el alta devolvia 422 {@code AP-BD-03} en toda base real: insertaba
 * {@code nivel_kyc = 'ESTANDAR'} (la restriccion admite BASICO, COMPLETO, INTERMEDIO y NINGUNO)
 * y dejaba sin {@code parametros_kdf} una columna obligatoria. Ninguna prueba con el repositorio
 * simulado lo podia ver: hace falta la restriccion de la base.
 */
class UsuariosInternosRepositorioTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-24T12:00:00Z");

    private static DSLContext dsl;
    private static TransactionTemplate tx;
    private static FixturaDeIdentidad fixtura;
    private static UsuariosInternosRepositorio repositorio;
    private static HasheadorDeCredencial hasheador;

    @BeforeAll
    static void preparar() {
        var pg = BaseDePrueba.contenedor();
        var ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        fixtura = new FixturaDeIdentidad(dsl);
        var accesos = new AccesosRepositorio();
        hasheador = new Argon2Hasheador("pimienta-de-prueba");
        repositorio = new UsuariosInternosRepositorio(accesos, hasheador, new RolesYPermisosRepositorio(accesos));
    }

    /** E.164 boliviano de ocho digitos, distinto en cada llamada: la base de pruebas es compartida. */
    private static String telefonoUnico() {
        return "+5917%07d".formatted(Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000L));
    }

    private static EntradaAltaUsuarioInterno entrada(String sufijo) {
        return new EntradaAltaUsuarioInterno(
                "Prueba",
                "Contable",
                "prueba." + sufijo + "@ejemplo.test",
                telefonoUnico(),
                "Finanzas",
                "Analista de pruebas",
                "CONTABILIDAD");
    }

    @Test
    @DisplayName("crear · el alta de un operador con un rol del catalogo termina, empieza sin KYC y con KDF informado")
    void crearDejaAlOperadorConNivelKycNingunoYParametrosKdf() {
        UUID otorgante = fixtura.usuario(telefonoUnico());
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        UsuarioInternoDetalle detalle = tx.execute(s -> repositorio.crear(dsl, entrada(sufijo), otorgante, AHORA));

        assertThat(detalle).isNotNull();
        assertThat(detalle.getRoles()).containsExactly("CONTABILIDAD");
        assertThat(detalle.getEstado()).isEqualTo("ACTIVO");
        assertThat(detalle.getDebeCambiarContrasena()).isTrue();

        String nivelKyc = dsl.resultQuery(
                        "SELECT nivel_kyc FROM identidad.usuario WHERE id = ?", detalle.getUsuarioId())
                .fetchOne(0, String.class);
        assertThat(nivelKyc)
                .as("un operador nuevo no verifico nada: parte de NINGUNO, igual que el registro")
                .isEqualTo("NINGUNO");

        var credencial = dsl.fetchOne(
                """
                SELECT algoritmo, parametros_kdf::text AS kdf, requiere_cambio
                FROM identidad.credencial_acceso WHERE usuario_id = ?
                """,
                detalle.getUsuarioId());
        assertThat(credencial).isNotNull();
        assertThat(credencial.get("algoritmo", String.class))
                .isEqualTo(hasheador.parametros().algoritmo());
        assertThat(credencial.get("kdf", String.class)).contains("iteraciones").contains("memoriaKiB");
        assertThat(credencial.get("requiere_cambio", Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("listar · el operador recien dado de alta aparece en la lista de personal interno")
    void elOperadorDeAltaApareceEnLaLista() {
        UUID otorgante = fixtura.usuario(telefonoUnico());
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        UsuarioInternoDetalle detalle = tx.execute(s -> repositorio.crear(dsl, entrada(sufijo), otorgante, AHORA));

        var lista = tx.execute(s -> repositorio.listar(dsl, null, null, "prueba." + sufijo, AHORA));

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getUsuarioId()).isEqualTo(detalle.getUsuarioId());
    }
}
