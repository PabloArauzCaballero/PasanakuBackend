package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.dominio.BorradorOnboardingDatos;
import bo.aportaya.identidad.infraestructura.BorradorOnboardingRepositorio;
import bo.aportaya.identidad.infraestructura.ProteccionDeVerificacionCorreo;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
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
 * La ruta sin sesion {@code PUT /usuarios/verificaciones/correo/{id}/borrador} contra la base
 * real: solo escribe quien ya confirmo el codigo de ESE correo. Es el control compensatorio que
 * declara {@code PoliticaDeAcceso} para {@code guardarBorradorOnboarding}.
 */
class BorradorOnboardingGuardadoRepositorioTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-21T12:00:00Z");

    private static DSLContext dsl;
    private static TransactionTemplate tx;
    private static ProteccionDeVerificacionCorreo proteccion;
    private static BorradorOnboardingRepositorio repositorio;

    @BeforeAll
    static void preparar() {
        var pg = BaseDePrueba.contenedor();
        var ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        proteccion = new ProteccionDeVerificacionCorreo("pimienta-de-prueba");
        repositorio = new BorradorOnboardingRepositorio(new Datos(dsl), proteccion);
        dsl.execute(
                """
                INSERT INTO identidad.politica_token
                    (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion,
                     max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia,
                     canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde)
                VALUES ('VERIFICACION_CORREO', 600, 6, 5, 2, 60, 5, 'CORREO', false, true,
                        '2026-10-07T00:00:00-04:00'::timestamptz)
                ON CONFLICT (proposito, vigente_desde) DO NOTHING
                """);
    }

    private static UUID token(String correo, String estado) {
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO identidad.token_verificacion
                    (id, usuario_id, politica_id, tipo_token, proposito, hash_token, algoritmo_hash,
                     canal_entrega, destino_enmascarado, estado, expira_en, max_intentos, reenvios,
                     ip_origen, agente_usuario, correlation_id, clave_idempotencia, firma_hmac)
                SELECT ?, NULL, p.id, 'OTP', 'VERIFICACION_CORREO', gen_random_uuid()::text, 'HMAC-SHA256',
                       'CORREO', 'a***@example.com', ?, now() + interval '10 minutes', 5, 0,
                       '127.0.0.1'::inet, 'prueba', gen_random_uuid(), gen_random_uuid()::text, ?
                  FROM identidad.politica_token p
                 WHERE p.proposito = 'VERIFICACION_CORREO'
                 LIMIT 1
                """,
                id,
                estado,
                proteccion.huellaDestino(correo));
        return id;
    }

    private static BorradorOnboardingDatos borrador(String correo) {
        return new BorradorOnboardingDatos(
                correo,
                "+59171234567",
                "Ana",
                "Quispe",
                LocalDate.of(1995, 6, 15),
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
    }

    private static ContextoSesion ctx() {
        return ContextoSesion.deSistema(
                UUID.fromString("00000000-0000-4000-8000-000000000001"),
                new Traza(UUID.randomUUID().toString()));
    }

    /** El repositorio corre dentro de la transaccion que abre su llamador (conContexto). */
    private static void guardar(UUID id, BorradorOnboardingDatos datos, OffsetDateTime ahora, ContextoSesion ctx) {
        tx.execute(s -> repositorio.guardar(id, datos, ahora, ctx));
    }

    private static int borradoresDe(String correo) {
        return (Integer)
                dsl.fetchValue("SELECT count(*)::int FROM identidad.borrador_onboarding WHERE correo = ?", correo);
    }

    @Test
    @DisplayName("guardar · con el codigo ya confirmado (CONSUMIDO) y su correo, el borrador queda guardado")
    void conCodigoConfirmadoGuarda() {
        String correo = "confirmado-" + UUID.randomUUID() + "@example.com";
        UUID id = token(correo, "CONSUMIDO");

        guardar(id, borrador(correo), AHORA, ctx());

        assertThat(borradoresDe(correo)).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza · el codigo enviado pero NO confirmado (ENVIADO) no alcanza para guardar")
    void sinConfirmarElCodigoRechaza() {
        String correo = "enviado-" + UUID.randomUUID() + "@example.com";
        UUID id = token(correo, "ENVIADO");

        assertThatThrownBy(() -> guardar(id, borrador(correo), AHORA, ctx())).isInstanceOf(ErrorDeNegocio.class);
        assertThat(borradoresDe(correo)).isZero();
    }

    @Test
    @DisplayName("rechaza · un id de verificacion confirmado no escribe el borrador de OTRO correo")
    void otroCorreoRechaza() {
        String propio = "propio-" + UUID.randomUUID() + "@example.com";
        String ajeno = "ajeno-" + UUID.randomUUID() + "@example.com";
        UUID id = token(propio, "CONSUMIDO");

        assertThatThrownBy(() -> guardar(id, borrador(ajeno), AHORA, ctx())).isInstanceOf(ErrorDeNegocio.class);
        assertThat(borradoresDe(ajeno)).isZero();
    }

    @Test
    @DisplayName("rechaza · un id de verificacion inexistente no escribe nada")
    void idInexistenteRechaza() {
        String correo = "inexistente-" + UUID.randomUUID() + "@example.com";

        assertThatThrownBy(() -> guardar(UUID.randomUUID(), borrador(correo), AHORA, ctx()))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(borradoresDe(correo)).isZero();
    }
}
