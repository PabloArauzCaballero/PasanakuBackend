package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CU19ReembolsarPago.EntradaReembolso;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.SalidaCobro;
import bo.aportaya.aportes.aplicacion.CU21CompensarAporte;
import bo.aportaya.aportes.aplicacion.CU21CompensarAporte.AporteNoAplicado;
import bo.aportaya.aportes.aplicacion.CU21CompensarAporte.Resultado;
import bo.aportaya.aportes.infraestructura.AuditoriaRepositorio;
import bo.aportaya.aportes.infraestructura.ObligacionRepositorio;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril Q · CU-21 · el aporte PAGADO que {@code nucleo-financiero} no pudo aplicar por falta de saldo se COMPENSA: el pago
 * pasa a {@code REVERSADO} y la obligacion vuelve a estar pendiente por ese importe. El productor del aviso es un doble de
 * contrato (el sobre de {@code contratos/pactos/aportes-nucleo-financiero.json}); NO esta cubierto el recorrido con Kafka y
 * nucleo reales.
 */
class CU21CompensaTest extends BaseDeAportes {

    private static final String FALLA = "SALDO_INSUFICIENTE";

    private CU21CompensarAporte compensarCU() {
        return compensarCon(new AuditoriaRepositorio());
    }

    private CU21CompensarAporte compensarCon(AuditoriaRepositorio auditoria) {
        return new CU21CompensarAporte(
                new Datos(dsl),
                new PagoRepositorio(),
                new ObligacionRepositorio(),
                consumidos,
                new Outbox("aportes"),
                auditoria,
                Reloj.delSistema());
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private record Caso(UUID usuario, UUID obligacion, UUID pago, ContextoSesion ctx) {}

    private SalidaCobro pagar(UUID obligacion, ContextoSesion ctx, String monto) {
        return transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        "cob-" + UUID.randomUUID(),
                        obligacion,
                        bob(monto),
                        bob("0.00"),
                        "BILLETERA_MOVIL",
                        "ref-" + UUID.randomUUID(),
                        Optional.empty(),
                        false,
                        true),
                ctx));
    }

    private Caso pagado(String monto, int diasHastaVencer) {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, monto, diasHastaVencer);
        ContextoSesion ctx = contextoDe(usuario);
        return new Caso(usuario, obligacion.id(), pagar(obligacion.id(), ctx, monto).pagoId(), ctx);
    }

    private AporteNoAplicado aviso(Caso c) {
        return new AporteNoAplicado(UUID.randomUUID(), UUID.randomUUID(), c.pago(), c.obligacion(), FALLA);
    }

    private CU21CompensarAporte.Salida compensar(AporteNoAplicado a) {
        return transaccion.execute(t -> compensarCU().procesar(a));
    }

    private String estadoDelPago(UUID pago) {
        return (String) dsl.fetchOne("SELECT estado FROM aportes.pago WHERE id = ?", pago)
                .get(0);
    }

    private BigDecimal pagadoDe(UUID obligacion) {
        return (BigDecimal) dsl.fetchOne("SELECT monto_pagado FROM aportes.obligacion_aporte WHERE id = ?", obligacion)
                .get(0);
    }

    private String estadoDe(UUID obligacion) {
        return (String) dsl.fetchOne("SELECT estado FROM aportes.obligacion_aporte WHERE id = ?", obligacion)
                .get(0);
    }

    /** Cuadre: lo pagado de la obligacion es la suma de sus pagos que siguen en pie (no reversados). */
    private BigDecimal pagosVigentes(UUID obligacion) {
        return (BigDecimal) dsl.fetchOne(
                        "SELECT COALESCE(SUM(monto), 0) FROM aportes.pago WHERE obligacion_id = ? AND estado = 'ACREDITADO'",
                        obligacion)
                .get(0);
    }

    private int eventos(String tipo, UUID obligacion) {
        return contar(
                "SELECT count(*)::int FROM aportes.evento_dominio WHERE tipo = ? AND agregado_id = ?",
                tipo,
                obligacion);
    }

    @Test
    @DisplayName("Dado un aporte de Bs 500 pagado con saldo que nucleo-financiero no pudo aplicar por falta de saldo ·"
            + " Cuando aportes consume el aviso aporte_no_aplicado · Entonces el pago queda REVERSADO y la obligación"
            + " vuelve a PENDIENTE con Bs 0 pagados")
    void compensaElAportePagado() {
        Caso c = pagado("500.00", 10);
        assertThat(estadoDe(c.obligacion())).isEqualTo("PAGADO");

        var salida = compensar(aviso(c));

        assertThat(salida.resultado()).isEqualTo(Resultado.COMPENSADO);
        assertThat(salida.estadoDeLaObligacion()).contains("PENDIENTE");
        assertThat(estadoDelPago(c.pago())).isEqualTo("REVERSADO");
        assertThat(estadoDe(c.obligacion())).isEqualTo("PENDIENTE");
        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("0.00");
        assertThat(pagosVigentes(c.obligacion())).isEqualByComparingTo(pagadoDe(c.obligacion()));
        assertThat(eventos("aportes.aporte_compensado", c.obligacion())).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM comun.bitacora_evento WHERE entidad = 'pago' AND entidad_id = ?",
                        c.pago()))
                .isEqualTo(1);
        System.out.println("COMPENSA | pago=" + c.pago() + " ACREDITADO -> " + estadoDelPago(c.pago()) + " | obligacion "
                + "PAGADO -> " + estadoDe(c.obligacion()) + " | monto_pagado 500.00 -> " + pagadoDe(c.obligacion()));
    }

    @Test
    @DisplayName("evento duplicado: el mismo aporte_no_aplicado dos veces compensa UNA vez y publica UN aporte_compensado")
    void eventoDuplicado() {
        Caso c = pagado("500.00", 10);
        var a = aviso(c);

        assertThat(compensar(a).resultado()).isEqualTo(Resultado.COMPENSADO);
        assertThat(compensar(a).resultado()).isEqualTo(Resultado.DUPLICADO);

        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("0.00");
        assertThat(eventos("aportes.aporte_compensado", c.obligacion())).isEqualTo(1);
    }

    @Test
    @DisplayName("reintento: el MISMO pago llega en OTRO evento · ya esta compensado: no se revierte dos veces (la obligacion"
            + " no queda con menos de cero pagado)")
    void mismoPagoOtroEvento() {
        Caso c = pagado("500.00", 10);

        assertThat(compensar(aviso(c)).resultado()).isEqualTo(Resultado.COMPENSADO);
        assertThat(compensar(aviso(c)).resultado()).isEqualTo(Resultado.YA_COMPENSADO);

        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("0.00");
        assertThat(eventos("aportes.aporte_compensado", c.obligacion())).isEqualTo(1);
    }

    @Test
    @DisplayName("cuadre: con dos pagos (Bs 200 y Bs 300) se compensa solo el que no se aplico: queda PAGADO_PARCIAL con"
            + " Bs 200, igual a la suma de los pagos vigentes")
    void compensaSoloElPagoFallido() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);
        ContextoSesion ctx = contextoDe(usuario);
        var primero = pagar(obligacion.id(), ctx, "200.00");
        var segundo = pagar(obligacion.id(), ctx, "300.00");
        assertThat(estadoDe(obligacion.id())).isEqualTo("PAGADO");

        var salida = compensar(new AporteNoAplicado(
                UUID.randomUUID(), UUID.randomUUID(), segundo.pagoId(), obligacion.id(), FALLA));

        assertThat(salida.resultado()).isEqualTo(Resultado.COMPENSADO);
        assertThat(estadoDelPago(primero.pagoId())).isEqualTo("ACREDITADO");
        assertThat(estadoDelPago(segundo.pagoId())).isEqualTo("REVERSADO");
        assertThat(estadoDe(obligacion.id())).isEqualTo("PAGADO_PARCIAL");
        assertThat(pagadoDe(obligacion.id())).isEqualByComparingTo("200.00");
        assertThat(pagosVigentes(obligacion.id())).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("compensa una obligacion YA VENCIDA: vuelve a EN_MORA (no a PENDIENTE), para que corran los recargos")
    void obligacionVencidaVuelveAMora() {
        Caso c = pagado("500.00", -10);

        var salida = compensar(aviso(c));

        assertThat(salida.resultado()).isEqualTo(Resultado.COMPENSADO);
        assertThat(estadoDe(c.obligacion())).isEqualTo("EN_MORA");
        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("rechaza un aviso cuyo pago es de OTRA obligacion: no se compensa a ciegas, no queda marca y nada cambia")
    void rechazaElSobreIncoherente() {
        Caso c = pagado("500.00", 10);
        Caso otro = pagado("300.00", 10);
        var a = new AporteNoAplicado(UUID.randomUUID(), UUID.randomUUID(), c.pago(), otro.obligacion(), FALLA);

        assertThatThrownBy(() -> compensar(a)).isInstanceOf(IllegalArgumentException.class);

        assertThat(estadoDelPago(c.pago())).isEqualTo("ACREDITADO");
        assertThat(estadoDe(c.obligacion())).isEqualTo("PAGADO");
        assertThat(estadoDe(otro.obligacion())).isEqualTo("PAGADO");
        assertThat(contar("SELECT count(*)::int FROM aportes.evento_consumido WHERE id_evento = ?", a.eventoId()))
                .isZero();
    }

    @Test
    @DisplayName("rechaza compensar lo que ya se toco por otro camino: un pago con reembolso solicitado o en disputa no se"
            + " reversa solo (queda para una persona) y no se pierde el evento")
    void noCompensaLoReembolsadoNiEnDisputa() {
        Caso conReembolso = pagado("500.00", 10);
        transaccion.execute(t -> reembolsoCU.solicitar(
                new EntradaReembolso(conReembolso.pago(), bob("100.00"), "DUPLICADO"), contextoDe(fixtura.usuario())));
        var a1 = aviso(conReembolso);
        assertThat(compensar(a1).resultado()).isEqualTo(Resultado.NO_COMPENSABLE);
        assertThat(estadoDelPago(conReembolso.pago())).isEqualTo("ACREDITADO");
        assertThat(estadoDe(conReembolso.obligacion())).isEqualTo("PAGADO");

        Caso enDisputa = pagado("400.00", 10);
        dslFixtura.execute("UPDATE aportes.pago SET estado = 'EN_DISPUTA' WHERE id = ?", enDisputa.pago());
        assertThat(compensar(aviso(enDisputa)).resultado()).isEqualTo(Resultado.NO_COMPENSABLE);
        assertThat(estadoDe(enDisputa.obligacion())).isEqualTo("PAGADO");
        assertThat(eventos("aportes.aporte_compensado", enDisputa.obligacion())).isZero();
    }

    @Test
    @DisplayName("rechaza compensar un pago que no existe en aportes: se marca consumido sin tocar nada (no hay a quien compensar)")
    void pagoInexistente() {
        Caso c = pagado("500.00", 10);
        var a = new AporteNoAplicado(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), c.obligacion(), FALLA);

        assertThat(compensar(a).resultado()).isEqualTo(Resultado.PAGO_INEXISTENTE);

        assertThat(estadoDe(c.obligacion())).isEqualTo("PAGADO");
    }

    @Test
    @DisplayName("concurrencia: dos eventos DISTINTOS del mismo pago al mismo tiempo compensan UNA sola vez: ni cero negativo ni"
            + " dos avisos")
    void concurrencia() throws Exception {
        Caso c = pagado("500.00", 10);

        var resultados = enParalelo(
                () -> compensar(aviso(c)).resultado(), () -> compensar(aviso(c)).resultado());

        assertThat(resultados).containsExactlyInAnyOrder(Resultado.COMPENSADO, Resultado.YA_COMPENSADO);
        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("0.00");
        assertThat(estadoDe(c.obligacion())).isEqualTo("PENDIENTE");
        assertThat(eventos("aportes.aporte_compensado", c.obligacion())).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza una caida a mitad: si se cae la auditoria DESPUES de reversar el pago, todo se revierte (pago y"
            + " obligacion intactos, sin marca) y el reproceso compensa una sola vez")
    void caidaAMitad() {
        Caso c = pagado("500.00", 10);
        var a = aviso(c);
        var auditoriaCaida = new AuditoriaRepositorio() {
            @Override
            public UUID registrar(
                    DSLContext d,
                    String entidad,
                    UUID entidadId,
                    String accion,
                    UUID actorUsuarioId,
                    String actorRol,
                    UUID correlationId,
                    String valorAnteriorJson,
                    String valorNuevoJson,
                    String motivo,
                    java.time.OffsetDateTime ahora) {
                throw new IllegalStateException("se cayo la bitacora a mitad de la transaccion");
            }
        };

        assertThatThrownBy(() -> transaccion.execute(t -> compensarCon(auditoriaCaida).procesar(a)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(estadoDelPago(c.pago())).isEqualTo("ACREDITADO");
        assertThat(estadoDe(c.obligacion())).isEqualTo("PAGADO");
        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("500.00");
        assertThat(contar("SELECT count(*)::int FROM aportes.evento_consumido WHERE id_evento = ?", a.eventoId()))
                .isZero();

        assertThat(compensar(a).resultado()).isEqualTo(Resultado.COMPENSADO);
        assertThat(compensar(a).resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("0.00");
    }

    @SafeVarargs
    private <T> List<T> enParalelo(Callable<T>... tareas) throws Exception {
        var hilos = Executors.newFixedThreadPool(tareas.length);
        try {
            var largada = new CountDownLatch(1);
            var futuros = new ArrayList<Future<T>>();
            for (var tarea : tareas) {
                futuros.add(hilos.submit(() -> {
                    largada.await();
                    return tarea.call();
                }));
            }
            largada.countDown();
            var resultados = new ArrayList<T>();
            for (var f : futuros) {
                resultados.add(f.get(60, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            hilos.shutdownNow();
        }
    }
}
