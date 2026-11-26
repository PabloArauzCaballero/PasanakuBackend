package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela;
import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela.Entrada;
import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela.Resultado;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.dominio.PasarelaDePagos.OrdenDeCobro;
import bo.aportaya.aportes.dominio.PasarelaDePagos.SolicitudDeCobro;
import bo.aportaya.aportes.dominio.SimuladorDePagoDePasarela.WebhookFirmado;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.aportes.infraestructura.ProveedorPagoRepositorio;
import bo.aportaya.aportes.infraestructura.WebhookRepositorio;
import bo.aportaya.aportes.infraestructura.pasarela.simulada.InterpreteDeWebhookJson;
import bo.aportaya.aportes.infraestructura.pasarela.simulada.PasarelaSimulada;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Punta a punta contra una base real: la pasarela SIMULADA emite el aviso firmado y CU-100 REAL lo verifica, deduplica
 * y concilia (regla 65, tres niveles). Verifica el flujo contra el DOBLE: no prueba ningun proveedor.
 *
 * <p>Usa el secreto de {@link BaseDeAportes#SECRETO_DE_PRUEBA}, el mismo con el que CU-100 verifica: el doble firma con
 * el secreto del entorno y no necesita ninguno de la pasarela.
 */
class PasarelaSimuladaFlujoTest extends BaseDeAportes {

    private static final String PROVEEDOR = "QR_TEST";
    private static final Instant AHORA = Instant.parse("2026-11-26T12:00:00Z");

    private PasarelaSimulada pasarela;

    @BeforeEach
    void preparar() {
        pasarela = new PasarelaSimulada(
                codigo -> Optional.of(SECRETO_DE_PRUEBA), Reloj.delSistema(), new ObjectMapper(), PROVEEDOR);
        fixtura.proveedor(PROVEEDOR, false, 1);
    }

    @AfterEach
    void limpiar() {
        dsl.deleteFrom(org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("aportes", "webhook_pasarela")))
                .execute();
        fixtura.limpiar();
    }

    /** La orden sale de la pasarela simulada; el pago lo registra CU-21 con la referencia que esa orden dio. */
    private UUID pagoDe(OrdenDeCobro orden) {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, orden.monto().monto().toPlainString(), 10);
        ContextoSesion ctx = contextoDe(usuario);
        return transaccion
                .execute(t -> cobroCU.acreditar(
                        new EntradaCobro(
                                "cob-" + orden.referenciaProveedor(),
                                obligacion.id(),
                                orden.monto(),
                                Dinero.de("0.00", Moneda.BOB),
                                "QR_INTEROPERABLE",
                                orden.referenciaProveedor(),
                                Optional.empty(),
                                false,
                                true),
                        ctx))
                .pagoId();
    }

    private Resultado recibir(WebhookFirmado aviso) {
        return recibir(webhookCU, aviso.cuerpoCrudo(), aviso.firma(), aviso.timestamp());
    }

    private Resultado recibir(CU100RecibirWebhookPasarela cu, String cuerpo, String firma, String timestamp) {
        return transaccion.execute(t -> cu.recibir(new Entrada(PROVEEDOR, cuerpo, firma, timestamp)));
    }

    private int filas(String idEvento) {
        return contar("SELECT count(*)::int FROM aportes.webhook_pasarela WHERE clave_idempotencia = ?", idEvento);
    }

    @Test
    @DisplayName("correcto: orden simulada -> pago -> aviso firmado -> CU-100 PROCESADO sobre ese pago")
    void flujoCompleto() {
        OrdenDeCobro orden =
                pasarela.crearOrdenDeCobro(new SolicitudDeCobro("flujo-1", Dinero.de("500.00", Moneda.BOB)));
        UUID pagoId = pagoDe(orden);

        Resultado r = recibir(pasarela.emitirWebhookDePago(orden.referenciaProveedor()));

        assertThat(r.estado()).isEqualTo(Resultado.Estado.PROCESADO);
        assertThat(r.pagoId()).isEqualTo(pagoId);
        assertThat(filas("sim-evt-" + orden.referenciaProveedor())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "limite: el MISMO aviso dos veces -> segundo DUPLICADO (mismo webhook y pago), una sola fila, un solo pago")
    void mismoEventoDosVeces() {
        OrdenDeCobro orden =
                pasarela.crearOrdenDeCobro(new SolicitudDeCobro("flujo-2", Dinero.de("300.00", Moneda.BOB)));
        UUID pagoId = pagoDe(orden);
        WebhookFirmado aviso = pasarela.emitirWebhookDePago(orden.referenciaProveedor());

        Resultado primera = recibir(aviso);
        Resultado segunda = recibir(aviso);

        assertThat(primera.estado()).isEqualTo(Resultado.Estado.PROCESADO);
        assertThat(segunda.estado()).isEqualTo(Resultado.Estado.DUPLICADO);
        assertThat(segunda.webhookId()).isEqualTo(primera.webhookId());
        assertThat(segunda.pagoId()).isEqualTo(pagoId);
        assertThat(filas("sim-evt-" + orden.referenciaProveedor())).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.pago WHERE referencia_proveedor = ?",
                        orden.referenciaProveedor()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("limite: timestamp a 5 min exactos se acepta y a 5 min 1 s se rechaza (reloj de CU-100 fijo)")
    void timestampAlBorde() {
        OrdenDeCobro dentro =
                pasarela.crearOrdenDeCobro(new SolicitudDeCobro("flujo-3a", Dinero.de("10.00", Moneda.BOB)));
        OrdenDeCobro fuera =
                pasarela.crearOrdenDeCobro(new SolicitudDeCobro("flujo-3b", Dinero.de("10.00", Moneda.BOB)));
        pagoDe(dentro);
        pagoDe(fuera);
        // la pasarela firma con el reloj de CU-100 desplazado: emite en AHORA, CU-100 recibe en AHORA + 300 s / + 301 s
        var emisora = new PasarelaSimulada(
                codigo -> Optional.of(SECRETO_DE_PRUEBA), Reloj.fijo(AHORA), new ObjectMapper(), PROVEEDOR);
        emisora.crearOrdenDeCobro(new SolicitudDeCobro("flujo-3a", Dinero.de("10.00", Moneda.BOB)));
        emisora.crearOrdenDeCobro(new SolicitudDeCobro("flujo-3b", Dinero.de("10.00", Moneda.BOB)));
        WebhookFirmado avisoDentro = emisora.emitirWebhookDePago(dentro.referenciaProveedor());
        WebhookFirmado avisoFuera = emisora.emitirWebhookDePago(fuera.referenciaProveedor());

        assertThat(recibir(
                                cuEn(AHORA.plusSeconds(300)),
                                avisoDentro.cuerpoCrudo(),
                                avisoDentro.firma(),
                                avisoDentro.timestamp())
                        .estado())
                .isEqualTo(Resultado.Estado.PROCESADO);
        assertThat(recibir(
                                cuEn(AHORA.plusSeconds(301)),
                                avisoFuera.cuerpoCrudo(),
                                avisoFuera.firma(),
                                avisoFuera.timestamp())
                        .estado())
                .isEqualTo(Resultado.Estado.FIRMA_INVALIDA);
        assertThat(filas("sim-evt-" + fuera.referenciaProveedor())).isZero();
    }

    @Test
    @DisplayName("invalido: monto distinto al del pago -> DESCARTADO, el pago no se concilia")
    void montoDistinto() {
        OrdenDeCobro orden =
                pasarela.crearOrdenDeCobro(new SolicitudDeCobro("flujo-4", Dinero.de("200.00", Moneda.BOB)));
        pagoDe(orden);

        Resultado r = recibir(pasarela.emitirWebhookDePago(
                orden.referenciaProveedor(), "evt-monto", Dinero.de("200.01", Moneda.BOB)));

        assertThat(r.estado()).isEqualTo(Resultado.Estado.DESCARTADO);
        assertThat(r.motivo()).contains("monto");
        assertThat(
                        contar(
                                "SELECT count(*)::int FROM aportes.webhook_pasarela WHERE estado = 'PROCESADO' AND clave_idempotencia = 'evt-monto'"))
                .isZero();
    }

    @Test
    @DisplayName("invalido: referencia de una orden que no existe como pago -> DESCARTADO")
    void ordenInexistente() {
        Resultado r =
                recibir(pasarela.emitirWebhookDePago("SIM-FANTASMA", "evt-fantasma", Dinero.de("5.00", Moneda.BOB)));

        assertThat(r.estado()).isEqualTo(Resultado.Estado.DESCARTADO);
        assertThat(r.motivo()).contains("referencia");
    }

    @Test
    @DisplayName("invalido: firma adulterada, o firmada con otro secreto -> FIRMA_INVALIDA y no se escribe nada")
    void firmaInvalida() {
        OrdenDeCobro orden =
                pasarela.crearOrdenDeCobro(new SolicitudDeCobro("flujo-5", Dinero.de("50.00", Moneda.BOB)));
        pagoDe(orden);
        WebhookFirmado aviso = pasarela.emitirWebhookDePago(orden.referenciaProveedor());
        String adulterada =
                (aviso.firma().charAt(0) == 'a' ? "b" : "a") + aviso.firma().substring(1);
        var otroSecreto = new PasarelaSimulada(
                codigo -> Optional.of("o".repeat(40)), Reloj.delSistema(), new ObjectMapper(), PROVEEDOR);
        otroSecreto.crearOrdenDeCobro(new SolicitudDeCobro("flujo-5", Dinero.de("50.00", Moneda.BOB)));

        assertThat(recibir(webhookCU, aviso.cuerpoCrudo(), adulterada, aviso.timestamp())
                        .estado())
                .isEqualTo(Resultado.Estado.FIRMA_INVALIDA);
        assertThat(recibir(otroSecreto.emitirWebhookDePago(orden.referenciaProveedor()))
                        .estado())
                .isEqualTo(Resultado.Estado.FIRMA_INVALIDA);
        assertThat(filas("sim-evt-" + orden.referenciaProveedor())).isZero();
    }

    private CU100RecibirWebhookPasarela cuEn(Instant ahora) {
        return new CU100RecibirWebhookPasarela(
                new Datos(dsl),
                new ProveedorPagoRepositorio(),
                new WebhookRepositorio(),
                new PagoRepositorio(),
                codigo -> Optional.of(SECRETO_DE_PRUEBA),
                Reloj.fijo(ahora),
                new InterpreteDeWebhookJson(new ObjectMapper()));
    }
}
