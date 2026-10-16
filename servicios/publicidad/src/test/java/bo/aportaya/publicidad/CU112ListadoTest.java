package bo.aportaya.publicidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.publicidad.aplicacion.CU112ModerarPieza.EntradaPieza;
import bo.aportaya.publicidad.aplicacion.ConsultarListadosDePublicidad.FilaPieza;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Backoffice carril A · H7.S1 · la cola de moderacion de piezas (CU-112) contra PostgreSQL real; escenario en {@link EscenarioDeListados}. */
class CU112ListadoTest extends EscenarioDeListados {

    @Test
    @DisplayName("Dada una pieza pendiente y otra ya moderada · Cuando el moderador abre su cola · Entonces solo ve la"
            + " pendiente, la mas antigua primero")
    void colaDeModeracion() {
        UUID pendiente = transaccion.execute(t -> moderacionCU.subir(
                new EntradaPieza(
                        anuncianteId, "Aporta con QR", "Texto de la pieza", "https://cdn.ejemplo/a.png", "IMAGEN"),
                operaciones));
        UUID moderada = transaccion.execute(t -> moderacionCU.subir(
                new EntradaPieza(anuncianteId, "Otra pieza", null, "https://cdn.ejemplo/b.png", "IMAGEN"),
                operaciones));
        OffsetDateTime antigua = OffsetDateTime.of(2001, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        dslFixtura.execute(
                """
                UPDATE publicidad.pieza_creativa SET creada_en = ?::timestamptz WHERE id IN (?, ?)
                """,
                antigua,
                pendiente,
                moderada);
        dslFixtura.execute(
                """
                UPDATE publicidad.pieza_creativa SET estado_moderacion = 'APROBADA' WHERE id = ?
                """,
                moderada);

        var cola = transaccion.execute(t -> listados.piezas(
                null, Optional.of(antigua.minusSeconds(1)), Optional.of(ANTES_DE_TODO), 25, operaciones));

        assertThat(cola.items())
                .extracting(FilaPieza::piezaCreativaId)
                .contains(pendiente)
                .doesNotContain(moderada);
        assertThat(cola.items().get(0).estadoModeracion()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("rechaza un filtro invalido o un cursor a medias con AP-CU112-04")
    void filtroInvalido() {
        assertThatThrownBy(() -> transaccion.execute(
                        t -> listados.piezas("EN_DUDA", Optional.empty(), Optional.empty(), 25, operaciones)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU112-04"));
    }
}
