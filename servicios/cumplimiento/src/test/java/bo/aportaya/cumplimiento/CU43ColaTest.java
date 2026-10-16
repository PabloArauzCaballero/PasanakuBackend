package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento.Reporte;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-43 · la cola de reportes regulatorios del backoffice (carril A · H9). */
class CU43ColaTest extends EscenarioDeColas {

    private UUID reporte(UUID catalogo, LocalDate corte, String estado) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO cumplimiento.reporte_regulatorio
                    (id, catalogo_reporte_id, generado_por, aprobado_por, periodo, fecha_corte, estado,
                     cantidad_registros, reporte_en_cero, monto_total, fecha_limite)
                VALUES (?, ?, ?, ?, ?, ?, ?, 3, false, 1250.50, ?)
                """,
                id,
                catalogo,
                fixtura.usuario(),
                // ck_reporte_segregacion: el ENVIADO lo aprobo alguien distinto de quien lo genero.
                "ENVIADO".equals(estado) ? fixtura.usuario() : null,
                corte.toString().substring(0, 7),
                corte,
                estado,
                corte.plusDays(10));
        return borrarAlTerminar("reporte_regulatorio", id);
    }

    @Test
    @DisplayName(
            "Dados reportes GENERADO y ENVIADO · Cuando el oficial lista la cola filtrando por GENERADO · Entonces ve solo los generados, del corte más reciente al más antiguo, con el monto sin moneda")
    void filtraYOrdena() {
        UUID catalogo =
                uif.catalogoDeReporte("COLA-" + UUID.randomUUID().toString().substring(0, 6), "UIF", 10);
        UUID viejo = reporte(catalogo, LocalDate.of(2026, 1, 31), "GENERADO");
        UUID nuevo = reporte(catalogo, LocalDate.of(2026, 2, 28), "GENERADO");
        UUID enviado = reporte(catalogo, LocalDate.of(2026, 3, 31), "ENVIADO");
        var ctx = operador();

        var filas = todas(
                (PaginaPedida<LocalDate> p) ->
                        colas().reportes(List.of("GENERADO"), p.antesDe(), p.antesDeId(), p.limite(), ctx),
                2);

        assertThat(propias(filas, Reporte::reporteId, List.of(viejo, nuevo, enviado)))
                .containsExactly(nuevo, viejo);
        assertThat(filas)
                .filteredOn(r -> r.reporteId().equals(nuevo))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.montoTotal()).isEqualByComparingTo("1250.50");
                    assertThat(r.cantidadRegistros()).isEqualTo(3);
                    assertThat(r.aprobadoPor()).isEmpty();
                });
    }

    @Test
    @DisplayName("rechaza un estado desconocido o un cursor a medias con AP-CU43-05")
    void rechazaFiltroInvalido() {
        var ctx = operador();
        rechazaCon(
                "AP-CU43-05",
                () -> transaccion.execute(
                        t -> colas().reportes(List.of("INVENTADO"), Optional.empty(), Optional.empty(), 10, ctx)));
        rechazaCon(
                "AP-CU43-05",
                () -> transaccion.execute(
                        t -> colas().reportes(List.of(), Optional.of(LocalDate.now()), Optional.empty(), 10, ctx)));
    }
}
