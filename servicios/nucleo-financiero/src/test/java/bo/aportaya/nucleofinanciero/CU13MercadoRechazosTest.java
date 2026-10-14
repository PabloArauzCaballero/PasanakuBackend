package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU13EjecutarConDestino.EntradaPagoATercero;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-13 · los rechazos del pago al vendedor con la retencion del comprador (mercado de turnos). El camino correcto, el
 * cuadre, el reintento y la reversa estan en {@link CU13MercadoTest}.
 */
class CU13MercadoRechazosTest extends BaseDeMercado {

    @Test
    @DisplayName("rechaza otra clave sobre una retencion ya pagada (AP-CU13-03): una sola ejecucion por retencion")
    void unaSolaEjecucion() {
        pagar("pag-5a", servicio);
        assertThatThrownBy(() -> pagar("pag-5b", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-03"));
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("480.00");
    }

    @Test
    @DisplayName("rechaza a un usuario final, sea el comprador, el vendedor u otro con BILLETERA_OPERAR (AP-CU13-09)")
    void usuarioFinalNoPaga() {
        for (UUID quien : new UUID[] {comprador, vendedor, fixtura.usuario()}) {
            assertThatThrownBy(() -> pagar("pag-6", contextoDe(quien)))
                    .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-09"));
        }
        assertThatThrownBy(() -> pagar("pag-6", comoServicio("SERVICIO_INVERSIONES")))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-09"));
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("0.00");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id=? AND estado='VIGENTE'",
                        retencion))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza si entregas no responde o la oferta no esta pagable (AP-CU13-08): no se mueve plata")
    void ofertaNoConfirmada() {
        ofertas.seCae(true);
        assertThatThrownBy(() -> pagar("pag-7", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-08"));
        ofertas.seCae(false);
        ofertas.publica(oferta, vendedor, "500.00", "20.00", "CANCELADA");
        assertThatThrownBy(() -> pagar("pag-7", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-08"));
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("0.00");
        assertThat(sistemaFx.saldoDelLibro(cuentaComprador)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("rechaza un beneficiario distinto del vendedor de la retencion y un vendedor que es el comprador")
    void beneficiarioIncorrecto() {
        // El beneficiario no llega en el pedido: lo trae entregas. Si traen al comprador como vendedor, se rechaza.
        ofertas.publica(oferta, comprador, "500.00", "20.00", "RESERVADA");
        assertThatThrownBy(() -> pagar("pag-8", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-08"));
        // Y una oferta que no es la que motivo la retencion tampoco se paga.
        assertThatThrownBy(() -> transaccion.execute(t -> ejecucionCU.pagarAlTercero(
                        new EntradaPagoATercero(
                                "pag-8b",
                                retencion,
                                UUID.randomUUID(),
                                vendedor,
                                Dinero.de("500.00", Moneda.BOB),
                                Dinero.de("20.00", Moneda.BOB)),
                        servicio)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-08"));
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("rechaza un precio que no es lo retenido (mayor o menor) o de otra moneda (AP-CU13-06)")
    void precioNoCabe() {
        ofertas.publica(oferta, vendedor, "500.01", "20.00", "RESERVADA");
        assertThatThrownBy(() -> pagar("pag-9", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-06"));
        ofertas.publica(oferta, vendedor, "499.99", "20.00", "RESERVADA");
        assertThatThrownBy(() -> pagar("pag-9a", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-06"));
        assertThatThrownBy(() -> transaccion.execute(t -> ejecucionCU.pagarAlTercero(
                        new EntradaPagoATercero(
                                "pag-9b",
                                retencion,
                                oferta,
                                vendedor,
                                Dinero.de("500.00", Moneda.USD),
                                Dinero.de("20.00", Moneda.USD)),
                        servicio)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-06"));
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("0.00");
        assertThat(sistemaFx.saldoDelLibro(cuentaComprador)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName(
            "rechaza cargos mayores que el precio, negativos o en otra moneda (AP-CU13-06): el vendedor no paga mas de"
                    + " lo que cobra y no se mueve nada")
    void cargosInvalidos() {
        ofertas.publica(oferta, vendedor, "500.00", "500.01", "RESERVADA");
        assertThatThrownBy(() -> pagar("pag-12", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-06"));
        assertThatThrownBy(() -> transaccion.execute(t -> ejecucionCU.pagarAlTercero(
                        new EntradaPagoATercero(
                                "pag-12b",
                                retencion,
                                oferta,
                                vendedor,
                                Dinero.de("500.00", Moneda.BOB),
                                Dinero.de("-1.00", Moneda.BOB)),
                        servicio)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-06"));
        assertThatThrownBy(() -> transaccion.execute(t -> ejecucionCU.pagarAlTercero(
                        new EntradaPagoATercero(
                                "pag-12c",
                                retencion,
                                oferta,
                                vendedor,
                                Dinero.de("500.00", Moneda.BOB),
                                Dinero.de("20.00", Moneda.USD)),
                        servicio)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-06"));
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("0.00");
        assertThat(sistemaFx.saldoDelLibro(cuentaComprador)).isEqualByComparingTo("500.00");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id=? AND estado='VIGENTE'",
                        retencion))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza una retencion liberada o de otro motivo (AP-CU13-03 / AP-CU13-05)")
    void retencionNoPagable() {
        transaccion.execute(t -> retencionCU.liberar(retencion, contextoDe(comprador)));
        assertThatThrownBy(() -> pagar("pag-10", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-03"));
        UUID deInversion = retenerInversion(comprador, cuentaComprador, "10.00", UUID.randomUUID());
        assertThatThrownBy(() -> transaccion.execute(t -> pagoCU.ejecutar(deInversion, "pag-10b", servicio)))
                .isInstanceOf(ErrorDeNegocio.class);
    }
}
