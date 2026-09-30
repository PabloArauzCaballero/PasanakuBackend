package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.grupos.aplicacion.CU69Invitar.EntradaInvitacion;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** La aceptación funciona con el rol y las políticas de fila de producción. */
class CU69RolRealTest extends BaseDeCU69 {

    @Test
    void invitacionEIngresoFuncionanConRolRealDelServicio() {
        UUID grupo = grupoConCupoLibre();
        UUID emisor = participanteActivo(grupo);
        UUID invitado = fixtura.usuario();
        UUID tokenEmitido = fixtura.tokenDeInvitacion();

        UUID invitacion = transaccion.execute(e -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return invitar.invitar(
                            new EntradaInvitacion(
                                    grupo, "+59176000042", "Contacto", "ENLACE", false, false, 3, tokenEmitido),
                            contexto(emisor))
                    .invitacionId()
                    .orElseThrow();
        });
        UUID token = tokenDe(invitacion);
        String hash = transaccion.execute(
                e -> enlace.datosDe(token, contexto(invitado)).hashReglamento());

        transaccion.execute(e -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            enlace.aceptar(token, hash, "127.0.0.1", BigDecimal.ZERO, contexto(invitado));
            return null;
        });

        assertThat(estadoDe(invitacion)).isEqualTo("ACEPTADA");
        assertThat(contar(
                        "SELECT count(*) FROM grupos.participante WHERE grupo_id = ? AND usuario_id = ?",
                        grupo,
                        invitado))
                .isEqualTo(1);
    }
}
