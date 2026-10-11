package bo.aportaya.garantia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.Resultado;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** R-GAR-13 y AP-CU23-10: la contingencia de caja se escala una vez y frena las reservas nuevas, no las ya hechas. */
class CU23ContingenciaRechazosTest extends BaseDeRespaldo {

    private static final String PLAN_VIVO =
            """
            INSERT INTO garantia.plan_contingencia
                (id, grupo_id, disparador, tipo, descripcion, impacto_estimado, requiere_acuerdo, estado, propuesto_en)
            VALUES (gen_random_uuid(), ?, 'FONDO_AGOTADO', 'EXTENSION_DE_PLAZO', 'copia', 1.00, false, ?, now())
            """;

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    @Test
    @DisplayName(
            "rechaza por R-GAR-13: un segundo plan vivo del mismo grupo y disparador, aunque uno sea PROPUESTO y el otro APROBADO")
    void rechazaSegundoPlanVivo() {
        fixturaDeRespaldo.capacidad("50000.00");
        var c = caso();
        reservar(c, "800.00");
        assertThat(cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno())).resultado())
                .isEqualTo(Resultado.INSUFICIENTE);

        assertThat(rechazaLaBase(PLAN_VIVO, c.escenario().grupoId(), "APROBADO"))
                .contains("uq_plan_contingencia_vivo");
        assertThat(rechazaLaBase(PLAN_VIVO, c.escenario().grupoId(), "PROPUESTO"))
                .contains("uq_plan_contingencia_vivo");
        // Uno ya EJECUTADO no cuenta como vivo: la base lo admite (la historia no se borra).
        assertThat(rechazaLaBase(PLAN_VIVO, c.escenario().grupoId(), "EJECUTADO"))
                .isEmpty();
    }

    @Test
    @DisplayName(
            "rechaza por AP-CU23-10 una reserva nueva mientras haya una falta de caja sin resolver, y no escribe nada")
    void rechazaReservaNuevaConContingencia() {
        fixturaDeRespaldo.capacidad("50000.00");
        var enFalta = caso();
        reservar(enFalta, "800.00");
        cubrir(enFalta, pedidoDelEjemplo(enFalta, "cobertura-" + enFalta.turno()));
        var nuevo = caso();
        long reservasAntes = contar("SELECT count(*)::int FROM garantia.reserva_respaldo");
        var comprometidoAntes = fixturaDeRespaldo.comprometido();

        assertThatThrownBy(() -> reservar(nuevo, "100.00"))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU23-10"));

        assertThat(contar("SELECT count(*)::int FROM garantia.reserva_respaldo"))
                .isEqualTo((int) reservasAntes);
        assertThat(fixturaDeRespaldo.comprometido()).isEqualByComparingTo(comprometidoAntes);
    }
}
