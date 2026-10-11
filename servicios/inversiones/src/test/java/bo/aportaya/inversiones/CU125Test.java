package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.aplicacion.VistaRescate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H10.S1.M6 (liquidacion), H11.S1.M3 (intereses confirmados, titular, periodo y retenciones),
 * H11.S1.M4 (comision de exito con marca maxima previa neta) y H11.S1.M5 (comprobantes
 * separables). Los valores esperados estan calculados a mano; el redondeo es half-even a
 * centavos y el residuo de un reparto se asigna al ultimo, nunca se pierde.
 *
 * <p>PostgreSQL real; el aliado y el libro son los dobles declarados. Las tasas y la
 * retencion son SINTETICAS: ninguna es una tarifa ni una alicuota real.
 */
class CU125Test extends BaseDeLiquidacion {

    // ------------------------------------------------------------------ DPF: intereses (H11.M3)
    @Test
    @DisplayName(
            "Dado el interes pagado por el emisor · Cuando se acredita · Entonces coincide con titular, periodo y retencion y se concilia contra el devengo propio")
    void criterioInteresesConfirmados() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        AHORA.set(LUNES.plusSeconds(180L * 86400));
        aliado.liquidacionDpf("197.26", "19.73", 180); // a mano: 10000 x 4 % x 180 / 365 y 10 % sintetico

        VistaRescate r = cu124.solicitar(pedir(posicion, null), ctx);

        assertThat(r.estado()).isEqualTo("LIQUIDADO");
        var conciliacion = dslFixtura.fetchOne(
                "select estado, interes_devengado, interes_externo, retencion_calculada, retencion_externa, periodo_desde, periodo_hasta"
                        + " from inversiones.conciliacion_interes where rescate_inversion_id = ?",
                r.rescateId());
        assertThat(conciliacion.get(0, String.class)).isEqualTo("CONCILIADA");
        assertThat(conciliacion.get(1, BigDecimal.class))
                .isEqualByComparingTo(conciliacion.get(2, BigDecimal.class))
                .isEqualByComparingTo("197.26");
        assertThat(conciliacion.get(3, BigDecimal.class))
                .isEqualByComparingTo(conciliacion.get(4, BigDecimal.class))
                .isEqualByComparingTo("19.73");
        assertThat(conciliacion.get(5, LocalDate.class)).isEqualTo(DIA0);
        assertThat(conciliacion.get(6, LocalDate.class)).isEqualTo(DIA0.plusDays(180));
        var comprobante = liquidacion(posicion);
        assertThat(comprobante.d().principal()).isEqualByComparingTo("10000.00");
        assertThat(comprobante.d().interes()).isEqualByComparingTo("197.26");
        assertThat(comprobante.d().impuesto()).isEqualByComparingTo("19.73");
        assertThat(comprobante.d().neto()).isEqualByComparingTo("10177.53");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("20177.53");
        assertThat(libro.asientos()).hasSize(2); // el debito de la suscripcion y el credito del rescate
        assertThat(libro.sumaDeDebe()).isEqualByComparingTo("20177.53");
    }

    @Test
    @DisplayName(
            "Dado un interes que NO coincide con el calculo propio · Cuando el aliado confirma el rescate · Entonces AP-CU125-01: discrepancia registrada, nada se acredita y el deposito sigue abierto")
    void discrepanciaConElEmisor() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        AHORA.set(LUNES.plusSeconds(180L * 86400));
        aliado.liquidacionDpf("197.27", "19.73", 180); // un centavo de mas

        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, null), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU125-01"));

        assertThat(dslFixtura
                        .fetchOne(
                                "select estado from inversiones.conciliacion_interes where posicion_inversion_id = ?",
                                posicion)
                        .get(0, String.class))
                .isEqualTo("DISCREPANCIA");
        assertThat(contar(
                        "select count(*)::int from inversiones.comprobante_inversion where posicion_inversion_id = ? and tipo = 'LIQUIDACION'",
                        posicion))
                .isZero();
        assertThat(contar(
                        "select count(*)::int from inversiones.posicion_inversion where id = ? and estado = 'ABIERTA'",
                        posicion))
                .isEqualTo(1);
        assertThat(libro.total(cuenta)).isEqualByComparingTo("10000.00");
        assertThat(contar(
                        "select count(*)::int from inversiones.instruccion_libro where tipo = 'ACREDITAR' and usuario_id = ?",
                        usuario))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dado un importe que no es principal + interes - retencion · Cuando el aliado confirma el rescate · Entonces AP-CU125-02 y no se acredita")
    void importeIncoherente() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        AHORA.set(LUNES.plusSeconds(180L * 86400));
        aliado.liquidacionDpf("197.26", "19.73", 180).distorsionDeMonto("5.00");

        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, null), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU125-02"));
        assertThat(libro.total(cuenta)).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("rechaza por R-INV-04: cerrar una posicion sin fecha de cierre o dejarla cerrada con cuotas")
    void rechazaRINV04() {
        // Una posicion cerrada tiene su fecha de cierre y ninguna cuota viva: el cierre del
        // rescate lo hace bien y la base impide cualquier otro camino.
        conSaldo("20000.00");
        UUID dpf = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        UUID cuotas = fondo("1000.00", "100.000000");

        assertThat(rechazaLaBase("update inversiones.posicion_inversion set estado = 'CERRADA' where id = ?", dpf))
                .contains("ck_posicion_inv_cierre");
        assertThat(rechazaLaBase(
                        "update inversiones.posicion_inversion set estado = 'CERRADA', cerrada_en = now() where id = ?",
                        cuotas))
                .contains("ck_posicion_inv_cerrada_sin_cuotas");
        assertThat(rechazaLaBase(
                        "update inversiones.posicion_inversion set estado = 'CERRADA', cerrada_en = now() where id = ?",
                        dpf))
                .as("control negativo: un deposito cerrado con su fecha de cierre, si")
                .isEmpty();
    }
}
