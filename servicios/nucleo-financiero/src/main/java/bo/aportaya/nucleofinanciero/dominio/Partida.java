package bo.aportaya.nucleofinanciero.dominio;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una línea del asiento: lo que se debita o se acredita de una cuenta, nunca las dos
 * cosas a la vez, y en una moneda.
 *
 * <p>{@code debe}/{@code haber} viajan como {@link BigDecimal} (el mayor guarda
 * {@code NUMERIC(16,2)}) y llevan SU moneda al lado, igual que {@code movimiento_contable}:
 * un importe sin moneda no se puede sumar con nada (regla 91.1.2). El cuadre se calcula
 * sobre {@code Dinero} en {@link CuadrarPartidas}, por moneda.
 */
public record Partida(String cuentaCodigo, BigDecimal debe, BigDecimal haber, Moneda moneda) {

    /**
     * La forma corta para quien opera en la moneda base del sistema (bolivianos). Existe
     * porque el mayor opero siempre en BOB; quien registra otra moneda usa el constructor
     * completo y la declara.
     */
    public Partida(String cuentaCodigo, BigDecimal debe, BigDecimal haber) {
        this(cuentaCodigo, debe, haber, Moneda.BOB);
    }

    public Partida {
        Objects.requireNonNull(cuentaCodigo, "cuentaCodigo");
        Objects.requireNonNull(debe, "debe");
        Objects.requireNonNull(haber, "haber");
        Objects.requireNonNull(moneda, "moneda");
        if (cuentaCodigo.isBlank()) {
            throw new ErrorDeDominio("Una partida sin código de cuenta no se puede resolver");
        }
        if (debe.signum() < 0 || haber.signum() < 0) {
            throw new ErrorDeDominio("Una partida no lleva importes negativos: %s debe=%s haber=%s"
                    .formatted(cuentaCodigo, debe, haber));
        }
        if (debe.signum() > 0 && haber.signum() > 0) {
            throw new ErrorDeDominio("Una partida es debe O haber, nunca las dos: %s debe=%s haber=%s"
                    .formatted(cuentaCodigo, debe, haber));
        }
        if (debe.signum() == 0 && haber.signum() == 0) {
            throw new ErrorDeDominio("Una partida con debe=0 y haber=0 no mueve nada: %s".formatted(cuentaCodigo));
        }
    }
}
