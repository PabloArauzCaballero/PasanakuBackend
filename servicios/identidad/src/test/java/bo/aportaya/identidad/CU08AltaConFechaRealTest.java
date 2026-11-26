package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** GU-17: el alta de un operador declara su fecha de nacimiento real, sin valor por omision (AP-CU08-13) y con 18 anos o mas (AP-CU08-14). */
class CU08AltaConFechaRealTest {
    private static final LocalDate HOY = LocalDate.of(2026, 11, 26);

    @Test
    @DisplayName(
            "Dada un alta de operador con una fecha de nacimiento futura · Cuando se la intenta crear · Entonces se rechaza con AP-CU08-13")
    void rechazaLaFechaFutura() {
        assertThatThrownBy(() -> GestionarUsuariosInternos.fechaDeNacimientoReal(HOY.plusDays(1), HOY))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("fecha de nacimiento");
    }

    @Test
    @DisplayName("rechaza por AP-CU08-13 el alta sin fecha de nacimiento: no hay valor por omision")
    void rechazaLaFechaAusente() {
        assertThatThrownBy(() -> GestionarUsuariosInternos.fechaDeNacimientoReal(null, HOY))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName(
            "Dada un alta de operador con una fecha de nacimiento real · Cuando se la valida · Entonces se acepta sin cambiarla")
    void aceptaLaFechaReal() {
        assertThatCode(() -> GestionarUsuariosInternos.fechaDeNacimientoReal(LocalDate.of(1985, 6, 15), HOY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Dada un alta de operador que cumple 18 años hoy · Cuando se la valida · Entonces se acepta")
    void aceptaQuienCumpleDieciochoHoy() {
        assertThatCode(() -> GestionarUsuariosInternos.fechaDeNacimientoReal(HOY.minusYears(18), HOY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName(
            "Dada un alta de operador con 17 años y 364 días · Cuando se la valida · Entonces se rechaza con AP-CU08-14")
    void rechazaAQuienCumpleDieciochoManiana() {
        assertThatThrownBy(() -> GestionarUsuariosInternos.fechaDeNacimientoReal(
                        HOY.minusYears(18).plusDays(1), HOY))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("mayor de 18");
    }

    @Test
    @DisplayName("rechaza por AP-CU08-14 a quien nacio un 29 de febrero hasta el 1 de marzo de su mayoria de edad")
    void rechazaElBordeDelAnioBisiesto() {
        LocalDate nacio = LocalDate.of(2008, 2, 29);
        assertThatThrownBy(() -> GestionarUsuariosInternos.fechaDeNacimientoReal(nacio, LocalDate.of(2026, 2, 28)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThatCode(() -> GestionarUsuariosInternos.fechaDeNacimientoReal(nacio, LocalDate.of(2026, 3, 1)))
                .doesNotThrowAnyException();
    }
}
