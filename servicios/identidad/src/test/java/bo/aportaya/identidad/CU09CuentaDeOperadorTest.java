package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.BitacoraDeCuenta;
import bo.aportaya.identidad.aplicacion.CambiarClaveDeCuenta;
import bo.aportaya.identidad.aplicacion.InvitacionesDeCuenta;
import bo.aportaya.identidad.aplicacion.RecuperacionesDeAcceso;
import bo.aportaya.identidad.aplicacion.ReglasDeGobierno;
import bo.aportaya.identidad.dominio.AlcanceDeCuenta;
import bo.aportaya.identidad.dominio.puertos.CorreoDeCuenta;
import bo.aportaya.identidad.infraestructura.CredencialRepositorio;
import bo.aportaya.identidad.infraestructura.FactorTotpRepositorio;
import bo.aportaya.identidad.infraestructura.LlavesDeFactor;
import bo.aportaya.identidad.infraestructura.RecuperacionesRepositorio;
import bo.aportaya.identidad.infraestructura.SecretoDeInvitacion;
import bo.aportaya.identidad.infraestructura.TokensDeCuentaRepositorio;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-09 §10-§11 contra PostgreSQL real: invitacion de un solo uso, recuperacion a cuatro ojos y la
 * restriccion R-SEG-16. El correo es un DOBLE declarado (regla 65): prueba que el enlace sale y que NO
 * lleva el token a ningun otro lado; que Gmail lo entregue lo prueba el ensayo en TEST.
 *
 * <p>Datos y llaves SINTETICOS (regla 97.6). Corre en {@code integrationTest}.
 */
class CU09CuentaDeOperadorTest extends BaseDeCU09 {

    private static final String LLAVE = "llave-sintetica-de-integracion-32-caracteres";
    private static final String SECRETO = "s".repeat(40);

    /** El doble del correo: guarda lo que se habria enviado, nunca sale a ningun lado. */
    private static final class CorreoDeCuentaEnMemoria implements CorreoDeCuenta {
        final List<String> enlaces = new ArrayList<>();
        final List<AlcanceDeCuenta> alcances = new ArrayList<>();

        @Override
        public void enviarInvitacion(String destino, String enlace, AlcanceDeCuenta alcance, Duration vigencia) {
            enlaces.add(enlace);
            alcances.add(alcance);
        }

        String ultimoToken() {
            String enlace = enlaces.get(enlaces.size() - 1);
            return enlace.substring(enlace.indexOf("#token=") + "#token=".length());
        }
    }

    private CorreoDeCuentaEnMemoria correo;
    private InvitacionesDeCuenta invitaciones;
    private RecuperacionesDeAcceso recuperaciones;
    private CambiarClaveDeCuenta cambioDeClave;

    @BeforeEach
    void armarCuenta() {
        Datos datos = new Datos(dsl);
        var tokens = new TokensDeCuentaRepositorio(new SecretoDeInvitacion(SECRETO));
        var solicitudes = new RecuperacionesRepositorio();
        var factores = new FactorTotpRepositorio();
        var llaves = new LlavesDeFactor("1:" + LLAVE, "1");
        var verificador =
                new VerificadorTotp(datos, factores, llaves, Reloj.delSistema(), transaccion.getTransactionManager());
        var bitacora = new BitacoraDeCuenta(new Outbox("identidad"));
        correo = new CorreoDeCuentaEnMemoria();
        invitaciones = new InvitacionesDeCuenta(
                datos,
                tokens,
                new UsuarioRepositorio(),
                new CredencialRepositorio(),
                solicitudes,
                factores,
                llaves,
                verificador,
                hasheador,
                correo,
                bitacora,
                Reloj.delSistema(),
                10,
                5,
                "https://backoffice.ejemplo.test");
        recuperaciones = new RecuperacionesDeAcceso(
                datos,
                solicitudes,
                invitaciones,
                bitacora,
                new ReglasDeGobierno((actor, evidencia) -> true),
                Reloj.delSistema(),
                Duration.ofHours(24),
                Duration.ofDays(7));
        cambioDeClave = new CambiarClaveDeCuenta(
                cambiarCredencial, datos, new UsuarioRepositorio(), bitacora, Reloj.delSistema(), 10, 5);
        for (String proposito : List.of("INVITACION_OPERADOR", "RECUPERACION_OPERADOR_CLAVE", "REINSCRIPCION_FACTOR")) {
            dsl.execute(
                    """
                    INSERT INTO identidad.politica_token
                        (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion,
                         max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia,
                         canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde)
                    VALUES (?, 259200, 32, 3, 0, 300, 5, 'CORREO', false, true,
                            '2026-01-01T00:00:00-04:00'::timestamptz)
                    ON CONFLICT (proposito, vigente_desde) DO NOTHING
                    """,
                    proposito);
        }
    }

