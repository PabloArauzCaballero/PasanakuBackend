package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.BitacoraDeCuenta;
import bo.aportaya.identidad.aplicacion.CambiarClaveDeCuenta;
import bo.aportaya.identidad.aplicacion.EmisorDeInvitaciones;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

abstract class BaseDeCuentaDeOperador extends BaseDeCU09 {

    static final String LLAVE = "llave-sintetica-de-integracion-32-caracteres";
    private static final String SECRETO = "s".repeat(40);

    /** El doble del correo: guarda lo que se habria enviado, nunca sale a ningun lado. */
    static final class CorreoDeCuentaEnMemoria implements CorreoDeCuenta {
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

    protected CorreoDeCuentaEnMemoria correo;
    protected InvitacionesDeCuenta invitaciones;
    protected RecuperacionesDeAcceso recuperaciones;
    protected EmisorDeInvitaciones emisor;
    protected CambiarClaveDeCuenta cambioDeClave;

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
                bitacora,
                Reloj.delSistema(),
                10,
                5);
        emisor = new EmisorDeInvitaciones(
                tokens, correo, bitacora, Reloj.delSistema(), "https://backoffice.ejemplo.test");
        recuperaciones = new RecuperacionesDeAcceso(
                datos,
                solicitudes,
                emisor,
                bitacora,
                new ReglasDeGobierno((actor, evidencia) -> true),
                Reloj.delSistema(),
                Duration.ofHours(24),
                Duration.ofDays(7),
                25,
                100,
                50);
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

    // apoyo compartido

    protected UUID operadorConClaveInutilizable(String telefono) {
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

    protected void emitir(UUID operador, AlcanceDeCuenta alcance) {
        transaccion.execute(e -> emisor.emitirYEnviar(
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

    protected InvitacionesDeCuenta.Resultado completar(
            String token, String claveNueva, String claveActual, UUID factorId, String codigo) {
        return transaccion.execute(e -> invitaciones.completar(
                new InvitacionesDeCuenta.Entrada(token, claveNueva, claveActual, factorId, codigo), origen()));
    }

    protected InvitacionesDeCuenta.Origen origen() {
        return new InvitacionesDeCuenta.Origen(
                "127.0.0.1", "prueba", new Traza(UUID.randomUUID().toString()));
    }

    protected ContextoSesion comoOperador(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    protected String rechazoDeConsultar(String token) {
        try {
            transaccion.execute(e -> invitaciones.verificarVigencia(token, origen()));
            return "SIN RECHAZO";
        } catch (ErrorDeNegocio rechazo) {
            return rechazo.codigo().valor() + " " + rechazo.getMessage();
        }
    }

    protected String rechazoDeEnrolar(String token) {
        try {
            transaccion.execute(e -> invitaciones.iniciarFactor(token, origen()));
            return "SIN RECHAZO";
        } catch (ErrorDeNegocio rechazo) {
            return rechazo.codigo().valor() + " " + rechazo.getMessage();
        }
    }

    protected void assertThatThrownByAprobar(UUID solicitud, ContextoSesion ctx) {
        try {
            transaccion.execute(
                    e -> recuperaciones.aprobar(solicitud, "Me apruebo a mi mismo", "evidencia-sintetica", ctx));
            throw new AssertionError("la autoaprobacion no debio pasar");
        } catch (ErrorDeNegocio rechazo) {
            assertThat(rechazo.codigo().valor()).isEqualTo("AP-CU09-09");
        }
    }

    protected int correrElTrabajo() {
        return transaccion.execute(e -> recuperaciones.emitirLasQueYaPuedenSalir(
                new Traza(UUID.randomUUID().toString())));
    }

    protected UUID abrirSolicitud(UUID operador) {
        transaccion.execute(e -> {
            recuperaciones.solicitar(correoDe(operador), "CLAVE", "Perdi mi clave", origen());
            return null;
        });
        return solicitudDe(operador);
    }

    protected UUID solicitudDe(UUID operador) {
        return dsl.fetchOne("SELECT id FROM identidad.solicitud_recuperacion_acceso WHERE usuario_id = ?", operador)
                .get(0, UUID.class);
    }

    protected String estadoDeLaSolicitud(UUID solicitud) {
        return dsl.fetchOne("SELECT estado FROM identidad.solicitud_recuperacion_acceso WHERE id = ?", solicitud)
                .get(0, String.class);
    }

    protected String estadoDelUltimoToken(UUID operador) {
        return dsl.fetchOne(
                        """
                        SELECT estado FROM identidad.token_verificacion WHERE usuario_id = ?
                        ORDER BY emitido_en DESC LIMIT 1
                        """,
                        operador)
                .get(0, String.class);
    }

    protected String correoDe(UUID operador) {
        return dsl.fetchOne("SELECT correo FROM identidad.usuario WHERE id = ?", operador)
                .get(0, String.class);
    }

    protected int factorConfirmadoDe(UUID operador) {
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
    protected void factoresViejo(UUID operador) {
        fixtura.factor(operador, "TOTP", true, true);
    }
}
