package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.cumplimiento.aplicacion.CU56EjecutarPruebaDeContinuidad.EntradaPrueba;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-56 por HTTP (carril A · H9): registrar una prueba de continuidad sin duplicarla en el reintento. */
class CU56AccionesTest extends EscenarioDeAcciones {

    private UUID planId;
    private UUID ejecutor;
    private LocalDate hoy;

    @BeforeEach
    void escenario() {
        hoy = LocalDate.now(ZoneOffset.UTC);
        planId = gobiernoFixtura.planDeContinuidad(
                "ACC-" + UUID.randomUUID().toString().substring(0, 8), 60, 15, 6, hoy.plusMonths(3));
        ejecutor = fixtura.usuario();
    }

    private EntradaPrueba prueba(UUID plan, int rto) {
        return new EntradaPrueba(
                plan, "PARCIAL", hoy, ejecutor, null, rto, 10, "https://evidencia.bo/1", false, "0.00");
    }

    @Test
    @DisplayName(
            "Dada una prueba que no alcanzó el RTO ya registrada · Cuando Riesgos la registra otra vez desde el backoffice · Entonces no se duplica ni se abre un segundo plan de acción")
    void sinDuplicar() {
        var riesgos = operador(fixtura.usuario());
        int planesAntes = contar("SELECT count(*)::int FROM cumplimiento.plan_accion_riesgo");

        var primera = transaccion.execute(t -> acciones().registrarPrueba(prueba(planId, 95), riesgos));
        var segunda = transaccion.execute(t -> acciones().registrarPrueba(prueba(planId, 95), riesgos));

        assertThat(primera.esNuevo()).isTrue();
        assertThat(primera.salida().planAccionId()).isNotNull();
        assertThat(segunda.esNuevo()).isFalse();
        assertThat(segunda.salida().pruebaId()).isEqualTo(primera.salida().pruebaId());
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.prueba_continuidad WHERE plan_continuidad_id = ?",
                        planId))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*)::int FROM cumplimiento.plan_accion_riesgo"))
                .isEqualTo(planesAntes + 1);
    }

    @Test
    @DisplayName("rechaza con AP-CU56-03 una prueba de un plan que no existe")
    void rechazaPlanInexistente() {
        var riesgos = operador(fixtura.usuario());
        rechazaCon(
                "AP-CU56-03",
                () -> transaccion.execute(t -> acciones().registrarPrueba(prueba(UUID.randomUUID(), 30), riesgos)));
    }
}