    // ------------------------------------------------------------------ criterios de la boveda

    @Test
    @DisplayName(
            "Dado un operador recién dado de alta con una invitación vigente · Cuando abre el enlace, elige una clave"
                    + " válida y confirma su TOTP · Entonces su credencial queda con la clave elegida y requiere_cambio en"
                    + " false · Y el token queda CONSUMIDO y no sirve una segunda vez")
    void criterio8() {
        UUID operador = operadorConClaveInutilizable("+59176100001");
        emitir(operador, AlcanceDeCuenta.ALTA);
        String token = correo.ultimoToken();

        var inicio = transaccion.execute(e -> invitaciones.iniciarFactor(token, origen()));
        String codigo = CodigoTotp.de(inicio.semillaBase32(), Instant.now());
        var resultado = completar(token, "una-clave-larga-del-operador-1", null, inicio.factorId(), codigo);

        assertThat(resultado.exito()).isTrue();
        assertThat(hasheador.coincide("una-clave-larga-del-operador-1".toCharArray(), hashDe(operador)))
                .isTrue();
        assertThat(dsl.fetchOne(
                                "SELECT requiere_cambio FROM identidad.credencial_acceso WHERE usuario_id = ?",
                                operador)
                        .get(0, Boolean.class))
                .isFalse();
        assertThat(estadoDelUltimoToken(operador)).isEqualTo("CONSUMIDO");
        assertThat(factorConfirmadoDe(operador)).isEqualTo(1);

        var segunda = completar(token, "otra-clave-larga-del-operador-2", null, inicio.factorId(), codigo);
        assertThat(segunda.exito()).isFalse();
        assertThat(segunda.rechazo().codigo().valor()).isEqualTo("AP-CU09-04");
    }

    @Test
    @DisplayName("Dado una invitación vencida, usada o de otro propósito · Cuando se intenta consultar o completar ·"
            + " Entonces se rechaza con TOKEN_INVALIDO, con el mismo mensaje en los tres casos")
    void criterio9() {
        UUID operador = operadorConClaveInutilizable("+59176100002");
        emitir(operador, AlcanceDeCuenta.ALTA);
        String vencido = correo.ultimoToken();
        dsl.execute(
                "UPDATE identidad.token_verificacion SET expira_en = now() - interval '1 minute' WHERE usuario_id = ?",
                operador);

        UUID otro = operadorConClaveInutilizable("+59176100003");
        emitir(otro, AlcanceDeCuenta.CLAVE);
        String deOtroProposito = correo.ultimoToken();

        UUID tercero = operadorConClaveInutilizable("+59176100004");
        emitir(tercero, AlcanceDeCuenta.ALTA);
        String usado = correo.ultimoToken();
        dsl.execute(
                """
                UPDATE identidad.token_verificacion SET estado = 'CONSUMIDO', consumido_en = now()
                WHERE usuario_id = ?
                """,
                tercero);

        String falso = "token-que-nadie-emitio-0123456789";
        String mensajeVencido = rechazoDeConsultar(vencido);
        assertThat(rechazoDeConsultar(usado)).isEqualTo(mensajeVencido);
        assertThat(rechazoDeConsultar(falso)).isEqualTo(mensajeVencido);
        // El token de recuperacion de la clave no sirve para enrolar un factor: otro proposito, mismo mensaje.
        assertThat(rechazoDeEnrolar(deOtroProposito)).isEqualTo(mensajeVencido);
        assertThat(mensajeVencido).contains("ya no sirve");
    }

