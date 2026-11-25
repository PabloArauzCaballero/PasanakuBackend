package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio;
import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.Alta;
import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.PedidoDeAlta;
import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.PedidoDeRotacion;
import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.Revocacion;
import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.Rotacion;
import bo.aportaya.identidad.dominio.puertos.HasheadorDeCredencial;
import bo.aportaya.identidad.infraestructura.Argon2Hasheador;
import bo.aportaya.identidad.infraestructura.CredencialRepositorio;
import bo.aportaya.identidad.infraestructura.CuentasDeServicioRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-136 contra PostgreSQL real (GU-18): registrar, rotar y revocar cuentas de servicio. La credencial nueva existe
 * solo en la respuesta de la rotacion (en la base queda su hash); R-SEG-07: nadie gobierna su propia cuenta; R-AUD-01:
 * una cuenta revocada conserva su fila.
 *
 * <p>Doble declarado (regla 65): la evidencia step-up es la de {@link BaseDeGobiernoCU08}.
 */
class CU136GobernarCuentasDeServicioTest extends BaseDeGobiernoCU08 {

    private static final Duration TOPE = Duration.ofDays(90);
    private static final HasheadorDeCredencial HASHEADOR = new Argon2Hasheador("pimienta-de-prueba");

    private static CU136GobernarCuentasDeServicio casoDeUso;

    @BeforeAll
    static void armarCU136() {
        casoDeUso = new CU136GobernarCuentasDeServicio(
                new Datos(dsl),
                new CuentasDeServicioRepositorio(),
                accesos,
                new GobiernoDeAccesosRepositorio(),
                new CredencialRepositorio(),
                HASHEADOR,
                DOBLE_DE_STEP_UP,
                new Outbox("identidad"),
                Reloj.delSistema(),
                TOPE);
    }

    private static OffsetDateTime enDias(int dias) {
        return OffsetDateTime.now().plusDays(dias);
    }

    /** Un usuario que porta un rol SERVICIO_* vigente y tiene una credencial guardada (solo su hash). */
    private static UUID cuentaAprovisionada() {
        UUID usuario = operador();
        UUID rol = rolLlamado(
                "SERVICIO_GU_F_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        fixtura.asignarRol(usuario, rol, operador());
        fixtura.credencial(usuario, HASHEADOR.hashear("clave-sintetica-anterior".toCharArray()));
        // R-SEG-10: un rol operativo no abre sesion sin TOTP confirmado; la cuenta de servicio lo tiene.
        fixtura.factor(usuario, "TOTP", true, true);
        return usuario;
    }

    private static Alta registrar(UUID actor, UUID usuario) {
        return casoDeUso.registrar(
                new PedidoDeAlta(usuario, "Servicio de prueba", enDias(30), "Alta del servicio aprobada", EVIDENCIA),
                como(actor));
    }

    private static PedidoDeRotacion rotacion() {
        return new PedidoDeRotacion(Optional.empty(), "Rotacion programada", EVIDENCIA);
    }

    private static void rechazaCon(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo(codigo));
    }

    @Test
    @DisplayName(
            "Dada una cuenta aprovisionada con un rol SERVICIO_* vigente · Cuando un operador con step-up la registra"
                    + " con nombre y vencimiento dentro del tope · Entonces la cuenta queda ACTIVA con quien la registro y"
                    + " deja identidad.cuenta_servicio_alta")
    void registraUnaCuentaAprovisionada() {
        enTransaccionQueSeDeshace(() -> {
            UUID actor = operador();
            UUID usuario = cuentaAprovisionada();

            Alta alta = registrar(actor, usuario);

            assertThat(columna("SELECT estado FROM identidad.cuenta_servicio WHERE id = ?", alta.cuentaId()))
                    .isEqualTo("ACTIVA");
            assertThat(columna("SELECT creada_por FROM identidad.cuenta_servicio WHERE id = ?", alta.cuentaId()))
                    .isEqualTo(actor);
            assertThat(alta.rolCodigo()).startsWith("SERVICIO_");
            assertThat(eventos("identidad.cuenta_servicio_alta", alta.cuentaId()))
                    .isEqualTo(1);
        });
    }

    @Test
    @DisplayName(
            "Dada una cuenta de servicio registrada con una sesion abierta · Cuando un operador rota su credencial con"
                    + " step-up · Entonces recibe la credencial una sola vez, en la base queda solo su hash y la sesion se"
                    + " cierra")
    void rotaYEntregaLaCredencialUnaSolaVez() {
        enTransaccionQueSeDeshace(() -> {
            UUID actor = operador();
            UUID usuario = cuentaAprovisionada();
            Alta alta = registrar(actor, usuario);
            fixtura.sesionAbierta(usuario);
            String hashAnterior = (String)
                    columna("SELECT hash_contrasena FROM identidad.credencial_acceso WHERE usuario_id = ?", usuario);

            Rotacion r = casoDeUso.rotar(alta.cuentaId(), rotacion(), como(actor));

            String hashNuevo = (String)
                    columna("SELECT hash_contrasena FROM identidad.credencial_acceso WHERE usuario_id = ?", usuario);
            assertThat(r.credencial()).hasSize(43);
            assertThat(hashNuevo).isNotEqualTo(hashAnterior).isNotEqualTo(r.credencial());
            assertThat(HASHEADOR.coincide(r.credencial().toCharArray(), hashNuevo))
                    .isTrue();
            assertThat(columna(
                            "SELECT count(*)::int FROM identidad.historial_credencial WHERE usuario_id = ?", usuario))
                    .isEqualTo(1);
            assertThat(sesionesVivas(usuario)).isZero();
            // El valor no esta en ningun evento: ni la bitacora ni el aviso lo llevan.
            assertThat(columna(
                            """
                            SELECT count(*)::int FROM identidad.evento_dominio
                             WHERE payload::text LIKE '%' || ? || '%'
                            """,
                            r.credencial()))
                    .isEqualTo(0);
            assertThat(r.toString()).doesNotContain(r.credencial());
        });
    }

