package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela;
import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela.Entrada;
import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela.Resultado;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.dominio.VerificadorDeFirmaWebhook;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.aportes.infraestructura.ProveedorPagoRepositorio;
import bo.aportaya.aportes.infraestructura.WebhookRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-100 · el webhook de la pasarela en tres niveles (H1.S2 del carril PR4-seguridad,
 * Q-02 DECIDIDA 2026-09-21): correcto, al borde de la ventana, e invalido (firma,
 * ventana, duplicado, monto, proveedor inexistente, pago inexistente).
 *
 * <p>El doble de la pasarela ES este mismo test: firma con
 * {@link VerificadorDeFirmaWebhook#firmar} el mismo cuerpo que manda, con el mismo
 * secreto que {@link BaseDeAportes#SECRETO_DE_PRUEBA} — asi se prueba el verificador
 * real contra una firma real, no contra un mock que "siempre dice que si".
 */
class CU100WebhookTest extends BaseDeAportes {

    private static final Instant AHORA_DE_BORDE = Instant.parse("2026-09-23T12:00:00Z");

    @AfterEach
    void limpiar() {
        dsl.deleteFrom(org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("aportes", "webhook_pasarela")))
                .execute();
        fixtura.limpiar();
    }

    private String cuerpo(String idEvento, String referencia, String monto, String moneda) {
        return """
                {"evento":"pago.acreditado","idEvento":"%s","referenciaProveedor":"%s","monto":"%s","moneda":"%s"}"""
                .formatted(idEvento, referencia, monto, moneda);
    }

    /** Un pago real, registrado por CU-21, con una referencia de proveedor conocida. */
    private UUID pagoConReferencia(String referencia, String monto) {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, monto, 10);
        ContextoSesion ctx = ContextoSesion.de(
                usuario,
                "PARTICIPANTE",
                new bo.aportaya.plataforma.dominio.Traza(UUID.randomUUID().toString()));
        var salida = transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        "cob-" + referencia,
                        obligacion.id(),
                        Dinero.de(monto, Moneda.BOB),
                        Dinero.de("0.00", Moneda.BOB),
                        "QR_INTEROPERABLE",
                        referencia,
                        Optional.empty(),
                        false,
                        true),
                ctx));
        return salida.pagoId();
    }

    private Resultado recibir(String proveedorCodigo, String cuerpo, String firma, long timestampEpochSegundos) {
        return transaccion.execute(t ->
                webhookCU.recibir(new Entrada(proveedorCodigo, cuerpo, firma, String.valueOf(timestampEpochSegundos))));
    }

    private Resultado recibirEnInstanteFijo(
            String proveedorCodigo, String cuerpo, String firma, long timestampEpochSegundos) {
        var casoDeUso = new CU100RecibirWebhookPasarela(
                new Datos(dsl),
                new ProveedorPagoRepositorio(),
                new WebhookRepositorio(),
                new PagoRepositorio(),
                codigo -> Optional.of(SECRETO_DE_PRUEBA),
                Reloj.fijo(AHORA_DE_BORDE),
                new ObjectMapper());
        return transaccion.execute(t ->
                casoDeUso.recibir(new Entrada(proveedorCodigo, cuerpo, firma, String.valueOf(timestampEpochSegundos))));
    }

    // ---------------------------------------------------------------- correcto --

    @Test
    @DisplayName("correcto: firma valida + ventana vigente + monto igual al pago -> PROCESADO")
    void firmaValidaProcesaYConcilia() {
        fixtura.proveedor("QR_TEST", false, 1);
        UUID pagoId = pagoConReferencia("ref-ok-1", "500.00");
        String cuerpo = cuerpo("evt-ok-1", "ref-ok-1", "500.00", "BOB");
        String firma = VerificadorDeFirmaWebhook.firmar(cuerpo, SECRETO_DE_PRUEBA);

        Resultado r = recibir("QR_TEST", cuerpo, firma, Instant.now().getEpochSecond());

        assertThat(r.estado()).isEqualTo(Resultado.Estado.PROCESADO);
        assertThat(r.pagoId()).isEqualTo(pagoId);
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.webhook_pasarela WHERE estado = 'PROCESADO' AND pago_id = ?",
                        pagoId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("correcto: el mismo evento entregado dos veces es DUPLICADO — un solo pago, una sola fila")
    void eventoRepetidoEsIdempotente() {
        fixtura.proveedor("QR_TEST", false, 1);
        UUID pagoId = pagoConReferencia("ref-dup-1", "300.00");
        String cuerpo = cuerpo("evt-dup-1", "ref-dup-1", "300.00", "BOB");
        String firma = VerificadorDeFirmaWebhook.firmar(cuerpo, SECRETO_DE_PRUEBA);
        long ts = Instant.now().getEpochSecond();

        Resultado primera = recibir("QR_TEST", cuerpo, firma, ts);
        Resultado segunda = recibir("QR_TEST", cuerpo, firma, ts);

        assertThat(primera.estado()).isEqualTo(Resultado.Estado.PROCESADO);
        assertThat(segunda.estado()).isEqualTo(Resultado.Estado.DUPLICADO);
        assertThat(segunda.pagoId()).isEqualTo(pagoId);
        assertThat(contar("SELECT count(*)::int FROM aportes.webhook_pasarela WHERE clave_idempotencia = 'evt-dup-1'"))
                .isEqualTo(1);
    }

    // -------------------------------------------------------------- al borde --

    @Test
    @DisplayName("limite: timestamp a 4 minutos 59 segundos (dentro de +-5 min) -> PROCESADO")
    void timestampAlBordeDeLaVentanaDentroSeAcepta() {
        fixtura.proveedor("QR_TEST", false, 1);
        pagoConReferencia("ref-borde-ok", "150.00");
        String cuerpo = cuerpo("evt-borde-ok", "ref-borde-ok", "150.00", "BOB");
        String firma = VerificadorDeFirmaWebhook.firmar(cuerpo, SECRETO_DE_PRUEBA);
        long ts = AHORA_DE_BORDE.minusSeconds(4 * 60 + 59).getEpochSecond();

        Resultado r = recibirEnInstanteFijo("QR_TEST", cuerpo, firma, ts);

        assertThat(r.estado()).isEqualTo(Resultado.Estado.PROCESADO);
    }

    @Test
    @DisplayName("limite: timestamp a 5 minutos 1 segundo (fuera de +-5 min) -> firma invalida, nada escrito")
    void timestampJustoFueraDeLaVentanaSeRechaza() {
        fixtura.proveedor("QR_TEST", false, 1);
        pagoConReferencia("ref-borde-mal", "150.00");
        String cuerpo = cuerpo("evt-borde-mal", "ref-borde-mal", "150.00", "BOB");
        String firma = VerificadorDeFirmaWebhook.firmar(cuerpo, SECRETO_DE_PRUEBA);
        long ts = AHORA_DE_BORDE.minusSeconds(5 * 60 + 1).getEpochSecond();

        Resultado r = recibirEnInstanteFijo("QR_TEST", cuerpo, firma, ts);

        assertThat(r.estado()).isEqualTo(Resultado.Estado.FIRMA_INVALIDA);
        assertThat(
                        contar(
                                "SELECT count(*)::int FROM aportes.webhook_pasarela WHERE clave_idempotencia = 'evt-borde-mal'"))
                .isZero();
    }

    // ---------------------------------------------------------------- invalido --

    @Test
    @DisplayName("invalido: firma que no corresponde al cuerpo -> firma invalida, nada escrito")
    void firmaInvalidaSeRechazaSinEscribirNada() {
        fixtura.proveedor("QR_TEST", false, 1);
        pagoConReferencia("ref-firma-mala", "200.00");
        String cuerpo = cuerpo("evt-firma-mala", "ref-firma-mala", "200.00", "BOB");
        String firmaAjena = VerificadorDeFirmaWebhook.firmar(cuerpo, "otro-secreto-que-no-es-el-configurado");

        Resultado r = recibir("QR_TEST", cuerpo, firmaAjena, Instant.now().getEpochSecond());

        assertThat(r.estado()).isEqualTo(Resultado.Estado.FIRMA_INVALIDA);
        assertThat(
                        contar(
                                "SELECT count(*)::int FROM aportes.webhook_pasarela WHERE clave_idempotencia = 'evt-firma-mala'"))
                .isZero();
    }

    @Test
    @DisplayName("invalido: proveedor inexistente o sin soporte de webhook -> mismo rechazo que firma invalida")
    void proveedorInexistenteSeRechaza() {
        String cuerpo = cuerpo("evt-sin-proveedor", "ref-x", "100.00", "BOB");
        String firma = VerificadorDeFirmaWebhook.firmar(cuerpo, SECRETO_DE_PRUEBA);

        Resultado r =
                recibir("PROVEEDOR_QUE_NO_EXISTE", cuerpo, firma, Instant.now().getEpochSecond());

        assertThat(r.estado()).isEqualTo(Resultado.Estado.FIRMA_INVALIDA);
    }

    @Test
    @DisplayName("invalido: firma valida pero el monto del webhook no coincide con el pago -> DESCARTADO, no acredita")
    void montoDistintoSeDescartaSinAcreditar() {
        fixtura.proveedor("QR_TEST", false, 1);
        pagoConReferencia("ref-monto-mal", "500.00");
        // La pasarela dice 999.00; el pago registrado es de 500.00. No se acredita "lo
        // que vino" (regla 91.3.2 / payments-qr-integration §4.3).
        String cuerpo = cuerpo("evt-monto-mal", "ref-monto-mal", "999.00", "BOB");
        String firma = VerificadorDeFirmaWebhook.firmar(cuerpo, SECRETO_DE_PRUEBA);

        Resultado r = recibir("QR_TEST", cuerpo, firma, Instant.now().getEpochSecond());

        assertThat(r.estado()).isEqualTo(Resultado.Estado.DESCARTADO);
        assertThat(r.motivo()).contains("no coincide");
    }

    @Test
    @DisplayName("invalido: firma valida pero ningun pago tiene esa referencia de proveedor -> DESCARTADO")
    void sinPagoParaLaReferenciaSeDescarta() {
        fixtura.proveedor("QR_TEST", false, 1);
        String cuerpo = cuerpo("evt-sin-pago", "ref-que-no-existe", "500.00", "BOB");
        String firma = VerificadorDeFirmaWebhook.firmar(cuerpo, SECRETO_DE_PRUEBA);

        Resultado r = recibir("QR_TEST", cuerpo, firma, Instant.now().getEpochSecond());

        assertThat(r.estado()).isEqualTo(Resultado.Estado.DESCARTADO);
        assertThat(r.motivo()).contains("referencia");
    }
}
