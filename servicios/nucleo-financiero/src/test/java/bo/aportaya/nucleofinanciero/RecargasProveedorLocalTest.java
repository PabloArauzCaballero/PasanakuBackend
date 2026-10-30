package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas.CobroQr;
import bo.aportaya.nucleofinanciero.infraestructura.RecargasProveedorLocal;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** El doble de prueba del proveedor de recargas, en los tres niveles de su contrato (regla 65). */
class RecargasProveedorLocalTest {

    private final RecargasProveedorLocal proveedor = new RecargasProveedorLocal();
    private final UUID orden = UUID.randomUUID();

    @Nested
    class Correcto {

        @Test
        void emiteUnQrDePruebaQueNombraLaOrdenYElMonto() {
            var qr = proveedor.solicitar(orden, Dinero.de("150.00", Moneda.BOB)).orElseThrow();

            assertThat(qr.origen()).isEqualTo("PRUEBA");
            assertThat(qr.contenido())
                    .startsWith("PASANAKU-PRUEBA|NO-PAGABLE|")
                    .contains("ref=" + orden)
                    .contains("monto=150.00|BOB");
        }

        @Test
        void pedirloDosVecesDevuelveElMismoQr() {
            var primero = proveedor.solicitar(orden, Dinero.de("150.00", Moneda.BOB));
            var segundo = proveedor.solicitar(orden, Dinero.de("150.00", Moneda.BOB));

            assertThat(segundo).isEqualTo(primero);
        }

        @Test
        void quedaPendienteHastaQueSeSimulaElPago() {
            proveedor.solicitar(orden, Dinero.de("150.00", Moneda.BOB));
            assertThat(proveedor.consultar(orden).estado()).isEqualTo("PENDIENTE");

            proveedor.marcarPagado(orden, Dinero.de("150.00", Moneda.BOB));

            var recibo = proveedor.consultar(orden);
            assertThat(recibo.estado()).isEqualTo("CONFIRMADO");
            assertThat(recibo.referencia()).isEqualTo(orden);
            assertThat(recibo.monto()).isEqualTo(Dinero.de("150.00", Moneda.BOB));
            assertThat(recibo.transaccionProveedor()).isNotNull();
            assertThat(recibo.liquidadaEn()).isNotNull();
        }
    }

    @Nested
    class Limite {

        @Test
        void elMontoMinimoTambienTieneQr() {
            var qr = proveedor.solicitar(orden, Dinero.de("0.01", Moneda.BOB)).orElseThrow();

            assertThat(qr.contenido()).contains("monto=0.01|BOB");
        }

        @Test
        void pagarDosVecesNoCreaUnaSegundaTransaccion() {
            proveedor.solicitar(orden, Dinero.de("150.00", Moneda.BOB));
            proveedor.marcarPagado(orden, Dinero.de("150.00", Moneda.BOB));
            var primera = proveedor.consultar(orden);

            proveedor.marcarPagado(orden, Dinero.de("150.00", Moneda.BOB));

            assertThat(proveedor.consultar(orden)).isEqualTo(primera);
        }

        @Test
        void trasUnReinicioElPagoVuelveARegistrarLaReferencia() {
            var reiniciado = new RecargasProveedorLocal();

            reiniciado.marcarPagado(orden, Dinero.de("150.00", Moneda.BOB));

            assertThat(reiniciado.consultar(orden).estado()).isEqualTo("CONFIRMADO");
        }
    }

    @Nested
    class Invalido {

        @Test
        void unaReferenciaDesconocidaNoEstaConfirmada() {
            assertThatThrownBy(() -> proveedor.consultar(UUID.randomUUID())).isInstanceOf(ErrorDeNegocio.class);
        }

        @Test
        void noEmiteQrPorCeroNiPorUnMontoNegativo() {
            assertThatThrownBy(() -> proveedor.solicitar(orden, Dinero.de("0.00", Moneda.BOB)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> proveedor.marcarPagado(orden, Dinero.de("-1.00", Moneda.BOB)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void unQrSinContenidoDemasiadoLargoODeOrigenDesconocidoNoExiste() {
            assertThatThrownBy(() -> new CobroQr(" ", "PRUEBA")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CobroQr("x".repeat(CobroQr.LARGO_MAXIMO + 1), "BANCO"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CobroQr("contenido", "OTRO")).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
