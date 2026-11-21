package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.CU04Autenticar;
import bo.aportaya.identidad.aplicacion.EnrolarTotp;
import bo.aportaya.identidad.aplicacion.EntradaAutenticacion;
import bo.aportaya.identidad.dominio.PoliticaDeIntentos;
import bo.aportaya.identidad.dominio.ResultadoDeAutenticacion;
import bo.aportaya.identidad.infraestructura.AccesoRepositorio;
import bo.aportaya.identidad.infraestructura.Argon2Hasheador;
import bo.aportaya.identidad.infraestructura.CorreoDeAccesoRepositorio;
import bo.aportaya.identidad.infraestructura.DesafioLocal;
import bo.aportaya.identidad.infraestructura.DesafioPorTipo;
import bo.aportaya.identidad.infraestructura.FactorTotpRepositorio;
import bo.aportaya.identidad.infraestructura.LlavesDeFactor;
import bo.aportaya.identidad.infraestructura.UsuarioRepositorio;
import bo.aportaya.identidad.infraestructura.VerificadorTotp;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.plataforma.web.clientes.CodigoTotp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F1.S3.M2 · IDCU-HUECO-077 de punta a punta contra PostgreSQL real: el titular enrola su
 * TOTP, recibe un rol de operador y entra con un codigo calculado por su app. {@code 000000}
 * no entra, un codigo no se reusa, y un enrolamiento ajeno no se confirma.
 *
 * <p>Datos y llave SINTETICOS (regla 97.6). Corre en {@code integrationTest}.
 */
class CU04TotpTest extends BaseDeCU04 {

    private static final String LLAVE = "llave-sintetica-de-integracion-32-caracteres";

    private EnrolarTotp enrolar;
    private DesafioPorTipo desafio;
    private CU04Autenticar cu04ConTotp;

    @BeforeEach
    void armarTotp() {
        var datos = new Datos(dsl);
        var factores = new FactorTotpRepositorio();
        var llaves = new LlavesDeFactor("1:" + LLAVE, "1");
        var verificador =
                new VerificadorTotp(datos, factores, llaves, Reloj.delSistema(), transaccion.getTransactionManager());
        enrolar = new EnrolarTotp(datos, factores, llaves, verificador, new Outbox("identidad"), Reloj.delSistema());
        desafio = new DesafioPorTipo(new DesafioLocal(correo), verificador);
        cu04ConTotp = new CU04Autenticar(
                datos,
                new UsuarioRepositorio(),
                new AccesoRepositorio(),
                new Argon2Hasheador("pimienta-de-prueba"),
                desafio,
                new CorreoDeAccesoRepositorio(proteccion),
                new Outbox("identidad"),
                Reloj.delSistema());
    }

