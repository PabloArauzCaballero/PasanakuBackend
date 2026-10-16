package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso.Entrada;
import bo.aportaya.grupos.aplicacion.CU68Postular.EntradaPostulacion;
import bo.aportaya.grupos.infraestructura.AdmisionRepositorio;
import bo.aportaya.grupos.infraestructura.CreacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Cupo concurrente: dos solicitudes distintas por el ÚLTIMO cupo, resueltas a la vez por backoffice.
 * Solo una se acepta; la otra no deja resolución ni miembro parcial y sigue pendiente.
 */
class CU68AdmisionConcurrenteTest extends BaseDeCU68 {

    private ContextoSesion sesion(UUID usuario, String rol) {
        return ContextoSesion.de(usuario, rol, new Traza(UUID.randomUUID().toString()));
    }

    private UUID postularComo(UUID grupo, UUID usuario) {
        return transaccion
                .execute(e -> postularCU.postular(
                        new EntradaPostulacion(
                                grupo,
                                (short) 1,
                                "quiero entrar",
                                false,
                                BigDecimal.ZERO,
                                true,
                                0,
                                4,
                                new BigDecimal("0.80"),
                                new BigDecimal("0.90"),
                                new BigDecimal("0.70"),
                                new BigDecimal("0.10")),
                        sesion(usuario, "PARTICIPANTE")))
                .solicitudId();
    }

    @Test
    void dosSolicitudesPorElUltimoCupoSoloUnaSeAcepta() throws Exception {
        var admision = new CU68AceptarIngreso(
                new Datos(dsl), new AdmisionRepositorio(), new Outbox("grupos"), Reloj.delSistema());
        UUID grupo = grupoConCupoLibre();
        dslFixtura.execute("UPDATE grupos.grupo SET estado='ABIERTO_A_INSCRIPCION' WHERE id=?", grupo);
        new CreacionRepositorio().configurar(dslFixtura, grupo, false);
        var existente = dslFixtura.fetchOne(
                "SELECT id, usuario_id FROM grupos.participante WHERE grupo_id=? ORDER BY id LIMIT 1", grupo);
        dslFixtura.execute(
                "UPDATE grupos.participante SET es_organizador=true WHERE id=?", existente.get("id", UUID.class));
        var administrador = sesion(existente.get("usuario_id", UUID.class), "ORGANIZADOR");
        criterioVigente();

        UUID unoUsuario = fixtura.usuario();
        UUID otroUsuario = fixtura.usuario();
        UUID uno = postularComo(grupo, unoUsuario);
        UUID otro = postularComo(grupo, otroUsuario);
        var propuestaUno = transaccion.execute(tx -> admision.proponer(
                new Entrada(uno, UUID.randomUUID(), "ACEPTAR", "Cumple y hay lugar", 0, null), administrador));
        var propuestaOtro = transaccion.execute(tx -> admision.proponer(
                new Entrada(otro, UUID.randomUUID(), "ACEPTAR", "Cumple y hay lugar", 0, null), administrador));

        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> resolver(admision, inicio, uno, propuestaUno.id()));
            var b = pool.submit(() -> resolver(admision, inicio, otro, propuestaOtro.id()));
            inicio.countDown();
            assertThat((a.get(30, TimeUnit.SECONDS) ? 1 : 0) + (b.get(30, TimeUnit.SECONDS) ? 1 : 0))
                    .isEqualTo(1);
        }

        assertThat(contar("SELECT count(*)::int FROM grupos.cupo WHERE grupo_id=? AND estado='LIBRE'", grupo))
                .isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.participante WHERE grupo_id=? AND usuario_id IN (?,?)",
                        grupo,
                        unoUsuario,
                        otroUsuario))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.solicitud_ingreso WHERE id IN (?,?) AND estado='PENDIENTE'",
                        uno,
                        otro))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.decision_ingreso WHERE solicitud_id IN (?,?) AND fase='RESOLUCION'",
                        uno,
                        otro))
                .isEqualTo(1);
    }

    private boolean resolver(CU68AceptarIngreso admision, CountDownLatch inicio, UUID solicitud, UUID propuesta)
            throws InterruptedException {
        inicio.await();
        try {
            transaccion.execute(tx -> admision.resolver(
                    new Entrada(solicitud, UUID.randomUUID(), "ACEPTAR", "Revisión humana documentada", 1, propuesta),
                    sesion(fixtura.usuario(), "BACKOFFICE")));
            return true;
        } catch (ErrorDeNegocio sinCupo) {
            return false;
        }
    }
}
