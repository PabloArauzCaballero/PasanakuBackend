package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H11.S1.M5 · el reporte de comprobantes del titular: principal, interes, impuesto y comision
 * separados, y cruzados linea por linea con lo que paso en el libro. El tratamiento fiscal real es
 * DR-INV-03: la retencion es una tasa SINTETICA y el reporte lo declara. PostgreSQL real; aliado y
 * libro dobles.
 */
class CU128Test extends BaseDeAnalisis {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2027, 4, 30);

    /** Un fondo de 100 cuotas comprado a 110 y rescatado a 112: interes 200,00 y comision 20,00. */
    private UUID fondoConBeneficio() {
        conSaldo("20000.00");
        UUID posicion = fondo("11000.00", "110.000000");
        aliado.valorCuotaAplicado("112.000000").rescateFondoInmediato(true);
        cu124.solicitar(pedir(posicion, null), ctx);
        return posicion;
    }

    private UUID dpfLiquidado() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        AHORA.set(LUNES.plusSeconds(180L * 86400));
        aliado.liquidacionDpf("197.26", "19.73", 180);
        cu124.solicitar(pedir(posicion, null), ctx);
        return posicion;
    }

    @Test
    @DisplayName(
            "Dado un rescate liquidado con tratamiento definido · Cuando se informa el periodo · Entonces principal, interes, impuesto y comision aparecen separados y suman el neto de cada linea")
    void lineasSeparables() {
        fondoConBeneficio();

        var r = cu128.reporte(DESDE, HASTA, ctx);

        assertThat(r.lineas()).extracting(l -> l.tipo()).containsExactly("SUSCRIPCION", "LIQUIDACION");
        var liquidacion = r.lineas().get(1);
        assertThat(liquidacion.principal()).isEqualByComparingTo("11000.00");
        assertThat(liquidacion.interes()).isEqualByComparingTo("200.00");
        assertThat(liquidacion.impuesto()).isEqualByComparingTo("0.00");
        assertThat(liquidacion.comision()).isEqualByComparingTo("20.00");
        assertThat(liquidacion.neto()).isEqualByComparingTo("11180.00");
        for (var l : r.lineas()) {
            assertThat(l.principal().add(l.interes()).subtract(l.impuesto()).subtract(l.comision()))
                    .isEqualByComparingTo(l.neto());
        }
        assertThat(r.totales().principalSuscrito()).isEqualByComparingTo("11000.00");
        assertThat(r.totales().interes()).isEqualByComparingTo("200.00");
        assertThat(r.totales().comision()).isEqualByComparingTo("20.00");
        assertThat(r.totales().netoAcreditado()).isEqualByComparingTo("11180.00");
    }

    @Test
    @DisplayName(
            "Dados una suscripcion y una liquidacion aplicadas en el libro · Cuando se informa · Entonces todas las lineas quedan CONCILIADAS y el reporte cuadra con el libro")
    void cuadraConElLibro() {
        dpfLiquidado();

        var r = cu128.reporte(DESDE, HASTA, ctx);

        assertThat(r.lineas()).extracting(l -> l.estadoEnLibro()).containsOnly("CONCILIADA");
        var c = r.conciliacion();
        assertThat(c.debitadoEnLibro())
                .isEqualByComparingTo("10000.00")
                .isEqualByComparingTo(c.debitadoSegunComprobantes());
        assertThat(c.acreditadoEnLibro())
                .isEqualByComparingTo("10177.53")
                .isEqualByComparingTo(c.acreditadoSegunComprobantes());
        assertThat(c.lineasPendientesEnLibro()).isZero();
        assertThat(c.lineasConDiferencia()).isZero();
        // Lo que el libro mueve de verdad: sale 10000,00 y vuelven 10177,53.
        assertThat(libro.total(cuenta)).isEqualByComparingTo("20177.53");
        assertThat(r.totales().impuesto()).isEqualByComparingTo("19.73");
        assertThat(r.totales().interes().subtract(r.totales().impuesto())).isEqualByComparingTo("177.53");
    }

    @Test
    @DisplayName(
            "Dado un credito que todavia no llego al libro · Cuando se informa · Entonces la linea queda PENDIENTE_EN_LIBRO y el reporte no cuadra")
    void creditoPendienteEnElLibro() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000");
        aliado.rescateFondoInmediato(true).valorCuotaAplicado("100.000000");
        libro.fallarEn("acreditar", true);
        cu124.solicitar(pedir(posicion, null), ctx);

        var r = cu128.reporte(DESDE, HASTA, ctx);

        assertThat(r.lineas()).extracting(l -> l.estadoEnLibro()).containsExactly("CONCILIADA", "PENDIENTE_EN_LIBRO");
        assertThat(r.conciliacion().lineasPendientesEnLibro()).isEqualTo(1);
        assertThat(r.conciliacion().acreditadoSegunComprobantes()).isEqualByComparingTo("1000.00");
        assertThat(r.conciliacion().acreditadoEnLibro()).isEqualByComparingTo("0.00");
        assertThat(r.conciliacion().cuadra()).isFalse();
    }

    @Test
    @DisplayName(
            "Dado un periodo con la fecha final anterior a la inicial, o de mas de un anio · Cuando se informa · Entonces AP-CU128-01")
    void periodoInvalido() {
        assertThatThrownBy(() -> cu128.reporte(HASTA, DESDE, ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU128-01"));
        assertThatThrownBy(() -> cu128.reporte(DESDE, DESDE.plusDays(366), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU128-01"));
        assertThat(cu128.reporte(DESDE, DESDE.plusDays(365), ctx).lineas())
                .as("el borde (366 dias) si se acepta")
                .isEmpty();
    }

    @Test
    @DisplayName(
            "Dado el reporte exportado · Cuando se compara con el reporte en pantalla · Entonces el CSV trae las mismas cifras, el encabezado fiscal y la fila de totales")
    void exportacionIgualAlReporte() {
        fondoConBeneficio();

        var r = cu128.reporte(DESDE, HASTA, ctx);
        String csv = cu128.exportarCsv(DESDE, HASTA, ctx);
        String[] filas = csv.split("\n");

        assertThat(filas[0]).startsWith("# Reporte de movimientos, no una declaracion tributaria");
        assertThat(filas[1]).contains("SINTETICO").contains("apto_produccion,false");
        assertThat(filas[2]).startsWith("fecha,comprobante,posicion,producto,tipo,principal,interes,impuesto,comision");
        assertThat(filas).hasSize(3 + r.lineas().size() + 1);
        var liquidacion = r.lineas().get(1);
        assertThat(filas[4])
                .contains(liquidacion.comprobanteId().toString())
                .contains(",LIQUIDACION,11000.00,200.00,0.00,20.00,0.00,11180.00,BOB,");
        String total = filas[filas.length - 1];
        assertThat(total).startsWith("TOTAL").contains("11000.00,200.00,0.00,20.00,0.00,11180.00");
        assertThat(csv).doesNotContain(usuario.toString());
    }

    @Test
    @DisplayName("Dados comprobantes de otra persona · Cuando se pide el reporte · Entonces no aparecen")
    void soloLosPropios() {
        dpfLiquidado();

        var deOtro = cu128.reporte(DESDE, HASTA, contextoDe(UUID.randomUUID()));

        assertThat(deOtro.lineas()).isEmpty();
        assertThat(deOtro.totales().netoAcreditado()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName(
            "Dada una distribucion de interes con discrepancia en el periodo · Cuando se informa · Entonces el reporte no cuadra y cuenta la discrepancia")
    void distribucionConDiscrepancia() {
        dpfLiquidado(); // sin devengo corrido: la distribucion nace con discrepancia

        var c = cu128.reporte(DESDE, HASTA, ctx).conciliacion();

        assertThat(c.distribucionesConDiscrepancia()).isEqualTo(1);
        assertThat(c.cuadra()).isFalse();
        assertThat(c.lineasConDiferencia()).isZero();
    }

    @Test
    @DisplayName(
            "Dado un producto sintetico · Cuando se informa · Entonces el reporte se declara SINTETICO, no apto para produccion y sin criterio fiscal real")
    void sinteticoYSinCriterioFiscal() {
        dpfLiquidado();

        var r = cu128.reporte(DESDE, HASTA, ctx);

        assertThat(r.origen().name()).isEqualTo("SINTETICO");
        assertThat(r.aptoProduccion()).isFalse();
        assertThat(r.notaFiscal()).contains("no una declaracion tributaria").contains("DR-INV-03");
        assertThat(r.lineas()).extracting(l -> l.origen().name()).containsOnly("SINTETICO");
    }

    @Test
    @DisplayName(
            "rechaza por R-INV-03: un comprobante que no descompone sin residuo no entra al reporte porque no existe")
    void baseNoDejaComprobantesQueNoCuadran() {
        conSaldo("20000.00");
        var orden = ordenar(PRODUCTO_DPF, "1000.00");
        String insercion =
                "insert into inversiones.comprobante_inversion (id, posicion_inversion_id, usuario_id, tipo, origen_id, sentido_titular,"
                        + " principal, interes, impuesto, comision, perdida_realizada, neto, costo_base, moneda, origen_datos, emitido_en)"
                        + " select gen_random_uuid(), posicion_inversion_id, usuario_id, tipo, gen_random_uuid(), sentido_titular, principal, interes,"
                        + " impuesto, comision, perdida_realizada, neto + ?, costo_base, moneda, origen_datos, emitido_en"
                        + " from inversiones.comprobante_inversion where origen_id = ?";

        assertThat(rechazaLaBase(insercion, new BigDecimal("0.01"), orden.ordenId()))
                .contains("ck_comprobante_inv_neto");
    }

    @Test
    @DisplayName(
            "rechaza por R-AUD-01: corregir o borrar un comprobante que ya entro a un reporte; se corrige con una liquidacion nueva, no editando")
    void baseNoDejaEditarComprobantes() {
        UUID posicion = dpfLiquidado();

        assertThat(rechazaLaBase(
                        "update inversiones.comprobante_inversion set interes = interes + 1, neto = neto + 1 where posicion_inversion_id = ?",
                        posicion))
                .contains("R-AUD-01");
        assertThat(rechazaLaBase(
                        "delete from inversiones.comprobante_inversion where posicion_inversion_id = ?", posicion))
                .contains("R-AUD-01");
    }
}
