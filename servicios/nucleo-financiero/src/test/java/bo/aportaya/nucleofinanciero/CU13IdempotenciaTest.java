package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo.EntradaRetencion;
import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo.SalidaRetencion;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
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
 * H2.S1.M3 · La retencion con clave: la huella de la peticion se persiste en
 * {@code respuesta_idempotente} y manda sobre el reintento. Y solo el titular aparta su saldo.
 */
class CU13IdempotenciaTest extends BaseDeBilletera {

    private UUID usuario;
    private UUID cuenta;
    private ContextoSesion ctx;

    @BeforeEach
    void preparar() {
        usuario = fixtura.usuario();
        cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal("1000.00"));
        ctx = contextoDe(usuario);
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private SalidaRetencion retener(String clave, String monto, String motivo, ContextoSesion quien) {
        return transaccion.execute(t -> retencionCU.retener(
                EntradaRetencion.simple(cuenta, Dinero.de(monto, Moneda.BOB), motivo), clave, quien));
    }

    private int retenido() {
        return contar("SELECT saldo_retenido::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta);
    }

    @Test
    @DisplayName("reintento: la misma retencion dos veces aparta una sola vez y devuelve la misma retencion")
    void mismaRetencionUnSoloEfecto() {
        var una = retener("r1", "300.00", "DISPUTA", ctx);
        var otra = retener("r1", "300.00", "DISPUTA", ctx);
        assertThat(otra.retencionId()).isEqualTo(una.retencionId());
        assertThat(retenido()).isEqualTo(300);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE cuenta_billetera_id=?",
                        cuenta))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.respuesta_idempotente WHERE usuario_id=? AND operacion='RETENER_SALDO' AND length(hash_solicitud)=64",
                        usuario))
                .as("la huella queda persistida")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza la misma clave con otro importe o motivo: no aparta nada mas")
    void otraPeticionConLaMismaClave() {
        retener("r2", "300.00", "DISPUTA", ctx);
        assertThatThrownBy(() -> retener("r2", "301.00", "DISPUTA", ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("idempotencia");
        assertThatThrownBy(() -> retener("r2", "300.00", "ENTREGA_EN_CURSO", ctx))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(retenido()).isEqualTo(300);
    }

    @Test
    @DisplayName("concurrencia: tres reintentos simultaneos de la misma retencion apartan una sola vez")
    void reintentoConcurrente() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        List<Future<SalidaRetencion>> salidas = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                salidas.add(pool.submit(() -> retener("par", "300.00", "DISPUTA", ctx)));
            }
            UUID primera = salidas.get(0).get().retencionId();
            for (var salida : salidas) {
                assertThat(salida.get().retencionId()).isEqualTo(primera);
            }
        } finally {
            pool.shutdown();
        }
        assertThat(retenido()).isEqualTo(300);
    }

    @Test
    @DisplayName("rechaza que otra persona aparte el saldo de mi billetera o libere mis retenciones")
    void soloElTitularRetieneYLibera() {
        var intruso = contextoDe(fixtura.usuario());
        assertThatThrownBy(() -> retener("intruso", "100.00", "DISPUTA", intruso))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> transaccion.execute(t -> retencionCU.retener(
                        EntradaRetencion.simple(cuenta, Dinero.de("100.00", Moneda.BOB), "DISPUTA"), intruso)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(retenido()).isZero();

        var mia = retener("mia", "300.00", "DISPUTA", ctx);
        assertThatThrownBy(() -> transaccion.execute(t -> retencionCU.liberar(mia.retencionId(), intruso)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> transaccion.execute(t -> retencionCU.ejecutar(mia.retencionId(), intruso)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(retenido()).isEqualTo(300);

        var sistema =
                ContextoSesion.deSistema(usuario, new Traza(UUID.randomUUID().toString()));
        transaccion.execute(t -> retencionCU.liberar(mia.retencionId(), sistema));
        assertThat(retenido()).isZero();
    }
}
