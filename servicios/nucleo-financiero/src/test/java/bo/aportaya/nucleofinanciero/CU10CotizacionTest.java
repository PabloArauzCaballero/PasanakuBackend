package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CotizarOperacion;
import bo.aportaya.nucleofinanciero.aplicacion.CotizarOperacion.Operacion;
import bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H11.S1.M1 · El precio se ve antes de confirmar, y el cargo es el que se vio (incluida su expiracion).
 *
 * <p>PostgreSQL y casos de uso reales; solo {@code tarifas} y el proveedor son dobles. El doble de
 * {@code tarifas} cumple su contrato: la misma clave devuelve la misma cotizacion y no compara el importe
 * (como el real), de modo que la proteccion contra «el precio de un importe sobre otro» tiene que estar
 * de este lado.
 */
class CU10CotizacionTest extends BaseDeBilletera {

    private final AtomicReference<Instant> ahora = new AtomicReference<>(Instant.now());
    private final TarifasFalso tarifas = new TarifasFalso(ahora);
    private final Set<UUID> enviadas = new HashSet<>();
    private RecargasConProveedor flujo;
    private CotizarOperacion cotizaciones;
    private UUID cuenta;
    private ContextoSesion titular;

    @BeforeEach
    void preparar() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("RECARGA", "ESTANDAR", "MES", new BigDecimal("10000.00"), null);
        UUID usuario = fixtura.usuario();
        cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        titular = contextoDe(usuario);
        cotizaciones = new CotizarOperacion(tarifas, () -> ahora.get(), true);
        flujo = FlujosDePrueba.recargas(
                new ProveedorDeRecargas() {
                    @Override
                    public void solicitar(UUID referencia, Dinero monto) {
                        enviadas.add(referencia);
                    }

                    @Override
                    public Confirmacion consultar(UUID referencia) {
                        throw new UnsupportedOperationException();
                    }
                },
                cotizaciones);
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private CU10RecargarSaldo.EntradaSolicitud pedido(String clave, String monto, Optional<UUID> cotizacion) {
        return new CU10RecargarSaldo.EntradaSolicitud(
                clave, cuenta, bob(monto), Dinero.cero(Moneda.BOB), "QR", Optional.empty(), cotizacion);
    }

    private int ordenes() {
        return contar("SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE cuenta_billetera_id=?", cuenta);
    }

