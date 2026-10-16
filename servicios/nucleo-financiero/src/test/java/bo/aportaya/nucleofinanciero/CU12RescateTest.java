package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU12AcreditarRescate.EntradaRescate;
import bo.aportaya.nucleofinanciero.aplicacion.CU12AcreditarRescate.SalidaRescate;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-12 · acreditar un rescate de inversion desde la custodia (lo pide {@code inversiones}, DR-INV-05), del lado
 * PRODUCTOR y contra Postgres real. Acreditar es confirmar que la plata volvio: solo el servicio, nunca el titular.
 */
class CU12RescateTest extends BaseDeInversiones {

    private UUID usuario;
    private UUID cuenta;
    private UUID custodia;
    private UUID rescate;
    private ContextoSesion servicio;

    @BeforeEach
    void preparar() {
        usuario = fixtura.usuario();
        cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        custodia = sistemaFx.custodiaDeInversion("BOB");
        fixtura.acreditar(custodia, new BigDecimal("500.00"));
        rescate = UUID.randomUUID();
        servicio = comoServicio("SERVICIO_INVERSIONES");
    }

    private SalidaRescate rescatar(
            String clave, UUID cuentaDestino, String monto, UUID referencia, ContextoSesion quien) {
        return transaccion.execute(t -> rescateCU.ejecutar(
                new EntradaRescate(clave, cuentaDestino, Dinero.de(monto, Moneda.BOB), referencia), quien));
    }

