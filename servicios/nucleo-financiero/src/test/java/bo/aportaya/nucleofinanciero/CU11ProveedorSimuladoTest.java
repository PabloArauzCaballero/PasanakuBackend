package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.SalidaRetiro;
import bo.aportaya.nucleofinanciero.aplicacion.RetirosConProveedor;
import bo.aportaya.nucleofinanciero.aplicacion.RetirosConProveedor.Desenlace;
import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor;
import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor.Tipo;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
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
 * H4.S1.M4 · Pago con resultado desconocido, contra un proveedor que se porta mal a proposito.
 *
 * <p>PostgreSQL y casos de uso reales. El doble del proveedor cumple su contrato: la referencia
 * es idempotente (reenviar no liquida dos veces) y la consulta devuelve lo que liquido.
 */
class CU11ProveedorSimuladoTest extends BaseDeBilletera {

    private final ProveedorDeRetirosFalso proveedor = new ProveedorDeRetirosFalso();
    private RetirosConProveedor flujo;
    private EscenarioDeRetiro e;
    private SalidaRetiro orden;

    @BeforeEach
    void preparar() {
        flujo = FlujosDePrueba.retiros(proveedor);
        e = EscenarioDeRetiro.con("1000.00");
        orden = e.solicitar("ret-" + UUID.randomUUID(), "400.00", "5.00");
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private int disponible() {
        return contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", e.cuenta());
    }

    private int retenido() {
        return contar("SELECT saldo_retenido::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", e.cuenta());
    }

    private int debitos() {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id=? AND sentido='DEBITO' AND glosa='Retiro pagado'",
                e.cuenta());
    }

    private String estadoDeLaOrden() {
        return (String)
                dsl.fetchOne("SELECT estado FROM nucleo_financiero.orden_retiro WHERE id=?", orden.ordenRetiroId())
                        .get(0);
    }

