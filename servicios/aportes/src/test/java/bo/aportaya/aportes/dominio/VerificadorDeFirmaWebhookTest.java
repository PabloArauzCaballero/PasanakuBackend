package bo.aportaya.aportes.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Atomo puro: sin PostgreSQL, sin Spring. Corre bajo el corredor `test`. */
class VerificadorDeFirmaWebhookTest {

    private static final String TS = "1767268800";
    private static final String VECTOR = "f4adddc72cc116116abb8145a9a4850e2e7cca03209a46f80f60d60e300106ba";

    @Test
    @DisplayName("F1.L1.M06 · el material firmado es timestamp, un punto y el cuerpo: vector fijo")
    void vectorFijo() {
        // Calculado aparte: printf '1767268800.{"a":1}' | openssl dgst -sha256 -hmac secreto
        assertThat(VerificadorDeFirmaWebhook.material(TS, "{\"a\":1}")).isEqualTo("1767268800.{\"a\":1}");
        assertThat(VerificadorDeFirmaWebhook.firmar(TS, "{\"a\":1}", "secreto")).isEqualTo(VECTOR);
    }

    @Test
    @DisplayName("F1.L1.M06 · un webhook capturado y reenviado con otro X-Timestamp ya no verifica")
    void reenvioConTimestampNuevo() {
        String firma = VerificadorDeFirmaWebhook.firmar(TS, "{\"a\":1}", "secreto");
        assertThat(VerificadorDeFirmaWebhook.firmaValida("1767269100", "{\"a\":1}", firma, "secreto"))
                .isFalse();
    }

    @Test
    @DisplayName("F1.L1.M06 · sin timestamp no hay firma valida")
    void timestampAusente() {
        String firma = VerificadorDeFirmaWebhook.firmar(TS, "{\"a\":1}", "secreto");
        assertThat(VerificadorDeFirmaWebhook.firmaValida(null, "{\"a\":1}", firma, "secreto"))
                .isFalse();
        assertThat(VerificadorDeFirmaWebhook.firmaValida(" ", "{\"a\":1}", firma, "secreto"))
                .isFalse();
    }

    @Test
    @DisplayName("F1.L1.M06 · la firma vieja (solo el cuerpo) ya no verifica")
    void firmaSoloDelCuerpoYaNoSirve() throws Exception {
        var mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(
                "secreto".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        String soloCuerpo = java.util.HexFormat.of()
                .formatHex(mac.doFinal("{\"a\":1}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(VerificadorDeFirmaWebhook.firmaValida(TS, "{\"a\":1}", soloCuerpo, "secreto"))
                .isFalse();
    }

    @Test
    @DisplayName("la firma correcta, con el mismo secreto, valida")
    void firmaCorrecta() {
        String firma = VerificadorDeFirmaWebhook.firmar(TS, "{\"a\":1}", "secreto");
        assertThat(VerificadorDeFirmaWebhook.firmaValida(TS, "{\"a\":1}", firma, "secreto"))
                .isTrue();
    }

    @Test
    @DisplayName("un solo caracter distinto en el cuerpo invalida la firma")
    void cuerpoAlterado() {
        String firma = VerificadorDeFirmaWebhook.firmar(TS, "{\"a\":1}", "secreto");
        assertThat(VerificadorDeFirmaWebhook.firmaValida(TS, "{\"a\":2}", firma, "secreto"))
                .isFalse();
    }

    @Test
    @DisplayName("el secreto equivocado invalida la firma, aunque el cuerpo sea el mismo")
    void secretoEquivocado() {
        String firma = VerificadorDeFirmaWebhook.firmar(TS, "{\"a\":1}", "secreto");
        assertThat(VerificadorDeFirmaWebhook.firmaValida(TS, "{\"a\":1}", firma, "otro-secreto"))
                .isFalse();
    }

    @Test
    @DisplayName("la comparacion no distingue mayusculas del hexadecimal")
    void hexadecimalEnMayusculas() {
        String firma = VerificadorDeFirmaWebhook.firmar(TS, "{\"a\":1}", "secreto");
        assertThat(VerificadorDeFirmaWebhook.firmaValida(
                        TS, "{\"a\":1}", firma.toUpperCase(java.util.Locale.ROOT), "secreto"))
                .isTrue();
    }

    @Test
    @DisplayName("firma nula o en blanco nunca es valida")
    void firmaAusente() {
        assertThat(VerificadorDeFirmaWebhook.firmaValida(TS, "{\"a\":1}", null, "secreto"))
                .isFalse();
        assertThat(VerificadorDeFirmaWebhook.firmaValida(TS, "{\"a\":1}", "   ", "secreto"))
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
