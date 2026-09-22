package bo.aportaya.aportes.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Atomo puro: sin PostgreSQL, sin Spring. Corre bajo el corredor `test`. */
class VerificadorDeFirmaWebhookTest {

    @Test
    @DisplayName("la firma correcta, con el mismo secreto, valida")
    void firmaCorrecta() {
        String firma = VerificadorDeFirmaWebhook.firmar("{\"a\":1}", "secreto");
        assertThat(VerificadorDeFirmaWebhook.firmaValida("{\"a\":1}", firma, "secreto"))
                .isTrue();
    }

    @Test
    @DisplayName("un solo caracter distinto en el cuerpo invalida la firma")
    void cuerpoAlterado() {
        String firma = VerificadorDeFirmaWebhook.firmar("{\"a\":1}", "secreto");
        assertThat(VerificadorDeFirmaWebhook.firmaValida("{\"a\":2}", firma, "secreto"))
                .isFalse();
    }

    @Test
    @DisplayName("el secreto equivocado invalida la firma, aunque el cuerpo sea el mismo")
    void secretoEquivocado() {
        String firma = VerificadorDeFirmaWebhook.firmar("{\"a\":1}", "secreto");
        assertThat(VerificadorDeFirmaWebhook.firmaValida("{\"a\":1}", firma, "otro-secreto"))
                .isFalse();
    }

    @Test
    @DisplayName("la comparacion no distingue mayusculas del hexadecimal")
    void hexadecimalEnMayusculas() {
        String firma = VerificadorDeFirmaWebhook.firmar("{\"a\":1}", "secreto");
        assertThat(VerificadorDeFirmaWebhook.firmaValida(
                        "{\"a\":1}", firma.toUpperCase(java.util.Locale.ROOT), "secreto"))
                .isTrue();
    }

    @Test
    @DisplayName("firma nula o en blanco nunca es valida")
    void firmaAusente() {
        assertThat(VerificadorDeFirmaWebhook.firmaValida("{\"a\":1}", null, "secreto"))
                .isFalse();
        assertThat(VerificadorDeFirmaWebhook.firmaValida("{\"a\":1}", "   ", "secreto"))
                .isFalse();
    }

    @Test
    @DisplayName("ventana: exactamente 5 minutos (borde inclusivo) todavia es valida")
    void ventanaBordeInclusive() {
        Instant ahora = Instant.parse("2026-01-01T12:05:00Z");
        long firmadoHaceCincoMinutos = ahora.minusSeconds(300).getEpochSecond();
        assertThat(VerificadorDeFirmaWebhook.dentroDeVentana(firmadoHaceCincoMinutos, ahora))
                .isTrue();
    }

    @Test
    @DisplayName("ventana: 5 minutos y 1 segundo ya esta fuera")
    void ventanaUnSegundoFuera() {
        Instant ahora = Instant.parse("2026-01-01T12:05:01Z");
        long firmadoHaceCincoMinutosUnSegundo = ahora.minusSeconds(301).getEpochSecond();
        assertThat(VerificadorDeFirmaWebhook.dentroDeVentana(firmadoHaceCincoMinutosUnSegundo, ahora))
                .isFalse();
    }

    @Test
    @DisplayName("ventana: un timestamp del FUTURO (reloj adelantado de la pasarela) tambien se acota")
    void ventanaFuturo() {
        Instant ahora = Instant.parse("2026-01-01T12:00:00Z");
        long firmadoEnElFuturo = ahora.plusSeconds(301).getEpochSecond();
        assertThat(VerificadorDeFirmaWebhook.dentroDeVentana(firmadoEnElFuturo, ahora))
                .isFalse();
    }
}
