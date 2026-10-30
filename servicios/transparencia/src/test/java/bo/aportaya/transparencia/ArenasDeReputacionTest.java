package bo.aportaya.transparencia;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.transparencia.dominio.ArenasDeReputacion;
import bo.aportaya.transparencia.dominio.ArenasDeReputacion.Arena;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Donde cae un puntaje y cuanto avanzo, sin base: la funcion pura (arenas de reputacion, 2026-10-30).
 *
 * <p>El catalogo de estas pruebas copia los tramos de la semilla v1, que es un catalogo interno
 * PROPUESTO (confirmar con Pablo): si cambia la semilla, la prueba que lo lee de la base es
 * {@code ArenaRepositorioTest}. Aca se fija la regla: {@code [desde, hasta)}, la ultima incluye
 * su {@code hasta}, progreso a cuatro decimales HALF_EVEN.
 */
class ArenasDeReputacionTest {

    private static final List<Arena> V1 = List.of(
            arena("CAMPAMENTO", 0, "0.00", "500.00"),
            arena("SEMILLA", 1, "500.00", "550.00"),
            arena("BROTE", 2, "550.00", "600.00"),
            arena("AGUAYO", 3, "600.00", "650.00"),
            arena("FERIA", 4, "650.00", "700.00"),
            arena("MERCADO", 5, "700.00", "760.00"),
            arena("ALTIPLANO", 6, "760.00", "820.00"),
            arena("SALAR", 7, "820.00", "880.00"),
            arena("ILLIMANI", 8, "880.00", "940.00"),
            arena("CONDOR", 9, "940.00", "980.00"),
            arena("INTI", 10, "980.00", "1000.00"));

    private static Arena arena(String codigo, int orden, String desde, String hasta) {
        return new Arena(codigo, codigo, orden, new BigDecimal(desde), new BigDecimal(hasta), null);
    }

    @ParameterizedTest(name = "{0} -> {1} con progreso {2}, siguiente {3}")
    @CsvSource({
        // correcto
        "575.00, BROTE, 0.5000, AGUAYO",
        "812.50, ALTIPLANO, 0.8750, SALAR",
        // limites del plan: el borde inferior es incluido y el superior excluido
        "549.99, SEMILLA, 0.9998, BROTE",
        "550.00, BROTE, 0.0000, AGUAYO",
        "0.00, CAMPAMENTO, 0.0000, SEMILLA",
        "500.00, SEMILLA, 0.0000, BROTE",
        // la ultima arena incluye su hasta: el maximo esta en Inti con la barra llena
        "1000.00, INTI, 1.0000, ''",
        "980.00, INTI, 0.0000, ''",
        "999.99, INTI, 0.9995, ''",
        // HALF_EVEN, no HALF_UP: 0,01 / 40 = 0,00025 queda en 0,0002 (con HALF_UP seria 0,0003)
        "940.01, CONDOR, 0.0002, INTI",
        // 39,99 / 40 = 0,99975: el 7 es impar y sube a 0,9998
        "979.99, CONDOR, 0.9998, INTI",
    })
    @DisplayName("arena · cada puntaje cae en una sola arena con su progreso continuo")
    void ubica(String puntaje, String codigo, String progreso, String siguiente) {
        var u = ArenasDeReputacion.ubicar(V1, new BigDecimal(puntaje)).orElseThrow();

        assertThat(u.arena().codigo()).isEqualTo(codigo);
        // toPlainString y no compareTo: la escala de cuatro decimales es parte del contrato.
        assertThat(u.progreso().toPlainString()).isEqualTo(progreso);
        if (siguiente.isEmpty()) {
            assertThat(u.siguiente()).as("la ultima no tiene siguiente").isNull();
        } else {
            assertThat(u.siguiente().codigo()).isEqualTo(siguiente);
        }
    }

    @Test
    @DisplayName("arena · el orden de la lista no importa: manda la columna orden")
    void ordenDelCatalogo() {
        var alReves = V1.reversed();

        assertThat(ArenasDeReputacion.ubicar(alReves, new BigDecimal("549.99"))
                        .orElseThrow()
                        .siguiente()
                        .codigo())
                .isEqualTo("BROTE");
    }

    @ParameterizedTest(name = "{0} queda sin arena")
    @CsvSource({"-0.01", "1000.01"})
    @DisplayName("arena · inválido: un puntaje fuera de la escala no recibe una arena inventada")
    void fueraDeLaEscala(String puntaje) {
        assertThat(ArenasDeReputacion.ubicar(V1, new BigDecimal(puntaje))).isEmpty();
    }

    @Test
    @DisplayName("arena · inválido: un hueco en el catálogo deja al puntaje sin arena, no en la vecina")
    void hueco() {
        var conHueco = List.of(arena("SEMILLA", 1, "500.00", "550.00"), arena("AGUAYO", 3, "600.00", "650.00"));

        assertThat(ArenasDeReputacion.ubicar(conHueco, new BigDecimal("575.00")))
                .isEmpty();
    }

    @Test
    @DisplayName("arena · inválido: sin catálogo o sin puntaje no hay arena")
    void sinCatalogo() {
        assertThat(ArenasDeReputacion.ubicar(List.of(), new BigDecimal("500.00")))
                .isEmpty();
        assertThat(ArenasDeReputacion.ubicar(V1, null)).isEmpty();
    }

    @Test
    @DisplayName("desbloqueo · solo el VIGENTE limita; el PROPUESTO espera confirmación")
    void soloVigente() {
        var propuesto = new ArenasDeReputacion.Desbloqueo(6, new BigDecimal("200.00"), "BOB", "PROPUESTO");
        var vigente = new ArenasDeReputacion.Desbloqueo(6, new BigDecimal("200.00"), "BOB", "VIGENTE");

        assertThat(propuesto.vigente()).isFalse();
        assertThat(vigente.vigente()).isTrue();
    }
}
