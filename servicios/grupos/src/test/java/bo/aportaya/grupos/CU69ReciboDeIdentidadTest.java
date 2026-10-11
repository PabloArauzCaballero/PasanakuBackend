package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-69 · la aceptacion de un solo uso con el recibo de consumo que emite identidad (el canje). */
class CU69ReciboDeIdentidadTest extends BaseDeCU69 {

    @Test
    @DisplayName("rechaza: aceptar otra vez con el mismo recibo de identidad (token ya consumido) da TOKEN_INVALIDO")
    void criterio2PorReciboDeIdentidad() {
        UUID grupo = grupoConCupoLibre();
        UUID emisor = participanteActivo(grupo);
        UUID invitacion = invitar(grupo, emisor, "+59176000014", false, false)
                .invitacionId()
                .orElseThrow();
        transaccion.execute(e -> {
            invitar.aceptar(invitacion, recibo(invitacion, emisor), contexto(emisor));
            return null;
        });

        assertThatThrownBy(() -> transaccion.execute(e -> {
                    invitar.aceptar(invitacion, recibo(invitacion, emisor), contexto(emisor));
                    return null;
                }))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no es valida");
    }

    @Test
    @DisplayName(
            "concurrencia: la aceptación con recibo de identidad pasa la invitación a ACEPTADA solo si sigue ENVIADA")
    void concurrenciaDelReciboDeIdentidad() {
        UUID grupo = grupoConCupoLibre();
        UUID emisor = participanteActivo(grupo);
        UUID invitacion = invitar(grupo, emisor, "+59176000015", false, false)
                .invitacionId()
                .orElseThrow();

        transaccion.execute(e -> {
            invitar.aceptar(invitacion, recibo(invitacion, emisor), contexto(emisor));
            return null;
        });

        assertThat(estadoDe(invitacion)).isEqualTo("ACEPTADA");
    }
}
