package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU13EjecutarConDestino.EntradaPagoATercero;
import bo.aportaya.nucleofinanciero.aplicacion.CU13EjecutarConDestino.SalidaEjecucion;
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
 * CU-13 · pagar al vendedor con la retencion del comprador (mercado de turnos, lo pide {@code entregas}).
 * {@code entregas} es un DOBLE declarado ({@link BaseDeInversiones.Ofertas}): lo verificado es el libro de
 * nucleo; el contrato real de la oferta queda pendiente de que {@code entregas} lo publique.
 */
class CU13MercadoTest extends BaseDeInversiones {

    private UUID comprador;
    private UUID cuentaComprador;
    private UUID vendedor;
    private UUID cuentaVendedor;
    private UUID ingresos;
    private UUID oferta;
    private UUID retencion;
    private ContextoSesion servicio;

    @BeforeEach
    void preparar() {
        comprador = fixtura.usuario();
        cuentaComprador = fixtura.billetera(comprador, "ESTANDAR", new BigDecimal("1000.00"));
        vendedor = fixtura.usuario();
        cuentaVendedor = fixtura.billetera(vendedor, "ESTANDAR", BigDecimal.ZERO);
        ingresos = sistemaFx.ingresosDePlataforma("BOB");
        oferta = UUID.randomUUID();
        ofertas.seCae(false);
        ofertas.publica(oferta, vendedor, "480.00", "RESERVADA");
        retencion = retenerCompra(comprador, cuentaComprador, "500.00", oferta);
        servicio = comoServicio("SERVICIO_ENTREGAS");
    }

    private SalidaEjecucion pagar(String clave, ContextoSesion quien) {
        return transaccion.execute(t -> pagoCU.ejecutar(retencion, clave, quien));
    }

