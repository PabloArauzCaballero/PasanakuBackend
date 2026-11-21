package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-22 · la entrega del pozo completo.
 *
 * <p>La entrega nace SOLO del fondeo: el pozo lo afirma aportes y el principal se entrega completo, sin deducciones
 * (ADR-055/056). La liquidacion con importes del pedido y el cierre manual con un monto libre se retiraron (F1.L2.M09,
 * F1.L2.M08 y punto 3 del lote L2). Los caminos del fondeo con faltante y sus dobles viven en {@code
 * CU133PozoCompleto*Test}.
 */
class CU22Test extends BaseDePozoCompleto {

    @Test
    @DisplayName(
            "Dado un pozo de Bs 6.000 con todos los aportes confirmados · Cuando se fondea el turno · Entonces nace una sola entrega PROGRAMADA con el pozo completo como principal y sin deducciones")
    void criterio1() {
        var c = caso();
        aportesConfirma(c, "6000.00");

        var salida = fondear(c, "cu22-1-" + c.e().turnoId());

        assertThat(salida.esNuevo()).isTrue();
        assertThat(entregasDelTurno(c)).isEqualTo(1);
        var fila = dsl.fetchOne(
                "SELECT estado, monto_bolsa_bruto, total_deducciones, monto_neto_a_entregar FROM entregas.entrega_fondo WHERE id = ?",
                salida.entregaId());
        assertThat(fila.get(0, String.class)).isEqualTo("PROGRAMADA");
        assertThat(fila.get(1, BigDecimal.class)).isEqualByComparingTo("6000.00");
        assertThat(fila.get(2, BigDecimal.class)).isEqualByComparingTo("0.00");
        assertThat(fila.get(3, BigDecimal.class)).isEqualByComparingTo("6000.00");
        assertThat(contar(
                        "SELECT count(*)::int FROM entregas.deduccion_entrega WHERE entrega_id = ?",
                        salida.entregaId()))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dado un turno ya fondeado · Cuando se repite el fondeo · Entonces devuelve la MISMA entrega y no crea otra · Y la base sigue rechazando una segunda entrega del turno (R-GRP-01)")
    void criterio2() {
        var c = caso();
        aportesConfirma(c, "6000.00");
        var primera = fondear(c, "cu22-2-" + c.e().turnoId());

        var otraVez = fondear(c, "cu22-2-" + c.e().turnoId());

        assertThat(otraVez.entregaId()).isEqualTo(primera.entregaId());
        assertThat(otraVez.esNuevo()).isFalse();
        assertThat(entregasDelTurno(c)).isEqualTo(1);
        assertThat(segundaEntregaDelTurno(c)).contains("uq_entrega_");
    }

    @Test
    @DisplayName(
            "Dado un pozo de Bs 6.000 con Bs 5.000 confirmados y nadie que cubra el faltante · Cuando se fondea el turno · Entonces la entrega queda BLOQUEADA con el pozo completo como principal y Bs 1.000 de deuda conservada")
    void criterio3() {
        var c = caso();
        aportesConfirma(c, "5000.00");
        // Nadie cubre: la empresa ya no (F2.L2.M01) y el fondo del grupo todavia no (F3.L2.M05).
        var salida = fondear(c, "cu22-3-" + c.e().turnoId());

        assertThat(salida.cifras().pendiente()).isEqualTo(bob("1000.00"));
        var fila = dsl.fetchOne(
                "SELECT estado, monto_bolsa_bruto FROM entregas.entrega_fondo WHERE id = ?", salida.entregaId());
        assertThat(fila.get(0, String.class)).isEqualTo("BLOQUEADA_POR_FONDO_INCOMPLETO");
        assertThat(fila.get(1, BigDecimal.class)).isEqualByComparingTo("6000.00");
        assertThat(numero("SELECT monto_pendiente FROM entregas.fondeo_entrega WHERE id = ?", salida.fondeoId()))
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("reintento: la misma clave de idempotencia dos veces devuelve la misma respuesta y un solo efecto")
    void reintento() {
        var c = caso();
        aportesConfirma(c, "6000.00");

        var a = fondear(c, "cu22-r-" + c.e().turnoId());
        var b = fondear(c, "cu22-r-" + c.e().turnoId());

        assertThat(b.entregaId()).isEqualTo(a.entregaId());
        assertThat(b.fondeoId()).isEqualTo(a.fondeoId());
        assertThat(contar("SELECT count(*)::int FROM entregas.fondeo_entrega WHERE turno_id = ?", c.e().turnoId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza por R-GRP-01")
    void rechazaRGRP01() {
        // Una entrega por turno. Dos es pagar dos veces el mismo premio, y el grupo se queda sin fondo para el
        // siguiente. La entrega nace del fondeo; la segunda llega por fuera del caso de uso y la frena la base.
        var c = caso();
        aportesConfirma(c, "6000.00");
        fondear(c, "cu22-g-" + c.e().turnoId());

        assertThat(segundaEntregaDelTurno(c)).contains("uq_entrega_");
    }

    private String segundaEntregaDelTurno(Caso c) {
        return rechazaLaBase(
                """
                INSERT INTO entregas.entrega_fondo
                    (id, grupo_id, periodo_id, turno_id, cupo_id, beneficiario_participante_id,
                     monto_bolsa_bruto, total_deducciones, monto_neto_a_entregar,
                     monto_efectivamente_entregado, moneda, estado, metodo_desembolso,
                     fecha_programada, version)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, 6000.00, 0, 6000.00, 0,
                        'BOB', 'PROGRAMADA', 'BILLETERA_MOVIL', current_date, 0)
                """,
                c.e().grupoId(),
                c.e().periodoId(),
                c.e().turnoId(),
                c.e().cupoId(),
                c.e().participanteId());
    }
}
