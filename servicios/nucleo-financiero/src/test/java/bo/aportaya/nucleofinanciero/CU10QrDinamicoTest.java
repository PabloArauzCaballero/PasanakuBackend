package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU10RechazarRecarga;
import bo.aportaya.nucleofinanciero.aplicacion.CotizarOperacion;
import bo.aportaya.nucleofinanciero.aplicacion.PagoDeRecargaDePrueba;
import bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor;
import bo.aportaya.nucleofinanciero.aplicacion.RegistrarDiscrepancia;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas;
import bo.aportaya.nucleofinanciero.dominio.puertos.SimuladorDePagoDeRecarga;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.DiscrepanciaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRecargaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.RecargasProveedorLocal;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * CU-10 con QR dinamico: la orden guarda el QR que emitio el proveedor y el pago de prueba acredita por
 * el camino real. PostgreSQL real; el proveedor es el doble que se despliega en TEST
 * ({@link RecargasProveedorLocal}), no un mock.
 */
class CU10QrDinamicoTest extends BaseDeBilletera {
    private static final String TRANSACCIONES_DE_LA_CUENTA =
            """
            SELECT count(DISTINCT transaccion_id)::int FROM nucleo_financiero.movimiento_billetera
             WHERE cuenta_billetera_id = ?""";
    private static final String DEBITOS_IGUAL_CREDITOS =
            """
            SELECT sum(CASE sentido WHEN 'DEBITO' THEN monto ELSE 0 END)::text || '='
                   || sum(CASE sentido WHEN 'CREDITO' THEN monto ELSE 0 END)::text
              FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id = ?""";

    private UUID cuenta;
    private ContextoSesion titular;
    private RecargasProveedorLocal proveedor;
    private RecargasConProveedor flujo;
    private PagoDeRecargaDePrueba pago;

    @BeforeEach
    void preparar() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("RECARGA", "ESTANDAR", "MES", new BigDecimal("10000.00"), null);
        UUID usuario = fixtura.usuario();
        cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        titular = contextoDe(usuario);
        proveedor = new RecargasProveedorLocal();
        flujo = flujoCon(proveedor);
        pago = new PagoDeRecargaDePrueba(flujo, recargaTransaccional(), FlujosDePrueba.qr(), simulador(proveedor));
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    @Test
    void laOrdenDevuelveYGuardaElQrDePrueba() {
        var solicitada = flujo.solicitarConQr(entrada("qr", "150.00"), titular);

        var qr = solicitada.qr().orElseThrow();
        var orden = solicitada.salida().ordenRecargaId();
        assertThat(qr.origen()).isEqualTo("PRUEBA");
        assertThat(qr.contenido()).startsWith("PASANAKU-PRUEBA|").contains("ref=" + orden);
        assertThat(texto("SELECT qr_contenido FROM nucleo_financiero.orden_recarga WHERE id=?", orden))
                .isEqualTo(qr.contenido());
        assertThat(texto("SELECT qr_origen FROM nucleo_financiero.orden_recarga WHERE id=?", orden))
                .isEqualTo("PRUEBA");
    }

