package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.dominio.TipoDeAcuerdo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TipoDeAcuerdoTest {

    @Test
    @DisplayName(
            "Los valores del contrato se traducen a los tipos del modelo, y el traspaso se vota como admision/reemplazo")
    void traduceElContrato() {
        assertThat(TipoDeAcuerdo.deContrato("TRASPASO_CUPO")).isEqualTo(TipoDeAcuerdo.ADMISION_REEMPLAZO);
        assertThat(TipoDeAcuerdo.deContrato("CONDONACION")).isEqualTo(TipoDeAcuerdo.CONDONACION_MORA);
        assertThat(TipoDeAcuerdo.deContrato("EXPULSION")).isEqualTo(TipoDeAcuerdo.EXPULSION_PARTICIPANTE);
        assertThat(TipoDeAcuerdo.deContrato("PERMUTA")).isEqualTo(TipoDeAcuerdo.PERMUTA_TURNOS);
        assertThat(TipoDeAcuerdo.deContrato("DISOLUCION")).isEqualTo(TipoDeAcuerdo.DISOLUCION_ANTICIPADA);
    }

    @Test
    @DisplayName("Un valor que coincide con el modelo pasa tal cual")
    void aceptaLosDelModelo() {
        assertThat(TipoDeAcuerdo.deContrato("CAMBIO_REGLAMENTO")).isEqualTo(TipoDeAcuerdo.CAMBIO_REGLAMENTO);
        assertThat(TipoDeAcuerdo.deContrato("PERMUTA_TURNOS")).isEqualTo(TipoDeAcuerdo.PERMUTA_TURNOS);
    }

    @Test
    @DisplayName("REPETIR_SORTEO no existe en el modelo: no se inventa un tipo para aceptarlo")
    void repetirSorteoNoExiste() {
        assertThatThrownBy(() -> TipoDeAcuerdo.deContrato("REPETIR_SORTEO"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
