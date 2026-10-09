package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.SalidaRetiro;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H2.S1.M3 · Repetir la clave de un retiro es repetir la MISMA solicitud (y solo el titular retira).
 */
class CU11IdempotenciaTest extends BaseDeBilletera {

    private EscenarioDeRetiro e;

    @BeforeEach
    void preparar() {
        e = EscenarioDeRetiro.con("1000.00");
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private int ordenes() {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE cuenta_billetera_id=?", e.cuenta());
    }

    private int retenido() {
        return contar("SELECT saldo_retenido::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", e.cuenta());
    }

    @Test
    @DisplayName("reintento: la misma solicitud dos veces devuelve la misma orden y retiene una sola vez")
    void mismaSolicitudMismaOrden() {
        SalidaRetiro una = e.solicitar("k1", "300.00", "5.00");
        SalidaRetiro otra = e.solicitar("k1", "300.00", "5.00");
        assertThat(otra).isEqualTo(una);
        assertThat(ordenes()).isEqualTo(1);
        assertThat(retenido()).isEqualTo(300);
    }

    @Test
    @DisplayName(
            "rechaza cambiar importe o destino con la misma clave, y un costo distinto devuelve el costo registrado: no retiene de mas")
    void cambiarLaSolicitudSeRechaza() {
        e.solicitar("k2", "300.00", "5.00");
        assertThatThrownBy(() -> e.solicitar("k2", "301.00", "5.00"))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("idempotencia");
        // El costo lo fija el servidor y el replay devuelve el ALMACENADO (comportamiento que ya corre en test):
        // pedir otro costo con la misma clave no abre otra orden ni cambia lo cobrado.
        SalidaRetiro conOtroCosto = e.solicitar("k2", "300.00", "6.00");
        assertThat(conOtroCosto.costoRetiro()).isEqualByComparingTo(EscenarioDeRetiro.bob("5.00"));
        var otroDestino = new EntradaRetiro(
                "k2",
                e.cuenta(),
                EscenarioDeRetiro.bob("300.00"),
                EscenarioDeRetiro.bob("5.00"),
                UUID.randomUUID(),
                true,
                false);
        assertThatThrownBy(() -> transaccion.execute(t -> retiroCU.solicitar(otroDestino, e.ctx())))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("idempotencia");
        assertThat(ordenes()).isEqualTo(1);
        assertThat(retenido()).isEqualTo(300);
    }

    @Test
    @DisplayName(
            "Dados dos titulares que usan la misma clave de idempotencia · Cuando cada uno solicita su retiro · Entonces cada billetera tiene su espacio de claves y cada uno tiene su propia orden · Y la clave de uno no devuelve la orden del otro ni la rompe")
    void laClaveEsDeCadaBilletera() {
        e.solicitar("compartida", "300.00", "5.00");
        var otra = EscenarioDeRetiro.nuevo("1000.00");
        SalidaRetiro suya = otra.solicitar("compartida", "300.00", "5.00");
        assertThat(suya).isNotNull();
        assertThat(e.solicitar("compartida", "300.00", "5.00").ordenRetiroId()).isNotEqualTo(suya.ordenRetiroId());
    }

    @Test
    @DisplayName("rechaza que otra persona retire de mi billetera aunque conozca mi cuenta y mi destino")
    void soloElTitularRetira() {
        var intruso = contextoDe(fixtura.usuario());
        assertThatThrownBy(
                        () -> transaccion.execute(t -> retiroCU.solicitar(e.pedido("robo", "300.00", "5.00"), intruso)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(ordenes()).isZero();
        assertThat(retenido()).isZero();
        // Ni siquiera con la clave de una orden legitima puede leer su resultado.
        e.solicitar("mia", "300.00", "5.00");
        assertThatThrownBy(
                        () -> transaccion.execute(t -> retiroCU.solicitar(e.pedido("mia", "300.00", "5.00"), intruso)))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName("concurrencia: el mismo reintento tres veces a la vez retiene una sola vez")
    void reintentoConcurrente() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        List<Future<SalidaRetiro>> salidas = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                salidas.add(pool.submit(() -> e.solicitar("par", "300.00", "5.00")));
            }
            var primera = salidas.get(0).get();
            for (var salida : salidas) {
                assertThat(salida.get()).isEqualTo(primera);
            }
        } finally {
            pool.shutdown();
        }
        assertThat(ordenes()).isEqualTo(1);
        assertThat(retenido()).isEqualTo(300);
    }
}
