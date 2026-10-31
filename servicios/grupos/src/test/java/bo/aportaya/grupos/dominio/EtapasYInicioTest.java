package bo.aportaya.grupos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Las reglas puras de las etapas y del inicio al 75 % (2026-10-30). Los tramos son los PROPUESTOS en el plan del
 * trabajo (catalogo interno, sin fuente externa, a confirmar con Pablo); aca se usan los primeros como datos de prueba.
 */
class EtapasYInicioTest {

    private static final Etapas.Etapa RECUPERACION = etapa("RECUPERACION", 0, "0.00", "500.00", null);
    private static final Etapas.Etapa INICIAL = etapa(
            "INICIAL", 1, "500.00", "550.00", new Etapas.Desbloqueo(8, new BigDecimal("300.00"), "BOB", "VIGENTE"));
    private static final Etapas.Etapa EN_MARCHA = etapa(
            "EN_MARCHA",
            2,
            "550.00",
            "600.00",
            new Etapas.Desbloqueo(10, new BigDecimal("500.00"), "BOB", "PROPUESTO"));
    private static final Etapas.Etapa PILAR = etapa("PILAR", 3, "980.00", "1000.00", null);
    private static final List<Etapas.Etapa> CATALOGO = List.of(RECUPERACION, INICIAL, EN_MARCHA, PILAR);

    private static Etapas.Etapa etapa(String codigo, int orden, String desde, String hasta, Etapas.Desbloqueo d) {
        return new Etapas.Etapa(codigo, orden, new BigDecimal(desde), new BigDecimal(hasta), d);
    }

    private static Etapas.DeLaPersona con(String puntaje) {
        BigDecimal p = new BigDecimal(puntaje);
        return new Etapas.DeLaPersona(p, Etapas.contiene(CATALOGO, p).orElseThrow(), CATALOGO);
    }

    @ParameterizedTest(name = "puntaje {0} → {1}")
    @CsvSource({
        "0.00,RECUPERACION",
        "499.99,RECUPERACION",
        "500.00,INICIAL",
        "549.99,INICIAL",
        "550.00,EN_MARCHA",
        "1000.00,PILAR"
    })
    @DisplayName("contiene: desde incluido, hasta excluido, y la ultima etapa incluye su hasta")
    void contiene(String puntaje, String etapa) {
        assertThat(Etapas.contiene(CATALOGO, new BigDecimal(puntaje)))
                .map(Etapas.Etapa::codigo)
                .contains(etapa);
    }

    @Test
    @DisplayName("contiene: fuera del catalogo (entre tramos, negativo o por encima del maximo) no hay etapa")
    void fueraDelCatalogo() {
        assertThat(Etapas.contiene(CATALOGO, new BigDecimal("700.00"))).isEmpty();
        assertThat(Etapas.contiene(CATALOGO, new BigDecimal("-0.01"))).isEmpty();
        assertThat(Etapas.contiene(CATALOGO, new BigDecimal("1000.01"))).isEmpty();
        assertThat(Etapas.contiene(List.of(), BigDecimal.ONE)).isEmpty();
    }

    @Test
    @DisplayName("alcanza: el piso de la etapa minima es incluido; un grupo sin minimo no pide nada")
    void alcanza() {
        BigDecimal inicial = INICIAL.desde();
        assertThat(Etapas.alcanza(con("499.99"), inicial)).isFalse();
        assertThat(Etapas.alcanza(con("500.00"), inicial)).isTrue();
        assertThat(Etapas.alcanza(con("0.00"), BigDecimal.ZERO)).isTrue();
        assertThat(Etapas.alcanza(con("0.00"), null)).isTrue();
    }

    @Test
    @DisplayName("excedido: solo limita el desbloqueo VIGENTE, por cupos, por aporte o por otra moneda")
    void excedido() {
        assertThat(Etapas.excedido(INICIAL, 8, new BigDecimal("300.00"), "BOB"))
                .as("en el borde")
                .isEmpty();
        assertThat(Etapas.excedido(INICIAL, 9, new BigDecimal("300.00"), "BOB"))
                .as("un cupo de mas")
                .isPresent();
        assertThat(Etapas.excedido(INICIAL, 8, new BigDecimal("300.01"), "BOB"))
                .as("un centavo de mas")
                .isPresent();
        assertThat(Etapas.excedido(INICIAL, 3, new BigDecimal("1.00"), "USD"))
                .as("sin tipo de cambio no se compara: se deniega")
                .isPresent();
        assertThat(Etapas.excedido(EN_MARCHA, 40, new BigDecimal("99999.00"), "BOB"))
                .as("PROPUESTO no limita")
                .isEmpty();
        assertThat(Etapas.excedido(RECUPERACION, 40, new BigDecimal("99999.00"), "BOB"))
                .as("sin desbloqueo")
                .isEmpty();
    }

    @Test
    @DisplayName("codigoDeLaMinima: el piso guardado se nombra con su etapa; 0 es «sin minima»")
    void codigoDeLaMinima() {
        assertThat(Etapas.codigoDeLaMinima(CATALOGO, new BigDecimal("550.00"))).contains("EN_MARCHA");
        assertThat(Etapas.codigoDeLaMinima(CATALOGO, BigDecimal.ZERO)).isEmpty();
        assertThat(Etapas.codigoDeLaMinima(CATALOGO, null)).isEmpty();
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
