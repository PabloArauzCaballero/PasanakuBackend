package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** GU-17: el alta de un operador declara su fecha de nacimiento real; no hay valor por omision (AP-CU08-13). */
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
    void aceptaLaFechaRealYElDiaDeHoy() {
        assertThatCode(() -> {
                    GestionarUsuariosInternos.fechaDeNacimientoReal(LocalDate.of(1985, 6, 15), HOY);
                    GestionarUsuariosInternos.fechaDeNacimientoReal(HOY, HOY);
                })
                .doesNotThrowAnyException();
    }
}
