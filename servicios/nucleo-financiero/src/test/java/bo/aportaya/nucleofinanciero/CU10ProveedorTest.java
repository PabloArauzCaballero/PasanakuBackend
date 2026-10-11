package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU10RechazarRecarga;
import bo.aportaya.nucleofinanciero.aplicacion.CotizarOperacion;
import bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor;
import bo.aportaya.nucleofinanciero.aplicacion.RegistrarDiscrepancia;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.DiscrepanciaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRecargaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Dominio y PostgreSQL reales; solo el proveedor externo es un doble. */
class CU10ProveedorTest extends BaseDeBilletera {
    private UUID cuenta;
    private ContextoSesion titular;
    private RecargasConProveedor flujo;
    private final Map<UUID, ProveedorDeRecargas.Confirmacion> confirmaciones = new HashMap<>();

    @BeforeEach
    void preparar() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("RECARGA", "ESTANDAR", "MES", new BigDecimal("10000.00"), null);
        UUID usuario = fixtura.usuario();
        cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        titular = contextoDe(usuario);
        var manager = transaccion.getTransactionManager();
        var discrepancias = TransaccionalDePrueba.conTransaccion(
                new RegistrarDiscrepancia(
                        new Datos(dsl),
                        new DiscrepanciaRepositorio(),
                        new Outbox("nucleo_financiero"),
                        Reloj.delSistema()),
                manager);
        var rechazos = TransaccionalDePrueba.conTransaccion(
                new CU10RechazarRecarga(
                        new Datos(dsl),
                        new OrdenRecargaRepositorio(),
                        new CuentaBilleteraRepositorio(),
                        new Outbox("nucleo_financiero")),
                manager);
        flujo = new RecargasConProveedor(
                TransaccionalDePrueba.conTransaccion(recargaCU, manager),
                rechazos,
                new ProveedorDeRecargas() {
                    @Override
                    public void solicitar(UUID referencia, Dinero monto) {
                        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                                .isFalse();
                        confirmaciones.putIfAbsent(
                                referencia, new Confirmacion(referencia, UUID.randomUUID(), monto, "PENDIENTE", null));
                    }

                    @Override
                    public Confirmacion consultar(UUID referencia) {
                        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                                .isFalse();
                        return confirmaciones.get(referencia);
                    }
                },
                new CotizarOperacion(new FlujosDePrueba.TarifaGratuita(), Reloj.delSistema(), false),
                FlujosDePrueba.existentes(),
                discrepancias);
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private CU10RecargarSaldo.EntradaSolicitud entrada(String clave, String monto) {
        return new CU10RecargarSaldo.EntradaSolicitud(
                clave, cuenta, Dinero.de(monto, Moneda.BOB), Dinero.de("45.00", Moneda.BOB), "QR", Optional.empty());
    }

    private void confirmarProveedor(UUID orden) {
        var anterior = confirmaciones.get(orden);
        confirmaciones.put(
                orden,
                new ProveedorDeRecargas.Confirmacion(
                        orden,
                        anterior.transaccionProveedor(),
                        anterior.monto(),
                        "CONFIRMADO",
                        OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void noAcreditaUnaOrdenPendienteAunqueElUsuarioLoSolicite() {
        var orden = flujo.solicitar(entrada("pendiente", "500.00"), titular);
        assertThatThrownBy(() -> flujo.confirmar(orden.ordenRecargaId(), titular))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta))
                .isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id=?",
                        cuenta))
                .isZero();
    }

    @Test
    void cobraPrecioDelServidorYRepiteResultadoSinSegundoAbono() {
        var orden = flujo.solicitar(entrada("confirmada", "500.00"), titular);
        assertThat(orden.acreditara()).isEqualTo(Dinero.de("500.00", Moneda.BOB));
        confirmarProveedor(orden.ordenRecargaId());
        var primera = flujo.confirmar(orden.ordenRecargaId(), titular);
        var siguiente = flujo.solicitar(entrada("otra", "100.00"), titular);
        confirmarProveedor(siguiente.ordenRecargaId());
        flujo.confirmar(siguiente.ordenRecargaId(), titular);
        var repetida = flujo.confirmar(orden.ordenRecargaId(), titular);
        assertThat(repetida).isEqualTo(primera);
        assertThat(primera.saldoDespues()).isEqualTo(Dinero.de("500.00", Moneda.BOB));
        assertThat(contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta))
                .isEqualTo(600);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id=?",
                        primera.transaccionId()))
                .isEqualTo(2);
    }

    @Test
    void otraPersonaNoPuedeCargarLaCuentaNiConfirmarla() {
        var ajeno = contextoDe(fixtura.usuario());
        assertThatThrownBy(() -> flujo.solicitar(entrada("ajeno", "500.00"), ajeno))
                .isInstanceOf(ErrorDeNegocio.class);
        var orden = flujo.solicitar(entrada("propia", "500.00"), titular);
        confirmarProveedor(orden.ordenRecargaId());
        assertThatThrownBy(() -> flujo.confirmar(orden.ordenRecargaId(), ajeno)).isInstanceOf(ErrorDeNegocio.class);
        assertThat(contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta))
                .isZero();
    }

    @Test
    void idempotenciaNoPermiteCambiarMonto() {
        flujo.solicitar(entrada("misma", "500.00"), titular);
        assertThatThrownBy(() -> flujo.solicitar(entrada("misma", "900.00"), titular))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("idempotencia");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE cuenta_billetera_id=?",
                        cuenta))
                .isEqualTo(1);
    }

    @Test
    void confirmacionConImporteDistintoNoAcredita() {
        var orden = flujo.solicitar(entrada("importe", "500.00"), titular);
        confirmaciones.put(
                orden.ordenRecargaId(),
                new ProveedorDeRecargas.Confirmacion(
                        orden.ordenRecargaId(),
                        UUID.randomUUID(),
                        Dinero.de("499.00", Moneda.BOB),
                        "CONFIRMADO",
                        OffsetDateTime.now(ZoneOffset.UTC)));
        assertThatThrownBy(() -> flujo.confirmar(orden.ordenRecargaId(), titular))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta))
                .isZero();
    }

    @Test
    void dominioRechazaAcreditarSinConfirmacion() {
        var orden = flujo.solicitar(entrada("sin-evidencia", "500.00"), titular);
        assertThatThrownBy(() -> transaccion.execute(tx -> recargaCU.acreditar(orden.ordenRecargaId(), null, titular)))
                .isInstanceOf(ErrorDeNegocio.class);
    }
}
