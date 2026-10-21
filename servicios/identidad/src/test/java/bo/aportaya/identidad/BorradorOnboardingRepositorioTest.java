package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.dominio.BorradorOnboardingDatos;
import bo.aportaya.identidad.dominio.ResultadoConfirmacionCorreo;
import bo.aportaya.identidad.infraestructura.BorradorOnboardingRepositorio;
import bo.aportaya.identidad.infraestructura.ProteccionDeVerificacionCorreo;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.LocalDate;
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
 * Borrador de onboarding: guarda, recupera, elimina y resuelve el resultado de confirmacion
 * contra la base real (Testcontainers). Cubre {@link BorradorOnboardingRepositorio}.
 */
class BorradorOnboardingRepositorioTest {

    private static DSLContext dsl;
    private static TransactionTemplate tx;
    private static FixturaDeIdentidad fixtura;
    private static BorradorOnboardingRepositorio repositorio;

    private static final String CORREO = "pablo+borrador@example.com";
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    @BeforeAll
    static void preparar() {
        var pg = BaseDePrueba.contenedor();
        var ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        fixtura = new FixturaDeIdentidad(dsl);
        repositorio = new BorradorOnboardingRepositorio(
                new Datos(dsl), new ProteccionDeVerificacionCorreo("pimienta-de-prueba"));
    }

    private static BorradorOnboardingDatos datosDePrueba(String paso) {
        return new BorradorOnboardingDatos(
                CORREO,
                "+59171234567",
                "Pablo",
                "Arauz",
                LocalDate.of(1995, 6, 15),
                "CI",
                "1234567",
                "SC",
                null,
                "DEPENDIENTE",
                "Ingeniero",
                "SUELDO",
                "1000",
                "2",
                paso);
    }

    private static ContextoSesion ctxSistema() {
        return ContextoSesion.deSistema(
                UUID.fromString("00000000-0000-4000-8000-000000000001"),
                new Traza(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("resolverResultado · sin borrador ni usuario incompleto devuelve verificada=true, incompleto=false")
    void sinBorradorNiUsuarioIncompletoDevuelveResultadoLimpio() {
        ResultadoConfirmacionCorreo resultado =
                tx.execute(s -> repositorio.resolverResultado(dsl, "inexistente@example.com", AHORA));

        assertThat(resultado.verificada()).isTrue();
        assertThat(resultado.onboardingIncompleto()).isFalse();
        assertThat(resultado.borrador()).isNull();
    }

    @Test
    @DisplayName("resolverResultado · con borrador vigente devuelve onboardingIncompleto=true y los datos")
    void conBorradorDevuelveOnboardingIncompleto() {
        String correoUnico = "borrador-" + UUID.randomUUID() + "@example.com";
        BorradorOnboardingDatos datos = new BorradorOnboardingDatos(
                correoUnico,
                "+59171111111",
                "Ana",
                "Quispe",
                LocalDate.of(1990, 1, 1),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "datos");

        // Insertar directamente en la base para no depender del guardado con token
        tx.execute(s -> {
            dsl.execute(
                    "INSERT INTO identidad.borrador_onboarding (correo, telefono_e164, nombres, apellidos, fecha_nacimiento, paso_pendiente, actualizado_en, expira_en) VALUES (?, ?, ?, ?, ?, ?, ?::timestamptz, ?::timestamptz)",
                    correoUnico,
                    "+59171111111",
                    "Ana",
                    "Quispe",
                    LocalDate.of(1990, 1, 1),
                    "datos",
                    AHORA,
                    AHORA.plusDays(7));
            return null;
        });

        ResultadoConfirmacionCorreo resultado = tx.execute(s -> repositorio.resolverResultado(dsl, correoUnico, AHORA));

        assertThat(resultado.verificada()).isTrue();
        assertThat(resultado.onboardingIncompleto()).isTrue();
        assertThat(resultado.borrador()).isNotNull();
        assertThat(resultado.borrador().nombres()).isEqualTo("Ana");
        assertThat(resultado.borrador().pasoPendiente()).isEqualTo("datos");
    }

    @Test
    @DisplayName("resolverResultado · borrador expirado no se devuelve (expira_en en el pasado)")
    void borradorExpiradoNoSeDevuelve() {
        String correoUnico = "expirado-" + UUID.randomUUID() + "@example.com";
        tx.execute(s -> {
            dsl.execute(
                    "INSERT INTO identidad.borrador_onboarding (correo, telefono_e164, nombres, apellidos, fecha_nacimiento, paso_pendiente, actualizado_en, expira_en) VALUES (?, ?, 'Viejo', 'Borrador', DATE '1990-01-01', 'datos', ?::timestamptz, ?::timestamptz)",
                    correoUnico,
                    "+59179999999",
                    AHORA.minusDays(10),
                    AHORA.minusDays(1));
            return null;
        });

        ResultadoConfirmacionCorreo resultado = tx.execute(s -> repositorio.resolverResultado(dsl, correoUnico, AHORA));

        assertThat(resultado.onboardingIncompleto()).isFalse();
        assertThat(resultado.borrador()).isNull();
    }

    @Test
    @DisplayName("resolverResultado · usuario con estado INCOMPLETO_KYC devuelve onboardingIncompleto=true")
    void usuarioIncompletoDevuelveOnboardingIncompleto() {
        UUID usuarioId = tx.execute(s -> {
            UUID id = UUID.randomUUID();
            String correoIncompleto = "incompleto-" + id + "@example.com";
            String telefono = "+59178" + id.toString().substring(0, 6);
            dsl.execute(
                    "INSERT INTO identidad.usuario (id, codigo_publico, nombres, apellidos, telefono_e164, correo, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro) VALUES (?, ?, 'Incompleto', 'KYC', ?, ?, DATE '1995-01-01', 'INCOMPLETO_KYC', 'BASICO', 'es', 'America/La_Paz', now())",
                    id,
                    "AY-" + id.toString().substring(0, 8),
                    telefono,
                    correoIncompleto);
            return id;
        });

        // Obtener el correo del usuario insertado
        String correoIncompleto = tx.<String>execute(
                s -> (String) dsl.fetchValue("SELECT correo FROM identidad.usuario WHERE id = ?", usuarioId));

        ResultadoConfirmacionCorreo resultado =
                tx.execute(s -> repositorio.resolverResultado(dsl, correoIncompleto, AHORA));

        assertThat(resultado.verificada()).isTrue();
        assertThat(resultado.onboardingIncompleto()).isTrue();
        assertThat(resultado.borrador()).isNotNull();
        assertThat(resultado.borrador().nombres()).isEqualTo("Incompleto");
    }

    @Test
    @DisplayName("eliminar · borra el borrador de la base de datos")
    void eliminarBorraBorrador() {
        String correoUnico = "eliminar-" + UUID.randomUUID() + "@example.com";
        tx.execute(s -> {
            dsl.execute(
                    "INSERT INTO identidad.borrador_onboarding (correo, telefono_e164, nombres, apellidos, fecha_nacimiento, paso_pendiente, actualizado_en, expira_en) VALUES (?, '+59179000000', 'A', 'B', DATE '2000-01-01', 'datos', now(), now() + interval '7 days')",
                    correoUnico);
            repositorio.eliminar(dsl, correoUnico);
            return null;
        });

        Integer count = (Integer)
                dsl.fetchValue("SELECT count(*)::int FROM identidad.borrador_onboarding WHERE correo = ?", correoUnico);
        assertThat(count).isZero();
    }
}
