package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor;
import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor;
import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor.Tipo;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H4.S1.M1 y M2 · Lo que el proveedor dice y el libro no puede creer.
 *
 * <p>PostgreSQL y casos de uso reales; solo el proveedor es un doble. Cada prueba de
 * discrepancia fija lo mismo: el saldo y los movimientos quedan <b>exactamente</b> como
 * estaban, y la evidencia queda escrita aunque la peticion termine en error.
 */
class CU10DiscrepanciaTest extends BaseDeBilletera {

    private UUID cuenta;
    private ContextoSesion titular;
    private RecargasConProveedor flujo;
    private final ProveedorDeRecargasFalso proveedor = new ProveedorDeRecargasFalso();

    @BeforeEach
    void preparar() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("RECARGA", "ESTANDAR", "MES", new BigDecimal("10000.00"), null);
        UUID usuario = fixtura.usuario();
        cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        titular = contextoDe(usuario);
        flujo = FlujosDePrueba.recargas(proveedor);
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private UUID ordenPendiente(String clave, String monto) {
        return flujo.solicitar(
                        new CU10RecargarSaldo.EntradaSolicitud(
                                clave,
                                cuenta,
                                Dinero.de(monto, Moneda.BOB),
                                Dinero.cero(Moneda.BOB),
                                "QR",
                                Optional.empty()),
                        titular)
                .ordenRecargaId();
    }

