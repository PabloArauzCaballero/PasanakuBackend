package bo.aportaya.garantia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-23 · las pruebas de RECHAZO de las restricciones del respaldo empresarial (R-GAR-08 a
 * R-GAR-12), una por restriccion citada.
 *
 * <p>La aplicacion ya valida estos casos; lo que se prueba aca es que la BASE los rechaza
 * aunque alguien llegue por fuera del caso de uso. El escenario completo de cada camino vive
 * en {@code CU131RespaldoTest}, {@code CU131CoberturaTest} y {@code CU131FaltanteDelCorteTest}.
 */
class CU23RespaldoRechazosTest extends BaseDeRespaldo {

    /** Una cobertura nueva con las mismas cifras y el turno que se le pase, para probar a mano lo que la base no deja repetir. */
    private static final String COPIA_DE_COBERTURA =
            """
            INSERT INTO garantia.cobertura_respaldo
                (reserva_respaldo_id, grupo_id, periodo_id, turno_id, moneda, monto_pozo, monto_confirmado,
                 monto_cubierto_mutual, monto_faltante, estado, corte_en, clave_idempotencia, solicitada_por,
                 responsable_id)
            SELECT reserva_respaldo_id, grupo_id, periodo_id, CAST(? AS uuid), moneda, monto_pozo, monto_confirmado,
                   monto_cubierto_mutual, monto_faltante, 'APLICADA', corte_en, 'directa-' || gen_random_uuid(),
                   solicitada_por, responsable_id
              FROM garantia.cobertura_respaldo WHERE id = ?
            """;

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    @Test
    @DisplayName("rechaza por R-GAR-08: comprometer mas que el tope de la capacidad")
    void rechazaRGAR08() {
        fixturaDeRespaldo.capacidad("100.00");

        String error = rechazaLaBase(
                "UPDATE garantia.capacidad_respaldo SET monto_comprometido = monto_tope + 0.01 WHERE ambito = 'GENERAL' AND moneda = 'BOB'");

        assertThat(error).contains("ck_capacidad_respaldo_comprometido");
    }

    @Test
    @DisplayName("rechaza por R-GAR-09: usar o recuperar de una reserva mas de lo reservado o aplicado")
    void rechazaRGAR09() {
        fixturaDeRespaldo.capacidad("20000.00");
        var reserva = reservar(caso(), "8000.00");

        assertThat(rechazaLaBase(
                        "UPDATE garantia.reserva_respaldo SET monto_aplicado = monto_reservado + 0.01 WHERE id = ?",
                        reserva.reservaId()))
                .contains("ck_reserva_respaldo_uso");
        assertThat(rechazaLaBase(
                        "UPDATE garantia.reserva_respaldo SET monto_recuperado = monto_aplicado + 0.01 WHERE id = ?",
                        reserva.reservaId()))
                .contains("ck_reserva_respaldo_recuperado");
        assertThat(rechazaLaBase(
                        "UPDATE garantia.reserva_respaldo SET version = version + 1 WHERE id = ?", reserva.reservaId()))
                .as("control negativo: el mismo update, coherente, entra")
                .isEmpty();
    }

    @Test
    @DisplayName("rechaza por R-GAR-10: un faltante que no cuadra con el pozo o una segunda cobertura viva del turno")
    void rechazaRGAR10() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        reservar(c, "8000.00");
        var cobertura = cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));

        assertThat(rechazaLaBase(
                        "UPDATE garantia.cobertura_respaldo SET monto_faltante = monto_faltante + 1 WHERE id = ?",
                        cobertura.coberturaId()))
                .contains("ck_cobertura_respaldo_cuadra");
        assertThat(rechazaLaBase(COPIA_DE_COBERTURA, c.turno(), cobertura.coberturaId()))
                .contains("uq_cobertura_respaldo_turno_viva");
    }

    @Test
    @DisplayName("rechaza por R-GAR-11: una linea de mas que hace que las lineas no sumen el faltante")
    void rechazaRGAR11() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        reservar(c, "8000.00");
        var cobertura = cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));
        UUID tercera = fixturaDeRespaldo.otraObligacion(fixtura, c.escenario(), "100.00");

        // Dos sentencias en una (el constraint diferido se fuerza antes de insertar): los ids son
        // UUID generados por la propia prueba, no entrada de afuera.
        String error = rechazaLaBase("SET CONSTRAINTS ALL IMMEDIATE; INSERT INTO garantia.cobertura_respaldo_linea "
                + "(id, cobertura_respaldo_id, obligacion_id, monto_cubierto, monto_recuperado) VALUES (gen_random_uuid(), '"
                + cobertura.coberturaId() + "', '" + tercera + "', 100.00, 0)");

        assertThat(error).contains("R-GAR-11");
    }

    @Test
    @DisplayName("rechaza por R-GAR-12: una cobertura cuyo turno es de otro grupo")
    void rechazaRGAR12() {
        fixturaDeRespaldo.capacidad("20000.00");
        var a = caso();
        var b = caso();
        reservar(a, "8000.00");
        var cobertura = cubrir(a, pedidoDelEjemplo(a, "cobertura-" + a.turno()));

        String error = rechazaLaBase(COPIA_DE_COBERTURA, b.turno(), cobertura.coberturaId());

        assertThat(error).contains("R-GAR-12");
    }
}