    @Test
    @DisplayName(
            "Dada una recarga de Bs 500 con un costo de Bs 5 · Cuando se cotiza y luego se confirma citando esa cotización · Entonces la vista muestra base, comisión, impuesto, total, neto y vigencia · Y la orden cobra exactamente lo cotizado y tarifas registra la aceptación")
    void cotizacionYCargoCoinciden() {
        UUID clave = UUID.randomUUID();
        var vista = cotizaciones.cotizar(Operacion.RECARGA, cuenta, bob("500.00"), clave);

        assertThat(vista.base()).isEqualTo(bob("500.00"));
        assertThat(vista.comision()).isEqualTo(bob("4.00"));
        assertThat(vista.impuesto()).isEqualTo(bob("1.00"));
        assertThat(vista.total()).isEqualTo(bob("5.00"));
        assertThat(vista.neto()).isEqualTo(bob("495.00"));
        assertThat(vista.validaHasta()).isPresent();

        var orden = flujo.solicitar(pedido(clave.toString(), "500.00", vista.cotizacionId()), titular);

        assertThat(orden.acreditara()).isEqualTo(vista.neto());
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE id=? AND cotizacion_id=? AND costo_proveedor=5.00 AND monto_acreditado=495.00",
                        orden.ordenRecargaId(),
                        vista.cotizacionId().orElseThrow()))
                .isEqualTo(1);
        assertThat(tarifas.aceptadas).containsExactly(vista.cotizacionId().orElseThrow());
        assertThat(enviadas).containsExactly(orden.ordenRecargaId());
    }

    @Test
    @DisplayName(
            "Dada una operación gravada sin cotización citada · Cuando se solicita la recarga · Entonces se rechaza · Y no se crea la orden ni se envía nada al proveedor")
    void sinCotizacionNoSeConfirma() {
        assertThatThrownBy(() -> flujo.solicitar(pedido("sin-cot", "500.00", Optional.empty()), titular))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("cotizacion");
        assertThat(ordenes()).isZero();
        assertThat(enviadas).isEmpty();
    }

    @Test
    @DisplayName(
            "Dada una cotización vencida · Cuando se solicita la recarga citándola · Entonces se rechaza porque venció · Y no se crea la orden, no se envía nada al proveedor y tarifas no registra ninguna aceptación")
    void cotizacionVencida() {
        UUID clave = UUID.randomUUID();
        var vista = cotizaciones.cotizar(Operacion.RECARGA, cuenta, bob("500.00"), clave);
        ahora.set(ahora.get().plus(Duration.ofMinutes(11)));

        assertThatThrownBy(() -> flujo.solicitar(pedido(clave.toString(), "500.00", vista.cotizacionId()), titular))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("vencio");
        assertThat(ordenes()).isZero();
        assertThat(tarifas.aceptadas).isEmpty();
        assertThat(enviadas).isEmpty();
    }

    @Test
    @DisplayName(
            "Dada una cotización emitida para Bs 500 · Cuando se solicita otro importe con la misma clave citando esa cotización · Entonces se rechaza porque la cotización no corresponde al importe · Y el nuevo importe obtiene su propia cotización en vez de reutilizar la anterior")
    void otroImporteOtraCotizacion() {
        UUID clave = UUID.randomUUID();
        var vista = cotizaciones.cotizar(Operacion.RECARGA, cuenta, bob("500.00"), clave);

        assertThatThrownBy(() -> flujo.solicitar(pedido(clave.toString(), "5000.00", vista.cotizacionId()), titular))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no corresponde");
        assertThat(ordenes()).isZero();
        assertThat(tarifas.guardadas)
                .as("el importe distinto produjo otra cotizacion, no reutilizo la vista")
                .hasSize(2);
    }

    @Test
    @DisplayName(
            "Dada una cotización que nunca se emitió para esta solicitud · Cuando se solicita la recarga citándola · Entonces se rechaza · Y no se crea la orden")
    void cotizacionAjena() {
        UUID clave = UUID.randomUUID();
        cotizaciones.cotizar(Operacion.RECARGA, cuenta, bob("500.00"), clave);
        assertThatThrownBy(() ->
                        flujo.solicitar(pedido(clave.toString(), "500.00", Optional.of(UUID.randomUUID())), titular))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(ordenes()).isZero();
    }

    @Test
    @DisplayName(
            "Dada una cotización vigente que tarifas no acepta registrar · Cuando se solicita la recarga citándola · Entonces se rechaza · Y no se crea la orden ni se envía nada al proveedor")
    void sinAceptacionNoHayOrden() {
        UUID clave = UUID.randomUUID();
        var vista = cotizaciones.cotizar(Operacion.RECARGA, cuenta, bob("500.00"), clave);
        tarifas.rechazaLaAceptacion = true;
        assertThatThrownBy(() -> flujo.solicitar(pedido(clave.toString(), "500.00", vista.cotizacionId()), titular))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(ordenes()).isZero();
        assertThat(enviadas).isEmpty();
    }

    @Test
    @DisplayName(
            "Dado tarifas caído · Cuando se cotiza o se solicita una recarga · Entonces ambas se rechazan: no se cobra cero por no saber · Y no se crea ninguna orden")
    void tarifasCaido() {
        tarifas.caido = true;
        assertThatThrownBy(() -> cotizaciones.cotizar(Operacion.RECARGA, cuenta, bob("500.00"), UUID.randomUUID()))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> flujo.solicitar(pedido("caido", "500.00", Optional.empty()), titular))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(ordenes()).isZero();
    }

    @Test
    @DisplayName(
            "Dada una operación gratuita de Bs 500 · Cuando se cotiza y se solicita sin citar cotización · Entonces la vista es gratuita, no hay cotización que citar y se acredita el importe completo · Y citar una cotización se rechaza")
    void operacionGratuita() {
        tarifas.gratuito = true;
        var vista = cotizaciones.cotizar(Operacion.RECARGA, cuenta, bob("500.00"), UUID.randomUUID());
        assertThat(vista.gratuita()).isTrue();
        assertThat(vista.cotizacionId()).isEmpty();
        assertThat(vista.neto()).isEqualTo(bob("500.00"));

        var orden = flujo.solicitar(pedido("gratis", "500.00", Optional.empty()), titular);
        assertThat(orden.acreditara()).isEqualTo(bob("500.00"));
        assertThatThrownBy(() -> flujo.solicitar(pedido("gratis-2", "500.00", Optional.of(UUID.randomUUID())), titular))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName(
            "Dado un costo que iguala o supera el importe de la recarga · Cuando se cotiza · Entonces se rechaza porque no hay recarga que acreditar")
    void costoMayorQueElImporte() {
        assertThatThrownBy(() -> cotizaciones.cotizar(Operacion.RECARGA, cuenta, bob("5.00"), UUID.randomUUID()))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("igualar ni superar");
        assertThatThrownBy(() -> cotizaciones.cotizar(Operacion.RECARGA, cuenta, bob("3.00"), UUID.randomUUID()))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName("reintento: una orden ya abierta devuelve la misma orden aunque la cotizacion haya vencido")
    void reintentoDespuesDeVencida() {
        UUID clave = UUID.randomUUID();
        var vista = cotizaciones.cotizar(Operacion.RECARGA, cuenta, bob("500.00"), clave);
        var primera = flujo.solicitar(pedido(clave.toString(), "500.00", vista.cotizacionId()), titular);
        ahora.set(ahora.get().plus(Duration.ofHours(2)));

        var reintento = flujo.solicitar(pedido(clave.toString(), "500.00", vista.cotizacionId()), titular);

        assertThat(reintento.ordenRecargaId()).isEqualTo(primera.ordenRecargaId());
        assertThat(ordenes()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un retiro gravado con cotización vigente · Cuando se cotiza, se confirma y se solicita citándola · Entonces la orden de retiro guarda la cotización y su neto descuenta el costo · Y un retiro gravado sin cotización se rechaza")
    void retiroConCotizacion() {
        var e = EscenarioDeRetiro.con("1000.00");
        UUID clave = UUID.randomUUID();
        var vista = cotizaciones.cotizar(Operacion.RETIRO, e.cuenta(), bob("400.00"), clave);
        var costo = cotizaciones.confirmar(
                Operacion.RETIRO, e.cuenta(), bob("400.00"), clave.toString(), vista.cotizacionId(), false);

        var orden = transaccion.execute(t -> retiroCU.solicitar(
                new bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro(
                        clave.toString(),
                        e.cuenta(),
                        bob("400.00"),
                        costo.costo(),
                        e.instrumento(),
                        true,
                        false,
                        costo.cotizacionId()),
                e.ctx()));

        assertThat(orden.montoNeto()).isEqualTo(bob("395.00"));
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id=? AND cotizacion_id=?",
                        orden.ordenRetiroId(),
                        vista.cotizacionId().orElseThrow()))
                .isEqualTo(1);
        assertThatThrownBy(() -> cotizaciones.confirmar(
                        Operacion.RETIRO,
                        e.cuenta(),
                        bob("400.00"),
                        UUID.randomUUID().toString(),
                        Optional.empty(),
                        false))
                .hasMessageContaining("cotizacion");
    }
}
