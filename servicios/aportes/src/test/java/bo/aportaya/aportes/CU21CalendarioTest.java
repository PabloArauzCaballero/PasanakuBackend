package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CalendarioDelPeriodo;
import bo.aportaya.aportes.aplicacion.GenerarObligacionesDelPeriodo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H8.S1.M1 · El calendario de obligaciones de un periodo contra PostgreSQL real y un DOBLE de `grupos`.
 *
 * <p>El doble ejercita el contrato propuesto de {@link CalendarioDelPeriodo} en tres niveles: lo
 * correcto (tres cupos), el limite (vencimiento sobre un feriado y un fin de semana) y lo invalido
 * (ver {@link CU21CalendarioRechazosTest}). Lo que queda verificado es la generacion de ESTE
 * servicio, no la ruta de `grupos`, que todavia no existe.
 */
class CU21CalendarioTest extends BaseDeCalendario {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    @Test
    @DisplayName(
            "Dado un período abierto con tres cupos ocupados y un aporte de Bs 500 · Cuando se generan las obligaciones del período · Entonces existe una obligación APORTE_PERIODICO por cupo con su titular, el turno, Bs 500 y estado PENDIENTE · Y el calendario las enumera sin duplicar")
    void criterio5() {
        fixtura.feriado(LocalDate.of(2026, 5, 1), "Dia del Trabajo - feriado nacional");
        var p = fixtura.periodoAbierto(3, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));
        afirma(p, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));

        var salida = generar(p.periodoId());

        assertThat(salida.creadas()).isEqualTo(3);
        assertThat(salida.calendario()).hasSize(3);
        assertThat(salida.turno()).isEqualTo(1);
        assertThat(salida.corridaPorNoHabil()).isFalse();
        assertThat(salida.calendario())
                .extracting(GenerarObligacionesDelPeriodo.Linea::titularParticipanteId)
                .containsExactlyInAnyOrderElementsOf(p.cupos().stream()
                        .map(FixturaDeAportes.CupoDePrueba::participanteId)
                        .toList());
        assertThat(salida.calendario()).allSatisfy(l -> {
            assertThat(l.monto()).isEqualTo(bob("500.00"));
            assertThat(l.estado()).isEqualTo("PENDIENTE");
            assertThat(l.turno()).isEqualTo(1);
            assertThat(l.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 5, 12));
            assertThat(l.esNueva()).isTrue();
        });
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.obligacion_aporte WHERE periodo_id = ? AND tipo = 'APORTE_PERIODICO' AND estado = 'PENDIENTE' AND monto_esperado = 500.00",
                        p.periodoId()))
                .isEqualTo(3);
        assertThat(eventosDeGeneracion(p.periodoId())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un período cuyo vencimiento contractual cae en feriado · Cuando se generan las obligaciones del período · Entonces fecha_vencimiento es el siguiente día hábil y la fecha contractual queda explícita · Y el evento avisa al grupo del corrimiento")
    void criterio6() {
        fixtura.feriado(LocalDate.of(2026, 5, 1), "Dia del Trabajo - feriado nacional");
        var p = fixtura.periodoAbierto(2, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4));
        afirma(p, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4));

        var salida = generar(p.periodoId());

        assertThat(salida.fechaContractual()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(salida.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(salida.corridaPorNoHabil()).isTrue();
        assertThat(salida.diasSalteados()).hasSize(3);
        assertThat(salida.calendario()).allSatisfy(l -> {
            assertThat(l.fechaContractual()).isEqualTo(LocalDate.of(2026, 5, 1));
            assertThat(l.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 5, 4));
            // La gracia conserva su largo (3 dias): se corre lo mismo que el vencimiento.
            assertThat(l.finDeGracia()).isEqualTo(LocalDate.of(2026, 5, 7));
        });
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.obligacion_aporte WHERE periodo_id = ? AND fecha_vencimiento = DATE '2026-05-04' AND fecha_fin_gracia = DATE '2026-05-07'",
                        p.periodoId()))
                .isEqualTo(2);
        var evento = dsl.fetchOne(
                "SELECT payload::text FROM aportes.evento_dominio WHERE tipo = 'aportes.obligaciones_generadas' AND agregado_id = ?",
                p.periodoId());
        assertThat(evento.get(0, String.class).replace(" ", ""))
                .contains("\"notificarAlGrupo\":true")
                .contains("2026-05-01")
                .contains("2026-05-04");
    }

    @Test
    @DisplayName(
            "Dadas las obligaciones de un período ya generadas · Cuando se generan otra vez, también a la vez · Entonces no se crea ninguna obligación nueva ni un segundo evento")
    void criterio7() throws Exception {
        fixtura.feriado(LocalDate.of(2026, 5, 1), "Dia del Trabajo - feriado nacional");
        var p = fixtura.periodoAbierto(3, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));
        afirma(p, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));

        var primera = generar(p.periodoId());
        var segunda = generar(p.periodoId());

        assertThat(primera.creadas()).isEqualTo(3);
        assertThat(segunda.creadas()).isZero();
        assertThat(segunda.calendario()).hasSize(3).allSatisfy(l -> assertThat(l.esNueva())
                .isFalse());
        assertThat(segunda.calendario())
                .extracting(GenerarObligacionesDelPeriodo.Linea::obligacionId)
                .containsExactlyInAnyOrderElementsOf(primera.calendario().stream()
                        .map(GenerarObligacionesDelPeriodo.Linea::obligacionId)
                        .toList());
        assertThat(eventosDeGeneracion(p.periodoId())).isEqualTo(1);

        // Y a la vez, sobre otro periodo: dos hilos, una sola escritura efectiva (R-GRP-03 en la base).
        var q = fixtura.periodoAbierto(3, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));
        afirma(q, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<GenerarObligacionesDelPeriodo.Salida>> futuros = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futuros.add(pool.submit(() -> {
                largada.await();
                return generar(q.periodoId());
            }));
        }
        largada.countDown();
        int creadasEnTotal = 0;
        for (var f : futuros) {
            creadasEnTotal += f.get(60, TimeUnit.SECONDS).creadas();
        }
        pool.shutdown();

        assertThat(creadasEnTotal).isEqualTo(3);
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.obligacion_aporte WHERE periodo_id = ? AND tipo = 'APORTE_PERIODICO'",
                        q.periodoId()))
                .isEqualTo(3);
        assertThat(eventosDeGeneracion(q.periodoId())).isEqualTo(1);
    }
}
