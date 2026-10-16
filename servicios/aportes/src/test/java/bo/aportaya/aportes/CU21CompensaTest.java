package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU21CompensarAporte.AporteNoAplicado;
import bo.aportaya.aportes.aplicacion.CU21CompensarAporte.Resultado;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril Q · CU-21 · el aporte PAGADO que {@code nucleo-financiero} no pudo aplicar por falta de saldo se COMPENSA: el pago
 * pasa a {@code REVERSADO} y la obligacion vuelve a estar pendiente por ese importe. El productor del aviso es un doble de
 * contrato (el sobre de {@code contratos/pactos/aportes-nucleo-financiero.json}); NO esta cubierto el recorrido con Kafka y
 * nucleo reales.
 */
class CU21CompensaTest extends BaseDeCompensacion {

    @Test
    @DisplayName("Dado un aporte de Bs 500 pagado con saldo que nucleo-financiero no pudo aplicar por falta de saldo ·"
            + " Cuando aportes consume el aviso aporte_no_aplicado · Entonces el pago queda REVERSADO y la obligación"
            + " vuelve a PENDIENTE con Bs 0 pagados")
    void compensaElAportePagado() {
        Caso c = pagado("500.00", 10);
        assertThat(estadoDe(c.obligacion())).isEqualTo("PAGADO");

        var salida = compensar(aviso(c));

        assertThat(salida.resultado()).isEqualTo(Resultado.COMPENSADO);
        assertThat(salida.estadoDeLaObligacion()).contains("PENDIENTE");
        assertThat(estadoDelPago(c.pago())).isEqualTo("REVERSADO");
        assertThat(estadoDe(c.obligacion())).isEqualTo("PENDIENTE");
        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("0.00");
        assertThat(pagosVigentes(c.obligacion())).isEqualByComparingTo(pagadoDe(c.obligacion()));
        assertThat(eventos("aportes.aporte_compensado", c.obligacion())).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM comun.bitacora_evento WHERE entidad = 'pago' AND entidad_id = ?",
                        c.pago()))
                .isEqualTo(1);
        System.out.println("COMPENSA | pago=" + c.pago() + " ACREDITADO -> " + estadoDelPago(c.pago())
                + " | obligacion " + "PAGADO -> " + estadoDe(c.obligacion()) + " | monto_pagado 500.00 -> "
                + pagadoDe(c.obligacion()));
    }

    @Test
    @DisplayName(
            "evento duplicado: el mismo aporte_no_aplicado dos veces compensa UNA vez y publica UN aporte_compensado")
    void eventoDuplicado() {
        Caso c = pagado("500.00", 10);
        var a = aviso(c);

        assertThat(compensar(a).resultado()).isEqualTo(Resultado.COMPENSADO);
        assertThat(compensar(a).resultado()).isEqualTo(Resultado.DUPLICADO);

        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("0.00");
        assertThat(eventos("aportes.aporte_compensado", c.obligacion())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "reintento: el MISMO pago llega en OTRO evento · ya esta compensado: no se revierte dos veces (la obligacion"
                    + " no queda con menos de cero pagado)")
    void mismoPagoOtroEvento() {
        Caso c = pagado("500.00", 10);

        assertThat(compensar(aviso(c)).resultado()).isEqualTo(Resultado.COMPENSADO);
        assertThat(compensar(aviso(c)).resultado()).isEqualTo(Resultado.YA_COMPENSADO);

        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("0.00");
        assertThat(eventos("aportes.aporte_compensado", c.obligacion())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "cuadre: con dos pagos (Bs 200 y Bs 300) se compensa solo el que no se aplico: queda PAGADO_PARCIAL con"
                    + " Bs 200, igual a la suma de los pagos vigentes")
    void compensaSoloElPagoFallido() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);
        ContextoSesion ctx = contextoDe(usuario);
        var primero = pagar(obligacion.id(), ctx, "200.00");
        var segundo = pagar(obligacion.id(), ctx, "300.00");
        assertThat(estadoDe(obligacion.id())).isEqualTo("PAGADO");

        var salida = compensar(
                new AporteNoAplicado(UUID.randomUUID(), UUID.randomUUID(), segundo.pagoId(), obligacion.id(), FALLA));

        assertThat(salida.resultado()).isEqualTo(Resultado.COMPENSADO);
        assertThat(estadoDelPago(primero.pagoId())).isEqualTo("ACREDITADO");
        assertThat(estadoDelPago(segundo.pagoId())).isEqualTo("REVERSADO");
        assertThat(estadoDe(obligacion.id())).isEqualTo("PAGADO_PARCIAL");
        assertThat(pagadoDe(obligacion.id())).isEqualByComparingTo("200.00");
        assertThat(pagosVigentes(obligacion.id())).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("compensa una obligacion YA VENCIDA: vuelve a EN_MORA (no a PENDIENTE), para que corran los recargos")
    void obligacionVencidaVuelveAMora() {
        Caso c = pagado("500.00", -10);

        var salida = compensar(aviso(c));

        assertThat(salida.resultado()).isEqualTo(Resultado.COMPENSADO);
        assertThat(estadoDe(c.obligacion())).isEqualTo("EN_MORA");
        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("0.00");
    }
}
