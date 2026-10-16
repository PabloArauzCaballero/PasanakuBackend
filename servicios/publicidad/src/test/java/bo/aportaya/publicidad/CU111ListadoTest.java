package bo.aportaya.publicidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.publicidad.aplicacion.ConsultarListadosDePublicidad.FilaCampana;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Backoffice carril A · H7.S1 · la cola de aprobacion de campanas (CU-111) contra PostgreSQL real; escenario en {@link EscenarioDeListados}. */
class CU111ListadoTest extends EscenarioDeListados {

    @Test
    @DisplayName("Dadas dos campanas en revision · Cuando Operaciones abre la cola sin filtro · Entonces ve las dos,"
            + " la que empieza antes primero, con presupuesto y moneda")
    void colaDeCampanas() {
        OffsetDateTime ventana = OffsetDateTime.of(2091, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        UUID segunda = campanaQueEmpieza(ventana.plusDays(2));
        UUID primera = campanaQueEmpieza(ventana);

        var cola = transaccion.execute(t -> listados.campanas(
                null, Optional.of(ventana.minusSeconds(1)), Optional.of(ANTES_DE_TODO), 2, operaciones));

        assertThat(cola.items()).extracting(FilaCampana::campanaId).containsExactly(primera, segunda);
        assertThat(cola.items().get(0).estado()).isEqualTo("EN_REVISION");
        assertThat(cola.items().get(0).presupuestoTotal()).isEqualByComparingTo("900.00");
        assertThat(cola.items().get(0).moneda()).isEqualTo("BOB");
        assertThat(cola.items().get(0).anuncianteId()).isEqualTo(anuncianteId);
    }

    @Test
    @DisplayName("Dada una ventana sin campanas · Cuando Operaciones abre la cola · Entonces recibe una pagina vacia y"
            + " sin cursor, nunca un error")
    void colaVacia() {
        var cola = transaccion.execute(t -> listados.campanas(
                null,
                Optional.of(OffsetDateTime.of(2099, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC)),
                Optional.of(ANTES_DE_TODO),
                25,
                operaciones));

        assertThat(cola.items()).isEmpty();
        assertThat(cola.proximoId()).isEmpty();
    }

    @Test
    @DisplayName("rechaza un filtro invalido o un cursor a medias con AP-CU111-04")
    void filtroInvalido() {
        assertThatThrownBy(() -> transaccion.execute(t ->
                        listados.campanas(null, Optional.of(OffsetDateTime.now()), Optional.empty(), 25, operaciones)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU111-04"));
    }
}
