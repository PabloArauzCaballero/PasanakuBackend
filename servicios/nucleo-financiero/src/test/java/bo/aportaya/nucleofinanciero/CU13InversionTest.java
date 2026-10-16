package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU13EjecutarConDestino.EntradaDebito;
import bo.aportaya.nucleofinanciero.aplicacion.CU13EjecutarConDestino.SalidaEjecucion;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-13 · debitar la inversion (lo pide {@code inversiones}, DR-INV-05), del lado PRODUCTOR y contra
 * Postgres real. Operacion de servicio a servicio: el usuario final no la ejecuta.
 */
class CU13InversionTest extends BaseDeInversiones {

    private UUID usuario;
    private UUID cuenta;
    private UUID custodia;
    private UUID orden;
    private UUID retencion;
    private ContextoSesion servicio;

    @BeforeEach
    void preparar() {
        usuario = fixtura.usuario();
        cuenta = fixtura.billetera(usuario, "ESTANDAR", new BigDecimal("1000.00"));
        custodia = sistemaFx.custodiaDeInversion("BOB");
        orden = UUID.randomUUID();
        retencion = retenerInversion(usuario, cuenta, "300.00", orden);
        servicio = comoServicio("SERVICIO_INVERSIONES");
    }

    private SalidaEjecucion debitar(String clave, UUID retencionId, String monto, ContextoSesion quien) {
        return transaccion.execute(t -> ejecucionCU.debitarInversion(
                new EntradaDebito(clave, retencionId, Dinero.de(monto, Moneda.BOB), Optional.of(usuario)), quien));
    }