    private static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    @Test
    @DisplayName(
            "Dada una retención COMPRA_EN_MERCADO de Bs 500 para una oferta de Bs 480 · Cuando el servicio de entregas la paga · Entonces el vendedor recibe Bs 480 y la plataforma Bs 20 · Y la retención queda EJECUTADA una sola vez")
    void criterio2() {
        BigDecimal ingresosAntes = sistemaFx.saldoDelLibro(ingresos);

        SalidaEjecucion salida = pagar("pag-1", servicio);

        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("480.00");
        assertThat(sistemaFx.saldoDelLibro(ingresos).subtract(ingresosAntes)).isEqualByComparingTo("20.00");
        assertThat(sistemaFx.saldoDelLibro(cuentaComprador)).isEqualByComparingTo("500.00");
        BigDecimal[] sumas = sistemaFx.sumas(salida.transaccionId());
        assertThat(sumas[0]).isEqualByComparingTo(sumas[1]).isEqualByComparingTo("500.00");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id=? AND estado='EJECUTADA'",
                        retencion))
                .isEqualTo(1);
        sistemaFx.imprimir("pagarAlTercero 500.00 = 480.00 vendedor + 20.00 plataforma", salida.transaccionId());
    }

    @Test
    @DisplayName("cuadre: el precio cuenta centavos, el reparto suma exactamente lo retenido y no sobra residuo")
    void repartoExacto() {
        ofertas.publica(oferta, vendedor, "33.33", "RESERVADA");
        UUID cien = UUID.randomUUID();
        ofertas.publica(cien, vendedor, "33.33", "RESERVADA");
        UUID ret = retenerCompra(comprador, cuentaComprador, "100.00", cien);
        var salida = transaccion.execute(t -> pagoCU.ejecutar(ret, "pag-2", servicio));
        BigDecimal[] sumas = sistemaFx.sumas(salida.transaccionId());
        assertThat(sumas[0]).isEqualByComparingTo("100.00").isEqualByComparingTo(sumas[1]);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id=? AND monto=66.67",
                        salida.transaccionId()))
                .as("los cargos son 100.00 - 33.33 = 66.67: la diferencia es de la plataforma, no se pierde")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("cuadre: un precio igual a lo retenido no escribe pata de plataforma")
    void sinCargos() {
        ofertas.publica(oferta, vendedor, "500.00", "RESERVADA");
        var salida = pagar("pag-3", servicio);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id=?",
                        salida.transaccionId()))
                .isEqualTo(2);
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName(
            "reintento: la misma clave devuelve el mismo pago sin otro asiento, aunque la oferta ya este LIQUIDADA")
    void reintento() {
        var una = pagar("pag-4", servicio);
        ofertas.publica(oferta, vendedor, "480.00", "LIQUIDADA");
        ofertas.seCae(true);
        var otra = pagar("pag-4", servicio);
        assertThat(otra.transaccionId()).isEqualTo(una.transaccionId());
        assertThat(otra.repetida()).isTrue();
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("480.00");
        System.out.println("IDEMPOTENCIA | pagarAlTercero x2 -> mismo transaccionId=" + una.transaccionId());
    }

    @Test
    @DisplayName("rechaza otra clave sobre una retencion ya pagada (AP-CU13-03): una sola ejecucion por retencion")
    void unaSolaEjecucion() {
        pagar("pag-5a", servicio);
        assertThatThrownBy(() -> pagar("pag-5b", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-03"));
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("480.00");
    }

    @Test
    @DisplayName("rechaza a un usuario final, sea el comprador, el vendedor u otro con BILLETERA_OPERAR (AP-CU13-09)")
    void usuarioFinalNoPaga() {
        for (UUID quien : new UUID[] {comprador, vendedor, fixtura.usuario()}) {
            assertThatThrownBy(() -> pagar("pag-6", contextoDe(quien)))
                    .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-09"));
        }
        assertThatThrownBy(() -> pagar("pag-6", comoServicio("SERVICIO_INVERSIONES")))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-09"));
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("0.00");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id=? AND estado='VIGENTE'",
                        retencion))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza si entregas no responde o la oferta no esta pagable (AP-CU13-08): no se mueve plata")
    void ofertaNoConfirmada() {
        ofertas.seCae(true);
        assertThatThrownBy(() -> pagar("pag-7", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-08"));
        ofertas.seCae(false);
        ofertas.publica(oferta, vendedor, "480.00", "CANCELADA");
        assertThatThrownBy(() -> pagar("pag-7", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-08"));
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("0.00");
        assertThat(sistemaFx.saldoDelLibro(cuentaComprador)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("rechaza un beneficiario distinto del vendedor de la retencion y un vendedor que es el comprador")
    void beneficiarioIncorrecto() {
        // El beneficiario no llega en el pedido: lo trae entregas. Si traen al comprador como vendedor, se rechaza.
        ofertas.publica(oferta, comprador, "480.00", "RESERVADA");
        assertThatThrownBy(() -> pagar("pag-8", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-08"));
        // Y una oferta que no es la que motivo la retencion tampoco se paga.
        assertThatThrownBy(() -> transaccion.execute(t -> ejecucionCU.pagarAlTercero(
                        new EntradaPagoATercero(
                                "pag-8b", retencion, UUID.randomUUID(), vendedor, Dinero.de("480.00", Moneda.BOB)),
                        servicio)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-08"));
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("rechaza un precio mayor que lo retenido o de otra moneda (AP-CU13-06): sin cargos negativos")
    void precioNoCabe() {
        ofertas.publica(oferta, vendedor, "500.01", "RESERVADA");
        assertThatThrownBy(() -> pagar("pag-9", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-06"));
        assertThatThrownBy(() -> transaccion.execute(t -> ejecucionCU.pagarAlTercero(
                        new EntradaPagoATercero("pag-9b", retencion, oferta, vendedor, Dinero.de("480.00", Moneda.USD)),
                        servicio)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-06"));
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("rechaza una retencion liberada o de otro motivo (AP-CU13-03 / AP-CU13-05)")
    void retencionNoPagable() {
        transaccion.execute(t -> retencionCU.liberar(retencion, contextoDe(comprador)));
        assertThatThrownBy(() -> pagar("pag-10", servicio))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-03"));
        UUID deInversion = retenerInversion(comprador, cuentaComprador, "10.00", UUID.randomUUID());
        assertThatThrownBy(() -> transaccion.execute(t -> pagoCU.ejecutar(deInversion, "pag-10b", servicio)))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName("concurrencia: seis pagos de la misma retencion con claves distintas pagan una sola vez")
    void concurrencia() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        long exitos = 0;
        try {
            var tareas = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 6; i++) {
                String clave = "pag-c" + i;
                tareas.add(pool.submit(() -> {
                    try {
                        pagar(clave, servicio);
                        return true;
                    } catch (ErrorDeNegocio rechazo) {
                        return false;
                    }
                }));
            }
            for (var t : tareas) {
                exitos += t.get() ? 1 : 0;
            }
        } finally {
            pool.shutdown();
        }
        assertThat(exitos).isEqualTo(1);
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("480.00");
    }

    @Test
    @DisplayName("compensa: reversar el pago devuelve al comprador lo suyo y deja el original intacto")
    void reversa() {
        var pago = pagar("pag-r", servicio);
        var reverso = transaccion.execute(t -> reversoCU.ejecutar(
                new bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.EntradaReverso(
                        "rev-" + pago.transaccionId(),
                        pago.transaccionId(),
                        "ERROR_OPERATIVO",
                        "La cesion se anulo despues del pago por una orden del comite",
                        fixtura.usuario()),
                contextoDe(comprador)));
        assertThat(reverso.generaObligacionDeRestitucion()).isFalse();
        assertThat(sistemaFx.saldoDelLibro(cuentaComprador)).isEqualByComparingTo("1000.00");
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor)).isEqualByComparingTo("0.00");
        sistemaFx.imprimir("REVERSO de pagarAlTercero", reverso.transaccionReversoId());
    }

    @Test
    @DisplayName("rechaza la base: un segundo pago de la misma oferta (R-BIL-24)")
    void unicidadEnLaBase() {
        pagar("pag-u", servicio);
        String mensaje = rechazaLaBase(
                """
                INSERT INTO nucleo_financiero.transaccion_billetera
                    (id, tipo, estado, moneda, monto_total, origen_tipo, origen_id, canal,
                     clave_idempotencia, hash_registro, ocurrida_en, registrada_en)
                VALUES (gen_random_uuid(), 'COMPRA_EN_MERCADO', 'INICIADA', 'BOB', 1.00, 'OFERTA_MERCADO', '%s', 'API',
                        'otra-clave', repeat('b', 64), now(), now())
                """
                        .formatted(oferta));
        assertThat(mensaje).contains("uq_tx_compra_mercado_origen");
    }
}
