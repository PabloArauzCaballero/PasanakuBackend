package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.trabajos.RecargosDeMoraDiarios;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-21 · El disparo diario de los recargos. Corre con el contexto de SISTEMA, no con el de
 * un participante: es lo que hace de verdad el reloj, y es donde una politica de fila mal
 * escrita dejaria el trabajo sin ver ninguna vencida.
 */
class CU21RecargosDiariosTest extends BaseDeAportes {

    private final RecargosDeMoraDiarios trabajo = new RecargosDeMoraDiarios(cobroCU);

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private int recargosDe(UUID obligacionId) {
        return contar(
                "SELECT count(*)::int FROM aportes.obligacion_aporte WHERE tipo = 'RECARGO_MORA' AND obligacion_origen_id = ?",
                obligacionId);
    }

    @Test
    @DisplayName("Dado un aporte vencido con política de mora · Cuando corre el trabajo diario con contexto de sistema"
            + " · Entonces se crea un RECARGO_MORA")
    void correcto() {
        var obligacion = fixtura.obligacion(fixtura.usuario(), "500.00", -10);
        fixtura.politicaDeMora(obligacion.grupoId(), 0, "PORCENTUAL", "0.05", "100.00");

        transaccion.executeWithoutResult(t -> trabajo.correr());

        assertThat(recargosDe(obligacion.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("límite: el trabajo corre dos veces el mismo día · Entonces sigue habiendo un solo recargo por atraso")
    void dosCorridasElMismoDia() {
        var obligacion = fixtura.obligacion(fixtura.usuario(), "500.00", -10);
        fixtura.politicaDeMora(obligacion.grupoId(), 0, "PORCENTUAL", "0.05", "100.00");

        transaccion.executeWithoutResult(t -> trabajo.correr());
        transaccion.executeWithoutResult(t -> trabajo.correr());

        assertThat(recargosDe(obligacion.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("inválido: un grupo sin política de mora · Entonces el trabajo no inventa un recargo y no falla")
    void sinPolitica() {
        var obligacion = fixtura.obligacion(fixtura.usuario(), "500.00", -10);

        transaccion.executeWithoutResult(t -> trabajo.correr());

        assertThat(recargosDe(obligacion.id())).isZero();
    }
}
