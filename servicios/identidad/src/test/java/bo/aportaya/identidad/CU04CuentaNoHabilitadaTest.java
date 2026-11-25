package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.EmitirAcceso;
import bo.aportaya.identidad.aplicacion.EntradaAutenticacion;
import bo.aportaya.identidad.aplicacion.RenovarSesion;
import bo.aportaya.identidad.dominio.PoliticaDeIntentos;
import bo.aportaya.identidad.dominio.ResultadoDeAutenticacion;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.Argon2Hasheador;
import bo.aportaya.identidad.infraestructura.EmisorDeAcceso;
import bo.aportaya.identidad.infraestructura.RolesYPermisosRepositorio;
import bo.aportaya.identidad.infraestructura.UsuarioRepositorio;
import bo.aportaya.identidad.infraestructura.UsuariosInternosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * F1.S3.M1 · IDCU-SEG-075 / IDCU-SEG-089, contra PostgreSQL real: el ingreso y la
 * renovacion miran {@code usuario.estado}, y desbloquear libera el modelo que lee el login.
 *
 * <p>Datos sinteticos (regla 97.6). Corre en {@code integrationTest} (Testcontainers).
 */
class CU04CuentaNoHabilitadaTest extends BaseDeCU04 {

    private static final String SEARCH_PATH_DEL_SERVICIO = "SET LOCAL search_path TO identidad, catalogo, comun";

    private RenovarSesion refresco;

    private static final EmisorDeAcceso emisor = new EmisorDeAcceso("", "");

