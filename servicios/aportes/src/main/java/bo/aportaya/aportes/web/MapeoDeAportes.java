package bo.aportaya.aportes.web;

import bo.aportaya.aportes.web.generado.modelo.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * El puente entre el {@code Dinero} del contrato y el del dominio.
 *
 * <p>Son dos tipos distintos y tienen que serlo: el del contrato lo genera OpenAPI y
 * lleva el importe como cadena; el del dominio es el que sabe sumar sin perder
 * centavos. Convertir aca —y no dejar que el generado entre al caso de uso— es lo que
 * mantiene el dominio libre del contrato.
 */
final class MapeoDeAportes {

    private MapeoDeAportes() {}

    /**
     * Un {@code Dinero} que ENTRA por el contrato (hoy, el del cobro). F1.L1.M01 · nunca negativo (regla
     * 91.2.2): el esquema admite signo porque tambien describe salidas, asi que la regla de entrada vive aca
     * y sale como 400 (AP-VAL-02) en vez de chocar con el CHECK de la base.
     */
    static bo.aportaya.plataforma.dominio.Dinero dinero(Dinero valor) {
        BigDecimal monto = new BigDecimal(valor.getMonto());
        if (monto.signum() < 0) {
            throw new jakarta.validation.ConstraintViolationException(
                    "monto: un importe de entrada no puede ser negativo", java.util.Set.of());
        }
        return bo.aportaya.plataforma.dominio.Dinero.de(
                monto, Moneda.valueOf(valor.getMoneda().getValue()));
    }

    /** F1.L1.M01 · el importe de entrada: el contrato ya garantizo que no es negativo. */
    static bo.aportaya.plataforma.dominio.Dinero dinero(bo.aportaya.aportes.web.generado.modelo.Importe valor) {
        return bo.aportaya.plataforma.dominio.Dinero.de(
                new BigDecimal(valor.getMonto()),
                Moneda.valueOf(valor.getMoneda().getValue()));
    }

    /** Un importe suelto —sin moneda al lado— tambien va como cadena, al centavo. */
    static String importe(java.math.BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }

    static Dinero dinero(bo.aportaya.plataforma.dominio.Dinero valor) {
        var salida = new Dinero();
        salida.setMonto(valor.monto().setScale(2, RoundingMode.UNNECESSARY).toPlainString());
        salida.setMoneda(Dinero.MonedaEnum.fromValue(valor.moneda().name()));
        return salida;
    }
}
