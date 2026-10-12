package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios.ConsumoInvitacion;
import bo.aportaya.grupos.infraestructura.InvitacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CU69CanjeTest extends BaseDeCU68 {
    private CanjearInvitacion canjes;
    private UUID invitacion;
    private UUID grupo;
    private ConsumoInvitacion recibo;

    @BeforeEach
    void preparar() {
        var invitaciones = new CU69Invitar(
                new Datos(dsl), new InvitacionRepositorio(), new Outbox("grupos"), Reloj.delSistema(), Ids.seguros());
        canjes = new CanjearInvitacion(new Datos(dsl), invitaciones, postularCU);
        grupo = grupoConCupoLibre();
        criterioVigente();
        var token = fixtura.tokenDeInvitacion();
        var emisor = dslFixtura
                .fetchOne("SELECT usuario_id FROM grupos.participante WHERE grupo_id=? LIMIT 1", grupo)
                .get(0, UUID.class);
        invitacion = new InvitacionRepositorio()
                .crear(
                        dslFixtura,
                        grupo,
                        "+59176000000",
                        "Contacto",
                        emisor,
                        token,
                        "ENLACE",
                        OffsetDateTime.now().minusMinutes(1),
                        OffsetDateTime.now().plusDays(1));
        recibo = new ConsumoInvitacion(token, grupo, contexto().usuarioId(), UUID.randomUUID(), OffsetDateTime.now());
    }

    @Test
    void canjeAbreSolicitudSinMembresiaYReintentoNoDuplica() {
        UUID uno = transaccion.execute(s -> canjes.ejecutar(invitacion, recibo, entrada(true), contexto()));
        UUID dos = transaccion.execute(s -> canjes.ejecutar(invitacion, recibo, entrada(true), contexto()));
        assertThat(dos).isEqualTo(uno);
        assertThat(dslFixtura
                        .fetchOne("SELECT estado FROM grupos.solicitud_ingreso WHERE id=?", uno)
                        .get(0))
                .isEqualTo("PENDIENTE");
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.participante WHERE grupo_id=? AND usuario_id=?",
                        grupo,
                        contexto().usuarioId()))
                .isZero();
        assertThat(contar("SELECT count(*)::int FROM grupos.solicitud_ingreso WHERE grupo_id=?", grupo))
                .isEqualTo(1);
    }

    @Test
    void falloDePostulacionRevierteInvitacionYPermiteRecuperar() {
        assertThatThrownBy(
                        () -> transaccion.execute(s -> canjes.ejecutar(invitacion, recibo, entrada(false), contexto())))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(dslFixtura
                        .fetchOne("SELECT estado FROM grupos.invitacion WHERE id=?", invitacion)
                        .get(0))
                .isEqualTo("ENVIADA");
        assertThat(contar("SELECT count(*)::int FROM grupos.solicitud_ingreso WHERE grupo_id=?", grupo))
                .isZero();
        assertThat(transaccion.<UUID>execute(s -> canjes.ejecutar(invitacion, recibo, entrada(true), contexto())))
                .isNotNull();
    }

    @Test
    void reciboDeOtroTokenNoSirve() {
        var falso = new ConsumoInvitacion(
                UUID.randomUUID(), grupo, contexto().usuarioId(), recibo.clave(), OffsetDateTime.now());
        assertThatThrownBy(
                        () -> transaccion.execute(s -> canjes.ejecutar(invitacion, falso, entrada(true), contexto())))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    void recuperaConsumoHechoAntesDelVencimientoAunqueLaRedDemore() {
        dslFixtura.execute(
                "UPDATE grupos.invitacion SET fecha_expiracion=now()-interval '1 second' WHERE id=?", invitacion);
        var previo = new ConsumoInvitacion(
                recibo.tokenId(),
                grupo,
                recibo.usuarioId(),
                recibo.clave(),
                OffsetDateTime.now().minusSeconds(5));
        assertThat(transaccion.<UUID>execute(s -> canjes.ejecutar(invitacion, previo, entrada(true), contexto())))
                .isNotNull();
    }

    @Test
    void dosPeticionesConcurrentesAbrenUnaSolicitud() throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> {
                inicio.await();
                return transaccion.execute(s -> canjes.ejecutar(invitacion, recibo, entrada(true), contexto()));
            });
            var b = executor.submit(() -> {
                inicio.await();
                return transaccion.execute(s -> canjes.ejecutar(invitacion, recibo, entrada(true), contexto()));
            });
            inicio.countDown();
            assertThat(a.get(10, TimeUnit.SECONDS)).isEqualTo(b.get(10, TimeUnit.SECONDS));
        }
        assertThat(contar("SELECT count(*)::int FROM grupos.solicitud_ingreso WHERE grupo_id=?", grupo))
                .isEqualTo(1);
    }

    @Test
    void canjeBajoRolRealDelServicio() {
        var resultado = transaccion.execute(s -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return canjes.ejecutar(invitacion, recibo, entrada(true), contexto());
        });
        assertThat(resultado).isNotNull();
    }

    private String codigoDelRechazo(Runnable comprobacion) {
        try {
            comprobacion.run();
        } catch (ErrorDeNegocio rechazo) {
            return rechazo.codigo().valor();
        }
        throw new AssertionError("La comprobacion debio rechazar");
    }

    private void comprobar(boolean kyc) {
        transaccion.executeWithoutResult(s -> canjes.comprobarAntesDeConsumir(invitacion, entrada(kyc), contexto()));
    }

    @Test
    @DisplayName(
            "Dado un invitado con una postulación pendiente en el grupo · Cuando canjea una invitación vigente de ese grupo · Entonces se rechaza con AP-CU68-06 y el enlace no se consume en identidad")
    void criterioCanjeConPostulacionPendiente() {
        transaccion.execute(s -> postularCU.postular(entrada(true), contexto()));

        // La comprobacion corre ANTES de consumir el enlace en identidad: rechaza con el codigo de la regla...
        assertThat(codigoDelRechazo(() -> comprobar(true))).isEqualTo("AP-CU68-06");
        // ...y no deja nada escrito: la invitacion sigue ENVIADA y sin solicitud, asi que el emisor puede revocarla.
        var fila = dslFixtura.fetchOne(
                "SELECT estado, solicitud_ingreso_id FROM grupos.invitacion WHERE id=?", invitacion);
        assertThat(fila.get("estado", String.class)).isEqualTo("ENVIADA");
        assertThat(fila.get("solicitud_ingreso_id", UUID.class)).isNull();
        assertThat(contar("SELECT count(*)::int FROM grupos.solicitud_ingreso WHERE grupo_id=?", grupo))
                .isEqualTo(1);
    }

    @Test
    void rechazaAntesDeConsumirPorCadaCondicionLocal() {
        // KYC insuficiente: la misma regla que postular.
        assertThat(codigoDelRechazo(() -> comprobar(false))).isEqualTo("AP-CU68-02");

        // Invitacion revocada, vencida o de otro grupo: no hay nada que canjear, y no se quema un enlace que ya no
        // sirve.
        dslFixtura.execute("UPDATE grupos.invitacion SET estado='REVOCADA' WHERE id=?", invitacion);
        assertThat(codigoDelRechazo(() -> comprobar(true))).isEqualTo("AP-CU69-05");
        dslFixtura.execute("UPDATE grupos.invitacion SET estado='ENVIADA' WHERE id=?", invitacion);
        dslFixtura.execute(
                "UPDATE grupos.invitacion SET fecha_expiracion=now()-interval '1 second' WHERE id=?", invitacion);
        assertThat(codigoDelRechazo(() -> comprobar(true))).isEqualTo("AP-CU69-05");
        dslFixtura.execute(
                "UPDATE grupos.invitacion SET fecha_expiracion=now()+interval '1 day' WHERE id=?", invitacion);
        UUID otroGrupo = grupoConCupoLibre();
        assertThat(codigoDelRechazo(() -> transaccion.executeWithoutResult(s -> canjes.comprobarAntesDeConsumir(
                        invitacion,
                        new CU68Postular.EntradaPostulacion(
                                otroGrupo,
                                (short) 1,
                                "otro",
                                false,
                                BigDecimal.ZERO,
                                true,
                                50,
                                0,
                                uno(),
                                uno(),
                                uno(),
                                uno()),
                        contexto()))))
                .isEqualTo("AP-CU69-05");
    }

    @Test
    void reintentoDeUnCanjeCompletadoNoLoRechazaLaComprobacion() {
        // El canje se completo y el cliente reintenta con la misma clave: la comprobacion previa no puede decir
        // «ya tenes una postulacion» (la que acaba de crear), porque entonces el reintento dejaria de ser idempotente.
        UUID solicitud = transaccion.execute(s -> canjes.ejecutar(invitacion, recibo, entrada(true), contexto()));

        comprobar(true);

        assertThat(transaccion.<UUID>execute(s -> canjes.ejecutar(invitacion, recibo, entrada(true), contexto())))
                .isEqualTo(solicitud);
    }

    private CU68Postular.EntradaPostulacion entrada(boolean kyc) {
        return new CU68Postular.EntradaPostulacion(
                grupo,
                (short) 1,
                "Solicitud por invitación",
                false,
                BigDecimal.ZERO,
                kyc,
                50,
                0,
                uno(),
                uno(),
                uno(),
                uno());
    }
}
