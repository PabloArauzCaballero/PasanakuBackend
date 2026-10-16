package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.ConsumirInvitacion;
import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.infraestructura.SecretoDeInvitacion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

class CU69TokenInvitacionTest {
    private static DSLContext dsl;
    private static TransactionTemplate tx;
    private static FixturaDeIdentidad fixtura;
    private static EmitirTokenDeInvitacion emitir;
    private static ConsumirInvitacion consumir;
    private static final AtomicReference<Instant> AHORA = new AtomicReference<>();
    private static final ConsumirInvitacion.Origen ORIGEN =
            new ConsumirInvitacion.Origen("127.0.0.1", "prueba-sintetica");
    private UUID emisor;
    private UUID destinatario;
    private UUID grupo;
    private UUID clave;
    private String telefono;

    @BeforeAll
    static void preparar() {
        var pg = BaseDePrueba.contenedor();
        var ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        fixtura = new FixturaDeIdentidad(dsl);
        var secretos = new SecretoDeInvitacion("secreto-sintetico-exclusivo-pruebas-cu69-0000");
        emitir = new EmitirTokenDeInvitacion(new Datos(dsl), AHORA::get, Ids.seguros(), secretos);
        consumir = new ConsumirInvitacion(new Datos(dsl), AHORA::get, secretos);
        dsl.execute(
                """
            INSERT INTO identidad.politica_token
            (proposito,ttl_segundos,longitud_codigo,max_intentos_validacion,max_reenvios_por_hora,
             cooldown_reenvio_segundos,max_emisiones_por_dia,canales_permitidos,exige_dispositivo_conocido,
             invalida_anteriores,vigente_desde)
            VALUES ('INVITACION_GRUPO',600,32,3,2,60,20,'ENLACE',false,false,'2029-01-01'::timestamptz)
            """);
    }

    @BeforeEach
    void caso() {
        AHORA.set(Instant.parse("2030-01-01T12:00:00Z"));
        String sufijo = String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000));
        telefono = "+591" + sufijo;
        destinatario = fixtura.usuario(telefono);
        emisor = fixtura.usuario(
                "+591" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000)));
        grupo = UUID.randomUUID();
        clave = UUID.randomUUID();
    }

    @Test
    void emisionRecuperableSinSecretoEnLaBase() {
        var uno = emitir();
        assertThat(emitir()).isEqualTo(uno);
        assertThat(uno.token()).matches("[0-9a-f]{64}");
        assertThat(uno.toString()).doesNotContain(uno.token());
        assertThat(dsl.fetchOne("SELECT hash_token FROM identidad.token_verificacion WHERE id=?", uno.tokenId())
                        .get(0))
                .isNotEqualTo(uno.token());
        assertThat(dsl.fetchOne("SELECT count(*) FROM identidad.alcance_invitacion WHERE clave_emision=?", clave)
                        .get(0, Integer.class))
                .isEqualTo(1);
    }

    @Test
    void mismaClaveConOtroGrupoNoReutilizaElToken() {
        emitir();
        grupo = UUID.randomUUID();
        assertThatThrownBy(this::emitir).isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    void soloDestinatarioYGrupoCorrectos() {
        var token = emitir();
        assertThat(canjear(token, grupo, emisor, UUID.randomUUID(), token.token()))
                .isEmpty();
        assertThat(canjear(token, UUID.randomUUID(), destinatario, UUID.randomUUID(), token.token()))
                .isEmpty();
        assertThat(canjear(token, grupo, destinatario, UUID.randomUUID(), token.token()))
                .isPresent();
    }

    @Test
    void reintentoMismaClaveRecuperaPeroOtraClaveNoConsumeDosVeces() {
        var token = emitir();
        var canje = UUID.randomUUID();
        var primero = canjear(token, grupo, destinatario, canje, token.token());
        assertThat(primero).isPresent();
        assertThat(canjear(token, grupo, destinatario, canje, token.token())).isEqualTo(primero);
        assertThat(canjear(token, grupo, destinatario, UUID.randomUUID(), token.token()))
                .isEmpty();
    }

    @Test
    void tresIntentosIncorrectosPersistenYBloquean() {
        var token = emitir();
        for (int i = 0; i < 3; i++)
            assertThat(canjear(token, grupo, destinatario, UUID.randomUUID(), "0".repeat(64)))
                    .isEmpty();
        assertThat(dsl.fetchOne(
                                "SELECT intentos_fallidos FROM identidad.token_verificacion WHERE id=?",
                                token.tokenId())
                        .get(0, Integer.class))
                .isEqualTo(3);
        assertThat(canjear(token, grupo, destinatario, UUID.randomUUID(), token.token()))
                .isEmpty();
    }

    @Test
    void vencimientoExactoNoAdmiteConsumo() {
        var token = emitir();
        AHORA.set(token.expiraEn().toInstant());
        assertThat(canjear(token, grupo, destinatario, UUID.randomUUID(), token.token()))
                .isEmpty();
    }

    @Test
    void revocaSoloElEmisor() {
        var token = emitir();
        assertThat(tx.<Boolean>execute(s -> consumir.revocar(token.tokenId(), ctx(destinatario))))
                .isFalse();
        assertThat(tx.<Boolean>execute(s -> consumir.revocar(token.tokenId(), ctx(emisor))))
                .isTrue();
        assertThat(canjear(token, grupo, destinatario, UUID.randomUUID(), token.token()))
                .isEmpty();
    }

    @Test
    void dosConsumosRealmenteConcurrentesSoloUnoGana() throws Exception {
        var token = emitir();
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> {
                inicio.await();
                return canjear(token, grupo, destinatario, UUID.randomUUID(), token.token())
                        .isPresent();
            });
            var b = executor.submit(() -> {
                inicio.await();
                return canjear(token, grupo, destinatario, UUID.randomUUID(), token.token())
                        .isPresent();
            });
            inicio.countDown();
            assertThat((a.get(10, TimeUnit.SECONDS) ? 1 : 0) + (b.get(10, TimeUnit.SECONDS) ? 1 : 0))
                    .isEqualTo(1);
        }
    }

    @Test
    void permisosRealesPermitenEmitirYConsumirSoloAlDestinatario() {
        var token = tx.execute(s -> {
            dsl.execute("SET LOCAL ROLE svc_identidad");
            return emitir.ejecutar(entrada(), ctx(emisor));
        });
        var resultado = tx.execute(s -> {
            dsl.execute("SET LOCAL ROLE svc_identidad");
            return consumir.ejecutar(
                    token.tokenId(), grupo, UUID.randomUUID(), token.token(), ORIGEN, ctx(destinatario));
        });
        assertThat(resultado).isPresent();
    }

    private EmitirTokenDeInvitacion.Entrada entrada() {
        return new EmitirTokenDeInvitacion.Entrada(clave, grupo, telefono, "ENLACE", "127.0.0.1", "prueba-sintetica");
    }

    private EmitirTokenDeInvitacion.Emitido emitir() {
        return tx.execute(s -> emitir.ejecutar(entrada(), ctx(emisor)));
    }

    private Optional<ConsumirInvitacion.Consumo> canjear(
            EmitirTokenDeInvitacion.Emitido token, UUID grupoId, UUID usuario, UUID key, String secreto) {
        return tx.execute(s -> consumir.ejecutar(token.tokenId(), grupoId, key, secreto, ORIGEN, ctx(usuario)));
    }

    private ContextoSesion ctx(UUID usuario) {
        return ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }
}
