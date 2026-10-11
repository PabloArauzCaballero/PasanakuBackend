package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.aplicacion.QrInterno;
import bo.aportaya.nucleofinanciero.aplicacion.QrInterno.ComprobanteQr;
import bo.aportaya.nucleofinanciero.aplicacion.QrInterno.EntradaPagoQr;
import bo.aportaya.nucleofinanciero.aplicacion.QrInterno.EntradaQr;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.nucleofinanciero.infraestructura.QrRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Lo comun a las pruebas de QR interno: dos billeteras, un reloj que se puede adelantar y el caso de uso real.
 *
 * <p>PostgreSQL real y el caso de uso de transferencia real: pagar por QR es una transferencia
 * comun, y lo que se prueba es que el QR la gobierne sin duplicarla ni inventarla.
 */
abstract class BaseDeQr extends BaseDeBilletera {

    static final String CLAVE_DE_PRUEBA = "clave-de-prueba-de-los-qr-internos-0001";

    protected final AtomicReference<Instant> ahora = new AtomicReference<>(Instant.now());
    protected QrInterno qrs;
    protected UUID cobra;
    protected UUID paga;
    protected ContextoSesion quienCobra;
    protected ContextoSesion quienPaga;

    @BeforeEach
    void preparar() {
        EscenarioDeRetiro.catalogo();
        UUID usuarioCobra = fixtura.usuario();
        cobra = fixtura.billetera(usuarioCobra, "ESTANDAR", BigDecimal.ZERO);
        UUID usuarioPaga = fixtura.usuario();
        paga = fixtura.billetera(usuarioPaga, "ESTANDAR", BigDecimal.ZERO);
        fixtura.acreditar(paga, new BigDecimal("1000.00"));
        quienCobra = contextoDe(usuarioCobra);
        quienPaga = contextoDe(usuarioPaga);
        qrs = TransaccionalDePrueba.conTransaccion(
                new QrInterno(
                        new Datos(dsl),
                        new CuentaBilleteraRepositorio(),
                        new QrRepositorio(),
                        new LibroDeBilletera(),
                        transferenciaCU,
                        new Outbox("nucleo_financiero"),
                        () -> ahora.get(),
                        CLAVE_DE_PRUEBA,
                        Duration.ofMinutes(15)),
                transaccion.getTransactionManager());
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    protected static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    protected QrInterno.QrEmitido dinamico(String monto) {
        return qrs.emitir(
                new EntradaQr(cobra, "DINAMICO", Optional.of(bob(monto)), Optional.of("almuerzo")), quienCobra);
    }

    protected QrInterno.QrEmitido estatico() {
        return qrs.emitir(new EntradaQr(cobra, "ESTATICO", Optional.empty(), Optional.empty()), quienCobra);
    }

    protected ComprobanteQr pagar(String contenido, String monto, String clave, ContextoSesion quien, UUID origen) {
        return qrs.pagar(
                new EntradaPagoQr(contenido, origen, Optional.ofNullable(monto).map(BaseDeQr::bob), clave), quien);
    }

    protected int total(UUID cuenta) {
        return contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta);
    }

    protected int pagos() {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.transferencia_p2p WHERE cuenta_billetera_destino_id=?",
                cobra);
    }

    protected String estadoDelQr(UUID qr) {
        return (String) dsl.fetchOne("SELECT estado FROM nucleo_financiero.qr_transferencia WHERE id=?", qr)
                .get(0);
    }

    protected static String ensuciar(String contenido) {
        char ultimo = contenido.charAt(contenido.length() - 1);
        return contenido.substring(0, contenido.length() - 1) + (ultimo == 'A' ? 'B' : 'A');
    }
}
