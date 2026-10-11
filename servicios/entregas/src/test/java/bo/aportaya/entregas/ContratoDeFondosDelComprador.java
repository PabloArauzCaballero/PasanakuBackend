package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.entregas.dominio.puertos.FondosDelComprador;
import bo.aportaya.entregas.dominio.puertos.FondosDelComprador.Resultado;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * EL CONTRATO de {@link FondosDelComprador} (retener, pagar al vendedor, liberar), escrito como prueba que
 * cualquier implementacion tiene que pasar: el doble de hoy y el adaptador contra nucleo-financiero cuando
 * este publique {@code pagarAlVendedor} (H9.S1.M5). Tres niveles: correcto, limite e invalido.
 *
 * <p>Una subclase aporta la implementacion y los observadores de saldo; el contrato no sabe como se guarda.
 * Los casos salen de lo que la interfaz promete, no de lo que seria comodo probar.
 */
abstract class ContratoDeFondosDelComprador {

    protected abstract FondosDelComprador implementacion();

    protected abstract void sembrarSaldo(UUID usuario, Dinero monto);

    protected abstract Dinero disponible(UUID usuario);

    protected abstract Dinero plataforma();

    protected abstract Dinero totalDelSistema();

    protected abstract void caer(boolean caido);

    protected FondosDelComprador fondos;
    protected final UUID comprador = UUID.randomUUID();
    protected final UUID vendedor = UUID.randomUUID();
    /** La oferta del mercado que la retencion nombra: sin ella el libro no sabe a quien pagar. */
    protected final UUID oferta = UUID.randomUUID();

    protected static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @BeforeEach
    void iniciar() {
        fondos = implementacion();
        sembrarSaldo(comprador, bob("10000.00"));
    }

    // ------------------------------------------------------------------ correcto

    @Test
    @DisplayName(
            "CORRECTO · retener, pagar al vendedor con cargos a la plataforma: la plata se mueve y el total no cambia")
    void retenerYPagar() {
        Dinero total = totalDelSistema();

        var ret = fondos.retener(comprador, oferta, bob("5500.00"), "k-ret-1");
        var pago = fondos.pagarAlVendedor(ret.referencia(), vendedor, bob("5450.00"), bob("50.00"), "k-pago-1");

        assertThat(ret.resultado()).isEqualTo(Resultado.OK);
        assertThat(pago.resultado()).isEqualTo(Resultado.OK);
        assertThat(disponible(comprador)).isEqualTo(bob("4500.00"));
        assertThat(disponible(vendedor)).isEqualTo(bob("5450.00"));
        assertThat(plataforma()).isEqualTo(bob("50.00"));
        assertThat(totalDelSistema()).isEqualTo(total);
    }

    @Test
    @DisplayName("CORRECTO · retener y liberar devuelve el saldo entero al comprador")
    void retenerYLiberar() {
        var ret = fondos.retener(comprador, oferta, bob("5500.00"), "k-ret-2");

        assertThat(fondos.liberar(ret.referencia(), "k-lib-2")).isEqualTo(Resultado.OK);
        assertThat(disponible(comprador)).isEqualTo(bob("10000.00"));
    }

    // ------------------------------------------------------------- idempotencia

    @Test
    @DisplayName(
            "IDEMPOTENCIA · repetir cada operacion con su clave devuelve la misma referencia y no mueve plata dos veces")
    void mismaClaveMismoEfecto() {
        var r1 = fondos.retener(comprador, oferta, bob("5500.00"), "k-ret-3");
        var r2 = fondos.retener(comprador, oferta, bob("5500.00"), "k-ret-3");
        var p1 = fondos.pagarAlVendedor(r1.referencia(), vendedor, bob("5450.00"), bob("50.00"), "k-pago-3");
        var p2 = fondos.pagarAlVendedor(r1.referencia(), vendedor, bob("5450.00"), bob("50.00"), "k-pago-3");

        assertThat(r2).isEqualTo(r1);
        assertThat(p2).isEqualTo(p1);
        assertThat(disponible(comprador)).isEqualTo(bob("4500.00"));
        assertThat(disponible(vendedor)).isEqualTo(bob("5450.00"));
        assertThat(plataforma()).isEqualTo(bob("50.00"));
    }

    // -------------------------------------------------------------------- limite