    @Test
    @DisplayName(
            "Dado un operador que pidió recuperar su clave · Cuando él mismo intenta aprobar la solicitud · Entonces"
                    + " se rechaza con APROBACION_REQUERIDA · Y la base lo rechaza aunque se evite la aplicación"
                    + " (R-SEG-16)")
    void criterio10() {
        UUID operador = operadorConClaveInutilizable("+59176100005");
        String correoDelOperador = correoDe(operador);

        transaccion.execute(e -> {
            recuperaciones.solicitar(correoDelOperador, "CLAVE", "Perdi mi clave", origen());
            return null;
        });
        UUID solicitud = solicitudDe(operador);

        assertThatThrownByAprobar(solicitud, comoOperador(operador));
        // R-SEG-16: aunque alguien se saltee la aplicacion, la base lo rechaza.
        assertThat(rechazaLaBase(
                        """
                        UPDATE identidad.solicitud_recuperacion_acceso
                        SET estado = 'APROBADA', decidida_por = '%s', decidida_en = now(),
                            motivo_decision = 'me apruebo yo', enfriamiento_hasta = now()
                        WHERE id = '%s'
                        """
                                .formatted(operador, solicitud)))
                .contains("ck_recuperacion_no_autoaprobada");
        assertThat(estadoDeLaSolicitud(solicitud)).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName(
            "Dado una solicitud aprobada con enfriamiento vigente · Cuando corre el trabajo de emisión · Entonces no"
                    + " se emite ninguna invitación · Y pasado el enfriamiento se emite una sola, aunque el trabajo corra"
                    + " dos veces")
    void criterio11() {
        UUID operador = operadorConClaveInutilizable("+59176100006");
        UUID aprobador = operadorConClaveInutilizable("+59176100007");
        transaccion.execute(e -> {
            recuperaciones.solicitar(correoDe(operador), "CLAVE", "Perdi mi clave", origen());
            return null;
        });
        UUID solicitud = solicitudDe(operador);
        transaccion.execute(e -> recuperaciones.aprobar(
                solicitud, "Verificado en persona con documento", "evidencia-sintetica", comoOperador(aprobador)));

        assertThat(correrElTrabajo()).isZero();
        assertThat(correo.enlaces).isEmpty();

        dsl.execute(
                """
                UPDATE identidad.solicitud_recuperacion_acceso
                SET enfriamiento_hasta = now() - interval '1 minute' WHERE id = ?
                """,
                solicitud);
        assertThat(correrElTrabajo()).isEqualTo(1);
        assertThat(correrElTrabajo()).isZero();
        assertThat(correo.enlaces).hasSize(1);
        assertThat(correo.alcances).containsExactly(AlcanceDeCuenta.CLAVE);
        assertThat(estadoDeLaSolicitud(solicitud)).isEqualTo("EMITIDA");
    }

    @Test
    @DisplayName(
            "Dado un operador con una invitación de reinscripción del factor · Cuando la completa con su clave actual"
                    + " y un código del TOTP nuevo · Entonces el factor anterior queda inactivo y el nuevo confirmado · Y"
                    + " todas sus sesiones quedan revocadas")
    void criterio12() {
        UUID operador = operadorConClaveInutilizable("+59176100008");
        String claveActual = "la-clave-actual-del-operador-8";
        dsl.execute(
                "UPDATE identidad.credencial_acceso SET hash_contrasena = ? WHERE usuario_id = ?",
                hasheador.hashear(claveActual.toCharArray()),
                operador);
        factoresViejo(operador);
        UUID sesion = fixtura.sesionAbierta(operador);
        emitir(operador, AlcanceDeCuenta.FACTOR);
        String token = correo.ultimoToken();

        var inicio = transaccion.execute(e -> invitaciones.iniciarFactor(token, origen()));
        String codigo = CodigoTotp.de(inicio.semillaBase32(), Instant.now());
        var resultado = completar(token, null, claveActual, inicio.factorId(), codigo);

        assertThat(resultado.exito()).isTrue();
        assertThat(factorConfirmadoDe(operador)).isEqualTo(1);
        assertThat(sesionViva(sesion)).isFalse();
        assertThat(sesionesVivasDe(operador)).isZero();
        assertThat(hasheador.coincide(claveActual.toCharArray(), hashDe(operador)))
                .isTrue();
    }

    @Test
    @DisplayName("Dado un operador con la marca de cambio de clave pendiente · Cuando cambia su clave con la actual"
            + " correcta · Entonces la marca queda en falso y la clave nueva rige · Y todas sus sesiones, incluida"
            + " la actual, quedan cerradas")
    void criterio13() {
        UUID operador = operadorConClaveInutilizable("+59176100013");
        String actual = "la-clave-actual-del-operador-13";
        dsl.execute(
                "UPDATE identidad.credencial_acceso SET hash_contrasena = ? WHERE usuario_id = ?",
                hasheador.hashear(actual.toCharArray()),
                operador);
        UUID sesion = fixtura.sesionAbierta(operador);
        fixtura.sesionAbierta(operador);

        var salida = transaccion.execute(
                e -> cambioDeClave.cambiar(actual, "la-clave-nueva-del-operador-13", sesion, comoOperador(operador)));

        assertThat(salida.cierraLaSesionActual()).isTrue();
        assertThat(sesionesVivasDe(operador)).isZero();
        assertThat(hasheador.coincide("la-clave-nueva-del-operador-13".toCharArray(), hashDe(operador)))
                .isTrue();
        assertThat(dsl.fetchOne(
                                "SELECT debe_cambiar_contrasena FROM identidad.usuario_interno WHERE usuario_id = ?",
                                operador)
                        .get(0, Boolean.class))
                .isFalse();
    }

    @Test
    @DisplayName("rechaza cambiar la clave con la actual equivocada y no toca nada")
    void rechazaClaveActualEquivocada() {
        UUID operador = operadorConClaveInutilizable("+59176100014");
        UUID sesion = fixtura.sesionAbierta(operador);

        try {
            transaccion.execute(e -> cambioDeClave.cambiar(
                    "no-es-la-clave-actual", "la-clave-nueva-del-operador-14", sesion, comoOperador(operador)));
            throw new AssertionError("la clave actual equivocada no debio pasar");
        } catch (ErrorDeNegocio rechazo) {
            assertThat(rechazo.codigo().valor()).isEqualTo("AP-CU09-01");
        }
        assertThat(sesionViva(sesion)).isTrue();
    }

    // ------------------------------------------------------------------ rechazos

    @Test
    @DisplayName("rechaza por R-SEG-16")
    void rechazaRSEG13() {
        UUID operador = operadorConClaveInutilizable("+59176100009");
        UUID solicitud = abrirSolicitud(operador);

        // Aprobada sin aprobador, sin fecha ni enfriamiento: la base no la acepta.
        assertThat(rechazaLaBase(
                        "UPDATE identidad.solicitud_recuperacion_acceso SET estado = 'APROBADA' WHERE id = '%s'"
                                .formatted(solicitud)))
                .contains("ck_recuperacion_decision_completa");
        // Ni una segunda solicitud viva del mismo operador y alcance.
        assertThat(rechazaLaBase(
                        """
                        INSERT INTO identidad.solicitud_recuperacion_acceso
                            (id, usuario_id, alcance, estado, motivo, clave_idempotencia)
                        VALUES (gen_random_uuid(), '%s', 'CLAVE', 'PENDIENTE', 'otra', 'REC:duplicada')
                        """
                                .formatted(operador)))
                .contains("uq_recuperacion_viva");
    }

    @Test
    @DisplayName("rechaza un codigo TOTP incorrecto sin consumir el token y cuenta el intento")
    void rechazaCodigoIncorrecto() {
        UUID operador = operadorConClaveInutilizable("+59176100010");
        emitir(operador, AlcanceDeCuenta.ALTA);
        String token = correo.ultimoToken();
        var inicio = transaccion.execute(e -> invitaciones.iniciarFactor(token, origen()));
        String bueno = CodigoTotp.de(inicio.semillaBase32(), Instant.now());
        String malo = "000000".equals(bueno) ? "111111" : "000000";

        var resultado = completar(token, "una-clave-larga-del-operador-10", null, inicio.factorId(), malo);

        assertThat(resultado.exito()).isFalse();
        assertThat(resultado.rechazo().codigo().valor()).isEqualTo("AP-CU04-04");
        assertThat(estadoDelUltimoToken(operador)).isEqualTo("EMITIDO");
        assertThat(dsl.fetchOne(
                                "SELECT intentos_fallidos FROM identidad.token_verificacion WHERE usuario_id = ?",
                                operador)
                        .get(0, Integer.class))
                .isEqualTo(1);
        // Con el codigo bueno, dentro del mismo token, todavia entra.
        assertThat(completar(token, "una-clave-larga-del-operador-10", null, inicio.factorId(), bueno)
                        .exito())
                .isTrue();
    }

    @Test
    @DisplayName("rechaza una clave debil sin consumir el token")
    void rechazaClaveDebil() {
        UUID operador = operadorConClaveInutilizable("+59176100011");
        emitir(operador, AlcanceDeCuenta.CLAVE);
        String token = correo.ultimoToken();

        var resultado = completar(token, "corta", null, null, null);

        assertThat(resultado.exito()).isFalse();
        assertThat(resultado.rechazo().codigo().valor()).isEqualTo("AP-CU09-02");
        assertThat(estadoDelUltimoToken(operador)).isEqualTo("EMITIDO");
    }

    @Test
    @DisplayName("rechaza revelar si el correo existe: el pedido no deja rastro de un correo desconocido")
    void rechazaRevelarElCorreo() {
        int antes = dsl.fetchCount(org.jooq.impl.DSL.table("identidad.solicitud_recuperacion_acceso"));

        // No lanza, no responde distinto y no deja rastro: nadie puede usarlo para averiguar quien es operador.
        transaccion.execute(e -> {
            recuperaciones.solicitar("nadie@ejemplo.test", "CLAVE", null, origen());
            return null;
        });

        assertThat(dsl.fetchCount(org.jooq.impl.DSL.table("identidad.solicitud_recuperacion_acceso")))
                .isEqualTo(antes);
        assertThat(correo.enlaces).isEmpty();
    }

    @Test
    @DisplayName("reintento del pedido vivo no duplica la solicitud")
    void reintentoDelPedido() {
        UUID operador = operadorConClaveInutilizable("+59176100012");
        String correoDelOperador = correoDe(operador);

        for (int i = 0; i < 2; i++) {
            transaccion.execute(e -> {
                recuperaciones.solicitar(correoDelOperador, "FACTOR", "Perdi el telefono", origen());
                return null;
            });
        }

        assertThat(dsl.fetchCount(
                        org.jooq.impl.DSL.table("identidad.solicitud_recuperacion_acceso"),
                        org.jooq.impl.DSL.field("usuario_id").eq(operador)))
                .isEqualTo(1);
    }

    // ------------------------------------------------------------------ apoyo

    private UUID operadorConClaveInutilizable(String telefono) {
        UUID usuario = fixtura.usuario(telefono);
        dsl.execute(
                "UPDATE identidad.usuario SET correo = ? WHERE id = ?",
                "op" + telefono.substring(8) + "@ejemplo.test",
                usuario);
        dsl.execute(
                """
                INSERT INTO identidad.usuario_interno (id, usuario_id, departamento, cargo, debe_cambiar_contrasena)
                VALUES (gen_random_uuid(), ?, 'Sistemas', 'Analista', true)
                """,
                usuario);
        fixtura.credencial(
                usuario, hasheador.hashear(UUID.randomUUID().toString().toCharArray()));
        fixtura.asignarRol(
                usuario, fixtura.rolGlobal("OPC" + UUID.randomUUID().toString().substring(0, 8)));
        return usuario;
    }

    private void emitir(UUID operador, AlcanceDeCuenta alcance) {
        transaccion.execute(e -> invitaciones.emitirYEnviar(
                dsl,
                operador,
                alcance,
                BitacoraDeCuenta.Accion.INVITACION_EMITIDA,
                new BitacoraDeCuenta.Actor(UUID.randomUUID(), "BACKOFFICE", BitacoraDeCuenta.ORIGEN_PANEL),
                "prueba",
                UUID.randomUUID(),
                "127.0.0.1",
                "prueba"));
    }

    private InvitacionesDeCuenta.Resultado completar(
            String token, String claveNueva, String claveActual, UUID factorId, String codigo) {
        return transaccion.execute(e -> invitaciones.completar(
                new InvitacionesDeCuenta.Entrada(token, claveNueva, claveActual, factorId, codigo), origen()));
    }

    private InvitacionesDeCuenta.Origen origen() {
        return new InvitacionesDeCuenta.Origen(
                "127.0.0.1", "prueba", new Traza(UUID.randomUUID().toString()));
    }

    private ContextoSesion comoOperador(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private String rechazoDeConsultar(String token) {
        try {
            transaccion.execute(e -> invitaciones.verificarVigencia(token, origen()));
            return "SIN RECHAZO";
        } catch (ErrorDeNegocio rechazo) {
            return rechazo.codigo().valor() + " " + rechazo.getMessage();
        }
    }

    private String rechazoDeEnrolar(String token) {
        try {
            transaccion.execute(e -> invitaciones.iniciarFactor(token, origen()));
            return "SIN RECHAZO";
        } catch (ErrorDeNegocio rechazo) {
            return rechazo.codigo().valor() + " " + rechazo.getMessage();
        }
    }

    private void assertThatThrownByAprobar(UUID solicitud, ContextoSesion ctx) {
        try {
            transaccion.execute(
                    e -> recuperaciones.aprobar(solicitud, "Me apruebo a mi mismo", "evidencia-sintetica", ctx));
            throw new AssertionError("la autoaprobacion no debio pasar");
        } catch (ErrorDeNegocio rechazo) {
            assertThat(rechazo.codigo().valor()).isEqualTo("AP-CU09-09");
        }
    }

    private int correrElTrabajo() {
        return transaccion.execute(e -> recuperaciones.emitirLasQueYaPuedenSalir(
                new Traza(UUID.randomUUID().toString())));
    }

    private UUID abrirSolicitud(UUID operador) {
        transaccion.execute(e -> {
            recuperaciones.solicitar(correoDe(operador), "CLAVE", "Perdi mi clave", origen());
            return null;
        });
        return solicitudDe(operador);
    }

    private UUID solicitudDe(UUID operador) {
        return dsl.fetchOne("SELECT id FROM identidad.solicitud_recuperacion_acceso WHERE usuario_id = ?", operador)
                .get(0, UUID.class);
    }

    private String estadoDeLaSolicitud(UUID solicitud) {
        return dsl.fetchOne("SELECT estado FROM identidad.solicitud_recuperacion_acceso WHERE id = ?", solicitud)
                .get(0, String.class);
    }

    private String estadoDelUltimoToken(UUID operador) {
        return dsl.fetchOne(
                        """
                        SELECT estado FROM identidad.token_verificacion WHERE usuario_id = ?
                        ORDER BY emitido_en DESC LIMIT 1
                        """,
                        operador)
                .get(0, String.class);
    }

    private String correoDe(UUID operador) {
        return dsl.fetchOne("SELECT correo FROM identidad.usuario WHERE id = ?", operador)
                .get(0, String.class);
    }

    private int factorConfirmadoDe(UUID operador) {
        return dsl.fetchCount(
                org.jooq.impl.DSL.table("identidad.factor_mfa"),
                org.jooq
                        .impl
                        .DSL
                        .field("usuario_id")
                        .eq(operador)
                        .and(org.jooq.impl.DSL.field("activo").isTrue())
                        .and(org.jooq.impl.DSL.field("confirmado_en").isNotNull()));
    }

    /** Un TOTP ya confirmado: el que el operador perdio con su telefono. */
    private void factoresViejo(UUID operador) {
        fixtura.factor(operador, "TOTP", true, true);
    }
}
