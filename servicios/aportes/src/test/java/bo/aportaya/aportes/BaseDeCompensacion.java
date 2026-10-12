package bo.aportaya.aportes;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.SalidaCobro;
import bo.aportaya.aportes.aplicacion.CU21CompensarAporte;
import bo.aportaya.aportes.aplicacion.CU21CompensarAporte.AporteNoAplicado;
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
import org.junit.jupiter.api.AfterEach;

/**
 * El escenario compartido de las pruebas de la compensacion de CU-21 (carril Q): el aviso {@code aporte_no_aplicado} es un
 * doble de contrato y el pago se cobra de verdad con {@code CU21CobrarAporte}.
 */
abstract class BaseDeCompensacion extends BaseDeAportes {

    protected static final String FALLA = "SALDO_INSUFICIENTE";

    protected CU21CompensarAporte compensarCU() {
        return compensarCon(new AuditoriaRepositorio());
    }

    protected CU21CompensarAporte compensarCon(AuditoriaRepositorio auditoria) {
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

    protected Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    protected record Caso(UUID usuario, UUID obligacion, UUID pago, ContextoSesion ctx) {}

    protected SalidaCobro pagar(UUID obligacion, ContextoSesion ctx, String monto) {
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

    protected Caso pagado(String monto, int diasHastaVencer) {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, monto, diasHastaVencer);
        ContextoSesion ctx = contextoDe(usuario);
        return new Caso(
                usuario, obligacion.id(), pagar(obligacion.id(), ctx, monto).pagoId(), ctx);
    }

    protected AporteNoAplicado aviso(Caso c) {
        return new AporteNoAplicado(UUID.randomUUID(), UUID.randomUUID(), c.pago(), c.obligacion(), FALLA);
    }

    protected CU21CompensarAporte.Salida compensar(AporteNoAplicado a) {
        return transaccion.execute(t -> compensarCU().procesar(a));
    }

    protected String estadoDelPago(UUID pago) {
        return (String) dsl.fetchOne("SELECT estado FROM aportes.pago WHERE id = ?", pago)
                .get(0);
    }

    protected BigDecimal pagadoDe(UUID obligacion) {
        return (BigDecimal) dsl.fetchOne("SELECT monto_pagado FROM aportes.obligacion_aporte WHERE id = ?", obligacion)
                .get(0);
    }

    protected String estadoDe(UUID obligacion) {
        return (String) dsl.fetchOne("SELECT estado FROM aportes.obligacion_aporte WHERE id = ?", obligacion)
                .get(0);
    }

    /** Cuadre: lo pagado de la obligacion es la suma de sus pagos que siguen en pie (no reversados). */
    protected BigDecimal pagosVigentes(UUID obligacion) {
        return (BigDecimal) dsl.fetchOne(
                        "SELECT COALESCE(SUM(monto), 0) FROM aportes.pago WHERE obligacion_id = ? AND estado = 'ACREDITADO'",
                        obligacion)
                .get(0);
    }

    protected int eventos(String tipo, UUID obligacion) {
        return contar(
                "SELECT count(*)::int FROM aportes.evento_dominio WHERE tipo = ? AND agregado_id = ?",
                tipo,
                obligacion);
    }

    @SafeVarargs
    protected final <T> List<T> enParalelo(Callable<T>... tareas) throws Exception {
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