    private int movimientosDe(UUID cuentaId) {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id=?",
                cuentaId);
    }

    @Test
    @DisplayName(
            "Dada una retención INVERSION_VOLUNTARIA de Bs 300 sobre una cuenta con Bs 1.000 · Cuando el servicio de inversiones la debita · Entonces el titular queda con Bs 700 disponibles y Bs 0 retenidos · Y la custodia de inversión recibe Bs 300 en una transacción que cuadra")
    void criterio1() {
        BigDecimal custodiaAntes = sistemaFx.saldoDelLibro(custodia);

        SalidaEjecucion salida = debitar("deb-1", retencion, "300.00", servicio);

        assertThat(salida.saldoDisponible().toString()).isEqualTo("700.00");
        assertThat(salida.saldoRetenido().toString()).isEqualTo("0.00");
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("700.00");
        assertThat(sistemaFx.saldoDelLibro(custodia).subtract(custodiaAntes)).isEqualByComparingTo("300.00");
        BigDecimal[] sumas = sistemaFx.sumas(salida.transaccionId());
        assertThat(sumas[0]).isEqualByComparingTo(sumas[1]).isEqualByComparingTo("300.00");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id=? AND estado='EJECUTADA'",
                        retencion))
                .isEqualTo(1);
        sistemaFx.imprimir("debitarInversion 300.00 BOB", salida.transaccionId());
    }

    @Test
    @DisplayName("reintento: la misma clave dos veces devuelve la misma transaccion y no mueve nada mas")
    void reintentoMismaClave() {
        var una = debitar("deb-2", retencion, "300.00", servicio);
        int movimientos = movimientosDe(cuenta);
        var otra = debitar("deb-2", retencion, "300.00", servicio);

        assertThat(otra.transaccionId()).isEqualTo(una.transaccionId());
        assertThat(otra.repetida()).isTrue();
        assertThat(movimientosDe(cuenta)).isEqualTo(movimientos);
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("700.00");
        System.out.println("IDEMPOTENCIA | debitarInversion x2 -> mismo transaccionId=" + una.transaccionId()
                + " movimientos antes/despues=" + movimientos + "/" + movimientosDe(cuenta));
    }

    @Test
    @DisplayName("rechaza otra clave sobre la misma orden: la retencion ya esta EJECUTADA (AP-CU13-03)")
    void otraClaveMismaOrden() {
        debitar("deb-3a", retencion, "300.00", servicio);
        assertThatThrownBy(() -> debitar("deb-3b", retencion, "300.00", servicio))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU13-03"));
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("rechaza la misma clave con otra retencion (AP-CU13-04): la clave ya identifica otra operacion")
    void claveDeOtraOperacion() {
        debitar("deb-4", retencion, "300.00", servicio);
        UUID otraRetencion = retenerInversion(usuario, cuenta, "100.00", UUID.randomUUID());
        assertThatThrownBy(() -> debitar("deb-4", otraRetencion, "100.00", servicio))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU13-04"));
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName(
            "rechaza a un usuario final aunque sea el titular y tenga BILLETERA_OPERAR (AP-CU13-09): no se mueve plata")
    void usuarioFinalNoDebita() {
        int movimientos = movimientosDe(cuenta);
        for (ContextoSesion final_ : new ContextoSesion[] {contextoDe(usuario), contextoDe(fixtura.usuario())}) {
            assertThatThrownBy(() -> debitar("deb-5", retencion, "300.00", final_))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .satisfies(e ->
                            assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU13-09"));
        }
        assertThatThrownBy(() -> debitar("deb-5", retencion, "300.00", comoServicio("SERVICIO_ENTREGAS")))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(movimientosDe(cuenta)).isEqualTo(movimientos);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id=? AND estado='VIGENTE'",
                        retencion))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "rechaza un titular que no es el dueño de la retencion: el libro verifica lo que dice el servicio llamador")
    void titularQueNoEs() {
        UUID ajeno = fixtura.usuario();
        assertThatThrownBy(() -> transaccion.execute(t -> ejecucionCU.debitarInversion(
                        new EntradaDebito("deb-6", retencion, Dinero.de("300.00", Moneda.BOB), Optional.of(ajeno)),
                        servicio)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU13-01"));
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName(
            "rechaza un importe distinto de lo retenido, aun por un centavo (AP-CU13-06): el residuo no se absorbe")
    void importeDistinto() {
        for (String monto : new String[] {"299.99", "300.01", "100.00"}) {
            assertThatThrownBy(() -> debitar("deb-7" + monto, retencion, monto, servicio))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .satisfies(e ->
                            assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU13-06"));
        }
        assertThatThrownBy(() -> Dinero.de("300.005", Moneda.BOB)).hasMessageContaining("redondea");
    }

    @Test
    @DisplayName("rechaza una retencion de otro motivo (AP-CU13-05) y una ya liberada (AP-CU13-03)")
    void motivoYEstado() {
        UUID deAporte = transaccion
                .execute(t -> retencionCU.retener(
                        bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo.EntradaRetencion.simple(
                                cuenta, Dinero.de("50.00", Moneda.BOB), "DISPUTA"),
                        contextoDe(usuario)))
                .retencionId();
        assertThatThrownBy(() -> debitar("deb-8a", deAporte, "50.00", servicio))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU13-05"));

        transaccion.execute(t -> retencionCU.liberar(retencion, contextoDe(usuario)));
        assertThatThrownBy(() -> debitar("deb-8b", retencion, "300.00", servicio))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU13-03"));
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("950.00");
    }

    @Test
    @DisplayName("rechaza sin custodia de inversion en esa moneda (AP-CU13-07): la retencion sigue vigente")
    void sinCustodia() {
        UUID enDolares = sistemaFx.billeteraEn(usuario, "USD", new BigDecimal("500.00"));
        UUID retUsd = transaccion
                .execute(t -> retencionCU.retener(
                        new bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo.EntradaRetencion(
                                enDolares,
                                Dinero.de("100.00", Moneda.USD),
                                "INVERSION_VOLUNTARIA",
                                Optional.empty(),
                                Optional.of("ORDEN_INVERSION"),
                                Optional.of(UUID.randomUUID()),
                                Optional.empty()),
                        contextoDe(usuario)))
                .retencionId();
        assertThatThrownBy(() -> transaccion.execute(t -> ejecucionCU.debitarInversion(
                        new EntradaDebito("deb-9", retUsd, Dinero.de("100.00", Moneda.USD), Optional.empty()),
                        servicio)))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU13-07"));
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id=? AND estado='VIGENTE'",
                        retUsd))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: ocho debitos de la misma orden con claves distintas mueven la plata una sola vez")
    void concurrencia() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            var tareas = new java.util.ArrayList<Future<Boolean>>();
            for (int i = 0; i < 8; i++) {
                String clave = "deb-c" + i;
                tareas.add(pool.submit(() -> {
                    try {
                        debitar(clave, retencion, "300.00", servicio);
                        return true;
                    } catch (ErrorDeNegocio rechazo) {
                        return false;
                    }
                }));
            }
            long exitos = 0;
            for (var t : tareas) {
                exitos += t.get() ? 1 : 0;
            }
            assertThat(exitos).isEqualTo(1);
        } finally {
            pool.shutdown();
        }
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE tipo='INVERSION' AND origen_id=?",
                        orden))
                .isEqualTo(1);
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("compensa: reversar el debito de inversion escribe el espejo y deja el original intacto")
    void reversa() {
        var debito = debitar("deb-r", retencion, "300.00", servicio);
        UUID tesorero = fixtura.usuario();
        var reverso = transaccion.execute(t -> reversoCU.ejecutar(
                new bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.EntradaReverso(
                        "rev-" + debito.transaccionId(),
                        debito.transaccionId(),
                        "ERROR_OPERATIVO",
                        "La orden de inversion se anulo antes de comprar la posicion",
                        tesorero),
                contextoDe(usuario)));

        assertThat(reverso.generaObligacionDeRestitucion()).isFalse();
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("1000.00");
        BigDecimal[] original = sistemaFx.sumas(debito.transaccionId());
        assertThat(original[0]).isEqualByComparingTo("300.00");
        sistemaFx.imprimir("REVERSO de debitarInversion", reverso.transaccionReversoId());
    }
}
