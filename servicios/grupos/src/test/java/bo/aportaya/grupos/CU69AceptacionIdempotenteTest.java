package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU69Enlace.Aceptada;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-69 · Aceptar por enlace es idempotente (regla 96.3.3): reintentar con la misma {@code Idempotency-Key}
 * tras un corte devuelve la participación original en vez de un 422 por algo que sí salió bien.
 *
 * <p>Los tres niveles del contrato: correcto (mismo usuario, misma clave), límite (el reintento no
 * duplica participante, cupo ni aceptación del reglamento) e inválido (otra clave, otra persona).
 */
class CU69AceptacionIdempotenteTest extends BaseDeCU69 {

    private record Aceptacion(UUID grupo, UUID invitacion, UUID token, UUID invitado, String hash) {}

    private Aceptacion invitacionLista(String telefono) {
        UUID grupo = grupoConCupoLibre();
        UUID invitacion = invitar(grupo, participanteActivo(grupo), telefono, false, false)
                .invitacionId()
                .orElseThrow();
        UUID invitado = fixtura.usuario();
        UUID token = tokenDe(invitacion);
        String hash = transaccion.execute(
                e -> enlace.datosDe(token, contexto(invitado)).hashReglamento());
        return new Aceptacion(grupo, invitacion, token, invitado, hash);
    }

    private Aceptada aceptar(Aceptacion a, UUID clave, UUID quien) {
        return transaccion.execute(
                e -> enlace.aceptar(a.token(), clave, a.hash(), "127.0.0.1", BigDecimal.ZERO, contexto(quien)));
    }

    @Test
    @DisplayName(
            "Dado un enlace ya aceptado · Cuando la misma persona repite con la MISMA clave · Entonces recibe la misma participación y no se duplica nada")
    void elReintentoDevuelveLoMismoYNoDuplica() {
        Aceptacion a = invitacionLista("+59176000081");
        UUID clave = UUID.randomUUID();

        Aceptada primera = aceptar(a, clave, a.invitado());
        Optional<Aceptada> repetida =
                transaccion.execute(e -> enlace.repeticion(a.token(), clave, contexto(a.invitado())));

        assertThat(repetida).contains(primera);
        // El reintento por el mismo camino de la aceptación (carrera con el primero) tampoco crea otra.
        assertThat(aceptar(a, clave, a.invitado())).isEqualTo(primera);
        assertThat(contar(
                        "SELECT count(*) FROM grupos.participante WHERE usuario_id = ? AND grupo_id = ?",
                        a.invitado(),
                        a.grupo()))
                .isEqualTo(1);
        assertThat(cuposLibresDe(a.grupo())).isZero();
        assertThat(contar(
                        "SELECT count(*) FROM grupos.aceptacion_reglamento WHERE participante_id = ?",
                        primera.participanteId()))
                .isEqualTo(1);
        assertThat(estadoDe(a.invitacion())).isEqualTo("ACEPTADA");
    }

    @Test
    @DisplayName("Antes de aceptar no hay repetición que devolver")
    void sinAceptacionPreviaNoHayRepeticion() {
        Aceptacion a = invitacionLista("+59176000082");

        Optional<Aceptada> previa =
                transaccion.execute(e -> enlace.repeticion(a.token(), UUID.randomUUID(), contexto(a.invitado())));

        assertThat(previa).isEmpty();
    }

    @Test
    @DisplayName("Otra clave sobre un enlace ya consumido sigue siendo un enlace inválido")
    void otraClaveNoEsUnReintento() {
        Aceptacion a = invitacionLista("+59176000083");
        aceptar(a, UUID.randomUUID(), a.invitado());

        assertThatThrownBy(() -> transaccion.execute(
                        e -> enlace.repeticion(a.token(), UUID.randomUUID(), contexto(a.invitado()))))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no es valida");
        assertThatThrownBy(() -> aceptar(a, UUID.randomUUID(), a.invitado()))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no es valida");
    }

    @Test
    @DisplayName("Otra persona con la misma clave no recibe la participación ajena")
    void otraPersonaNoRecibeLaParticipacionAjena() {
        Aceptacion a = invitacionLista("+59176000084");
        UUID clave = UUID.randomUUID();
        aceptar(a, clave, a.invitado());
        UUID intruso = fixtura.usuario();

        assertThatThrownBy(() -> transaccion.execute(e -> enlace.repeticion(a.token(), clave, contexto(intruso))))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no es valida");
        assertThatThrownBy(() -> aceptar(a, clave, intruso))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no es valida");
        assertThat(contar("SELECT count(*) FROM grupos.participante WHERE usuario_id = ?", intruso))
                .isZero();
    }
}
