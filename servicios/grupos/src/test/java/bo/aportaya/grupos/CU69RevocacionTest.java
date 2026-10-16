package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU69Invitar.EntradaInvitacion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-69 · revocación y regla de reemisión: una sola invitación viva por número y grupo, la anterior se
 * CONSERVA (nunca se reemplaza sola) y emitir otra exige revocarla antes.
 */
class CU69RevocacionTest extends BaseDeCU69 {

    private UUID invitar(UUID grupo, UUID emisor, String telefono) {
        return invitar(grupo, emisor, telefono, false, false).invitacionId().orElseThrow();
    }

    private void revocar(UUID invitacion, UUID usuario) {
        transaccion.execute(e -> {
            invitar.revocar(invitacion, contexto(usuario));
            return null;
        });
    }

    @Test
    @DisplayName(
            "Dada una invitación ENVIADA y vigente a un número en un grupo · Cuando se emite otra al mismo número con otra clave · Entonces se rechaza y no reemplaza a la anterior · Y la anterior sigue ENVIADA y el grupo tiene una sola invitación")
    void reemisionConservaLaAnterior() {
        UUID grupo = grupoConCupoLibre();
        UUID emisor = participanteActivo(grupo);
        UUID primera = invitar(grupo, emisor, "+59176100001");

        assertThatThrownBy(() -> invitar(grupo, emisor, "+59176100001"))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-09"));

        assertThat(estadoDe(primera)).isEqualTo("ENVIADA");
        assertThat(invitacionesDe(grupo)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una invitación vigente a un número en un grupo · Cuando se invita al mismo número en otro grupo · Entonces la invitación se emite sin chocar con la regla de reemisión")
    void otroGrupoNoChoca() {
        UUID uno = grupoConCupoLibre();
        UUID dos = grupoConCupoLibre();
        invitar(uno, participanteActivo(uno), "+59176100002");
        assertThat(invitar(dos, participanteActivo(dos), "+59176100002")).isNotNull();
    }

    @Test
    @DisplayName(
            "Dada una invitación ENVIADA · Cuando su emisor la revoca · Entonces queda REVOCADA con fecha de respuesta y se emite el evento de revocación · Y se puede emitir otra invitación al mismo número")
    void revocarPermiteReemitir() {
        UUID grupo = grupoConCupoLibre();
        UUID emisor = participanteActivo(grupo);
        UUID primera = invitar(grupo, emisor, "+59176100003");

        revocar(primera, emisor);

        assertThat(estadoDe(primera)).isEqualTo("REVOCADA");
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.invitacion WHERE id = ? AND fecha_respuesta IS NOT NULL",
                        primera))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.evento_dominio WHERE agregado_id = ? AND tipo = 'grupos.invitacion_revocada'",
                        primera))
                .isEqualTo(1);
        UUID segunda = invitar(grupo, emisor, "+59176100003");
        assertThat(segunda).isNotEqualTo(primera);
        assertThat(estadoDe(segunda)).isEqualTo("ENVIADA");
    }

    @Test
    @DisplayName(
            "Dada una invitación ENVIADA · Cuando su emisor la revoca dos veces · Entonces queda REVOCADA sin error · Y hay un solo evento de revocación")
    void revocarEsIdempotente() {
        UUID grupo = grupoConCupoLibre();
        UUID emisor = participanteActivo(grupo);
        UUID invitacion = invitar(grupo, emisor, "+59176100004");

        revocar(invitacion, emisor);
        revocar(invitacion, emisor);

        assertThat(estadoDe(invitacion)).isEqualTo("REVOCADA");
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.evento_dominio WHERE agregado_id = ? AND tipo = 'grupos.invitacion_revocada'",
                        invitacion))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una invitación ENVIADA emitida por un participante · Cuando otro participante del grupo intenta revocarla · Entonces se rechaza y la invitación sigue ENVIADA")
    void otroNoRevoca() {
        UUID grupo = grupoConCupoLibre();
        UUID emisor = participanteActivo(grupo);
        UUID otro = (UUID) dslFixtura
                .fetchOne(
                        "SELECT usuario_id FROM grupos.participante WHERE grupo_id = ? AND usuario_id <> ? LIMIT 1",
                        grupo,
                        emisor)
                .get(0);
        UUID invitacion = invitar(grupo, emisor, "+59176100005");

        assertThatThrownBy(() -> revocar(invitacion, otro)).isInstanceOf(ErrorDeNegocio.class);
        assertThat(estadoDe(invitacion)).isEqualTo("ENVIADA");
    }

    @Test
    @DisplayName(
            "Dada una invitación ya ACEPTADA · Cuando su emisor intenta revocarla · Entonces se rechaza y la invitación sigue ACEPTADA")
    void aceptadaNoSeRevoca() {
        UUID grupo = grupoConCupoLibre();
        UUID emisor = participanteActivo(grupo);
        UUID invitacion = invitar(grupo, emisor, "+59176100006");
        transaccion.execute(e -> {
            invitar.aceptar(invitacion, recibo(invitacion, emisor), contexto(emisor));
            return null;
        });

        assertThatThrownBy(() -> revocar(invitacion, emisor)).isInstanceOf(ErrorDeNegocio.class);
        assertThat(estadoDe(invitacion)).isEqualTo("ACEPTADA");
    }

    @Test
    @DisplayName(
            "Dada una invitación cuya fecha de expiración ya pasó · Cuando se emite otra al mismo número · Entonces la vencida se cierra como EXPIRADA · Y la nueva queda ENVIADA")
    void vencidaNoBloquea() {
        UUID grupo = grupoConCupoLibre();
        UUID emisor = participanteActivo(grupo);
        UUID vieja = new bo.aportaya.grupos.infraestructura.InvitacionRepositorio()
                .crear(
                        dslFixtura,
                        grupo,
                        "+59176100007",
                        "Contacto",
                        emisor,
                        fixtura.tokenDeInvitacion(),
                        "ENLACE",
                        OffsetDateTime.now().minusDays(8),
                        OffsetDateTime.now().minusDays(1));

        UUID nueva = invitar(grupo, emisor, "+59176100007");

        assertThat(estadoDe(vieja)).isEqualTo("EXPIRADA");
        assertThat(estadoDe(nueva)).isEqualTo("ENVIADA");
    }

    @Test
    @DisplayName("concurrencia: dos emisiones simultaneas al mismo numero: una gana y la otra recibe 69-09")
    void emisionesSimultaneas() throws Exception {
        UUID grupo = grupoConCupoLibre();
        UUID emisor = participanteActivo(grupo);
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tareas = java.util.List.of(
                    pool.submit(() -> emitirSimultanea(inicio, grupo, emisor)),
                    pool.submit(() -> emitirSimultanea(inicio, grupo, emisor)));
            inicio.countDown();
            int ganaron = 0;
            for (var t : tareas) if (t.get(30, TimeUnit.SECONDS)) ganaron++;
            assertThat(ganaron).isEqualTo(1);
        }
        assertThat(invitacionesDe(grupo)).isEqualTo(1);
    }

    private boolean emitirSimultanea(CountDownLatch inicio, UUID grupo, UUID emisor) throws InterruptedException {
        inicio.await();
        try {
            transaccion.execute(e -> invitar.invitar(
                    new EntradaInvitacion(
                            grupo,
                            "+59176100008",
                            "Contacto",
                            "ENLACE",
                            false,
                            false,
                            3,
                            fixtura.tokenDeInvitacion(),
                            OffsetDateTime.now().plusDays(7)),
                    contexto(emisor)));
            return true;
        } catch (ErrorDeNegocio rechazada) {
            assertThat(rechazada.codigo().valor()).isEqualTo("AP-CU69-09");
            return false;
        }
    }
}
