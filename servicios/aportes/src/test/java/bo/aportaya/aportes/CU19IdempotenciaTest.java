package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CU19ReembolsarPago.EntradaReembolso;
import bo.aportaya.aportes.aplicacion.CU19ReembolsarPago.SalidaSolicitud;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** F1.L1.M09 (ANF-RESILIENCIA-001) · CU-19 con la Idempotency-Key de verdad, contra la base (R-BIL-28). */
class CU19IdempotenciaTest extends BaseDeAportes {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private UUID pagoDe(UUID usuario) {
        var obligacion = fixtura.obligacion(usuario, "100.00", 10);
        return transaccion
                .execute(t -> cobroCU.acreditar(
                        new EntradaCobro(
                                "cob-" + UUID.randomUUID(),
                                obligacion.id(),
                                bob("100.00"),
                                bob("0.00"),
                                "BILLETERA_MOVIL",
                                "ref-" + UUID.randomUUID(),
                                Optional.empty(),
                                false,
                                true),
                        contextoDe(usuario)))
                .pagoId();
    }

    private SalidaSolicitud pedir(UUID pago, String monto, String clave, ContextoSesion ctx) {
        return transaccion.execute(
                t -> reembolsoCU.solicitar(new EntradaReembolso(pago, bob(monto), "DUPLICADO", clave), ctx));
    }

    @Test
    @DisplayName("reintento: la misma clave dos veces en serie devuelve el mismo reembolsoId y deja una sola fila")
    void reintentoEnSerie() {
        UUID usuario = fixtura.usuario();
        UUID pago = pagoDe(usuario);
        var ctx = contextoDe(usuario);

        var primera = pedir(pago, "40.00", "clave-serie", ctx);
        var segunda = pedir(pago, "40.00", "clave-serie", ctx);

        assertThat(segunda.reembolsoId()).isEqualTo(primera.reembolsoId());
        assertThat(contar("SELECT count(*)::int FROM aportes.reembolso WHERE pago_id = ?", pago))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.evento_dominio WHERE tipo = 'aportes.reembolso_solicitado' AND agregado_id = ?",
                        primera.reembolsoId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: ocho envios simultaneos con la misma clave dejan una sola solicitud y un solo id")
    void reintentoConcurrente() throws Exception {
        UUID usuario = fixtura.usuario();
        UUID pago = pagoDe(usuario);
        var ctx = contextoDe(usuario);
        Set<UUID> ids = ConcurrentHashMap.newKeySet();
        List<Callable<Void>> tareas = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tareas.add(() -> {
                ids.add(pedir(pago, "40.00", "clave-paralela", ctx).reembolsoId());
                return null;
            });
        }
        var hilos = Executors.newFixedThreadPool(8);
        try {
            for (var f : hilos.invokeAll(tareas)) {
                f.get();
            }
        } finally {
            hilos.shutdown();
        }

        assertThat(ids).hasSize(1);
        assertThat(contar("SELECT count(*)::int FROM aportes.reembolso WHERE pago_id = ?", pago))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza la misma clave con otro monto (AP-CU19-02): no es un reintento y no crea otra solicitud")
    void rechazaLaClaveConOtroMonto() {
        UUID usuario = fixtura.usuario();
        UUID pago = pagoDe(usuario);
        var ctx = contextoDe(usuario);
        pedir(pago, "40.00", "clave-otra", ctx);

        assertThatThrownBy(() -> pedir(pago, "41.00", "clave-otra", ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("otra solicitud");
        assertThat(contar("SELECT count(*)::int FROM aportes.reembolso WHERE pago_id = ?", pago))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza en la base un segundo reembolso con la misma clave sobre el mismo pago (R-BIL-28)")
    void rechazaLaBaseElDuplicado() {
        UUID usuario = fixtura.usuario();
        UUID pago = pagoDe(usuario);
        var ctx = contextoDe(usuario);
        var primera = pedir(pago, "40.00", "clave-base", ctx);

        assertThatThrownBy(() -> transaccion.execute(t -> dsl.execute(
                        """
                        INSERT INTO aportes.reembolso
                            (pago_id, monto, motivo, estado, solicitado_por, clave_idempotencia, fecha_solicitud)
                        SELECT pago_id, monto, motivo, estado, solicitado_por, clave_idempotencia, now()
                          FROM aportes.reembolso WHERE id = ?
                        """,
                        primera.reembolsoId())))
                .hasMessageContaining("uq_reembolso_clave");
    }

    @Test
    @DisplayName(
            "reintento: quien ya aprobo un reembolso y reintenta recibe la misma respuesta y la obligacion se mueve una vez")
    void reintentoDeLaAprobacion() {
        UUID solicitante = fixtura.usuario();
        var aprobador = contextoDe(fixtura.usuario());
        UUID pago = pagoDe(solicitante);
        var pedido = pedir(pago, "40.00", "clave-aprobar", contextoDe(solicitante));

        var primera = transaccion.execute(t -> reembolsoCU.aprobar(pedido.reembolsoId(), aprobador));
        var segunda = transaccion.execute(t -> reembolsoCU.aprobar(pedido.reembolsoId(), aprobador));

        assertThat(segunda).isEqualTo(primera);
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.historial_estado_reembolso WHERE reembolso_id = ?",
                        pedido.reembolsoId()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.evento_dominio WHERE tipo = 'aportes.reembolso_ejecutado' AND agregado_id = ?",
                        pedido.reembolsoId()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT monto_pagado::int FROM aportes.obligacion_aporte o JOIN aportes.pago p ON p.obligacion_id = o.id WHERE p.id = ?",
                        pago))
                .isEqualTo(60);
    }

    @Test
    @DisplayName("rechaza que otra persona apruebe un reembolso ya ejecutado (AP-CU19-03)")
    void rechazaOtraAprobacion() {
        UUID solicitante = fixtura.usuario();
        UUID pago = pagoDe(solicitante);
        var pedido = pedir(pago, "40.00", "clave-otra", contextoDe(solicitante));
        transaccion.execute(t -> reembolsoCU.aprobar(pedido.reembolsoId(), contextoDe(fixtura.usuario())));

        var otro = contextoDe(fixtura.usuario());
        assertThatThrownBy(() -> transaccion.execute(t -> reembolsoCU.aprobar(pedido.reembolsoId(), otro)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no esta solicitado");
    }
}
