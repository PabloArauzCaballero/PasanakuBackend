package bo.aportaya.auditoria.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FamiliasVisiblesTest {

    @Test
    @DisplayName("correcto: cada permiso de tablero habilita solo sus familias; AUDITORIA_LEER, todas")
    void porPermiso() {
        assertThat(FamiliasVisibles.para("TESORERIA", Set.of("TABLERO_FINANCIERO_VER")))
                .containsExactlyInAnyOrder("FINANZAS", "RIESGO");
        assertThat(FamiliasVisibles.para("COMERCIAL", Set.of("TABLERO_COMERCIAL_VER")))
                .containsExactly("NEGOCIO");
        assertThat(FamiliasVisibles.para("ADMIN_PLATAFORMA", Set.of("TABLERO_SISTEMAS_VER", "CATALOGO_EDITAR")))
                .containsExactly("OPERACION");
        assertThat(FamiliasVisibles.para("AUDITOR_INTERNO", Set.of("AUDITORIA_LEER")))
                .isEqualTo(FamiliasVisibles.TODAS);
    }

    @Test
    @DisplayName("límite: varios permisos suman familias; el rol también cuenta como permiso")
    void union() {
        assertThat(FamiliasVisibles.para("X", Set.of("TABLERO_FINANCIERO_VER", "TABLERO_COMERCIAL_VER")))
                .containsExactlyInAnyOrder("FINANZAS", "RIESGO", "NEGOCIO");
        assertThat(FamiliasVisibles.para("TABLERO_SISTEMAS_VER", Set.of())).containsExactly("OPERACION");
    }

    @Test
    @DisplayName("inválido: sin ningún permiso de tablero no se ve ninguna familia")
    void ninguna() {
        assertThat(FamiliasVisibles.para("PARTICIPANTE", Set.of("BILLETERA_VER")))
                .isEmpty();
    }
}
