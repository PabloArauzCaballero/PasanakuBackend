package bo.aportaya.plataforma.web.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** La regla pura de los cuatro ojos: dos personas, cada una con su sesion. */
class CuatroOjosTest {

    private static final CodigoError CODIGO = CodigoError.de(14, 2);
    private static final UUID ANA = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");
    private static final UUID BETO = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000002");
    private static final UUID SESION_1 = UUID.fromString("5e510000-0000-4000-8000-000000000001");
    private static final UUID SESION_2 = UUID.fromString("5e510000-0000-4000-8000-000000000002");

    @Test
    @DisplayName("correcto: otra persona en otra sesion firma, y quedan los dos actores")
    void otraPersonaFirma() {
        var firma =
                CuatroOjos.exigir(new ActorAutenticado(ANA, SESION_1), new ActorAutenticado(BETO, SESION_2), CODIGO);
        assertThat(firma.primero().usuarioId()).isEqualTo(ANA);
        assertThat(firma.segundo().usuarioId()).isEqualTo(BETO);
        assertThat(firma.segundo().sesionId()).isEqualTo(SESION_2);
    }

    @Test
    @DisplayName("limite: la misma persona desde OTRA sesion no son cuatro ojos")
    void mismaPersonaOtraSesion() {
        assertThatThrownBy(() -> CuatroOjos.exigir(
                        new ActorAutenticado(ANA, SESION_1), new ActorAutenticado(ANA, SESION_2), CODIGO))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("otra persona")
                .extracting(e -> ((ErrorDeNegocio) e).codigo())
                .isEqualTo(CODIGO);
    }

    @Test
    @DisplayName("invalido: la misma persona en la misma sesion se rechaza")
    void mismaPersonaMismaSesion() {
        assertThatThrownBy(() -> CuatroOjos.exigir(
                        new ActorAutenticado(ANA, SESION_1), new ActorAutenticado(ANA, SESION_1), CODIGO))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName("invalido: dos usuarios que comparten sesion indican algo roto, y no se firma")
    void sesionCompartida() {
        assertThatThrownBy(() -> CuatroOjos.exigir(
                        new ActorAutenticado(ANA, SESION_1), new ActorAutenticado(BETO, SESION_1), CODIGO))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("otra sesion");
    }

    @Test
    @DisplayName("limite: primer actor rehidratado sin sesion; decide la persona")
    void primeroSinSesion() {
        var firma =
                CuatroOjos.exigir(ActorAutenticado.registrado(ANA, null), new ActorAutenticado(BETO, SESION_2), CODIGO);
        assertThat(firma.primero().sesion()).isEmpty();
        assertThatThrownBy(() -> CuatroOjos.exigir(
                        ActorAutenticado.registrado(ANA, null), new ActorAutenticado(ANA, SESION_2), CODIGO))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName("invalido: un actor sin usuario no existe")
    void actorSinUsuario() {
        assertThatThrownBy(() -> new ActorAutenticado(null, SESION_1)).isInstanceOf(NullPointerException.class);
    }
}