    @BeforeEach
    void armarRefresco() {
        dsl.execute(
                """
                INSERT INTO identidad.politica_token
                    (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion, max_reenvios_por_hora,
                     cooldown_reenvio_segundos, max_emisiones_por_dia, canales_permitidos,
                     exige_dispositivo_conocido, invalida_anteriores, vigente_desde)
                VALUES ('REFRESCO_SESION', 43200, 64, 1, 0, 0, 200, 'COOKIE_HTTPONLY', false, false,
                        '2026-01-01T00:00:00-04:00')
                ON CONFLICT (proposito, vigente_desde) DO NOTHING
                """);
        refresco = new RenovarSesion(new Datos(dsl), new UsuarioRepositorio(), Reloj.delSistema(), Ids.seguros());
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLOQUEADO_SEGURIDAD", "BAJA_VOLUNTARIA", "ELIMINADO"})
    @DisplayName(
            "Dada una cuenta BLOQUEADO_SEGURIDAD, BAJA_VOLUNTARIA o ELIMINADO · Cuando inicia sesión con la clave correcta · Entonces se responde AP-CU04-02 y no se abre sesión")
    void noHabilitadaNoEntra(String estado) {
        String telefono = telefonoUnico();
        UUID usuario = participanteConCredencial(telefono);
        cambiarEstado(usuario, estado);
        int sesionesAntes = sesionesDe(usuario);

        ResultadoDeAutenticacion resultado = autenticar(telefono, "clave-correcta");

        assertThat(resultado.exitoso()).isFalse();
        assertThat(resultado.codigo()).contains(CodigoError.de(4, 2));
        assertThat(resultado.sesionId()).isEmpty();
        assertThat(sesionesDe(usuario)).isEqualTo(sesionesAntes);
        assertThat(dsl.fetchValue(
                        "SELECT motivo_fallo FROM identidad.intento_autenticacion WHERE usuario_id = ? ORDER BY fecha_hora DESC LIMIT 1",
                        usuario))
                .isEqualTo("CUENTA_NO_HABILITADA");
    }

    @Test
    @DisplayName(
            "Dado un participante con la cuenta SUSPENDIDO por una medida de CU-134 · Cuando inicia sesión con la clave correcta · Entonces no se lo rechaza por su estado · Y el token que se le emite lleva alcance RESTRINGIDO")
    void participanteSuspendidoEntraRestringido() throws Exception {
        String telefono = telefonoUnico();
        UUID usuario = participanteConCredencial(telefono);
        UUID activo = participanteConCredencial(telefonoUnico());
        cambiarEstado(usuario, "SUSPENDIDO");

        ResultadoDeAutenticacion resultado = autenticar(telefono, "clave-correcta");

        assertThat(resultado.codigo().filter(CodigoError.de(4, 2)::equals)).isEmpty();
        assertThat(alcanceDelToken(usuario)).isEqualTo("RESTRINGIDO");
        // El limite: la misma emision para una cuenta ACTIVO no lleva el reclamo.
        assertThat(alcanceDelToken(activo)).isNull();
    }

    @Test
    @DisplayName(
            "Dado un operador del personal interno con la cuenta SUSPENDIDO · Cuando inicia sesión con la clave correcta · Entonces se responde AP-CU04-02 y no se abre sesión")
    void operadorSuspendidoNoEntra() {
        String telefono = telefonoUnico();
        UUID operador = participanteConCredencial(telefono);
        fixtura.asignarRol(operador, fixtura.rolGlobal("OPS" + telefono.substring(8)));
        cambiarEstado(operador, "SUSPENDIDO");
        int sesionesAntes = sesionesDe(operador);

        ResultadoDeAutenticacion resultado = autenticar(telefono, "clave-correcta");

        assertThat(resultado.codigo()).contains(CodigoError.de(4, 2));
        assertThat(sesionesDe(operador)).isEqualTo(sesionesAntes);
    }

    @Test
    @DisplayName(
            "Dada una cuenta suspendida · Cuando se intenta entrar con una clave equivocada · Entonces se responde AP-CU04-01 sin revelar su estado")
    void suspendidaConClaveMalaNoRevelaEstado() {
        String telefono = telefonoUnico();
        UUID usuario = participanteConCredencial(telefono);
        cambiarEstado(usuario, "SUSPENDIDO");

        ResultadoDeAutenticacion resultado = autenticar(telefono, "clave-equivocada");

        assertThat(resultado.codigo()).contains(CodigoError.de(4, 1));
    }

    @Test
    @DisplayName(
            "Dada una cuenta PENDIENTE_VERIFICACION · Cuando inicia sesión con la clave correcta · Entonces no se la rechaza por su estado")
    void enVerificacionNoSeRechazaPorEstado() {
        String telefono = telefonoUnico();
        UUID usuario = participanteConCredencial(telefono);
        cambiarEstado(usuario, "PENDIENTE_VERIFICACION");

        ResultadoDeAutenticacion resultado = autenticar(telefono, "clave-correcta");

        assertThat(resultado.codigo().filter(CodigoError.de(4, 2)::equals)).isEmpty();
    }

    @Test
    @DisplayName(
            "Dada una cuenta suspendida con una sesión viva · Cuando renueva su refresh · Entonces no se renueva y la sesión queda revocada · Y reactivar la cuenta no resucita esa sesión")
    void suspendidaNoRefresca() {
        UUID usuario = participanteConCredencial(telefonoUnico());
        UUID sesion = fixtura.sesionAbierta(usuario);
        String token = emitir(usuario, sesion);
        cambiarEstado(usuario, "SUSPENDIDO");

        var renovacion = renovar(token);

        assertThat(renovacion.renovada()).isFalse();
        assertThat(dsl.fetchValue(
                        "SELECT motivo_revocacion FROM identidad.sesion WHERE id = ? AND revocada_en IS NOT NULL",
                        sesion))
                .isEqualTo("CUENTA_NO_HABILITADA");
        // Reactivar la cuenta no resucita esa sesion: hay que volver a entrar.
        cambiarEstado(usuario, "ACTIVO");
        assertThat(renovar(token).renovada()).isFalse();
    }

    @Test
    @DisplayName(
            "Dado un interno con un bloqueo_cuenta vigente · Cuando un operador lo desbloquea · Entonces el login ya no ve un bloqueo vigente · Y queda registrado quién lo liberó")
    void desbloquearLiberaElBloqueoQueLeeElLogin() {
        UUID interno = fixtura.usuario(telefonoUnico());
        UUID operador = fixtura.usuario(telefonoUnico());
        // Datos SINTETICOS: el detalle que devuelve desbloquear es el de un usuario interno.
        dsl.execute(
                "INSERT INTO identidad.usuario_interno (usuario_id, departamento, cargo) VALUES (?, 'SOPORTE', 'Analista sintetico')",
                interno);
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        dsl.execute(
                "INSERT INTO identidad.bloqueo_cuenta (usuario_id, motivo, bloqueada_en, desbloquea_en) VALUES (?, 'INTENTOS_FALLIDOS', ?::timestamptz, ?::timestamptz)",
                interno,
                ahora,
                ahora.plusMinutes(30));
        assertThat(bloqueoQueLeeElLogin(interno)).isTrue();

        var accesos = new AccesosRepositorio();
        var repositorio = new UsuariosInternosRepositorio(
                accesos, new Argon2Hasheador("pimienta-de-prueba"), new RolesYPermisosRepositorio(accesos));
        transaccion.execute(e -> {
            dsl.execute(SEARCH_PATH_DEL_SERVICIO);
            return repositorio.desbloquear(dsl, interno, "verificado por soporte", operador, ahora);
        });

        assertThat(bloqueoQueLeeElLogin(interno)).isFalse();
        assertThat(dsl.fetchValue("SELECT liberada_por FROM identidad.bloqueo_cuenta WHERE usuario_id = ?", interno))
                .isEqualTo(operador);
    }

    /** La MISMA consulta que hace CU04Autenticar (el helper de la base cuenta tambien los liberados). */
    private boolean bloqueoQueLeeElLogin(UUID usuario) {
        return new UsuarioRepositorio().tieneBloqueoVigente(dsl, usuario, OffsetDateTime.now(ZoneOffset.UTC));
    }

    private void cambiarEstado(UUID usuario, String estado) {
        dsl.execute("UPDATE identidad.usuario SET estado = ? WHERE id = ?", estado, usuario);
    }

    private ResultadoDeAutenticacion autenticar(String telefono, String clave) {
        var entrada = new EntradaAutenticacion(
                telefono,
                clave.toCharArray(),
                "huella-" + telefono,
                "ANDROID",
                "127.0.0.1",
                "prueba",
                UUID.randomUUID().toString(),
                Optional.empty(),
                false,
                Duration.ofHours(12));
        return transaccion.execute(e -> cu04.ejecutar(entrada, new PoliticaDeIntentos(5, Duration.ofMinutes(30))));
    }

    private String emitir(UUID usuario, UUID sesion) {
        return transaccion
                .execute(e -> {
                    dsl.execute(SEARCH_PATH_DEL_SERVICIO);
                    return refresco.emitir(
                            usuario,
                            sesion,
                            "127.0.0.1",
                            "prueba",
                            UUID.randomUUID().toString());
                })
                .token();
    }

    private RenovarSesion.Renovacion renovar(String token) {
        return transaccion.execute(e -> {
            dsl.execute(SEARCH_PATH_DEL_SERVICIO);
            return refresco.renovar(
                    token, "127.0.0.1", "prueba", UUID.randomUUID().toString());
        });
    }

    /** El reclamo {@code alcance} del token que emite identidad para ese usuario (null si no lo lleva). */
    private String alcanceDelToken(UUID usuario) throws Exception {
        var emitir = new EmitirAcceso(
                new Datos(dsl), new AccesosRepositorio(), emisor, Reloj.delSistema(), new UsuarioRepositorio());
        String token = transaccion
                .execute(e -> emitir.ejecutar(usuario, "PARTICIPANTE", "BASICO", "disp", null))
                .token();
        return SignedJWT.parse(token).getJWTClaimsSet().getStringClaim("alcance");
    }

    private static String telefonoUnico() {
        return "+5917%07d".formatted(Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000L));
    }
}
