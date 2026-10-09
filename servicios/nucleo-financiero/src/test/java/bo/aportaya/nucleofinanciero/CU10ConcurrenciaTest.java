package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo.EntradaSolicitud;
import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo.SalidaSolicitud;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-10 · Recargar saldo, en paralelo.
 *
 * <p>Vive aparte de {@code CU10Test} por la regla de las 300 lineas, y la separacion se
 * lee bien: aquella fija el comportamiento de una recarga, esta fija que dos recargas a
 * la vez sobre la misma billetera no se pisen.
 */
class CU10ConcurrenciaTest extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private UUID billeteraConLimite() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("RECARGA", ESTANDAR, "MES", new BigDecimal("10000.00"), null);
        return fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
    }

    private SalidaSolicitud solicitar(UUID cuenta, String monto, String clave, ContextoSesion ctx) {
        return transaccion.execute(e -> recargaCU.solicitar(
                new EntradaSolicitud(clave, cuenta, bob(monto), bob("0.00"), "QR", Optional.empty()), ctx));
    }

    @Test
    @DisplayName("concurrencia: dos acreditaciones a la vez sobre la misma billetera no pierden ningun movimiento")
    void dosAcreditacionesALaVez() throws Exception {
        // La prueba de arriba que se llama «concurrencia» es en realidad secuencial:
        // comprueba idempotencia, no paralelismo. Esta si corre las dos a la vez, y lo que
        // fija es que el saldo sea la suma: `ck_cuenta_saldo_no_negativo` se evalua sobre
        // esa columna, asi que si un movimiento se pierde el control de sobregiro empieza
        // a mirar un numero falso.
        //
        // Lo que esta prueba NO hace, y conviene decirlo: no reproduce el interbloqueo que
        // tenia el equivalente contable de CU-24. Se probo —pasa igual con el `FOR UPDATE`
        // viejo y con el `FOR NO KEY UPDATE` nuevo—, porque `acreditar` hace mas trabajo
        // antes de tocar el saldo y las dos transacciones no se cruzan tan justo. El nivel
        // de bloqueo de `fn_bil_recalcular_saldos` se corrigio igual, por ser el correcto
        // para lo que esa funcion escribe, no porque esta prueba lo exija.
        UUID cuenta = billeteraConLimite();
        ContextoSesion ctx = contextoDe(fixtura.usuario());
        SalidaSolicitud una = solicitar(cuenta, "300.00", "rec-par-1", ctx);
        SalidaSolicitud otra = solicitar(cuenta, "450.00", "rec-par-2", ctx);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> primera =
                    pool.submit(() -> transaccion.execute(e -> recargaCU.acreditar(una.ordenRecargaId(), ctx)));
            Future<?> segunda =
                    pool.submit(() -> transaccion.execute(e -> recargaCU.acreditar(otra.ordenRecargaId(), ctx)));
            primera.get();
            segunda.get();
        } finally {
            pool.shutdown();
        }

        // Las dos entraron y el saldo es la suma: ninguna piso a la otra.
        assertThat(contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", cuenta))
                .isEqualTo(750);
    }
}
