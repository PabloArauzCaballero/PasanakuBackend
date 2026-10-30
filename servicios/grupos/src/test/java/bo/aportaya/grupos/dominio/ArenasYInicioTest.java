package bo.aportaya.grupos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Las reglas puras de las arenas y del inicio al 75 % (2026-10-30). Los tramos son los PROPUESTOS en el plan del
 * trabajo (catalogo interno, sin fuente externa, a confirmar con Pablo); aca se usan los primeros como datos de prueba.
 */
class ArenasYInicioTest {

    private static final Arenas.Arena CAMPAMENTO = arena("CAMPAMENTO", 0, "0.00", "500.00", null);
    private static final Arenas.Arena SEMILLA = arena(
            "SEMILLA", 1, "500.00", "550.00", new Arenas.Desbloqueo(8, new BigDecimal("300.00"), "BOB", "VIGENTE"));
    private static final Arenas.Arena BROTE = arena(
            "BROTE", 2, "550.00", "600.00", new Arenas.Desbloqueo(10, new BigDecimal("500.00"), "BOB", "PROPUESTO"));
    private static final Arenas.Arena INTI = arena("INTI", 3, "980.00", "1000.00", null);
    private static final List<Arenas.Arena> CATALOGO = List.of(CAMPAMENTO, SEMILLA, BROTE, INTI);

    private static Arenas.Arena arena(String codigo, int orden, String desde, String hasta, Arenas.Desbloqueo d) {
        return new Arenas.Arena(codigo, orden, new BigDecimal(desde), new BigDecimal(hasta), d);
    }

    private static Arenas.DeLaPersona con(String puntaje) {
        BigDecimal p = new BigDecimal(puntaje);
        return new Arenas.DeLaPersona(p, Arenas.contiene(CATALOGO, p).orElseThrow(), CATALOGO);
    }

    @ParameterizedTest(name = "puntaje {0} → {1}")
    @CsvSource({
        "0.00,CAMPAMENTO",
        "499.99,CAMPAMENTO",
        "500.00,SEMILLA",
        "549.99,SEMILLA",
        "550.00,BROTE",
        "1000.00,INTI"
    })
    @DisplayName("contiene: desde incluido, hasta excluido, y la ultima arena incluye su hasta")
    void contiene(String puntaje, String arena) {
        assertThat(Arenas.contiene(CATALOGO, new BigDecimal(puntaje)))
                .map(Arenas.Arena::codigo)
                .contains(arena);
    }

    @Test
    @DisplayName("contiene: fuera del catalogo (entre tramos, negativo o por encima del maximo) no hay arena")
    void fueraDelCatalogo() {
        assertThat(Arenas.contiene(CATALOGO, new BigDecimal("700.00"))).isEmpty();
        assertThat(Arenas.contiene(CATALOGO, new BigDecimal("-0.01"))).isEmpty();
        assertThat(Arenas.contiene(CATALOGO, new BigDecimal("1000.01"))).isEmpty();
        assertThat(Arenas.contiene(List.of(), BigDecimal.ONE)).isEmpty();
    }

    @Test
    @DisplayName("alcanza: el piso de la arena minima es incluido; un grupo sin minimo no pide nada")
    void alcanza() {
        BigDecimal semilla = SEMILLA.desde();
        assertThat(Arenas.alcanza(con("499.99"), semilla)).isFalse();
        assertThat(Arenas.alcanza(con("500.00"), semilla)).isTrue();
        assertThat(Arenas.alcanza(con("0.00"), BigDecimal.ZERO)).isTrue();
        assertThat(Arenas.alcanza(con("0.00"), null)).isTrue();
    }

    @Test
    @DisplayName("excedido: solo limita el desbloqueo VIGENTE, por cupos, por aporte o por otra moneda")
    void excedido() {
        assertThat(Arenas.excedido(SEMILLA, 8, new BigDecimal("300.00"), "BOB"))
                .as("en el borde")
                .isEmpty();
        assertThat(Arenas.excedido(SEMILLA, 9, new BigDecimal("300.00"), "BOB"))
                .as("un cupo de mas")
                .isPresent();
        assertThat(Arenas.excedido(SEMILLA, 8, new BigDecimal("300.01"), "BOB"))
                .as("un centavo de mas")
                .isPresent();
        assertThat(Arenas.excedido(SEMILLA, 3, new BigDecimal("1.00"), "USD"))
                .as("sin tipo de cambio no se compara: se deniega")
                .isPresent();
        assertThat(Arenas.excedido(BROTE, 40, new BigDecimal("99999.00"), "BOB"))
                .as("PROPUESTO no limita")
                .isEmpty();
        assertThat(Arenas.excedido(CAMPAMENTO, 40, new BigDecimal("99999.00"), "BOB"))
                .as("sin desbloqueo")
                .isEmpty();
    }

    @Test
    @DisplayName("codigoDeLaMinima: el piso guardado se nombra con su arena; 0 es «sin minima»")
    void codigoDeLaMinima() {
        assertThat(Arenas.codigoDeLaMinima(CATALOGO, new BigDecimal("550.00"))).contains("BROTE");
        assertThat(Arenas.codigoDeLaMinima(CATALOGO, BigDecimal.ZERO)).isEmpty();
        assertThat(Arenas.codigoDeLaMinima(CATALOGO, null)).isEmpty();
    }

    @ParameterizedTest(name = "{0} cupos al {1} % → {2}")
    @CsvSource({"10,75,8", "4,75,3", "3,75,3", "7,75,6", "8,75,6", "40,75,30", "12,75,9"})
    @DisplayName("minimoParaIniciar: ceil(porcentaje × cupos / 100)")
    void minimoParaIniciar(int cupos, int porcentaje, int minimo) {
        assertThat(InicioDelGrupo.minimoParaIniciar(cupos, porcentaje)).isEqualTo(minimo);
    }

    @Test
    @DisplayName("sePuedeIniciar: abierto, alcanza el minimo y nadie aceptado sin firmar")
    void sePuedeIniciar() {
        assertThat(InicioDelGrupo.sePuedeIniciar("ABIERTO_A_INSCRIPCION", 8, 0, 10, 75))
                .isTrue();
        assertThat(InicioDelGrupo.sePuedeIniciar("ABIERTO_A_INSCRIPCION", 7, 0, 10, 75))
                .isFalse();
        assertThat(InicioDelGrupo.sePuedeIniciar("ABIERTO_A_INSCRIPCION", 8, 1, 10, 75))
                .isFalse();
        assertThat(InicioDelGrupo.sePuedeIniciar("CONFORMADO", 10, 0, 10, 75)).isFalse();
    }
}