    @Test
    @DisplayName("LIMITE · retener exactamente todo el saldo se puede; un centavo mas, no")
    void bordeDelSaldo() {
        var justo = fondos.retener(comprador, oferta, bob("10000.00"), "k-ret-4a");
        var deMas = fondos.retener(vendedor, oferta, bob("0.01"), "k-ret-4b");

        assertThat(justo.resultado()).isEqualTo(Resultado.OK);
        assertThat(disponible(comprador)).isEqualTo(bob("0.00"));
        assertThat(deMas.resultado()).isEqualTo(Resultado.SALDO_INSUFICIENTE);
        assertThat(deMas.referencia()).isNull();
    }

    @Test
    @DisplayName("LIMITE · pagar sin cargos: todo el precio es del vendedor y la plataforma no cobra")
    void sinCargos() {
        var ret = fondos.retener(comprador, oferta, bob("5500.00"), "k-ret-5");

        var pago = fondos.pagarAlVendedor(ret.referencia(), vendedor, bob("5500.00"), bob("0.00"), "k-pago-5");

        assertThat(pago.resultado()).isEqualTo(Resultado.OK);
        assertThat(disponible(vendedor)).isEqualTo(bob("5500.00"));
        assertThat(plataforma()).isEqualTo(bob("0.00"));
    }

    // ------------------------------------------------------------------ invalido

    @Test
    @DisplayName("INVALIDO · pagar un importe que no es exactamente la retencion no mueve nada")
    void pagoQueNoCuadraConLaRetencion() {
        Dinero total = totalDelSistema();
        var ret = fondos.retener(comprador, oferta, bob("5500.00"), "k-ret-6");

        var corto = fondos.pagarAlVendedor(ret.referencia(), vendedor, bob("5400.00"), bob("50.00"), "k-pago-6a");
        var largo = fondos.pagarAlVendedor(ret.referencia(), vendedor, bob("5450.00"), bob("100.00"), "k-pago-6b");

        assertThat(corto.resultado()).isEqualTo(Resultado.NO_DISPONIBLE);
        assertThat(largo.resultado()).isEqualTo(Resultado.NO_DISPONIBLE);
        assertThat(disponible(vendedor)).isEqualTo(bob("0.00"));
        assertThat(plataforma()).isEqualTo(bob("0.00"));
        assertThat(totalDelSistema()).isEqualTo(total);
    }

    @Test
    @DisplayName(
            "INVALIDO · lo ejecutado no se libera y lo liberado no se paga: lo irreversible no se deshace a ciegas")
    void ejecutadaNoSeLiberaYLiberadaNoSePaga() {
        var paga = fondos.retener(comprador, oferta, bob("1000.00"), "k-ret-7a");
        fondos.pagarAlVendedor(paga.referencia(), vendedor, bob("1000.00"), bob("0.00"), "k-pago-7a");
        var libre = fondos.retener(comprador, oferta, bob("2000.00"), "k-ret-7b");
        fondos.liberar(libre.referencia(), "k-lib-7b");

        assertThat(fondos.liberar(paga.referencia(), "k-lib-7a")).isEqualTo(Resultado.NO_DISPONIBLE);
        assertThat(fondos.pagarAlVendedor(libre.referencia(), vendedor, bob("2000.00"), bob("0.00"), "k-pago-7b")
                        .resultado())
                .isEqualTo(Resultado.NO_DISPONIBLE);
        assertThat(disponible(vendedor)).isEqualTo(bob("1000.00"));
        assertThat(disponible(comprador)).isEqualTo(bob("9000.00"));
    }

    @Test
    @DisplayName("INVALIDO · una retencion que no existe no se paga ni se libera")
    void retencionDesconocida() {
        assertThat(fondos.pagarAlVendedor(UUID.randomUUID(), vendedor, bob("10.00"), bob("0.00"), "k-pago-8")
                        .resultado())
                .isEqualTo(Resultado.NO_DISPONIBLE);
        assertThat(fondos.liberar(UUID.randomUUID(), "k-lib-8")).isEqualTo(Resultado.NO_DISPONIBLE);
    }

    @Test
    @DisplayName(
            "INVALIDO · con nucleo caido todo responde NO_DISPONIBLE y no retiene ni paga; al volver, el reintento funciona")
    void nucleoCaido() {
        caer(true);
        var ret = fondos.retener(comprador, oferta, bob("5500.00"), "k-ret-9");
        assertThat(ret.resultado()).isEqualTo(Resultado.NO_DISPONIBLE);
        assertThat(disponible(comprador)).isEqualTo(bob("10000.00"));

        caer(false);
        var reintento = fondos.retener(comprador, oferta, bob("5500.00"), "k-ret-9");
        assertThat(reintento.resultado()).isEqualTo(Resultado.OK);
        assertThat(disponible(comprador)).isEqualTo(bob("4500.00"));
    }
}
