package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.infraestructura.SecretoDeInvitacion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Carril P (H2) · la politica de token {@code INVITACION_GRUPO} es DATO VERSIONADO: rige la fila con {@code vigente_desde}
 * mas reciente que ya paso. Agregar el canal ENLACE (el de la app) es una version nueva, no la edicion de la fila sembrada
 * (la siembra es {@code DO NOTHING}: una base poblada, como TEST, conservaba la vieja y {@code ENLACE} daba 500).
 *
 * <p>Las versiones de la prueba son las de {@code seeders/minimos/13-politicas-de-token.json} con fechas propias del
 * ensayo: la vieja (SMS,WHATSAPP,CORREO), la nueva (con ENLACE) y una futura que todavia no rige.
 */
class CU69PoliticaVersionadaTest {
    private static DSLContext dsl;
    private static TransactionTemplate tx;
    private static FixturaDeIdentidad fixtura;
    private static EmitirTokenDeInvitacion emitir;
    private static final AtomicReference<Instant> AHORA = new AtomicReference<>();
    private UUID emisor;
    private String telefono;

    @BeforeAll
    static void preparar() {
        var pg = BaseDePrueba.contenedor();
        var ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        fixtura = new FixturaDeIdentidad(dsl);
        emitir = new EmitirTokenDeInvitacion(
                new Datos(dsl),
                AHORA::get,
                Ids.seguros(),
                new SecretoDeInvitacion("secreto-sintetico-exclusivo-pruebas-politica-0000"));
        version("2040-01-01", "SMS,WHATSAPP,CORREO");
        version("2041-01-01", "ENLACE,SMS,WHATSAPP,CORREO");
        version("2042-01-01", "SMS");
    }

    private static void version(String desde, String canales) {
        dsl.execute(
                """
                INSERT INTO identidad.politica_token
                (proposito,ttl_segundos,longitud_codigo,max_intentos_validacion,max_reenvios_por_hora,
                 cooldown_reenvio_segundos,max_emisiones_por_dia,canales_permitidos,exige_dispositivo_conocido,
                 invalida_anteriores,vigente_desde)
                VALUES ('INVITACION_GRUPO',604800,32,5,2,300,20,?,false,false,?::timestamptz)
                """,
                canales,
                desde);
    }

    @BeforeEach
    void caso() {
        telefono = "+591" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000));
        emisor = fixtura.usuario("+591" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000)));
    }

    private EmitirTokenDeInvitacion.Emitido emitir(String canal) {
        var entrada = new EmitirTokenDeInvitacion.Entrada(
                UUID.randomUUID(), UUID.randomUUID(), telefono, canal, "127.0.0.1", "prueba-sintetica");
        return tx.execute(s -> emitir.ejecutar(
                entrada,
                ContextoSesion.de(emisor, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()))));
    }

    @Test
    @DisplayName("rechaza ENLACE mientras rige la version vieja (la que conservaba TEST) y acepta los canales que tenia")
    void conLaVersionViejaEnlaceSeRechaza() {
        AHORA.set(Instant.parse("2040-06-01T12:00:00Z"));

        assertThatThrownBy(() -> emitir("ENLACE"))
                .isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(e.codigo().valor())
                        .isEqualTo("AP-CU69-05"));
        assertThat(emitir("SMS").tokenId()).isNotNull();
    }

    @Test
    @DisplayName("la version nueva, con vigente_desde posterior, permite ENLACE y conserva los canales anteriores")
    void conLaVersionNuevaEnlaceSePermite() {
        AHORA.set(Instant.parse("2041-06-01T12:00:00Z"));

        assertThat(emitir("ENLACE").tokenId()).isNotNull();
        assertThat(emitir("SMS").tokenId()).isNotNull();
        assertThat(emitir("CORREO").tokenId()).isNotNull();
    }

    @Test
    @DisplayName("una version futura todavia no rige: ni quita ni agrega canales antes de su vigente_desde")
    void unaVersionFuturaNoRige() {
        AHORA.set(Instant.parse("2041-06-01T12:00:00Z"));

        // La del 2042 solo permite SMS: si rigiera, ENLACE se rechazaria.
        assertThat(emitir("ENLACE").tokenId()).isNotNull();

        AHORA.set(Instant.parse("2042-06-01T12:00:00Z"));
        assertThatThrownBy(() -> emitir("ENLACE")).isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName("rechaza un canal que ninguna version permite")
    void unCanalDesconocidoSiempreSeRechaza() {
        AHORA.set(Instant.parse("2041-06-01T12:00:00Z"));

        assertThatThrownBy(() -> emitir("PALOMA_MENSAJERA")).isInstanceOf(ErrorDeNegocio.class);
    }
}
