package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CU19ReembolsarPago.EntradaReembolso;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** F1.L1.M12 (ANF-DINERO-002) · el reembolso lleva la moneda del pago, contra la base (R-BIL-29). */
class CU19MonedaTest extends BaseDeAportes {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private UUID pagoEnUsd(UUID usuario) {
        var obligacion = fixtura.obligacion(usuario, "100.00", 10, "USD");
        return transaccion
                .execute(t -> cobroCU.acreditar(
                        new EntradaCobro(
                                "cob-" + UUID.randomUUID(),
                                obligacion.id(),
                                Dinero.de("100.00", Moneda.USD),
                                Dinero.de("0.00", Moneda.USD),
                                "BILLETERA_MOVIL",
                                "ref-" + UUID.randomUUID(),
                                Optional.empty(),
                                false,
                                true),
                        contextoDe(usuario)))
                .pagoId();
    }

    @Test
    @DisplayName("cuadre: el reembolso de un pago en USD se registra y se lee en USD, no en una moneda fija")
    void reembolsoEnUsd() {
        UUID usuario = fixtura.usuario();
        UUID pago = pagoEnUsd(usuario);

        var salida = transaccion.execute(t -> reembolsoCU.solicitar(
                new EntradaReembolso(pago, Dinero.de("40.00", Moneda.USD), "DUPLICADO", "moneda-usd"),
                contextoDe(usuario)));

        assertThat(salida.disponibleRestante().moneda()).isEqualTo(Moneda.USD);
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.reembolso WHERE id = ? AND moneda = 'USD'",
                        salida.reembolsoId()))
                .isEqualTo(1);
        assertThat(transaccion
                        .execute(t -> reembolsoCU.solicitar(
                                new EntradaReembolso(pago, Dinero.de("40.00", Moneda.USD), "DUPLICADO", "moneda-usd"),
                                contextoDe(usuario)))
                        .disponibleRestante()
                        .moneda())
                .isEqualTo(Moneda.USD);
    }

    @Test
    @DisplayName("rechaza un reembolso en una moneda distinta de la del pago (AP-CU19-02, R-BIL-29) y no inserta nada")
    void rechazaOtraMoneda() {
        UUID usuario = fixtura.usuario();
        UUID pago = pagoEnUsd(usuario);

        assertThatThrownBy(() -> transaccion.execute(t -> reembolsoCU.solicitar(
                        new EntradaReembolso(pago, Dinero.de("40.00", Moneda.BOB), "DUPLICADO", "moneda-bob"),
                        contextoDe(usuario))))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("la moneda del pago");
        assertThat(contar("SELECT count(*)::int FROM aportes.reembolso WHERE pago_id = ?", pago))
                .isZero();
    }

    @Test
    @DisplayName("rechaza la base una moneda fuera del catalogo en reembolso (R-BIL-29)")
    void rechazaLaBaseUnaMonedaInventada() {
        UUID usuario = fixtura.usuario();
        UUID pago = pagoEnUsd(usuario);
        var salida = transaccion.execute(t -> reembolsoCU.solicitar(
                new EntradaReembolso(pago, Dinero.de("40.00", Moneda.USD), "DUPLICADO", "moneda-xyz"),
                contextoDe(usuario)));
        // F1.L1.M08 · el reembolso es append-only: la moneda invalida se prueba en el alta, no con UPDATE.
        assertThatThrownBy(() -> transaccion.execute(t -> dsl.execute(
                        """
                        INSERT INTO aportes.reembolso
                            (id, pago_id, monto, moneda, motivo, estado, solicitado_por, fecha_solicitud)
                        SELECT gen_random_uuid(), pago_id, monto, 'XYZ', motivo, 'SOLICITADO', solicitado_por, now()
                          FROM aportes.reembolso WHERE id = ?
                        """,
                        salida.reembolsoId())))
                .hasMessageContaining("ck_reembolso_moneda");
    }
}
