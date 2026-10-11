package bo.aportaya.organizador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.organizador.aplicacion.CU90ResolverHabilitacion.Entrada;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * CU-90 · resolución humana de la habilitación: aprobar, rechazar con motivo, idempotencia y concurrencia.
 *
 * <p>Amenazas: autoaprobación, doble resolución simultánea, aprobación sin requisitos, rechazo
 * automático por falta de datos y un reintento que duplica.
 */
class CU90ResolucionTest extends BaseDeResolucion {

    @Test
    @DisplayName(
            "Dada una solicitud pendiente de un postulante que cumple los requisitos · Cuando una persona de backoffice la aprueba · Entonces se crea el organizador pendiente de capacitación y la solicitud queda APROBADA · Y queda una sola decisión conservada y un solo evento de resolución")
    void apruebaYConserva() {
        UUID solicitante = fixtura.usuario();
        UUID solicitud = postular(solicitante);

        var resultado =
                resolver(entrada(solicitud, UUID.randomUUID(), "APROBAR", "Cumple y tiene historial", 0), backoffice);

        assertThat(resultado.nueva()).isTrue();
        assertThat(resultado.decision().revision()).isEqualTo(1);
        assertThat(resultado.decision().organizadorId()).isNotNull();
        assertThat(estadoSolicitud(solicitud)).isEqualTo("APROBADA");
        assertThat(dsl.fetchOne(
                                "SELECT estado FROM organizador.organizador WHERE id=?",
                                resultado.decision().organizadorId())
                        .get(0))
                .isEqualTo("CAPACITACION_PENDIENTE");
        assertThat(decisiones(solicitud)).isEqualTo(1);
        assertThat(eventos("organizador.habilitacion_resuelta", solicitud)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una solicitud ya aprobada con una clave de idempotencia · Cuando se reintenta la resolución con la misma clave · Entonces devuelve la decisión original sin crear otra · Y hay un solo organizador, una sola decisión y un solo evento")
    void idempotente() {
        UUID solicitud = postular(fixtura.usuario());
        UUID clave = UUID.randomUUID();
        var uno = resolver(entrada(solicitud, clave, "APROBAR", "Cumple", 0), backoffice);
        var dos = resolver(entrada(solicitud, clave, "APROBAR", "Cumple", 0), backoffice);

        assertThat(dos.nueva()).isFalse();
        assertThat(dos.decision().id()).isEqualTo(uno.decision().id());
        assertThat(decisiones(solicitud)).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*) FROM organizador.organizador WHERE id=?",
                        uno.decision().organizadorId()))
                .isEqualTo(1);
        assertThat(eventos("organizador.habilitacion_resuelta", solicitud)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una solicitud aprobada con una clave de idempotencia · Cuando se reusa esa clave con otro contenido · Entonces se rechaza y la solicitud sigue APROBADA con una sola decisión")
    void claveConOtroContenido() {
        UUID solicitud = postular(fixtura.usuario());
        UUID clave = UUID.randomUUID();
        resolver(entrada(solicitud, clave, "APROBAR", "Cumple", 0), backoffice);

        assertThatThrownBy(() -> resolver(entrada(solicitud, clave, "RECHAZAR", "Cambio de idea", 0), backoffice))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU90-08"));
        assertThat(estadoSolicitud(solicitud)).isEqualTo("APROBADA");
        assertThat(decisiones(solicitud)).isEqualTo(1);
    }

    @Test
    @DisplayName("Rechazar es una decisión humana con motivo que SE PERSISTE (no se pierde por una excepción)")
    void rechazoHumanoPersiste() {
        UUID solicitud = postular(fixtura.usuario());

        var resultado = resolver(
                entrada(solicitud, UUID.randomUUID(), "RECHAZAR", "Referencias no verificables", 0), backoffice);

        assertThat(estadoSolicitud(solicitud)).isEqualTo("RECHAZADA");
        assertThat(dsl.fetchOne("SELECT motivo_rechazo FROM organizador.solicitud_organizador WHERE id=?", solicitud)
                        .get(0))
                .isEqualTo("Referencias no verificables");
        assertThat(resultado.decision().organizadorId()).isNull();
        assertThat(contar(
                        "SELECT count(*) FROM organizador.organizador WHERE usuario_id=(SELECT usuario_id FROM organizador.solicitud_organizador WHERE id=?)",
                        solicitud))
                .isZero();
        assertThat(decisiones(solicitud)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una solicitud pendiente cuyos requisitos medidos no alcanzan o no existen · Cuando una persona de backoffice intenta aprobarla · Entonces se rechaza el intento sin aprobar ni rechazar la solicitud · Y la solicitud sigue PENDIENTE para una persona, sin decisión ni organizador")
    void sinRequisitosNiAprobaNiRechazaSolo() {
        UUID solicitud = postular(fixtura.usuario());
        var insuficientes = medidosDe("10", "1");
        var sinDatos = Map.<String, BigDecimal>of();

        for (var datos : List.of(insuficientes, sinDatos)) {
            assertThatThrownBy(() -> resolver(
                            new Entrada(solicitud, UUID.randomUUID(), "APROBAR", "Intento", 0, datos), backoffice))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .satisfies(e ->
                            assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU90-05"));
        }

        assertThat(estadoSolicitud(solicitud)).isEqualTo("PENDIENTE");
        assertThat(decisiones(solicitud)).isZero();
        assertThat(contar("SELECT count(*) FROM organizador.organizador")).isZero();
    }

    @Test
    @DisplayName(
            "Dada una solicitud pendiente de un postulante · Cuando el propio postulante intenta resolverla, aunque actúe con rol de backoffice · Entonces se rechaza y la solicitud sigue PENDIENTE")
    void sinAutoaprobacion() {
        UUID solicitante = fixtura.usuario();
        UUID solicitud = postular(solicitante);

        assertThatThrownBy(
                        () -> resolver(entrada(solicitud, UUID.randomUUID(), "APROBAR", "Me apruebo", 0), solicitante))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU90-04"));
        assertThat(estadoSolicitud(solicitud)).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName(
            "Dado un participante sin rol de backoffice y una solicitud pendiente de otra persona · Cuando intenta resolverla, ver la bandeja o abrir su expediente · Entonces cada intento se niega por rol insuficiente · Y la solicitud sigue PENDIENTE")
    void rolInsuficiente() {
        UUID solicitud = postular(fixtura.usuario());
        UUID otro = fixtura.usuario();
        var comoParticipante = contextoDe(otro);

        assertThatThrownBy(() -> transaccion.execute(t -> resolucionCU.resolver(
                        entrada(solicitud, UUID.randomUUID(), "APROBAR", "x", 0), comoParticipante)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> transaccion.execute(
                        t -> bandejaCU.bandeja(List.of("PENDIENTE"), null, null, 10, comoParticipante)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> transaccion.execute(t -> bandejaCU.expediente(solicitud, comoParticipante)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(estadoSolicitud(solicitud)).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName(
            "Dada una solicitud pendiente · Cuando se la resuelve con una revisión obsoleta, y después de resuelta se intenta resolverla otra vez · Entonces se rechaza cada intento · Y la solicitud conserva su primera resolución, con una sola decisión")
    void obsoletaOYaResuelta() {
        UUID solicitud = postular(fixtura.usuario());

        assertThatThrownBy(() -> resolver(entrada(solicitud, UUID.randomUUID(), "RECHAZAR", "x", 3), backoffice))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU90-08"));

        resolver(entrada(solicitud, UUID.randomUUID(), "RECHAZAR", "No cumple el perfil", 0), backoffice);
        assertThatThrownBy(() -> resolver(entrada(solicitud, UUID.randomUUID(), "APROBAR", "Ahora si", 1), backoffice))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU90-03"));
        assertThat(estadoSolicitud(solicitud)).isEqualTo("RECHAZADA");
        assertThat(decisiones(solicitud)).isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: dos revisores resolviendo a la vez: solo una resolución gana y queda una sola decisión")
    void resolucionesSimultaneas() throws Exception {
        UUID solicitud = postular(fixtura.usuario());
        UUID otroRevisor = fixtura.usuario();
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() ->
                    simultanea(inicio, entrada(solicitud, UUID.randomUUID(), "APROBAR", "Cumple", 0), backoffice));
            var b = pool.submit(() ->
                    simultanea(inicio, entrada(solicitud, UUID.randomUUID(), "RECHAZAR", "No cumple", 0), otroRevisor));
            inicio.countDown();
            assertThat((a.get(30, TimeUnit.SECONDS) ? 1 : 0) + (b.get(30, TimeUnit.SECONDS) ? 1 : 0))
                    .isEqualTo(1);
        }
        assertThat(decisiones(solicitud)).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*) FROM organizador.solicitud_organizador WHERE id=? AND estado IN ('APROBADA','RECHAZADA')",
                        solicitud))
                .isEqualTo(1);
    }

    private boolean simultanea(CountDownLatch inicio, Entrada entrada, UUID revisor) throws InterruptedException {
        inicio.await();
        try {
            resolver(entrada, revisor);
            return true;
        } catch (ErrorDeNegocio perdio) {
            return false;
        }
    }
}
