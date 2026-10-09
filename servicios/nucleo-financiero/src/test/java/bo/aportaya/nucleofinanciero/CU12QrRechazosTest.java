package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.QrInterno.EntradaQr;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H4.S1.M5 y M6 · Lo que el QR interno rechaza (vencido, alterado, reutilizado, sin confirmar) y el QR
 * bancario, que no se afirma como aceptado ni se procesa como interno.
 */
class CU12QrRechazosTest extends BaseDeQr {

    @Test
    @DisplayName("rechaza el QR dinamico vencido: no se lee ni se paga, y no se mueve nada")
    void dinamicoVencido() {
        var qr = dinamico("50.00");
        ahora.set(ahora.get().plus(Duration.ofMinutes(16)));

        assertThatThrownBy(() -> qrs.leer(qr.contenido(), quienPaga)).hasMessageContaining("vencio");
        assertThatThrownBy(() -> pagar(qr.contenido(), "50.00", "qr-4", quienPaga, paga))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("vencio");

        assertThat(total(paga)).isEqualTo(1000);
        assertThat(total(cobra)).isZero();
        assertThat(estadoDelQr(qr.qrId())).isEqualTo("VIGENTE");
    }

    @Test
    @DisplayName(
            "rechaza el QR alterado: cambiar un caracter del identificador o de la firma, o recortarlo, lo invalida")
    void qrAlterado() {
        var qr = dinamico("50.00");
        String[] partes = qr.contenido().split("\\.");
        String firmaDeOtro = dinamico("5000.00").contenido().split("\\.")[2];
        List<String> alterados = List.of(
                ensuciar(qr.contenido()),
                "PSNK1." + ensuciar(partes[1]) + "." + partes[2],
                "PSNK1." + partes[1] + "." + firmaDeOtro,
                qr.contenido().substring(0, qr.contenido().length() - 1),
                qr.contenido() + "x",
                "PSNK2." + partes[1] + "." + partes[2],
                "psnk1." + partes[1] + "." + partes[2],
                "PSNK1.." + partes[2]);
        for (String alterado : alterados) {
            assertThatThrownBy(() -> qrs.leer(alterado, quienPaga))
                    .as("leer «%s»", alterado)
                    .isInstanceOf(ErrorDeNegocio.class)
                    .hasMessageContaining("no es valido");
            assertThatThrownBy(() -> pagar(alterado, "50.00", "qr-5", quienPaga, paga))
                    .isInstanceOf(ErrorDeNegocio.class);
        }
        assertThat(total(paga)).isEqualTo(1000);
        assertThat(pagos()).isZero();
        // El original sigue intacto: alterar una copia no daña el cobro.
        assertThat(pagar(qr.contenido(), "50.00", "qr-5-ok", quienPaga, paga).monto())
                .isEqualTo(bob("50.00"));
    }

    @Test
    @DisplayName("rechaza pagar el QR dinamico con otro importe o sin importe: no mueve nada")
    void importeNoConfirmado() {
        var qr = dinamico("50.00");
        for (String importe : new String[] {"49.99", "500.00", null}) {
            assertThatThrownBy(() -> pagar(qr.contenido(), importe, "qr-6-" + importe, quienPaga, paga))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .hasMessageContaining("importe");
        }
        assertThat(total(paga)).isEqualTo(1000);
        assertThat(estadoDelQr(qr.qrId())).isEqualTo("VIGENTE");
    }

    @Test
    // Traza de la prueba original: M6
    @DisplayName("rechaza el QR bancario: no se afirma como aceptado ni se procesa como interno")
    void qrInteroperableNoSeProcesa() {
        String bancario = "00020101021226280012ejemplo-sin-proveedor5204000053030685802BO6304ABCD";
        assertThatThrownBy(() -> qrs.leer(bancario, quienPaga))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("otro sistema de pagos");
        assertThatThrownBy(() -> pagar(bancario, "50.00", "qr-7", quienPaga, paga))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("otro sistema de pagos");
        for (String basura : new String[] {"", "   ", "hola", "https://example.com/pago", "PSNK1."}) {
            assertThatThrownBy(() -> qrs.leer(basura, quienPaga)).isInstanceOf(ErrorDeNegocio.class);
        }
        assertThat(total(paga)).isEqualTo(1000);
        assertThat(pagos()).isZero();
    }

    @Test
    @DisplayName(
            "rechaza emitir un QR invalido: solo el titular, dinamico con importe positivo en la moneda de la cuenta, estatico sin importe")
    void emisionInvalida() {
        assertThatThrownBy(() -> qrs.emitir(
                        new EntradaQr(cobra, "DINAMICO", Optional.of(bob("50.00")), Optional.empty()), quienPaga))
                .isInstanceOf(ErrorDeNegocio.class);
        for (var malo : List.of(
                new EntradaQr(cobra, "DINAMICO", Optional.empty(), Optional.empty()),
                new EntradaQr(cobra, "DINAMICO", Optional.of(bob("0.00")), Optional.empty()),
                new EntradaQr(cobra, "DINAMICO", Optional.of(Dinero.de("5.00", Moneda.USD)), Optional.empty()),
                new EntradaQr(cobra, "ESTATICO", Optional.of(bob("5.00")), Optional.empty()),
                new EntradaQr(cobra, "OTRA", Optional.empty(), Optional.empty()),
                new EntradaQr(cobra, "ESTATICO", Optional.empty(), Optional.of("x".repeat(141))))) {
            assertThatThrownBy(() -> qrs.emitir(malo, quienCobra)).isInstanceOf(ErrorDeNegocio.class);
        }
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.qr_transferencia WHERE cuenta_billetera_id=?",
                        cobra))
                .isZero();
    }

    @Test
    @DisplayName("rechaza que alguien se pague un QR a si mismo")
    void noSePagaASiMismo() {
        var qr = dinamico("50.00");
        assertThatThrownBy(() -> pagar(qr.contenido(), "50.00", "qr-8", quienCobra, cobra))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(estadoDelQr(qr.qrId())).isEqualTo("VIGENTE");
    }

    @Test
    @DisplayName("rechaza por R-BIL-21")
    void rechazaRBIL21() {
        // El dinamico fija importe y vencimiento, el estatico no fija importe, y "usado" solo se dice con la
        // transaccion que lo salda: lo rechaza la BASE aunque la aplicacion se equivoque.
        String dinamicoSinImporte =
                """
                INSERT INTO nucleo_financiero.qr_transferencia
                    (cuenta_billetera_id, modalidad, monto, moneda, estado, expira_en)
                VALUES ('%s', 'DINAMICO', NULL, 'BOB', 'VIGENTE', now() + interval '10 minutes')
                """
                        .formatted(cobra);
        String usadoSinTransaccion =
                """
                INSERT INTO nucleo_financiero.qr_transferencia
                    (cuenta_billetera_id, modalidad, monto, moneda, estado, expira_en, usado_en)
                VALUES ('%s', 'DINAMICO', 50.00, 'BOB', 'USADO', now() + interval '10 minutes', now())
                """
                        .formatted(cobra);
        String estaticoUsado =
                """
                INSERT INTO nucleo_financiero.qr_transferencia
                    (cuenta_billetera_id, modalidad, monto, moneda, estado)
                VALUES ('%s', 'ESTATICO', NULL, 'BOB', 'USADO')
                """
                        .formatted(cobra);

        assertThat(rechazaLaBase(dinamicoSinImporte)).contains("ck_qr_modalidad");
        assertThat(rechazaLaBase(usadoSinTransaccion)).contains("ck_qr_uso");
        assertThat(rechazaLaBase(estaticoUsado)).contains("ck_qr_");
    }
}