    @Test
    @DisplayName(
            "Dado un usuario que enroló y confirmó su TOTP y después recibió un rol de operador · Cuando inicia sesión con 000000, con el código vigente y con ese mismo código otra vez · Entonces solo entra con el código vigente la primera vez · Y 000000 y el código reusado responden AP-CU04-04")
    void operadorEntraConTotpReal() {
        String telefono = telefonoUnico();
        UUID usuario = participanteConCredencial(telefono);
        String semilla = enrolarYConfirmar(usuario);
        fixtura.asignarRol(usuario, fixtura.rolGlobal("OPT" + telefono.substring(8)));

        ResultadoDeAutenticacion conCeros = autenticar(telefono, Optional.of("000000"));
        assertThat(conCeros.exitoso()).isFalse();
        assertThat(conCeros.codigo()).map(c -> c.valor()).contains("AP-CU04-04");
        assertThat(sesionesDe(usuario)).isZero();

        String vigente = CodigoTotp.de(semilla, Instant.now());
        ResultadoDeAutenticacion conVigente = autenticar(telefono, Optional.of(vigente));
        assertThat(conVigente.exitoso()).as("rechazo: %s", conVigente.codigo()).isTrue();
        assertThat(conVigente.esOperador()).isTrue();
        assertThat(sesionesDe(usuario)).isEqualTo(1);

        ResultadoDeAutenticacion reusado = autenticar(telefono, Optional.of(vigente));
        assertThat(reusado.exitoso()).isFalse();
        assertThat(reusado.codigo()).map(c -> c.valor()).contains("AP-CU04-04");
        assertThat(sesionesDe(usuario)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un operador sin TOTP enrolado · Cuando inicia sesión presentando 000000 · Entonces la respuesta es FACTOR_NO_ENROLADO (AP-CU04-06)")
    void sinEnrolamiento() {
        String telefono = telefonoUnico();
        UUID usuario = participanteConCredencial(telefono);
        fixtura.asignarRol(usuario, fixtura.rolGlobal("OPS" + telefono.substring(8)));

        ResultadoDeAutenticacion resultado = autenticar(telefono, Optional.of("000000"));

        assertThat(resultado.codigo()).map(c -> c.valor()).contains("AP-CU04-06");
    }

    @Test
    @DisplayName(
            "Dado un enrolamiento TOTP pendiente · Cuando se lo confirma con un código que no es de su semilla · Entonces se rechaza con AP-CU04-04 y el factor sigue inactivo")
    void confirmarConCodigoMalo() {
        UUID usuario = participanteConCredencial(telefonoUnico());
        var inicio = iniciar(ctx(usuario));
        String malo = "000000".equals(CodigoTotp.de(inicio.semillaBase32(), Instant.now())) ? "111111" : "000000";

        assertThatThrownBy(() -> confirmar(inicio.factorId(), malo, usuario))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no coincide");
        assertThat(dsl.fetchValue("SELECT activo FROM identidad.factor_mfa WHERE id = ?", inicio.factorId()))
                .isEqualTo(false);
    }

    @Test
    @DisplayName(
            "Dado el enrolamiento TOTP pendiente de otra persona · Cuando se lo intenta confirmar con el código correcto · Entonces se rechaza con AP-CU04-08")
    void enrolamientoAjeno() {
        UUID titular = participanteConCredencial(telefonoUnico());
        UUID otro = participanteConCredencial(telefonoUnico());
        var inicio = iniciar(ctx(titular));
        String vigente = CodigoTotp.de(inicio.semillaBase32(), Instant.now());

        assertThatThrownBy(() -> confirmar(inicio.factorId(), vigente, otro))
                .isInstanceOf(ErrorDeNegocio.class)
                .extracting(e -> ((ErrorDeNegocio) e).codigo().valor())
                .isEqualTo("AP-CU04-08");
    }

    @Test
    @DisplayName(
            "Dado un titular con un TOTP confirmado · Cuando inicia otro enrolamiento con su sesión · Entonces se rechaza con AP-CU04-09")
    void noReemplazaElConfirmado() {
        UUID usuario = participanteConCredencial(telefonoUnico());
        enrolarYConfirmar(usuario);

        assertThatThrownBy(() -> iniciar(ctx(usuario)))
                .isInstanceOf(ErrorDeNegocio.class)
                .extracting(e -> ((ErrorDeNegocio) e).codigo().valor())
                .isEqualTo("AP-CU04-09");
    }

    @Test
    @DisplayName(
            "Dado un enrolamiento TOTP iniciado · Cuando se lee su fila en factor_mfa · Entonces el secreto está cifrado con su version_llave y nunca en claro")
    void secretoCifrado() {
        UUID usuario = participanteConCredencial(telefonoUnico());
        var inicio = iniciar(ctx(usuario));

        var fila = dsl.fetchOne(
                "SELECT secreto_cifrado, version_llave FROM identidad.factor_mfa WHERE id = ?", inicio.factorId());
        assertThat(fila.get("secreto_cifrado", String.class))
                .startsWith("enc:v1:")
                .doesNotContain(inicio.semillaBase32());
        assertThat(fila.get("version_llave", Short.class)).isEqualTo((short) 1);
    }

    private String enrolarYConfirmar(UUID usuario) {
        var inicio = iniciar(ctx(usuario));
        // Se confirma con el codigo del paso ANTERIOR (dentro de la ventana): confirmar tambien
        // gasta el paso, y el ingreso de la prueba tiene que poder usar el codigo vigente.
        confirmar(
                inicio.factorId(),
                CodigoTotp.de(inicio.semillaBase32(), Instant.now().minusSeconds(30)),
                usuario);
        return inicio.semillaBase32();
    }

    private EnrolarTotp.Inicio iniciar(ContextoSesion ctx) {
        return transaccion.execute(e -> enrolar.iniciar(ctx));
    }

    private void confirmar(UUID factorId, String codigo, UUID usuario) {
        transaccion.execute(e -> enrolar.confirmar(factorId, codigo, ctx(usuario)));
    }

    private static ContextoSesion ctx(UUID usuario) {
        return ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    private ResultadoDeAutenticacion autenticar(String telefono, Optional<String> factor) {
        var entrada = new EntradaAutenticacion(
                telefono,
                "clave-correcta".toCharArray(),
                "huella-" + telefono,
                "WEB",
                "127.0.0.1",
                "prueba",
                UUID.randomUUID().toString(),
                factor,
                false,
                Duration.ofHours(8));
        return transaccion.execute(
                e -> cu04ConTotp.ejecutar(entrada, new PoliticaDeIntentos(5, Duration.ofMinutes(30))));
    }

    private static String telefonoUnico() {
        return "+5917%07d".formatted(Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000L));
    }
}
