package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.SalidaCobro;
import bo.aportaya.aportes.aplicacion.CU21PagarConSaldo;
import bo.aportaya.aportes.dominio.PasarelaDePagos.EstadoDeCobro;
import bo.aportaya.aportes.infraestructura.pasarela.simulada.PasarelaSimulada;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-21 con la pasarela SIMULADA conectada (regla 65, tres niveles): el pago con saldo abre una orden del doble y deja su
 * referencia en el pago, y NADA de lo que responde cambia. Verificado contra el doble, no contra un proveedor.
 */
class CU21PagoConOrdenSimuladaTest extends BaseDeAportes {

    private PasarelaSimulada pasarela;

    @BeforeEach
    void preparar() {
        pasarela = new PasarelaSimulada(
                codigo -> Optional.of(SECRETO_DE_PRUEBA), Reloj.delSistema(), new ObjectMapper(), "QR_INTEROP");
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private CU21PagarConSaldo conSaldoCU() {
        return new CU21PagarConSaldo(
                new Datos(dsl),
                new bo.aportaya.aportes.infraestructura.ObligacionRepositorio(),
                new bo.aportaya.aportes.infraestructura.PagoRepositorio(),
                cobroCU,
                pasarela);
    }

    private SalidaCobro pagar(UUID obligacion, String clave, Optional<Dinero> declarado, ContextoSesion ctx) {
        return transaccion.execute(t -> conSaldoCU().pagarConSaldo(clave, obligacion, declarado, true, ctx));
    }

    private String referenciaDelPago(UUID pagoId) {
        return (String) dsl.fetchOne("SELECT referencia_proveedor FROM aportes.pago WHERE id = ?", pagoId)
                .get(0);
    }

    @Test
    @DisplayName(
            "cuadre: el pago queda con la referencia de una orden simulada PENDIENTE del mismo monto, y la respuesta es la de siempre")
    void ordenYReferencia() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "100.00", 10);

        var salida = pagar(obligacion.id(), "orden-1", Optional.empty(), contextoDe(usuario));

        assertThat(salida.estadoObligacion()).isEqualTo("PAGADO");
        assertThat(salida.esNuevo()).isTrue();
        String referencia = referenciaDelPago(salida.pagoId());
        assertThat(referencia).startsWith("SIM-").doesNotStartWith("SALDO-");
        var consultada = pasarela.consultarEstado(referencia).orElseThrow();
        assertThat(consultada.estado()).isEqualTo(EstadoDeCobro.PENDIENTE);
        assertThat(consultada.monto()).isEqualTo(Dinero.de("100.00", Moneda.BOB));
    }

    @Test
    @DisplayName("reintento: la misma clave devuelve el MISMO pago (esNuevo=false), una sola orden y un solo pago")
    void reintentoIdempotente() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "100.00", 10);
        var ctx = contextoDe(usuario);

        var primero = pagar(obligacion.id(), "orden-2", Optional.empty(), ctx);
        var segundo = pagar(obligacion.id(), "orden-2", Optional.empty(), ctx);

        assertThat(segundo.pagoId()).isEqualTo(primero.pagoId());
        assertThat(segundo.esNuevo()).isFalse();
        assertThat(contar("SELECT count(*)::int FROM aportes.pago WHERE obligacion_id = ?", obligacion.id()))
                .isEqualTo(1);
        assertThat(referenciaDelPago(primero.pagoId())).startsWith("SIM-");
    }

    @Test
    @DisplayName(
            "rechaza con los MISMOS errores de siempre: monto declarado distinto, cuota ya pagada y obligacion inexistente")
    void erroresIntactos() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "100.00", 10);
        var ctx = contextoDe(usuario);

        assertThatThrownBy(() -> pagar(obligacion.id(), "orden-3", Optional.of(Dinero.de("99.00", Moneda.BOB)), ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("El monto no coincide");
        pagar(obligacion.id(), "orden-4", Optional.empty(), ctx);
        assertThatThrownBy(() -> pagar(obligacion.id(), "orden-5", Optional.empty(), ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("PAGADO");
        assertThatThrownBy(() -> pagar(UUID.randomUUID(), "orden-6", Optional.empty(), ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no existe");
        assertThat(pasarela.consultarEstado("SIM-INEXISTENTE")).isEmpty();
    }
}
