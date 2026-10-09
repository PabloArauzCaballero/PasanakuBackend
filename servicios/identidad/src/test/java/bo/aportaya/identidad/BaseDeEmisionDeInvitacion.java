package bo.aportaya.identidad;

import bo.aportaya.identidad.aplicacion.ConsumirInvitacion;
import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.infraestructura.SecretoDeInvitacion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/** Armado compartido de las pruebas de emisión, reemisión y auditoría del token de invitación. */
abstract class BaseDeEmisionDeInvitacion {
    protected static DSLContext dsl;
    protected static TransactionTemplate tx;
    protected static FixturaDeIdentidad fixtura;
    protected static EmitirTokenDeInvitacion emitir;
    protected static ConsumirInvitacion consumir;
    protected static final AtomicReference<Instant> AHORA = new AtomicReference<>();
    protected static final ConsumirInvitacion.Origen ORIGEN =
            new ConsumirInvitacion.Origen("127.0.0.1", "prueba-sintetica");
    protected static final Instant BASE = Instant.parse("2031-03-01T12:00:00Z");
    protected static final int TOPE_DIARIO = 4;
    protected final ListAppender<ILoggingEvent> bitacora = new ListAppender<>();
    protected UUID emisor;
    protected UUID destinatario;
    protected UUID grupo;
    protected String telefono;

    @BeforeAll
    static void preparar() {
        var pg = BaseDePrueba.contenedor();
        var ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        fixtura = new FixturaDeIdentidad(dsl);
        var secretos = new SecretoDeInvitacion("secreto-sintetico-exclusivo-pruebas-cu69-emision-0");
        emitir = new EmitirTokenDeInvitacion(new Datos(dsl), AHORA::get, Ids.seguros(), secretos);
        consumir = new ConsumirInvitacion(new Datos(dsl), AHORA::get, secretos);
        // Vigente desde 2031: aísla estas pruebas de la política de otras clases (que usan 2030).
        politica(BASE, TOPE_DIARIO, false);
        // Otra política más nueva, que SÍ invalida las anteriores, para el caso explícito.
        politica(BASE.plusSeconds(86400L * 400), 50, true);
    }

    private static void politica(Instant desde, int tope, boolean invalida) {
        dsl.execute(
                """
            INSERT INTO identidad.politica_token
            (proposito,ttl_segundos,longitud_codigo,max_intentos_validacion,max_reenvios_por_hora,
             cooldown_reenvio_segundos,max_emisiones_por_dia,canales_permitidos,exige_dispositivo_conocido,
             invalida_anteriores,vigente_desde)
            VALUES ('INVITACION_GRUPO',600,32,3,2,60,?,'ENLACE',false,?,?::timestamptz)
            ON CONFLICT DO NOTHING
            """,
                tope,
                invalida,
                desde.toString());
    }

    @BeforeEach
    void caso() {
        AHORA.set(BASE.plusSeconds(60));
        telefono = "+591" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000));
        destinatario = fixtura.usuario(telefono);
        emisor = fixtura.usuario(
                "+591" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000)));
        grupo = UUID.randomUUID();
        bitacora.start();
        for (String nombre : List.of(EmitirTokenDeInvitacion.class.getName(), ConsumirInvitacion.class.getName())) {
            ((Logger) LoggerFactory.getLogger(nombre)).addAppender(bitacora);
        }
    }

    @AfterEach
    void limpiar() {
        for (String nombre : List.of(EmitirTokenDeInvitacion.class.getName(), ConsumirInvitacion.class.getName())) {
            ((Logger) LoggerFactory.getLogger(nombre)).detachAppender(bitacora);
        }
        bitacora.stop();
        bitacora.list.clear();
    }

    protected UUID grupoDe(EmitirTokenDeInvitacion.Emitido token) {
        return dsl.fetchOne(
                        "SELECT grupo_destino_id FROM identidad.alcance_invitacion WHERE token_id=?", token.tokenId())
                .get(0, UUID.class);
    }

    protected List<String> resultados(EmitirTokenDeInvitacion.Emitido token) {
        return dsl.fetch(
                        "SELECT resultado FROM identidad.intento_validacion_token WHERE token_id=? ORDER BY fecha_hora",
                        token.tokenId())
                .getValues(0, String.class);
    }

    protected String estado(EmitirTokenDeInvitacion.Emitido token) {
        return dsl.fetchOne("SELECT estado FROM identidad.token_verificacion WHERE id=?", token.tokenId())
                .get(0, String.class);
    }

    protected String motivo(EmitirTokenDeInvitacion.Emitido token) {
        return dsl.fetchOne("SELECT motivo_invalidacion FROM identidad.token_verificacion WHERE id=?", token.tokenId())
                .get(0, String.class);
    }

    protected EmitirTokenDeInvitacion.Entrada entrada(UUID clave, UUID grupoId, String tel) {
        return new EmitirTokenDeInvitacion.Entrada(clave, grupoId, tel, "ENLACE", "127.0.0.1", "prueba-sintetica");
    }

    protected EmitirTokenDeInvitacion.Emitido emitir(UUID clave, UUID grupoId, String tel) {
        return tx.execute(s -> emitir.ejecutar(entrada(clave, grupoId, tel), ctx(emisor)));
    }

    protected EmitirTokenDeInvitacion.Emitido emitirMismo(UUID clave, EmitirTokenDeInvitacion.Emitido original) {
        return tx.execute(s -> emitir.ejecutar(entrada(clave, grupoDe(original), telefono), ctx(emisor)));
    }

    protected java.util.Optional<ConsumirInvitacion.Consumo> canjear(
            EmitirTokenDeInvitacion.Emitido token, UUID grupoId, UUID usuario) {
        return canjearConSecreto(token, grupoId, usuario, token.token());
    }

    protected java.util.Optional<ConsumirInvitacion.Consumo> canjearConSecreto(
            EmitirTokenDeInvitacion.Emitido token, UUID grupoId, UUID usuario, String secreto) {
        // Cada intento ocurre un segundo después: el orden de la auditoría es el del tiempo.
        AHORA.set(AHORA.get().plusSeconds(1));
        return tx.execute(
                s -> consumir.ejecutar(token.tokenId(), grupoId, UUID.randomUUID(), secreto, ORIGEN, ctx(usuario)));
    }

    protected ContextoSesion ctx(UUID usuario) {
        return ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }
}
