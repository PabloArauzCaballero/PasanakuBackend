package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.QrInterno.ComprobanteQr;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H4.S1.M5 · Transferir por QR interno: el camino correcto, el reintento, el estatico reutilizable y
 * la carrera por un QR dinamico.
 */
class CU12QrTest extends BaseDeQr {

    @Test
    @DisplayName(
            "Dado un QR dinámico de Bs 50 · Cuando el pagador lo lee, confirma el importe y paga · Entonces leer no mueve nada, se paga una vez y queda el comprobante con el QR USADO · Y la transacción suma cero y toca exactamente las dos billeteras")
    void dinamicoCorrecto() {
        var qr = dinamico("50.00");
        var leido = qrs.leer(qr.contenido(), quienPaga);
        assertThat(leido.monto()).contains(bob("50.00"));
        assertThat(leido.destinatario()).startsWith("****").doesNotContain(cobra.toString());
        assertThat(total(paga)).as("leer no mueve nada").isEqualTo(1000);

        var comprobante = pagar(qr.contenido(), "50.00", "qr-1", quienPaga, paga);

        assertThat(comprobante.monto()).isEqualTo(bob("50.00"));
        assertThat(comprobante.saldoDespues()).isEqualTo(bob("950.00"));
        assertThat(total(paga)).isEqualTo(950);
        assertThat(total(cobra)).isEqualTo(50);
        assertThat(estadoDelQr(qr.qrId())).isEqualTo("USADO");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.qr_transferencia WHERE id=? AND transaccion_id=? AND usado_en IS NOT NULL",
                        qr.qrId(),
                        comprobante.transaccionId()))
                .isEqualTo(1);
        // Cuadre desde el mayor: la transaccion suma cero y toca exactamente las dos billeteras.
        assertThat(contar(
                        "SELECT COALESCE(SUM(CASE WHEN sentido='CREDITO' THEN monto ELSE -monto END),0)::int FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id=?",
                        comprobante.transaccionId()))
                .isZero();
    }

    @Test
    @DisplayName(
            "rechaza reutilizar el QR dinamico ya pagado: ni quien ya pago con otra clave ni otra persona pueden pagarlo de nuevo")
    void dinamicoReutilizado() {
        var qr = dinamico("50.00");
        pagar(qr.contenido(), "50.00", "qr-2", quienPaga, paga);

        assertThatThrownBy(() -> pagar(qr.contenido(), "50.00", "qr-2-otra-clave", quienPaga, paga))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no esta disponible");
        UUID tercero = fixtura.usuario();
        UUID cuentaTercero = fixtura.billetera(tercero, "ESTANDAR", BigDecimal.ZERO);
        fixtura.acreditar(cuentaTercero, new BigDecimal("500.00"));
        assertThatThrownBy(() -> pagar(qr.contenido(), "50.00", "qr-2-tercero", contextoDe(tercero), cuentaTercero))
                .isInstanceOf(ErrorDeNegocio.class);

        assertThat(total(cobra)).as("cobro una sola vez").isEqualTo(50);
        assertThat(total(cuentaTercero)).isEqualTo(500);
        assertThat(pagos()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "reintento: el mismo pago de QR con la misma clave devuelve el mismo comprobante sin mover plata de nuevo")
    void reintentoDelMismoPago() {
        var qr = dinamico("50.00");
        var uno = pagar(qr.contenido(), "50.00", "qr-3", quienPaga, paga);
        var otro = pagar(qr.contenido(), "50.00", "qr-3", quienPaga, paga);
        assertThat(otro.transaccionId()).isEqualTo(uno.transaccionId());
        assertThat(total(paga)).isEqualTo(950);
        assertThat(pagos()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un QR estático · Cuando se paga varias veces, cada una con su importe y su clave, y pasa el tiempo · Entonces cada pago mueve su importe, repetir una clave no cobra de nuevo, y el QR nunca se consume ni vence · Y pagar sin importe o con importe cero se rechaza")
    void estaticoSePagaMuchasVeces() {
        var qr = estatico();
        ahora.set(ahora.get().plus(Duration.ofDays(30)));

        pagar(qr.contenido(), "30.00", "est-1", quienPaga, paga);
        pagar(qr.contenido(), "20.00", "est-2", quienPaga, paga);
        pagar(qr.contenido(), "20.00", "est-2", quienPaga, paga);

        assertThat(total(cobra)).isEqualTo(50);
        assertThat(total(paga)).isEqualTo(950);
        assertThat(pagos()).isEqualTo(2);
        assertThat(estadoDelQr(qr.qrId())).isEqualTo("VIGENTE");
        assertThatThrownBy(() -> pagar(qr.contenido(), null, "est-3", quienPaga, paga))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("importe");
        assertThatThrownBy(() -> pagar(qr.contenido(), "0.00", "est-4", quienPaga, paga))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName(
            "concurrencia: dos personas pagan el mismo QR dinamico a la vez, cobra una sola y la otra no pierde plata")
    void pagoSimultaneoDelDinamico() throws Exception {
        var qr = dinamico("50.00");
        UUID otroUsuario = fixtura.usuario();
        UUID otraCuenta = fixtura.billetera(otroUsuario, "ESTANDAR", BigDecimal.ZERO);
        fixtura.acreditar(otraCuenta, new BigDecimal("500.00"));
        var otroCtx = contextoDe(otroUsuario);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<ComprobanteQr>> pagos = new ArrayList<>();
        try {
            pagos.add(pool.submit(() -> {
                salida.await();
                return pagar(qr.contenido(), "50.00", "sim-a", quienPaga, paga);
            }));
            pagos.add(pool.submit(() -> {
                salida.await();
                return pagar(qr.contenido(), "50.00", "sim-b", otroCtx, otraCuenta);
            }));
            salida.countDown();
            int exitos = 0;
            for (var pago : pagos) {
                try {
                    pago.get();
                    exitos++;
                } catch (java.util.concurrent.ExecutionException rechazado) {
                    assertThat(rechazado.getCause()).isInstanceOf(ErrorDeNegocio.class);
                }
            }
            assertThat(exitos).isEqualTo(1);
        } finally {
            pool.shutdown();
        }
        assertThat(total(cobra)).isEqualTo(50);
        assertThat(total(paga) + total(otraCuenta))
                .as("solo salio un pago de 50")
                .isEqualTo(1000 + 500 - 50);
        assertThat(pagos()).isEqualTo(1);
    }
}
