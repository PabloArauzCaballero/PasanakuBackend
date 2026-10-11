package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia;
import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.SalidaTransferencia;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H2.S1.M3 / H4.S1.M1 · La transferencia que se reintenta, y solo la mueve su titular.
 *
 * <p>Un reintento igual devuelve el mismo comprobante y un solo efecto contable; uno distinto
 * se rechaza; y la clave de una persona no es visible ni utilizable por otra.
 */
class CU12IdempotenciaTest extends BaseDeBilletera {

    private UUID usuario;
    private UUID origen;
    private UUID destino;
    private ContextoSesion ctx;

    @BeforeEach
    void preparar() {
        EscenarioDeRetiro.catalogo();
        usuario = fixtura.usuario();
        origen = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal("1000.00"));
        destino = fixtura.billetera(fixtura.usuario(), "ESTANDAR", BigDecimal.ZERO);
        ctx = contextoDe(usuario);
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private EntradaTransferencia pedido(String clave, UUID hacia, String monto, String concepto) {
        return new EntradaTransferencia(
                clave, origen, hacia, Dinero.de(monto, Moneda.BOB), concepto, Optional.empty(), Optional.empty());
    }

    private SalidaTransferencia transferir(EntradaTransferencia entrada, ContextoSesion quien) {
        return transaccion.execute(t -> transferenciaCU.ejecutar(entrada, quien));
    }

    private int debitos() {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id=? AND sentido='DEBITO'",
                origen);
    }

    private int total(UUID cuenta) {
        return contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta);
    }

    @Test
    @DisplayName("reintento: la misma transferencia dos veces devuelve el mismo comprobante y un solo debito")
    void mismaTransferenciaUnSoloEfecto() {
        var una = transferir(pedido("t1", destino, "250.00", "alquiler"), ctx);
        var otra = transferir(pedido("t1", destino, "250.00", "alquiler"), ctx);
        assertThat(otra.transaccionId()).isEqualTo(una.transaccionId());
        assertThat(debitos()).isEqualTo(1);
        assertThat(total(origen)).isEqualTo(750);
        assertThat(total(destino)).isEqualTo(250);
    }

    @Test
    @DisplayName("rechaza cambiar importe, destino o concepto con la misma clave: no mueve nada")
    void cambiarLaPeticionSeRechaza() {
        transferir(pedido("t2", destino, "250.00", "alquiler"), ctx);
        UUID otroDestino = fixtura.billetera(fixtura.usuario(), "ESTANDAR", BigDecimal.ZERO);
        for (var distinto : List.of(
                pedido("t2", destino, "251.00", "alquiler"),
                pedido("t2", otroDestino, "250.00", "alquiler"),
                pedido("t2", destino, "250.00", "otra cosa"))) {
            assertThatThrownBy(() -> transferir(distinto, ctx))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .hasMessageContaining("idempotencia");
        }
        assertThat(debitos()).isEqualTo(1);
        assertThat(total(otroDestino)).isZero();
    }

    @Test
    @DisplayName(
            "Dados dos titulares que usan la misma clave de idempotencia · Cuando cada uno transfiere desde su billetera · Entonces la clave es de cada titular y cada uno recibe su propio comprobante · Y la clave de uno no devuelve el comprobante del otro ni lo rompe")
    void laClaveEsDeCadaTitular() {
        var mia = transferir(pedido("compartida", destino, "100.00", "mia"), ctx);
        UUID otroUsuario = fixtura.usuario();
        UUID suyaOrigen = fixtura.billetera(otroUsuario, "ESTANDAR", BigDecimal.ZERO);
        fixtura.acreditar(suyaOrigen, new BigDecimal("500.00"));
        var suya = transferir(
                new EntradaTransferencia(
                        "compartida",
                        suyaOrigen,
                        destino,
                        Dinero.de("40.00", Moneda.BOB),
                        "suya",
                        Optional.empty(),
                        Optional.empty()),
                contextoDe(otroUsuario));
        assertThat(suya.transaccionId()).isNotEqualTo(mia.transaccionId());
        assertThat(transferir(pedido("compartida", destino, "100.00", "mia"), ctx)
                        .transaccionId())
                .isEqualTo(mia.transaccionId());
    }

    @Test
    @DisplayName("rechaza mover plata de una billetera ajena aunque se conozca su identificador")
    void soloElTitularTransfiere() {
        var intruso = contextoDe(fixtura.usuario());
        assertThatThrownBy(() -> transferir(pedido("robo", destino, "100.00", "robo"), intruso))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(debitos()).isZero();
        assertThat(total(origen)).isEqualTo(1000);
    }

    @Test
    @DisplayName("concurrencia: el mismo reintento en tres hilos a la vez produce un solo debito")
    void reintentoConcurrente() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        List<Future<SalidaTransferencia>> salidas = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                salidas.add(pool.submit(() -> transferir(pedido("par", destino, "100.00", "par"), ctx)));
            }
            UUID primera = salidas.get(0).get().transaccionId();
            for (var salida : salidas) {
                assertThat(salida.get().transaccionId()).isEqualTo(primera);
            }
        } finally {
            pool.shutdown();
        }
        assertThat(debitos()).isEqualTo(1);
    }
}