    private int saldo() {
        return contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta);
    }

    private int movimientos() {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id=?", cuenta);
    }

    private int discrepancias(UUID orden, Tipo tipo) {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.discrepancia_proveedor WHERE referencia_id=? AND tipo=?",
                orden,
                tipo.name());
    }

    private int alertas(UUID orden) {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.evento_dominio WHERE agregado_id=? AND tipo='nucleo_financiero.discrepancia_detectada'",
                orden);
    }

    @Test
    // Traza de la prueba original: H4.S1.M2
    @DisplayName(
            "Dada una confirmación del proveedor con firma inválida · Cuando se intenta confirmar la recarga · Entonces el saldo y los movimientos no cambian y la orden sigue PENDIENTE · Y se abre una discrepancia FIRMA_INVALIDA con una alerta correlacionada")
    void firmaInvalidaNoAcreditaYAbreDiscrepancia() {
        UUID orden = ordenPendiente("firma-1", "500.00");
        proveedor.falla = new DiscrepanciaDelProveedor(Tipo.FIRMA_INVALIDA, "a".repeat(64), "La firma no coincide.");

        assertThatThrownBy(() -> flujo.confirmar(orden, titular)).isInstanceOf(ErrorDeNegocio.class);

        assertThat(saldo()).isZero();
        assertThat(movimientos()).isZero();
        assertThat(discrepancias(orden, Tipo.FIRMA_INVALIDA)).isEqualTo(1);
        assertThat(alertas(orden)).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.discrepancia_proveedor WHERE referencia_id=? AND correlacion_id=?",
                        orden,
                        UUID.fromString(titular.traza().id())))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE id=? AND estado='PENDIENTE'",
                        orden))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada la misma evidencia hostil del proveedor repetida tres veces · Cuando se intenta confirmar cada vez · Entonces la discrepancia se registra una sola vez y la alerta no se repite · Y el saldo no cambia")
    void laMismaEvidenciaSeRegistraUnaVez() {
        UUID orden = ordenPendiente("firma-2", "500.00");
        proveedor.falla = new DiscrepanciaDelProveedor(Tipo.FIRMA_INVALIDA, "b".repeat(64), "La firma no coincide.");
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> flujo.confirmar(orden, titular)).isInstanceOf(ErrorDeNegocio.class);
        }
        assertThat(discrepancias(orden, Tipo.FIRMA_INVALIDA)).isEqualTo(1);
        assertThat(alertas(orden)).isEqualTo(1);
        assertThat(saldo()).isZero();
    }

    @Test
    @DisplayName(
            "Dado un proveedor que informa un importe distinto del de la orden · Cuando se intenta confirmar la recarga · Entonces no acredita ni crea movimientos · Y la discrepancia MONTO_DISTINTO deja el importe esperado y el informado")
    void montoDistintoQuedaRegistrado() {
        UUID orden = ordenPendiente("monto-1", "500.00");
        proveedor.dice(orden, UUID.randomUUID(), "499.00", "CONFIRMADO");

        assertThatThrownBy(() -> flujo.confirmar(orden, titular)).isInstanceOf(ErrorDeNegocio.class);

        assertThat(saldo()).isZero();
        assertThat(movimientos()).isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.discrepancia_proveedor WHERE referencia_id=? AND tipo='MONTO_DISTINTO' AND monto_esperado=500.00 AND monto_informado=499.00 AND moneda='BOB'",
                        orden))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un proveedor que responde por otra referencia · Cuando se intenta confirmar la recarga · Entonces no acredita · Y queda registrada una discrepancia REFERENCIA_DISTINTA")
    void referenciaDistintaQuedaRegistrada() {
        UUID orden = ordenPendiente("ref-1", "500.00");
        proveedor.dice(UUID.randomUUID(), UUID.randomUUID(), "500.00", "CONFIRMADO");

        assertThatThrownBy(() -> flujo.confirmar(orden, titular)).isInstanceOf(ErrorDeNegocio.class);

        assertThat(saldo()).isZero();
        assertThat(discrepancias(orden, Tipo.REFERENCIA_DISTINTA)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una recarga ya acreditada en el libro · Cuando el proveedor ahora dice rechazado o confirma con otra transacción · Entonces se registra un estado contradictorio por cada respuesta · Y el saldo y los movimientos quedan intactos")
    void estadoContradictorioTrasAcreditar() {
        UUID orden = ordenPendiente("contra-1", "500.00");
        UUID transaccionDelProveedor = UUID.randomUUID();
        proveedor.dice(orden, transaccionDelProveedor, "500.00", "CONFIRMADO");
        flujo.confirmar(orden, titular);
        assertThat(saldo()).isEqualTo(500);
        int movimientosAntes = movimientos();

        proveedor.dice(orden, transaccionDelProveedor, "500.00", "RECHAZADO");
        assertThatThrownBy(() -> flujo.confirmar(orden, titular)).isInstanceOf(ErrorDeNegocio.class);

        proveedor.dice(orden, UUID.randomUUID(), "500.00", "CONFIRMADO");
        assertThatThrownBy(() -> flujo.confirmar(orden, titular)).isInstanceOf(ErrorDeNegocio.class);

        assertThat(saldo()).isEqualTo(500);
        assertThat(movimientos()).isEqualTo(movimientosAntes);
        assertThat(discrepancias(orden, Tipo.ESTADO_CONTRADICTORIO)).isEqualTo(2);
    }

    @Test
    @DisplayName(
            "Dada una orden de recarga ya cerrada en el libro · Cuando el proveedor la confirma · Entonces se registra una discrepancia de estado contradictorio · Y no se acredita")
    void confirmacionTardiaDeUnaOrdenCerrada() {
        UUID orden = ordenPendiente("tarde-1", "500.00");
        dslFixtura.execute("UPDATE nucleo_financiero.orden_recarga SET estado='EXPIRADA' WHERE id=?", orden);
        proveedor.dice(orden, UUID.randomUUID(), "500.00", "CONFIRMADO");

        assertThatThrownBy(() -> flujo.confirmar(orden, titular)).isInstanceOf(ErrorDeNegocio.class);

        assertThat(saldo()).isZero();
        assertThat(movimientos()).isZero();
        assertThat(discrepancias(orden, Tipo.ESTADO_CONTRADICTORIO)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un proveedor que rechaza legítimamente la recarga · Cuando se intenta confirmar, también al repetir · Entonces la orden queda RECHAZADA con un solo evento · Y no se abre ninguna discrepancia ni se toca el libro")
    void rechazoLegitimoCierraLaOrden() {
        UUID orden = ordenPendiente("rechazo-1", "500.00");
        proveedor.dice(orden, UUID.randomUUID(), "500.00", "RECHAZADO");

        assertThatThrownBy(() -> flujo.confirmar(orden, titular)).isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> flujo.confirmar(orden, titular))
                .as("repetir el rechazo no abre nada nuevo")
                .isInstanceOf(ErrorDeNegocio.class);

        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE id=? AND estado='RECHAZADA'",
                        orden))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.evento_dominio WHERE agregado_id=? AND tipo='nucleo_financiero.recarga_rechazada'",
                        orden))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.discrepancia_proveedor WHERE referencia_id=?",
                        orden))
                .isZero();
        assertThat(saldo()).isZero();
        assertThat(movimientos()).isZero();
    }

    @Test
    // Traza de la prueba original: H4.S1.M1
    @DisplayName("concurrencia: la confirmacion llega dos veces a la vez, un solo abono y el mismo comprobante")
    void dobleConfirmacionConcurrenteAcreditaUnaVez() throws Exception {
        UUID orden = ordenPendiente("doble-1", "500.00");
        proveedor.dice(orden, UUID.randomUUID(), "500.00", "CONFIRMADO");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<CU10RecargarSaldo.SalidaAcreditacion>> salidas = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                salidas.add(pool.submit(() -> flujo.confirmar(orden, titular)));
            }
            var primera = salidas.get(0).get();
            var segunda = salidas.get(1).get();
            assertThat(segunda).isEqualTo(primera);
        } finally {
            pool.shutdown();
        }

        assertThat(saldo()).isEqualTo(500);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id=? AND sentido='CREDITO'",
                        cuenta))
                .isEqualTo(1);
    }
}