    private static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    private int transacciones(UUID referencia) {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE tipo='RESCATE_INVERSION' AND origen_id=?",
                referencia);
    }

    @Test
    @DisplayName(
            "Dada una custodia de inversión con fondos suficientes · Cuando el servicio de inversiones acredita un rescate de Bs 300 al titular · Entonces el titular recibe Bs 300 y la custodia baja Bs 300 · Y la transacción cuadra")
    void criterio5() {
        BigDecimal custodiaAntes = sistemaFx.saldoDelLibro(custodia);

        SalidaRescate salida = rescatar("res-1", cuenta, "300.00", rescate, servicio);

        assertThat(salida.saldoDespues().toString()).isEqualTo("300.00");
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("300.00");
        assertThat(custodiaAntes.subtract(sistemaFx.saldoDelLibro(custodia))).isEqualByComparingTo("300.00");
        BigDecimal[] sumas = sistemaFx.sumas(salida.transaccionId());
        assertThat(sumas[0]).isEqualByComparingTo(sumas[1]).isEqualByComparingTo("300.00");
        sistemaFx.imprimir("acreditarRescate 300.00 BOB", salida.transaccionId());
    }

    @Test
    @DisplayName(
            "reintento: la misma clave, o el mismo rescate con otra clave, devuelve el comprobante y acredita una sola vez")
    void reintento() {
        var una = rescatar("res-2", cuenta, "300.00", rescate, servicio);
        var igual = rescatar("res-2", cuenta, "300.00", rescate, servicio);
        var otraClave = rescatar("res-2-otra", cuenta, "300.00", rescate, comoServicio("SERVICIO_INVERSIONES"));

        assertThat(igual.transaccionId()).isEqualTo(una.transaccionId());
        assertThat(otraClave.transaccionId()).isEqualTo(una.transaccionId());
        assertThat(transacciones(rescate)).isEqualTo(1);
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("300.00");
        System.out.println("IDEMPOTENCIA | acreditarRescate x3 -> mismo transaccionId=" + una.transaccionId()
                + " transacciones=" + transacciones(rescate));
    }

    @Test
    @DisplayName(
            "rechaza a un usuario final, a tesoreria y al servicio de entregas (AP-CU12-08): solo inversiones acredita")
    void soloElServicio() {
        for (ContextoSesion quien : new ContextoSesion[] {
            contextoDe(usuario),
            contextoDe(fixtura.usuario()),
            comoServicio("SERVICIO_ENTREGAS"),
            comoServicio("TESORERIA")
        }) {
            assertThatThrownBy(() -> rescatar("res-3", cuenta, "300.00", rescate, quien))
                    .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU12-08"));
        }
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("0.00");
        assertThat(transacciones(rescate)).isZero();
    }

    @Test
    @DisplayName("rechaza si la custodia no cubre el rescate (AP-CU12-01): ni un centavo sale")
    void custodiaSinFondos() {
        assertThatThrownBy(() -> rescatar("res-4", cuenta, "90000000.00", rescate, servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU12-01"));
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("rechaza un destino inexistente, de otra moneda, que no es de una persona o no operativo")
    void destinoInvalido() {
        UUID enDolares = sistemaFx.billeteraEn(usuario, "USD", BigDecimal.ZERO);
        UUID cerrada = fixtura.billetera(fixtura.usuario(), "ESTANDAR", BigDecimal.ZERO);
        dslFixtura.execute("UPDATE nucleo_financiero.cuenta_billetera SET estado='LIMITADA' WHERE id=?", cerrada);
        assertThatThrownBy(() -> rescatar("res-5a", UUID.randomUUID(), "10.00", rescate, servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU12-02"));
        assertThatThrownBy(() -> rescatar("res-5b", enDolares, "10.00", rescate, servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU12-02"));
        assertThatThrownBy(() -> rescatar("res-5c", custodia, "10.00", rescate, servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU12-02"));
        assertThatThrownBy(() -> rescatar("res-5d", cerrada, "10.00", rescate, servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU12-03"));
        assertThat(transacciones(rescate)).isZero();
    }

    @Test
    @DisplayName("rechaza la misma clave, o el mismo rescate, con otro importe o destino (AP-CU12-06)")
    void claveDeOtroRescate() {
        rescatar("res-6", cuenta, "300.00", rescate, servicio);
        assertThatThrownBy(() -> rescatar("res-6", cuenta, "200.00", rescate, servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU12-06"));
        assertThatThrownBy(() -> rescatar("res-6x", cuenta, "299.99", rescate, servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU12-06"));
        UUID otra = fixtura.billetera(fixtura.usuario(), "ESTANDAR", BigDecimal.ZERO);
        assertThatThrownBy(() -> rescatar("res-6y", otra, "300.00", rescate, servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU12-06"));
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("concurrencia: seis acreditaciones del mismo rescate con claves distintas acreditan una sola vez")
    void concurrencia() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            var tareas = new ArrayList<Future<UUID>>();
            for (int i = 0; i < 6; i++) {
                String clave = "res-c" + i;
                tareas.add(pool.submit(() ->
                        rescatar(clave, cuenta, "300.00", rescate, servicio).transaccionId()));
            }
            for (var t : tareas) {
                t.get();
            }
        } finally {
            pool.shutdown();
        }
        assertThat(transacciones(rescate)).isEqualTo(1);
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("cuadre: invertir y rescatar conserva el total del sistema (titular mas custodia)")
    void idaYVuelta() {
        UUID dueno = fixtura.usuario();
        UUID suya = fixtura.billetera(dueno, "ESTANDAR", new BigDecimal("1000.00"));
        BigDecimal antes = sistemaFx.saldoDelLibro(suya).add(sistemaFx.saldoDelLibro(custodia));
        UUID orden = UUID.randomUUID();
        UUID retencion = retenerInversion(dueno, suya, "400.00", orden);
        transaccion.execute(t -> ejecucionCU.debitarInversion(
                new bo.aportaya.nucleofinanciero.aplicacion.CU13EjecutarConDestino.EntradaDebito(
                        "ida", retencion, Dinero.de("400.00", Moneda.BOB), java.util.Optional.of(dueno)),
                servicio));
        rescatar("vuelta", suya, "412.50", UUID.randomUUID(), servicio);
        // El rescate trae 12.50 de rendimiento que el aliado fondea en la custodia: el dinero solo cambia de
        // bolsillo, asi que titular + custodia no varia (el rendimiento llega a la custodia por otro asiento).
        BigDecimal despues = sistemaFx.saldoDelLibro(suya).add(sistemaFx.saldoDelLibro(custodia));
        assertThat(despues).isEqualByComparingTo(antes);
        System.out.println("CUADRE | titular+custodia antes=" + antes + " despues=" + despues);
    }

    @Test
    @DisplayName("compensa: reversar el rescate devuelve a la custodia lo acreditado y deja el original intacto")
    void reversa() {
        var salida = rescatar("res-r", cuenta, "300.00", rescate, servicio);
        var reverso = transaccion.execute(t -> reversoCU.ejecutar(
                new bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.EntradaReverso(
                        "rev-" + salida.transaccionId(),
                        salida.transaccionId(),
                        "ERROR_OPERATIVO",
                        "El aliado informo el rescate dos veces por error de conciliacion",
                        fixtura.usuario()),
                contextoDe(usuario)));
        assertThat(reverso.generaObligacionDeRestitucion()).isFalse();
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("0.00");
        sistemaFx.imprimir("REVERSO de acreditarRescate", reverso.transaccionReversoId());
    }

    @Test
    @DisplayName("rechaza la base: una segunda transaccion de rescate para el mismo origen (R-BIL-24)")
    void unicidadEnLaBase() {
        rescatar("res-u", cuenta, "10.00", rescate, servicio);
        String mensaje = rechazaLaBase(
                """
                INSERT INTO nucleo_financiero.transaccion_billetera
                    (id, tipo, estado, moneda, monto_total, origen_tipo, origen_id, canal,
                     clave_idempotencia, hash_registro, ocurrida_en, registrada_en)
                VALUES (gen_random_uuid(), 'RESCATE_INVERSION', 'INICIADA', 'BOB', 1.00, 'RESCATE_INVERSION', '%s', 'API',
                        'otra-clave', repeat('b', 64), now(), now())
                """
                        .formatted(rescate));
        assertThat(mensaje).contains("uq_tx_rescate_origen");
    }
}
