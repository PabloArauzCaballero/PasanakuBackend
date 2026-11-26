package bo.aportaya.aportes.infraestructura.pasarela.simulada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.dominio.PasarelaDePagos.EstadoDeCobro;
import bo.aportaya.aportes.dominio.PasarelaDePagos.OrdenDeCobro;
import bo.aportaya.aportes.dominio.PasarelaDePagos.SolicitudDeCobro;
import bo.aportaya.aportes.dominio.SimuladorDePagoDePasarela.WebhookFirmado;
import bo.aportaya.aportes.dominio.VerificadorDeFirmaWebhook;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El DOBLE de la pasarela en los tres niveles del contrato de {@code PasarelaDePagos} (regla 65). */
class PasarelaSimuladaTest {

    /** Secreto SINTETICO de prueba (>= 32): no es el de ningun entorno. */
    private static final String SECRETO = "secreto-sintetico-de-la-prueba-simulada-0001";

    private static final Instant AHORA = Instant.parse("2026-11-26T12:00:00Z");

    private static PasarelaSimulada nueva() {
        return new PasarelaSimulada(
                codigo -> Optional.of(SECRETO), Reloj.fijo(AHORA), new ObjectMapper(), "QR_INTEROP");
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    // ---------------------------------------------------------------- correcto --

    @Test
    @DisplayName(
            "correcto: crear la orden es determinista (la misma clave da la misma referencia en otra instancia) y nace PENDIENTE y marcada como doble")
    void ordenDeterminista() {
        OrdenDeCobro a = nueva().crearOrdenDeCobro(new SolicitudDeCobro("cobro-1", bob("500.00")));
        OrdenDeCobro b = nueva().crearOrdenDeCobro(new SolicitudDeCobro("cobro-1", bob("500.00")));

        assertThat(a).isEqualTo(b);
        assertThat(a.referenciaProveedor()).startsWith("SIM-");
        assertThat(a.estado()).isEqualTo(EstadoDeCobro.PENDIENTE);
        assertThat(a.esDoble()).isTrue();
        assertThat(a.contenidoQr()).startsWith("PASANAKU-SIMULADA:");
        assertThat(a.monto()).isEqualTo(bob("500.00"));
    }

    @Test
    @DisplayName(
            "correcto: el aviso emitido trae firma verificable con el secreto del entorno, timestamp del reloj y el monto exacto")
    void avisoFirmadoVerificable() {
        var pasarela = nueva();
        OrdenDeCobro orden = pasarela.crearOrdenDeCobro(new SolicitudDeCobro("cobro-2", bob("120.50")));

        WebhookFirmado aviso = pasarela.emitirWebhookDePago(orden.referenciaProveedor());

        assertThat(VerificadorDeFirmaWebhook.firmaValida(
                        aviso.timestamp(), aviso.cuerpoCrudo(), aviso.firma(), SECRETO))
                .isTrue();
        assertThat(aviso.timestamp()).isEqualTo(String.valueOf(AHORA.getEpochSecond()));
        assertThat(aviso.proveedorCodigo()).isEqualTo("QR_INTEROP");
        assertThat(aviso.cuerpoCrudo())
                .contains("\"referenciaProveedor\":\"" + orden.referenciaProveedor() + "\"")
                .contains("\"monto\":\"120.50\"")
                .contains("\"moneda\":\"BOB\"");
        assertThat(pasarela.consultarEstado(orden.referenciaProveedor())
                        .orElseThrow()
                        .estado())
                .isEqualTo(EstadoDeCobro.PAGADO);
        // el mismo aviso dos veces es byte a byte el mismo: determinista
        assertThat(pasarela.emitirWebhookDePago(orden.referenciaProveedor())).isEqualTo(aviso);
    }

    // ------------------------------------------------------------------ limite --

    @Test
    @DisplayName(
            "limite: la misma solicitud repetida devuelve la MISMA orden (idempotencia); el monto minimo 0.01 es valido")
    void idempotenteYMontoMinimo() {
        var pasarela = nueva();
        OrdenDeCobro primera = pasarela.crearOrdenDeCobro(new SolicitudDeCobro("cobro-3", bob("0.01")));
        OrdenDeCobro segunda = pasarela.crearOrdenDeCobro(new SolicitudDeCobro("cobro-3", bob("0.01")));

        assertThat(segunda).isEqualTo(primera);
        assertThat(pasarela.consultarEstado(primera.referenciaProveedor())).isPresent();
    }

    @Test
    @DisplayName("limite: claves distintas dan referencias distintas aunque el monto sea igual")
    void referenciasDistintas() {
        var pasarela = nueva();
        var a = pasarela.crearOrdenDeCobro(new SolicitudDeCobro("cobro-a", bob("10.00")));
        var b = pasarela.crearOrdenDeCobro(new SolicitudDeCobro("cobro-b", bob("10.00")));

        assertThat(a.referenciaProveedor()).isNotEqualTo(b.referenciaProveedor());
    }

    // ----------------------------------------------------------------- invalido --

    @Test
    @DisplayName("invalido: monto nulo, cero o negativo y clave vacia o nula se rechazan antes de abrir nada")
    void solicitudInvalida() {
        assertThatThrownBy(() -> new SolicitudDeCobro("c", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SolicitudDeCobro("c", bob("0.00"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SolicitudDeCobro("c", bob("-1.00"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SolicitudDeCobro(" ", bob("1.00"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SolicitudDeCobro(null, bob("1.00"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("invalido: la misma clave con OTRO monto no abre una segunda orden: falla")
    void mismaClaveOtroMonto() {
        var pasarela = nueva();
        pasarela.crearOrdenDeCobro(new SolicitudDeCobro("cobro-4", bob("10.00")));

        assertThatThrownBy(() -> pasarela.crearOrdenDeCobro(new SolicitudDeCobro("cobro-4", bob("11.00"))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName(
            "invalido: una referencia que la pasarela no emitio no tiene estado ni aviso 'normal'; el aviso explicito si se puede forzar")
    void referenciaDesconocida() {
        var pasarela = nueva();

        assertThat(pasarela.consultarEstado("SIM-NO-EXISTE")).isEmpty();
        assertThat(pasarela.consultarEstado(null)).isEmpty();
        assertThatThrownBy(() -> pasarela.emitirWebhookDePago("SIM-NO-EXISTE"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(pasarela.emitirWebhookDePago("SIM-NO-EXISTE", "evt-x", bob("1.00"))
                        .cuerpoCrudo())
                .contains("SIM-NO-EXISTE");
    }

    @Test
    @DisplayName("invalido: sin secreto de verificacion el doble no firma (no inventa uno)")
    void sinSecretoNoFirma() {
        var sinSecreto =
                new PasarelaSimulada(codigo -> Optional.empty(), Reloj.fijo(AHORA), new ObjectMapper(), "QR_INTEROP");

        assertThatThrownBy(() -> sinSecreto.emitirWebhookDePago("SIM-X", "evt", bob("1.00")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PasarelaSimulada(
                        codigo -> Optional.of(SECRETO), Reloj.fijo(AHORA), new ObjectMapper(), " "))
                .isInstanceOf(IllegalStateException.class);
    }
}
