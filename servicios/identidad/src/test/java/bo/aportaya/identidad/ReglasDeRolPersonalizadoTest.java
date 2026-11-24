package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.dominio.ReglasDeRolPersonalizado;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Regla 65 / 80: los tres niveles del contrato de un rol personalizado: correcto, limite e invalido. */
class ReglasDeRolPersonalizadoTest {

    private static final Map<String, String> RIESGO =
            Map.of("TABLERO_VER", "BAJO", "ENTREGA_AUTORIZAR", "ALTO", "SEGURIDAD_ACCESO_RESTABLECER", "CRITICO");

    @DisplayName("correcto · un codigo con forma de catalogo se acepta")
    @ParameterizedTest
    @ValueSource(strings = {"ANALISTA_DE_ZONA", "AB1", "A_B_C_1_2_3"})
    void codigoCorrecto(String codigo) {
        assertThat(ReglasDeRolPersonalizado.codigoValido(codigo)).isTrue();
    }

    @DisplayName("limite · 30 caracteres entra, 31 no; 3 entra, 2 no")
    @Test
    void codigoEnElBorde() {
        assertThat(ReglasDeRolPersonalizado.codigoValido("A" + "B".repeat(29))).isTrue();
        assertThat(ReglasDeRolPersonalizado.codigoValido("A" + "B".repeat(30))).isFalse();
        assertThat(ReglasDeRolPersonalizado.codigoValido("AB")).isFalse();
    }

    @DisplayName("invalido · minusculas, espacios, nulo, servicio y participante se rechazan")
    @ParameterizedTest
    @ValueSource(strings = {"analista", "A B C", "1ABC", "SERVICIO_NUEVO", "PARTICIPANTE", "ORGANIZADOR", "ANUNCIANTE", ""})
    void codigoInvalido(String codigo) {
        assertThat(ReglasDeRolPersonalizado.codigoValido(codigo)).isFalse();
    }

    @Test
    @DisplayName("invalido · un codigo nulo se rechaza sin romper")
    void codigoNulo() {
        assertThat(ReglasDeRolPersonalizado.codigoValido(null)).isFalse();
    }

    @Test
    @DisplayName("correcto/limite · cuatro ojos solo si hay un permiso ALTO o CRITICO")
    void cuatroOjos() {
        assertThat(ReglasDeRolPersonalizado.exigeCuatroOjos(Set.of("TABLERO_VER"), RIESGO)).isFalse();
        assertThat(ReglasDeRolPersonalizado.exigeCuatroOjos(Set.of("TABLERO_VER", "ENTREGA_AUTORIZAR"), RIESGO))
                .isTrue();
        assertThat(ReglasDeRolPersonalizado.exigeCuatroOjos(Set.of("SEGURIDAD_ACCESO_RESTABLECER"), RIESGO))
                .isTrue();
        assertThat(ReglasDeRolPersonalizado.exigeCuatroOjos(Set.of(), RIESGO)).isFalse();
    }

    @Test
    @DisplayName("invalido · un permiso fuera del catalogo no cuenta como de bajo riesgo en silencio: no se presume")
    void permisoDesconocidoNoSumaRiesgo() {
        assertThat(ReglasDeRolPersonalizado.exigeCuatroOjos(Set.of("NO_EXISTE"), RIESGO)).isFalse();
    }

    @Test
    @DisplayName("invalido · el par incompatible de P-3 se detecta, y uno solo del par no")
    void segregacion() {
        assertThat(ReglasDeRolPersonalizado.conflicto(Set.of("ENTREGA_AUTORIZAR", "ENTREGA_EJECUTAR")))
                .isPresent();
        assertThat(ReglasDeRolPersonalizado.conflicto(Set.of("ENTREGA_AUTORIZAR"))).isEmpty();
    }

    @Test
    @DisplayName("limite · el motivo vacio, en blanco o de mas de 160 se rechaza; 160 entra")
    void motivo() {
        assertThat(ReglasDeRolPersonalizado.motivoValido("a".repeat(160))).isTrue();
        assertThat(ReglasDeRolPersonalizado.motivoValido("a".repeat(161))).isFalse();
        assertThat(ReglasDeRolPersonalizado.motivoValido("  ")).isFalse();
        assertThat(ReglasDeRolPersonalizado.motivoValido(null)).isFalse();
    }
}
