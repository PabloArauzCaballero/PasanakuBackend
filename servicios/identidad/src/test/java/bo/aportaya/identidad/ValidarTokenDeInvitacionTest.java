package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.ValidarTokenDeInvitacion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

class ValidarTokenDeInvitacionTest {
    private static final String TELEFONO = "+59176543210";
    private static DSLContext dsl;
    private static TransactionTemplate transaccion;
    private static ValidarTokenDeInvitacion validar;
    private static EmitirTokenDeInvitacion emitir;

    @BeforeAll
    static void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        validar = new ValidarTokenDeInvitacion(new Datos(dsl));
        emitir = new EmitirTokenDeInvitacion(new Datos(dsl), Reloj.delSistema(), Ids.seguros());
    }

    @Test
    void soloElTitularDelTelefonoConElSecretoVigentePuedeUsarElEnlace() {
        UUID titular = new FixturaDeIdentidad(dsl).usuario(TELEFONO);
        UUID tercero = new FixturaDeIdentidad(dsl).usuario("+59176543211");
        UUID politica = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO identidad.politica_token
                  (id, proposito, ttl_segundos, longitud_codigo, max_intentos_validacion,
                   max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia,
                   canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde)
                VALUES (?, 'INVITACION_GRUPO', 604800, 32, 3, 3, 60, 10,
                        'ENLACE', false, false, now())
                """,
                politica);
        var emisor = ContextoSesion.de(
                titular, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
        var emitido = transaccion.execute(
                estado -> emitir.ejecutar("ENLACE", "+591*****10", UUID.randomUUID(), "127.0.0.1", "prueba", emisor));
        UUID token = emitido.tokenId();
        String secreto = emitido.token();

        assertThat(verificar(token, secreto, TELEFONO, titular)).isTrue();
        var ctx = ContextoSesion.de(
                titular, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
        assertThat(transaccion.execute(estado -> validar.ejecutar(token, secreto, TELEFONO, "COMPLETO", ctx)))
                .isFalse();
        assertThat(verificar(token, "f".repeat(64), TELEFONO, titular)).isFalse();
        assertThat(verificar(token, secreto, TELEFONO, tercero)).isFalse();
        assertThat(verificar(token, secreto, "+59176543211", titular)).isFalse();

        dsl.execute(
                "UPDATE identidad.token_verificacion SET expira_en = now() - interval '1 second' WHERE id = ?", token);
        assertThat(verificar(token, secreto, TELEFONO, titular)).isFalse();
    }

    private boolean verificar(UUID token, String secreto, String telefono, UUID usuario) {
        var ctx = ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
        return transaccion.execute(estado -> validar.ejecutar(token, secreto, telefono, "BASICO", ctx));
    }
}