    @Test
    @DisplayName("Dada una cuenta de servicio registrada · Cuando un operador la revoca con step-up · Entonces queda"
            + " REVOCADA, sin asignaciones vivas, con la credencial vencida y el usuario suspendido")
    void revocaLaCuentaPorCompleto() {
        enTransaccionQueSeDeshace(() -> {
            UUID actor = operador();
            UUID usuario = cuentaAprovisionada();
            Alta alta = registrar(actor, usuario);
            fixtura.sesionAbierta(usuario);

            Revocacion r = casoDeUso.revocar(alta.cuentaId(), "Servicio dado de baja", EVIDENCIA, como(actor));

            assertThat(r.asignacionesRevocadas()).isEqualTo(1);
            assertThat(columna("SELECT estado FROM identidad.cuenta_servicio WHERE id = ?", alta.cuentaId()))
                    .isEqualTo("REVOCADA");
            assertThat(columna(
                            """
                            SELECT count(*)::int FROM identidad.asignacion_rol
                             WHERE usuario_id = ? AND revocada_en IS NULL
                            """,
                            usuario))
                    .isEqualTo(0);
            assertThat(columna(
                            "SELECT expira_en IS NOT NULL FROM identidad.credencial_acceso WHERE usuario_id = ?",
                            usuario))
                    .isEqualTo(true);
            assertThat(columna("SELECT estado FROM identidad.usuario WHERE id = ?", usuario))
                    .isEqualTo("SUSPENDIDO");
            assertThat(sesionesVivas(usuario)).isZero();
            // R-AUD-01: la fila de la cuenta revocada se conserva con quien la reviso y por que.
            assertThat(columna("SELECT revocada_por FROM identidad.cuenta_servicio WHERE id = ?", alta.cuentaId()))
                    .isEqualTo(actor);
            assertThat(eventos("identidad.cuenta_servicio_revocada", alta.cuentaId()))
                    .isEqualTo(1);
        });
    }

    @Test
    @DisplayName(
            "Dado un usuario que no porta un rol SERVICIO_* · Cuando un operador intenta registrarlo como cuenta de"
                    + " servicio · Entonces se rechaza con SIN_ROL_DE_SERVICIO")
    void rechazaUnUsuarioSinRolDeServicio() {
        enTransaccionQueSeDeshace(() -> {
            UUID comun = operador();
            fixtura.asignarRol(
                    comun, rol("GU_F_COMUN_" + UUID.randomUUID().toString().substring(0, 6)), operador());

            rechazaCon(() -> registrar(operador(), comun), "AP-CU136-05");
            assertThat(columna("SELECT count(*)::int FROM identidad.cuenta_servicio WHERE usuario_id = ?", comun))
                    .isEqualTo(0);
        });
    }

    @Test
    @DisplayName("Dada una cuenta de servicio revocada · Cuando un operador intenta rotar su credencial · Entonces se"
            + " rechaza con CUENTA_REVOCADA")
    void rechazaRotarUnaCuentaRevocada() {
        enTransaccionQueSeDeshace(() -> {
            UUID actor = operador();
            Alta alta = registrar(actor, cuentaAprovisionada());
            casoDeUso.revocar(alta.cuentaId(), "Servicio dado de baja", EVIDENCIA, como(actor));

            rechazaCon(() -> casoDeUso.rotar(alta.cuentaId(), rotacion(), como(actor)), "AP-CU136-08");
        });
    }

    @Test
    @DisplayName("Rechaza que alguien gobierne su propia cuenta (R-SEG-07)")
    void rechazaGobernarLaPropiaCuenta() {
        enTransaccionQueSeDeshace(() -> {
            UUID usuario = cuentaAprovisionada();

            rechazaCon(() -> registrar(usuario, usuario), "AP-CU136-09");
        });
    }

    @Test
    @DisplayName("Rechaza un vencimiento pasado o mas alla del tope, y la falta de step-up, sin escribir nada")
    void rechazaVencimientoYStepUp() {
        enTransaccionQueSeDeshace(() -> {
            UUID actor = operador();
            UUID usuario = cuentaAprovisionada();

            rechazaCon(
                    () -> casoDeUso.registrar(
                            new PedidoDeAlta(usuario, "Servicio", enDias(91), "Motivo", EVIDENCIA), como(actor)),
                    "AP-CU136-03");
            rechazaCon(
                    () -> casoDeUso.registrar(
                            new PedidoDeAlta(usuario, "Servicio", enDias(-1), "Motivo", EVIDENCIA), como(actor)),
                    "AP-CU136-03");
            rechazaCon(
                    () -> casoDeUso.registrar(
                            new PedidoDeAlta(usuario, "Servicio", enDias(30), "Motivo", "otra-cosa"), como(actor)),
                    "AP-CU136-04");
            assertThat(columna("SELECT count(*)::int FROM identidad.cuenta_servicio WHERE usuario_id = ?", usuario))
                    .isEqualTo(0);
        });
    }
}
