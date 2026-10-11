package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.SalidaCobro;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-21 · el reintento con la MISMA clave que llega mientras el original todavia no confirmo.
 *
 * <p>Hallado por el ensayo de integracion real (carril G): ocho envios simultaneos de la misma clave daban un 201, tres 200 y
 * cuatro 422 «Esa obligacion esta PAGADO». El dinero no se duplicaba (un solo pago), pero el cliente que reintenta por un
 * corte de red recibia un rechazo en lugar de la respuesta del original. La causa: la clave se miraba ANTES del candado de la
 * obligacion y nunca despues.
 */
class CU21ReintentoConcurrenteTest extends BaseDeAportes {

    private static final int ENVIOS = 8;

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private SalidaCobro cobrar(UUID obligacionId, String clave, ContextoSesion ctx) {
        var bob = Dinero.de("500.00", Moneda.BOB);
        return transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        clave,
                        obligacionId,
                        bob,
                        Dinero.de("0.00", Moneda.BOB),
                        "BILLETERA_MOVIL",
                        "ref-" + clave,
                        Optional.empty(),
                        false,
                        true),
                ctx));
    }

    @Test
    @DisplayName(
            "concurrencia: la misma clave en simultaneo devuelve a todos el pago del original y no cobra dos veces")
    void concurrenciaDeLaMismaClave() throws Exception {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);
        ContextoSesion ctx = contextoDe(usuario);
        var barrera = new CyclicBarrier(ENVIOS);
        ExecutorService pool = Executors.newFixedThreadPool(ENVIOS);
        try {
            List<Future<SalidaCobro>> envios = new ArrayList<>();
            for (int i = 0; i < ENVIOS; i++) {
                envios.add(pool.submit(() -> {
                    barrera.await();
                    return cobrar(obligacion.id(), "cob-misma-clave", ctx);
                }));
            }
            List<SalidaCobro> salidas = new ArrayList<>();
            for (Future<SalidaCobro> envio : envios) {
                salidas.add(envio.get()); // un 422 aca lanzaria ErrorDeNegocio: el reintento NO debe fallar
            }

            assertThat(salidas)
                    .extracting(SalidaCobro::pagoId)
                    .containsOnly(salidas.get(0).pagoId());
            assertThat(salidas).filteredOn(SalidaCobro::esNuevo).hasSize(1);
            assertThat(contar("SELECT count(*)::int FROM aportes.pago WHERE obligacion_id = ?", obligacion.id()))
                    .isEqualTo(1);
            assertThat(contar("SELECT monto_pagado::int FROM aportes.obligacion_aporte WHERE id = ?", obligacion.id()))
                    .isEqualTo(500);
        } finally {
            pool.shutdownNow();
        }
    }
}
