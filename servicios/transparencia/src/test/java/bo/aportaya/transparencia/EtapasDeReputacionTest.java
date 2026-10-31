package bo.aportaya.transparencia;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.transparencia.dominio.EtapasDeReputacion;
import bo.aportaya.transparencia.dominio.EtapasDeReputacion.Etapa;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Donde cae un puntaje y cuanto avanzo, sin base: la funcion pura (etapas de reputacion, 2026-10-30).
 *
 * <p>El catalogo de estas pruebas copia los tramos de la semilla v1, que es un catalogo interno
 * PROPUESTO (confirmar con Pablo): si cambia la semilla, la prueba que lo lee de la base es
 * {@code EtapaRepositorioTest}. Aca se fija la regla: {@code [desde, hasta)}, la ultima incluye
 * su {@code hasta}, progreso a cuatro decimales HALF_EVEN.
 */
class EtapasDeReputacionTest {

    private static final List<Etapa> V1 = List.of(
            etapa("RECUPERACION", 0, "0.00", "500.00"),
            etapa("INICIAL", 1, "500.00", "550.00"),
            etapa("EN_MARCHA", 2, "550.00", "600.00"),
            etapa("CONSTANTE", 3, "600.00", "650.00"),
            etapa("PUNTUAL", 4, "650.00", "700.00"),
            etapa("COMPROMETIDO", 5, "700.00", "760.00"),
            etapa("SOLIDO", 6, "760.00", "820.00"),
            etapa("DESTACADO", 7, "820.00", "880.00"),
            etapa("EXPERTO", 8, "880.00", "940.00"),
            etapa("EJEMPLAR", 9, "940.00", "980.00"),
            etapa("PILAR", 10, "980.00", "1000.00"));

    private static Etapa etapa(String codigo, int orden, String desde, String hasta) {
        return new Etapa(codigo, codigo, orden, new BigDecimal(desde), new BigDecimal(hasta), null);
    }

    @ParameterizedTest(name = "{0} -> {1} con progreso {2}, siguiente {3}")
    @CsvSource({
        // correcto
        "575.00, EN_MARCHA, 0.5000, CONSTANTE",
        "812.50, SOLIDO, 0.8750, DESTACADO",
        // limites del plan: el borde inferior es incluido y el superior excluido
        "549.99, INICIAL, 0.9998, EN_MARCHA",
        "550.00, EN_MARCHA, 0.0000, CONSTANTE",
        "0.00, RECUPERACION, 0.0000, INICIAL",
        "500.00, INICIAL, 0.0000, EN_MARCHA",
        // la ultima etapa incluye su hasta: el maximo esta en Pilar con la barra llena
        "1000.00, PILAR, 1.0000, ''",
        "980.00, PILAR, 0.0000, ''",
        "999.99, PILAR, 0.9995, ''",
        // HALF_EVEN, no HALF_UP: 0,01 / 40 = 0,00025 queda en 0,0002 (con HALF_UP seria 0,0003)
        "940.01, EJEMPLAR, 0.0002, PILAR",
        // 39,99 / 40 = 0,99975: el 7 es impar y sube a 0,9998
        "979.99, EJEMPLAR, 0.9998, PILAR",
    })
    @DisplayName("etapa · cada puntaje cae en una sola etapa con su progreso continuo")
    void ubica(String puntaje, String codigo, String progreso, String siguiente) {
        var u = EtapasDeReputacion.ubicar(V1, new BigDecimal(puntaje)).orElseThrow();

        assertThat(u.etapa().codigo()).isEqualTo(codigo);
        // toPlainString y no compareTo: la escala de cuatro decimales es parte del contrato.
        assertThat(u.progreso().toPlainString()).isEqualTo(progreso);
        if (siguiente.isEmpty()) {
            assertThat(u.siguiente()).as("la ultima no tiene siguiente").isNull();
        } else {
            assertThat(u.siguiente().codigo()).isEqualTo(siguiente);
        }
    }

    @Test
    @DisplayName("etapa · el orden de la lista no importa: manda la columna orden")
    void ordenDelCatalogo() {
        var alReves = V1.reversed();

        assertThat(EtapasDeReputacion.ubicar(alReves, new BigDecimal("549.99"))
                        .orElseThrow()
                        .siguiente()
                        .codigo())
                .isEqualTo("EN_MARCHA");
    }

    @ParameterizedTest(name = "{0} queda sin etapa")
    @CsvSource({"-0.01", "1000.01"})
    @DisplayName("etapa · inválido: un puntaje fuera de la escala no recibe una etapa inventada")
    void fueraDeLaEscala(String puntaje) {
        assertThat(EtapasDeReputacion.ubicar(V1, new BigDecimal(puntaje))).isEmpty();
    }

    @Test
    @DisplayName("etapa · inválido: un hueco en el catálogo deja al puntaje sin etapa, no en la vecina")
    void hueco() {
        var conHueco = List.of(etapa("INICIAL", 1, "500.00", "550.00"), etapa("CONSTANTE", 3, "600.00", "650.00"));

        assertThat(EtapasDeReputacion.ubicar(conHueco, new BigDecimal("575.00")))
                .isEmpty();
    }

    @Test
    @DisplayName("etapa · inválido: sin catálogo o sin puntaje no hay etapa")
    void sinCatalogo() {
        assertThat(EtapasDeReputacion.ubicar(List.of(), new BigDecimal("500.00")))
                .isEmpty();
        assertThat(EtapasDeReputacion.ubicar(V1, null)).isEmpty();
    }

    @Test
    @DisplayName("desbloqueo · solo el VIGENTE limita; el PROPUESTO espera confirmación")
    void soloVigente() {
        var propuesto = new EtapasDeReputacion.Desbloqueo(6, new BigDecimal("200.00"), "BOB", "PROPUESTO");
        var vigente = new EtapasDeReputacion.Desbloqueo(6, new BigDecimal("200.00"), "BOB", "VIGENTE");

        assertThat(propuesto.vigente()).isFalse();
        assertThat(vigente.vigente()).isTrue();
    }
}