    @Test
    void reintentarConLaMismaClaveDevuelveLaMismaOrdenYElMismoQr() {
        var primera = flujo.solicitarConQr(entrada("repetida", "150.00"), titular);
        var segunda = flujo.solicitarConQr(entrada("repetida", "150.00"), titular);

        assertThat(segunda.salida().ordenRecargaId()).isEqualTo(primera.salida().ordenRecargaId());
        assertThat(segunda.qr()).isEqualTo(primera.qr());
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE cuenta_billetera_id=?",
                        cuenta))
                .isEqualTo(1);
    }

    @Test
    void simularElPagoAcreditaUnaSolaVezYElAsientoCuadra() {
        var orden = flujo.solicitarConQr(entrada("pagada", "150.00"), titular)
                .salida()
                .ordenRecargaId();

        var primera = pago.pagar(orden, titular);
        var repetida = pago.pagar(orden, titular);

        assertThat(primera.saldoDespues()).isEqualTo(Dinero.de("150.00", Moneda.BOB));
        assertThat(repetida).isEqualTo(primera);
        assertThat(contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta))
                .isEqualTo(150);
        assertThat(texto("SELECT estado FROM nucleo_financiero.orden_recarga WHERE id=?", orden))
                .isEqualTo("ACREDITADA");
        // Una sola transaccion, de dos patas que cuadran.
        assertThat(contar(TRANSACCIONES_DE_LA_CUENTA, cuenta)).isEqualTo(1);
        assertThat(texto(DEBITOS_IGUAL_CREDITOS, primera.transaccionId())).isEqualTo("150.00=150.00");
    }

    @Test
    void otraPersonaNoPuedeSimularElPagoDeUnaOrdenAjena() {
        var orden = flujo.solicitarConQr(entrada("ajena", "150.00"), titular)
                .salida()
                .ordenRecargaId();
        var ajeno = contextoDe(fixtura.usuario());

        assertThatThrownBy(() -> pago.pagar(orden, ajeno)).isInstanceOf(ErrorDeNegocio.class);
        assertThat(texto("SELECT estado FROM nucleo_financiero.orden_recarga WHERE id=?", orden))
                .isEqualTo("PENDIENTE");
        assertThat(contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta))
                .isZero();
    }

    @Test
    void unaOrdenSinQrDePruebaNoSePuedeDarPorPagada() {
        var sinQr = new ProveedorDeRecargasFalso();
        var orden = flujoCon(sinQr).solicitarConQr(entrada("sin-qr", "150.00"), titular);
        assertThat(orden.qr()).isEmpty();

        var ordenId = orden.salida().ordenRecargaId();
        assertThatThrownBy(() -> pago.pagar(ordenId, titular))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("QR de prueba");
        assertThat(contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta))
                .isZero();
    }

    @Test
    void sinElDobleDePruebaLaOperacionNoEstaDisponible() {
        var sinDoble = new PagoDeRecargaDePrueba(
                flujo,
                recargaTransaccional(),
                FlujosDePrueba.qr(),
                new StaticListableBeanFactory().getBeanProvider(SimuladorDePagoDeRecarga.class));

        assertThat(sinDoble.disponible()).isFalse();
        assertThat(pago.disponible()).isTrue();
    }

    /** Como en el servicio desplegado: las lecturas del caso de uso necesitan su transaccion. */
    private CU10RecargarSaldo recargaTransaccional() {
        return TransaccionalDePrueba.conTransaccion(recargaCU, transaccion.getTransactionManager());
    }

    private RecargasConProveedor flujoCon(ProveedorDeRecargas proveedorDeRecargas) {
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
        return new RecargasConProveedor(
                TransaccionalDePrueba.conTransaccion(recargaCU, manager),
                rechazos,
                proveedorDeRecargas,
                new CotizarOperacion(new FlujosDePrueba.TarifaGratuita(), Reloj.delSistema(), false),
                FlujosDePrueba.existentes(),
                discrepancias,
                FlujosDePrueba.qr());
    }

    private static org.springframework.beans.factory.ObjectProvider<SimuladorDePagoDeRecarga> simulador(
            SimuladorDePagoDeRecarga simulador) {
        var fabrica = new StaticListableBeanFactory();
        fabrica.addBean("simulador", simulador);
        return fabrica.getBeanProvider(SimuladorDePagoDeRecarga.class);
    }

    private CU10RecargarSaldo.EntradaSolicitud entrada(String clave, String monto) {
        return new CU10RecargarSaldo.EntradaSolicitud(
                clave, cuenta, Dinero.de(monto, Moneda.BOB), Dinero.de("0.00", Moneda.BOB), "QR", Optional.empty());
    }

    private String texto(String sql, UUID id) {
        return dslFixtura.fetchValue(sql, id).toString();
    }
}
