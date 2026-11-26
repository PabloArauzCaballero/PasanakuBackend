package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.web.generado.modelo.EntradaActualizarUsuarioInterno;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** GU-17 / A8: al operador «sin dato» se le completa la fecha de nacimiento real, una sola vez y con bitacora. */
class CU08CompletarFechaTest extends BaseDeGobiernoCU08 {

    private static UUID operadorSinDato() {
        UUID operador = operador();
        dsl.execute("UPDATE identidad.usuario SET fecha_nacimiento = NULL WHERE id = ?", operador);
        return operador;
    }

    private static EntradaActualizarUsuarioInterno completar(LocalDate fecha) {
        var entrada = new EntradaActualizarUsuarioInterno();
        entrada.setFechaNacimiento(fecha);
        entrada.setMotivo("Completa el dato que faltaba");
        entrada.setEvidenciaMfa(EVIDENCIA);
        return entrada;
    }

    @Test
    @DisplayName(
            "Dado un operador sin dato de fecha de nacimiento · Cuando se completa su fecha real con step-up · Entonces queda guardada · Y la bitácora lo registra")
    void criterioCompletaLaFecha() {
        enTransaccionQueSeDeshace(() -> {
            UUID administrador = operador();
            UUID operador = operadorSinDato();
            var antes = gestion.actualizar(operador, completar(LocalDate.of(1985, 6, 15)), como(administrador));

            assertThat(antes.getFechaNacimientoPendiente()).isFalse();
            assertThat(columna("SELECT fecha_nacimiento::text FROM identidad.usuario WHERE id = ?", operador))
                    .isEqualTo("1985-06-15");
            assertThat(eventos("identidad.usuario_interno_fecha_completada", operador))
                    .isEqualTo(1);
        });
    }

    @Test
    @DisplayName(
            "Dado un operador que ya tiene su fecha de nacimiento · Cuando se intenta completarla otra vez · Entonces se rechaza con AP-CU08-15")
    void criterioNoReescribe() {
        enTransaccionQueSeDeshace(() -> {
            UUID administrador = operador();
            UUID operador = operadorSinDato();
            gestion.actualizar(operador, completar(LocalDate.of(1985, 6, 15)), como(administrador));

            assertThatThrownBy(() ->
                            gestion.actualizar(operador, completar(LocalDate.of(1970, 1, 1)), como(administrador)))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .hasMessageContaining("ya tiene su fecha");
            assertThat(columna("SELECT fecha_nacimiento::text FROM identidad.usuario WHERE id = ?", operador))
                    .isEqualTo("1985-06-15");
        });
    }
}
