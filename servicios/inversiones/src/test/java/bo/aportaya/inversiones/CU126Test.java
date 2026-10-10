package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H11.S1.M3 · el interes que paga el emisor se reparte al titular, por el periodo y con la retencion
 * del comprobante, y se concilia contra el devengo propio. Los valores esperados estan calculados a
 * mano (10000 x 4 % x 180 / 365 = 197,26; retencion sintetica 10 % = 19,73). PostgreSQL real; aliado
 * y libro dobles. La retencion es SINTETICA, no un criterio fiscal (DR-INV-03).
 */
class CU126Test extends BaseDeAnalisis {

    private UUID dpfLiquidado(boolean devengoCompleto) {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        if (devengoCompleto) {
            for (int dia = 0; dia <= 180; dia++) {
                cu123Devengo.devengar(DIA0.plusDays(dia), sistema());
            }
        }
        AHORA.set(LUNES.plusSeconds(180L * 86400));
        aliado.liquidacionDpf("197.26", "19.73", 180);
        cu124.solicitar(pedir(posicion, null), ctx);
        return posicion;
    }

    @Test
    @DisplayName(
            "Dado el interes pagado por el emisor y un devengo diario completo · Cuando se acredita · Entonces se reparte al titular de la posicion, por su periodo y con su retencion, y concilia contra el devengo")
    void interesRepartidoYConciliado() {
        UUID posicion = dpfLiquidado(true);

        var lista = cu126.listar(posicion, ctx);

        assertThat(lista).hasSize(1);
        var d = lista.get(0);
        assertThat(d.estado()).isEqualTo("CONCILIADA");
        assertThat(d.motivoDiscrepancia()).isNull();
        assertThat(d.periodoDesde()).isEqualTo(DIA0);
        assertThat(d.periodoHasta()).isEqualTo(DIA0.plusDays(180));
        assertThat(d.interesDevengado()).isEqualByComparingTo("197.26");
        assertThat(d.penalizacion()).isEqualByComparingTo("0.00");
        assertThat(d.interesEsperado()).isEqualByComparingTo("197.26");
        assertThat(d.interesPagado()).isEqualByComparingTo("197.26");
        assertThat(d.retencion()).isEqualByComparingTo("19.73");
        assertThat(d.interesNeto()).isEqualByComparingTo("177.53");
        var fila = dslFixtura.fetchOne(
                "select usuario_id from inversiones.distribucion_interes where posicion_inversion_id = ?", posicion);
        assertThat(fila.get(0, UUID.class)).isEqualTo(usuario);
        // Lo distribuido es el interes que el comprobante separo: el mismo numero, no uno recalculado aparte.
        var comprobante = liquidacion(posicion).d();
        assertThat(comprobante.interes()).isEqualByComparingTo(d.interesPagado());
        assertThat(comprobante.impuesto()).isEqualByComparingTo(d.retencion());
    }

    @Test
    @DisplayName(
            "Dado un devengo propio que no corrio hasta el corte · Cuando se acredita el interes · Entonces queda una discrepancia con su motivo a la vista y el titular cobra igual lo que pago el emisor")
    void sinDevengoQuedaComoDiscrepancia() {
        UUID posicion = dpfLiquidado(false);

        var d = cu126.listar(posicion, ctx).get(0);

        assertThat(d.estado()).isEqualTo("DISCREPANCIA");
        assertThat(d.motivoDiscrepancia()).isEqualTo("SIN_DEVENGO_AL_CORTE");
        assertThat(d.interesDevengado()).isEqualByComparingTo("0.00");
        assertThat(d.interesPagado()).isEqualByComparingTo("197.26");
        assertThat(libro.total(cuenta))
                .as("el pago del emisor ya paso el control fuerte (CU-125): el titular cobra")
                .isEqualByComparingTo("20177.53");
    }

