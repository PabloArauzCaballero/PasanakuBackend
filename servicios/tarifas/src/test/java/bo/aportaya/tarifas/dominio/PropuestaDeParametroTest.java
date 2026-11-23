package bo.aportaya.tarifas.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.tarifas.dominio.PropuestaDeParametro.Catalogo;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-132 · la coherencia de una propuesta con su parámetro, en sus tres niveles. Ningún valor es real. */
class PropuestaDeParametroTest {

    private static final Catalogo PORCENTAJE_POR_NIVEL =
            new Catalogo("APORTE_FONDO_GRUPO", Optional.of("PORCENTAJE"), true);
    private static final Catalogo DIAS = new Catalogo("DIAS_DE_GRACIA", Optional.of("DIAS"), false);
    private static final Catalogo MONTO = new Catalogo("UN_MONTO", Optional.of("MONTO"), false);
    private static final Catalogo SIN_UNIDAD = new Catalogo("SIN_DECIDIR", Optional.empty(), false);

    @Test
    @DisplayName("Correcto · un porcentaje por nivel con su nivel y sin moneda encaja")
    void porcentajePorNivel() {
        assertThat(PropuestaDeParametro.incoherencia(
                        PORCENTAJE_POR_NIVEL, Optional.of("NIVEL_A"), new BigDecimal("1.5"), Optional.empty()))
                .isEmpty();
    }

    @Test
    @DisplayName("Correcto · un monto lleva su moneda")
    void montoConMoneda() {
        assertThat(PropuestaDeParametro.incoherencia(MONTO, Optional.empty(), BigDecimal.TEN, Optional.of("BOB")))
                .isEmpty();
    }

    @Test
    @DisplayName("Límite · cero es un valor válido (no negativo)")
    void ceroEncaja() {
        assertThat(PropuestaDeParametro.incoherencia(DIAS, Optional.empty(), BigDecimal.ZERO, Optional.empty()))
                .isEmpty();
    }

    @Test
    @DisplayName("Inválido · un valor negativo no encaja")
    void negativo() {
        assertThat(PropuestaDeParametro.incoherencia(DIAS, Optional.empty(), new BigDecimal("-0.01"), Optional.empty()))
                .hasValueSatisfying(m -> assertThat(m).contains("negativo"));
    }

    @Test
    @DisplayName("Inválido · por nivel sin nivel, y sin nivel con nivel")
    void nivelSoloSiEsPorNivel() {
        assertThat(PropuestaDeParametro.incoherencia(
                        PORCENTAJE_POR_NIVEL, Optional.empty(), BigDecimal.ONE, Optional.empty()))
                .isPresent();
        assertThat(PropuestaDeParametro.incoherencia(DIAS, Optional.of("NIVEL_A"), BigDecimal.ONE, Optional.empty()))
                .isPresent();
    }

    @Test
    @DisplayName("Inválido · un monto sin moneda, y una moneda en lo que no es monto")
    void monedaSoloEnMonto() {
        assertThat(PropuestaDeParametro.incoherencia(MONTO, Optional.empty(), BigDecimal.ONE, Optional.empty()))
                .isPresent();
        assertThat(PropuestaDeParametro.incoherencia(DIAS, Optional.empty(), BigDecimal.ONE, Optional.of("BOB")))
                .isPresent();
    }

    @Test
    @DisplayName("Sin unidad decidida no se aprueba; con unidad sí")
    void aprobable() {
        assertThat(PropuestaDeParametro.aprobable(SIN_UNIDAD)).isFalse();
        assertThat(PropuestaDeParametro.aprobable(DIAS)).isTrue();
    }
}