    @Test
    @DisplayName(
            "Dado un retiro cuyo proveedor liquida pero pierde la respuesta · Cuando se despacha la orden · Entonces se consulta al proveedor en vez de reenviar y el libro paga una sola vez · Y repetir el despacho devuelve lo mismo sin otro envío")
    void liquidaYPierdeLaRespuesta() {
        proveedor.liquidaYPierde = true;

        var primero = flujo.despachar(orden.ordenRetiroId(), e.ctx());

        assertThat(primero.desenlace()).isEqualTo(Desenlace.PAGADO);
        assertThat(proveedor.envios).as("un solo envio").isEqualTo(1);
        assertThat(proveedor.operaciones).hasSize(1);
        assertThat(debitos()).isEqualTo(1);
        assertThat(disponible()).isEqualTo(600);
        assertThat(retenido()).isZero();

        var repetido = flujo.despachar(orden.ordenRetiroId(), e.ctx());
        assertThat(repetido).isEqualTo(primero);
        assertThat(proveedor.envios).isEqualTo(1);
        assertThat(debitos()).isEqualTo(1);
        assertThat(estadoDeLaOrden()).isEqualTo("PAGADA");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id=? AND transaccion_id IS NOT NULL AND referencia_proveedor=?",
                        orden.ordenRetiroId(),
                        orden.ordenRetiroId().toString()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un envío al proveedor que se pierde antes de llegar · Cuando se despacha la orden · Entonces queda en proceso con el dinero retenido y sin débito · Y el siguiente intento lo envía y, al confirmarse, el libro paga una vez")
    void pierdeAntesDeLlegar() {
        proveedor.pierdeAntes = true;

        assertThat(flujo.despachar(orden.ordenRetiroId(), e.ctx()).desenlace()).isEqualTo(Desenlace.EN_PROCESO);
        assertThat(estadoDeLaOrden()).isEqualTo("EN_PROCESO");
        assertThat(retenido()).isEqualTo(400);
        assertThat(debitos()).isZero();
        assertThat(proveedor.operaciones).isEmpty();

        assertThat(flujo.despachar(orden.ordenRetiroId(), e.ctx()).desenlace()).isEqualTo(Desenlace.EN_PROCESO);
        assertThat(proveedor.operaciones).hasSize(1);

        proveedor.resuelve(orden.ordenRetiroId(), "CONFIRMADO");
        assertThat(flujo.resolver(orden.ordenRetiroId(), e.ctx()).desenlace()).isEqualTo(Desenlace.PAGADO);
        assertThat(debitos()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un proveedor caído · Cuando se despacha la orden · Entonces no se envía nada: sin poder preguntar no hay un segundo envío a ciegas · Y la orden queda en proceso con el dinero retenido")
    void sinPoderPreguntarNoSeEnvia() {
        proveedor.caido = true;

        var resultado = flujo.despachar(orden.ordenRetiroId(), e.ctx());

        assertThat(resultado.desenlace()).isEqualTo(Desenlace.EN_PROCESO);
        assertThat(proveedor.envios).isZero();
        assertThat(retenido()).isEqualTo(400);
        assertThat(debitos()).isZero();
    }

    @Test
    @DisplayName(
            "Dada una orden despachada que el proveedor rechaza en firme · Cuando se resuelve la orden · Entonces la orden queda RECHAZADA, la retención se libera y el saldo disponible vuelve · Y el libro no se toca, tampoco al repetir la resolución")
    void rechazoLiberaLaRetencion() {
        flujo.despachar(orden.ordenRetiroId(), e.ctx());
        proveedor.resuelve(orden.ordenRetiroId(), "RECHAZADO");

        var resultado = flujo.resolver(orden.ordenRetiroId(), e.ctx());

        assertThat(resultado.desenlace()).isEqualTo(Desenlace.RECHAZADO);
        assertThat(estadoDeLaOrden()).isEqualTo("RECHAZADA");
        assertThat(disponible()).isEqualTo(1000);
        assertThat(retenido()).isZero();
        assertThat(debitos()).isZero();
        assertThat(flujo.resolver(orden.ordenRetiroId(), e.ctx()).desenlace()).isEqualTo(Desenlace.RECHAZADO);
    }

    @Test
    @DisplayName(
            "Dada una orden despachada cuya respuesta del proveedor trae otro importe o referencia · Cuando se resuelve la orden · Entonces se registra una discrepancia MONTO_DISTINTO con el importe esperado y el informado · Y la orden sigue en proceso, el dinero retenido y no se debita nada")
    void respuestaQueNoCoincideNoMueveElLibro() {
        flujo.despachar(orden.ordenRetiroId(), e.ctx());
        proveedor.dice(orden.ordenRetiroId(), "395.01", "CONFIRMADO");

        assertThatThrownBy(() -> flujo.resolver(orden.ordenRetiroId(), e.ctx())).isInstanceOf(ErrorDeNegocio.class);

        assertThat(estadoDeLaOrden()).isEqualTo("EN_PROCESO");
        assertThat(retenido()).isEqualTo(400);
        assertThat(debitos()).isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.discrepancia_proveedor WHERE referencia_id=? AND referencia_tipo='ORDEN_RETIRO' AND tipo='MONTO_DISTINTO' AND monto_esperado=395.00 AND monto_informado=395.01",
                        orden.ordenRetiroId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una orden despachada cuya consulta al proveedor llega con firma inválida · Cuando se resuelve la orden · Entonces se registra una discrepancia FIRMA_INVALIDA · Y nada se paga y el dinero sigue retenido")
    void firmaInvalidaNoPaga() {
        flujo.despachar(orden.ordenRetiroId(), e.ctx());
        proveedor.hostil = new DiscrepanciaDelProveedor(Tipo.FIRMA_INVALIDA, "c".repeat(64), "La firma no coincide.");

        assertThatThrownBy(() -> flujo.resolver(orden.ordenRetiroId(), e.ctx())).isInstanceOf(ErrorDeNegocio.class);

        assertThat(debitos()).isZero();
        assertThat(retenido()).isEqualTo(400);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.discrepancia_proveedor WHERE referencia_id=? AND tipo='FIRMA_INVALIDA'",
                        orden.ordenRetiroId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una orden ya pagada en el libro · Cuando el proveedor dice después que la rechazó · Entonces se registra un estado contradictorio · Y el pago no se deshace: sigue PAGADA con su único débito")
    void contradiccionTrasPagar() {
        proveedor.liquidaYPierde = true;
        flujo.despachar(orden.ordenRetiroId(), e.ctx());
        assertThat(estadoDeLaOrden()).isEqualTo("PAGADA");

        proveedor.resuelve(orden.ordenRetiroId(), "RECHAZADO");
        assertThatThrownBy(() -> flujo.resolver(orden.ordenRetiroId(), e.ctx())).isInstanceOf(ErrorDeNegocio.class);

        assertThat(estadoDeLaOrden()).isEqualTo("PAGADA");
        assertThat(debitos()).isEqualTo(1);
        assertThat(disponible()).isEqualTo(600);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.discrepancia_proveedor WHERE referencia_id=? AND tipo='ESTADO_CONTRADICTORIO'",
                        orden.ordenRetiroId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza que otra persona despache un retiro ajeno: solo quien pidio el retiro lo despacha")
    void otraPersonaNoDespacha() {
        var ajeno = contextoDe(fixtura.usuario());
        assertThatThrownBy(() -> flujo.despachar(orden.ordenRetiroId(), ajeno)).isInstanceOf(ErrorDeNegocio.class);
        assertThat(proveedor.envios).isZero();
        assertThat(estadoDeLaOrden()).isEqualTo("AUTORIZADA");
    }

    @Test
    @DisplayName("rechaza despachar una orden que exige dos firmas sin la segunda")
    void sinSegundaFirmaNoSale() {
        var dosFirmas = transaccion.execute(t -> retiroCU.solicitar(
                new bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro(
                        "dos-firmas",
                        e.cuenta(),
                        EscenarioDeRetiro.bob("100.00"),
                        EscenarioDeRetiro.bob("5.00"),
                        e.instrumento(),
                        true,
                        true),
                e.ctx()));
        assertThatThrownBy(() -> flujo.despachar(dosFirmas.ordenRetiroId(), e.ctx()))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(proveedor.envios).isZero();
    }

    @Test
    @DisplayName("concurrencia: dos despachos simultaneos de la misma orden, una liquidacion y un debito")
    void despachosSimultaneos() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<RetirosConProveedor.ResultadoDeRetiro>> salidas = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                salidas.add(pool.submit(() -> flujo.despachar(orden.ordenRetiroId(), e.ctx())));
            }
            for (var salida : salidas) {
                salida.get();
            }
        } finally {
            pool.shutdown();
        }
        assertThat(proveedor.operaciones).hasSize(1);
        proveedor.resuelve(orden.ordenRetiroId(), "CONFIRMADO");
        flujo.resolver(orden.ordenRetiroId(), e.ctx());
        flujo.resolver(orden.ordenRetiroId(), e.ctx());
        assertThat(debitos()).isEqualTo(1);
        assertThat(disponible()).isEqualTo(600);
    }
}
