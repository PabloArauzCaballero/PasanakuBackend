package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario.EntradaRegistro;
import bo.aportaya.identidad.aplicacion.CU02IniciarVerificacion;
import bo.aportaya.identidad.dominio.AperturaDeCuenta;
import bo.aportaya.identidad.dominio.DocumentoDeIdentidad;
import bo.aportaya.identidad.infraestructura.RegistroRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-01 sin documento y CU-02 diferido (2026-10-17): la cuenta se abre para conocer la app y la
 * verificacion se inicia despues, cuando la persona quiere operar con dinero.
 */
class CU01AltaSinDocumentoTest extends BaseDeCU01 {

    private static final Reloj RELOJ = Reloj.delSistema();
    private static CU02IniciarVerificacion iniciar;

    @BeforeAll
    static void armarVerificacion() {
        iniciar = new CU02IniciarVerificacion(new Datos(dsl), new RegistroRepositorio(), RELOJ);
    }

    private EntradaRegistro sinDocumento(String telefono) {
        var base = entrada(telefono, "0000000", true, true);
        return new EntradaRegistro(
                base.telefonoE164(),
                base.nombres(),
                base.apellidos(),
                base.fechaNacimiento(),
                base.correo(),
                base.canalVerificacion(),
                base.verificacionCorreoId(),
                null,
                null,
                base.contrasena(),
                base.aceptaContratos(),
                base.licenciaHabilitaBilletera(),
                base.ip(),
                base.agente());
    }

    private UUID abrirSinDocumento(String telefono) {
        return transaccion
                .execute(e -> registrarUsuario.ejecutar(sinDocumento(telefono), contexto()))
                .usuarioId();
    }

    private static DocumentoDeIdentidad carnet(String numero, LocalDate vence) {
        return DocumentoDeIdentidad.de(DocumentoDeIdentidad.Tipo.CI, numero, "pimienta-de-prueba", "BO", "LP", vence);
    }

    private UUID enviar(UUID usuario, DocumentoDeIdentidad documento) {
        return transaccion.execute(e -> iniciar.ejecutar(
                usuario,
                documento,
                "cifrado:" + documento.hashNumero(),
                UUID.randomUUID().toString()));
    }

    private String estadoDe(UUID usuario) {
        return dsl.fetchOne("SELECT estado FROM identidad.usuario WHERE id = ?", usuario)
                .get(0, String.class);
    }

    private long filas(String tabla, UUID usuario) {
        String consulta =
                switch (tabla) {
                    case "documento_identidad" ->
                        "SELECT count(*) FROM identidad.documento_identidad WHERE usuario_id = ?";
                    case "verificacion_kyc" -> "SELECT count(*) FROM identidad.verificacion_kyc WHERE usuario_id = ?";
                    default -> throw new IllegalArgumentException(tabla);
                };
        return dsl.fetchOne(consulta, usuario).get(0, Long.class);
    }

    @Test
    @DisplayName(
            "Dado un alta sin documento · Cuando se crea · Entonces la cuenta queda INCOMPLETO_KYC, con credencial y sin documento ni verificacion")
    void altaSinDocumento() {
        var salida = transaccion.execute(e -> registrarUsuario.ejecutar(sinDocumento("+59177090001"), contexto()));

        assertThat(salida.estado()).isEqualTo(AperturaDeCuenta.INCOMPLETO_KYC);
        assertThat(estadoDe(salida.usuarioId())).isEqualTo("INCOMPLETO_KYC");
        assertThat(hashDeLaCredencial(salida.usuarioId())).isPresent();
        assertThat(filas("documento_identidad", salida.usuarioId())).isZero();
        assertThat(filas("verificacion_kyc", salida.usuarioId())).isZero();
    }

    @Test
    @DisplayName(
            "Dada una cuenta sin documento · Cuando envia su carnet vigente · Entonces pasa a PENDIENTE_VERIFICACION con una verificacion EN_REVISION")
    void enviaSuDocumentoDespues() {
        UUID usuario = abrirSinDocumento("+59177090002");

        UUID verificacion = enviar(usuario, carnet("9090902", RELOJ.hoy().plusYears(3)));

        assertThat(estadoDe(usuario)).isEqualTo("PENDIENTE_VERIFICACION");
        assertThat(dsl.fetchOne(
                                "SELECT estado FROM identidad.verificacion_kyc WHERE id = ? AND usuario_id = ?",
                                verificacion,
                                usuario)
                        .get(0, String.class))
                .isEqualTo("EN_REVISION");
        assertThat(filas("documento_identidad", usuario)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una verificacion ya iniciada · Cuando se envia otra vez · Entonces AP-CU01-12 y sigue habiendo una sola")
    void noSeAbreDosVeces() {
        UUID usuario = abrirSinDocumento("+59177090003");
        enviar(usuario, carnet("9090903", RELOJ.hoy().plusYears(3)));

        assertThatThrownBy(() -> enviar(usuario, carnet("9090913", RELOJ.hoy().plusYears(3))))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU01-12"));
        assertThat(filas("verificacion_kyc", usuario)).isEqualTo(1);
        assertThat(filas("documento_identidad", usuario)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un carnet que ya tiene otra cuenta · Cuando se envia · Entonces AP-CU01-03 campo documento y la cuenta sigue INCOMPLETO_KYC")
    void documentoDeOtraCuenta() {
        registrar("+59177090004", "9090904");
        UUID usuario = abrirSinDocumento("+59177090005");

        assertThatThrownBy(() -> enviar(usuario, carnet("9090904", RELOJ.hoy().plusYears(3))))
                .isInstanceOfSatisfying(ErrorDeNegocio.class, e -> {
                    assertThat(e.codigo().valor()).isEqualTo("AP-CU01-03");
                    assertThat(e.detalle()).containsEntry("campo", "documento");
                });
        assertThat(estadoDe(usuario)).isEqualTo("INCOMPLETO_KYC");
        assertThat(filas("verificacion_kyc", usuario)).isZero();
    }

    @Test
    @DisplayName(
            "Dado un carnet vencido o sin fecha · Cuando se envia · Entonces se rechaza (AP-CU01-07 / AP-CU01-10) sin tocar la cuenta")
    void documentoVencidoOSinFecha() {
        UUID usuario = abrirSinDocumento("+59177090006");

        assertThatThrownBy(() -> enviar(usuario, carnet("9090906", RELOJ.hoy().minusDays(1))))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU01-07"));
        assertThatThrownBy(() -> enviar(usuario, carnet("9090906", null)))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU01-10"));
        assertThat(estadoDe(usuario)).isEqualTo("INCOMPLETO_KYC");
    }

    @Test
    @DisplayName(
            "Dado un carnet que vence hoy · Cuando se envia · Entonces abre la verificacion porque el ultimo dia todavia vale")
    void venceHoyTodaviaVale() {
        UUID usuario = abrirSinDocumento("+59177090007");

        enviar(usuario, carnet("9090907", RELOJ.hoy()));

        assertThat(estadoDe(usuario)).isEqualTo("PENDIENTE_VERIFICACION");
    }
}
