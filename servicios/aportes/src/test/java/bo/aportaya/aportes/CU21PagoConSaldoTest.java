package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.SalidaCobro;
import bo.aportaya.aportes.aplicacion.CU21PagarConSaldo;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F1.L1.M11, contra PostgreSQL real: el pago con saldo registra el pendiente de la obligacion leido por el servidor,
 * con el canal y la referencia que fija el servidor, y nunca lo que diga el cliente.
 */
class CU21PagoConSaldoTest extends BaseDeAportes {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private static CU21PagarConSaldo conSaldoCU() {
        return new CU21PagarConSaldo(
                new bo.aportaya.plataforma.datos.Datos(dsl),
                new bo.aportaya.aportes.infraestructura.ObligacionRepositorio(),
                new bo.aportaya.aportes.infraestructura.PagoRepositorio(),
                cobroCU,
                SIN_ORDEN_POR_COBRO);
    }

    /** Una pasarela que NO pide orden por cobro (el comportamiento de siempre): si se la llamara, la prueba falla. */
    private static final bo.aportaya.aportes.dominio.PasarelaDePagos SIN_ORDEN_POR_COBRO =
            new bo.aportaya.aportes.dominio.PasarelaDePagos() {
                @Override
                public OrdenDeCobro crearOrdenDeCobro(SolicitudDeCobro solicitud) {
                    throw new AssertionError("no debia abrir una orden");
                }

                @Override
                public Optional<CobroConsultado> consultarEstado(String referenciaProveedor) {
                    throw new AssertionError("no debia consultar");
                }
            };

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private SalidaCobro conSaldo(UUID obligacionId, String clave, Optional<Dinero> declarado, ContextoSesion ctx) {
        return transaccion.execute(t -> conSaldoCU().pagarConSaldo(clave, obligacionId, declarado, true, ctx));
    }

    private int pagos(UUID obligacionId) {
        return contar("SELECT count(*)::int FROM aportes.pago WHERE obligacion_id = ?", obligacionId);
    }

    @Test
    @DisplayName(
            "Dada una obligación de Bs 100 pendiente · Cuando el participante paga la cuota con saldo sin decir el monto"
                    + " · Entonces se registra un pago de Bs 100 por BILLETERA_MOVIL con la referencia del servidor")
    void registraElPendiente() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "100.00", 10);

        var salida = conSaldo(obligacion.id(), "saldo-1", Optional.empty(), contextoDe(usuario));

        assertThat(salida.estadoObligacion()).isEqualTo("PAGADO");
        assertThat(salida.esNuevo()).isTrue();
        assertThat(contar(
                        """
                        SELECT count(*)::int FROM aportes.pago
                         WHERE id = ? AND monto = 100.00 AND canal = 'BILLETERA_MOVIL' AND referencia_proveedor = ?
                        """,
                        salida.pagoId(),
                        CU21PagarConSaldo.referenciaDelSaldo(obligacion.id(), "saldo-1")))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dada una obligación de Bs 100 con Bs 30 ya pagados · Cuando el participante paga la cuota con saldo"
            + " · Entonces se registra un pago de Bs 70 y la obligación queda PAGADO")
    void cobraLoQueFalta() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "100.00", 10);
        var ctx = contextoDe(usuario);
        transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        "parcial-1",
                        obligacion.id(),
                        bob("30.00"),
                        bob("0.00"),
                        "BILLETERA_MOVIL",
                        "ref-parcial-1",
                        Optional.empty(),
                        false,
                        true),
                ctx));

        var salida = conSaldo(obligacion.id(), "saldo-2", Optional.empty(), ctx);

        assertThat(salida.estadoObligacion()).isEqualTo("PAGADO");
        assertThat(contar("SELECT count(*)::int FROM aportes.pago WHERE id = ? AND monto = 70.00", salida.pagoId()))
                .isEqualTo(1);
        assertThat(contar("SELECT monto_pagado::int FROM aportes.obligacion_aporte WHERE id = ?", obligacion.id()))
                .isEqualTo(100);
    }

    @Test
    @DisplayName("rechaza un monto declarado distinto del pendiente (AP-CU21-02) y no escribe nada")
    void montoDeclaradoDistinto() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "100.00", 10);

        assertThatThrownBy(() -> conSaldo(obligacion.id(), "saldo-3", Optional.of(bob("1.00")), contextoDe(usuario)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no coincide");
        assertThat(pagos(obligacion.id())).isZero();
    }

    @Test
    @DisplayName("reintento con la misma clave: devuelve el mismo pago aunque ya no haya pendiente")
    void reintentoDevuelveElOriginal() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "100.00", 10);
        var ctx = contextoDe(usuario);

        var primero = conSaldo(obligacion.id(), "saldo-4", Optional.of(bob("100.00")), ctx);
        var segundo = conSaldo(obligacion.id(), "saldo-4", Optional.of(bob("100.00")), ctx);

        assertThat(segundo.pagoId()).isEqualTo(primero.pagoId());
        assertThat(segundo.esNuevo()).isFalse();
        assertThat(pagos(obligacion.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza otra clave sobre la cuota ya pagada (AP-CU21-01): no hay segundo pago")
    void otraClaveSobreLoPagado() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "100.00", 10);
        var ctx = contextoDe(usuario);
        conSaldo(obligacion.id(), "saldo-5", Optional.empty(), ctx);

        assertThatThrownBy(() -> conSaldo(obligacion.id(), "saldo-6", Optional.empty(), ctx))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(pagos(obligacion.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza una obligacion que no existe (AP-CU21-01)")
    void obligacionInexistente() {
        UUID usuario = fixtura.usuario();

        assertThatThrownBy(() -> conSaldo(UUID.randomUUID(), "saldo-7", Optional.empty(), contextoDe(usuario)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no existe");
    }
}