    @Test
    @DisplayName(
            "Dado un DPF cancelado antes del vencimiento · Cuando se acredita el interes · Entonces la penalizacion es la parte del devengo que no se paga y la distribucion concilia")
    void cancelacionAnticipadaConPenalizacion() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF_ANTICIPABLE, "10000.00");
        cu123Devengo.devengar(DIA0.plusDays(90), sistema());
        AHORA.set(LUNES.plusSeconds(90L * 86400));
        aliado.liquidacionDpf("56.25", "5.62", 90); // 112,50 devengado, 50 % de penalizacion sintetica

        cu124.solicitar(pedir(posicion, null), ctx);

        var d = cu126.listar(posicion, ctx).get(0);
        assertThat(d.estado()).isEqualTo("CONCILIADA");
        assertThat(d.interesDevengado()).isEqualByComparingTo("112.50");
        assertThat(d.penalizacion()).isEqualByComparingTo("56.25");
        assertThat(d.interesPagado()).isEqualByComparingTo("56.25");
        assertThat(d.retencion()).isEqualByComparingTo("5.62");
        assertThat(d.interesNeto()).isEqualByComparingTo("50.63");
        assertThat(d.periodoHasta()).isEqualTo(DIA0.plusDays(90));
    }

    @Test
    @DisplayName(
            "Dado un interes que el emisor informa distinto del calculo propio · Cuando se confirma el rescate · Entonces no se reparte nada y queda la discrepancia con el emisor")
    void sinRepartoSiElEmisorDiscrepa() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        AHORA.set(LUNES.plusSeconds(180L * 86400));
        aliado.liquidacionDpf("197.27", "19.73", 180);

        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, null), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU125-01"));

        assertThat(cu126.listar(posicion, ctx)).isEmpty();
    }

    @Test
    @DisplayName("reintento: sincronizar el mismo rescate dos veces deja UNA distribucion, no dos")
    void unaSolaDistribucion() {
        UUID posicion = dpfLiquidado(true);
        UUID rescate = dslFixtura
                .fetchOne("select id from inversiones.rescate_inversion where posicion_inversion_id = ?", posicion)
                .get(0, UUID.class);

        cu125.sincronizar(rescate, ctx);
        cu125.sincronizar(rescate, ctx);

        assertThat(contar(
                        "select count(*)::int from inversiones.distribucion_interes where posicion_inversion_id = ?",
                        posicion))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("cuadre: principal devuelto + interes neto repartido es exactamente lo que acredito el libro")
    void cuadraConElLibro() {
        UUID posicion = dpfLiquidado(true);

        var d = cu126.listar(posicion, ctx).get(0);

        assertThat(new BigDecimal("10000.00").add(d.interesNeto()))
                .isEqualByComparingTo(libro.total(cuenta).subtract(new BigDecimal("10000.00")));
    }

    @Test
    @DisplayName(
            "rechaza: pedir las distribuciones de la posicion de OTRA persona es AP-CU123-01, como si no existiera")
    void posicionAjena() {
        UUID posicion = dpfLiquidado(true);

        assertThatThrownBy(() -> cu126.listar(posicion, contextoDe(UUID.randomUUID())))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU123-01"));
    }

    @Test
    @DisplayName(
            "rechaza por R-INV-09: una distribucion de otro titular, de otro periodo o con otra retencion que el comprobante, y por R-AUD-01 un UPDATE o DELETE")
    void baseProtegeLaDistribucion() {
        UUID posicion = dpfLiquidado(true);
        String insercion =
                "insert into inversiones.distribucion_interes (id, posicion_inversion_id, rescate_inversion_id, usuario_id, periodo_desde,"
                        + " periodo_hasta, interes_devengado, penalizacion, interes_esperado, interes_pagado, retencion, interes_neto,"
                        + " moneda, estado, motivo_discrepancia, distribuida_en)"
                        + " select gen_random_uuid(), posicion_inversion_id, rescate_inversion_id, ?, periodo_desde + ?,"
                        + " periodo_hasta, interes_devengado, penalizacion, interes_esperado, interes_pagado, retencion + ?,"
                        + " interes_neto - ?, moneda, estado, motivo_discrepancia, distribuida_en"
                        + " from inversiones.distribucion_interes where posicion_inversion_id = ?";
        BigDecimal cero = BigDecimal.ZERO;
        assertThat(rechazaLaBase(insercion, UUID.randomUUID(), 0, cero, cero, posicion))
                .contains("R-INV-09")
                .contains("titular");
        assertThat(rechazaLaBase(insercion, usuario, 1, cero, cero, posicion))
                .contains("R-INV-09")
                .contains("arranca");
        assertThat(rechazaLaBase(insercion, usuario, 0, new BigDecimal("0.01"), new BigDecimal("0.01"), posicion))
                .contains("R-INV-09")
                .contains("comprobante");
        assertThat(rechazaLaBase(
                        "update inversiones.distribucion_interes set estado = 'DISCREPANCIA' where posicion_inversion_id = ?",
                        posicion))
                .contains("R-AUD-01");
        assertThat(rechazaLaBase(
                        "delete from inversiones.distribucion_interes where posicion_inversion_id = ?", posicion))
                .contains("R-AUD-01");
    }

    @Test
    @DisplayName("rechaza por R-INV-09: una distribucion marcada CONCILIADA cuyo interes pagado no es el esperado")
    void baseProtegeLaConciliacionDelReparto() {
        UUID posicion = dpfLiquidado(true);
        String insercion =
                "insert into inversiones.distribucion_interes (id, posicion_inversion_id, rescate_inversion_id, usuario_id, periodo_desde,"
                        + " periodo_hasta, interes_devengado, penalizacion, interes_esperado, interes_pagado, retencion, interes_neto,"
                        + " moneda, estado, motivo_discrepancia, distribuida_en)"
                        + " select gen_random_uuid(), posicion_inversion_id, rescate_inversion_id, usuario_id, periodo_desde,"
                        + " periodo_hasta, interes_devengado + ?, penalizacion, interes_esperado + ?, interes_pagado, retencion,"
                        + " interes_neto, moneda, 'CONCILIADA', null, distribuida_en"
                        + " from inversiones.distribucion_interes where posicion_inversion_id = ?";
        BigDecimal uno = new BigDecimal("1.00");
        assertThat(rechazaLaBase(insercion, uno, uno, posicion))
                .as("CONCILIADA con lo esperado distinto de lo pagado")
                .contains("ck_distribucion_interes_estado");
        assertThat(rechazaLaBase(insercion, BigDecimal.ZERO, uno, posicion))
                .as("lo esperado tiene que ser el devengo menos la penalizacion")
                .contains("ck_distribucion_interes_esperado");
    }
}
