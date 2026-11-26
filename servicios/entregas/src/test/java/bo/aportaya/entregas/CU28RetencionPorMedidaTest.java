package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.entregas.aplicacion.CU28EmitirDesembolso.EntradaOrden;
import bo.aportaya.entregas.aplicacion.CU28EmitirDesembolso.SalidaOrden;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-28 × CU-134 (D-11 y A-1) contra PostgreSQL real: la evidencia de la regla 91.6 de la retención por medida.
 *
 * <p>Retener no mueve dinero: no hay orden, ni intento, ni evento con monto, y la entrega sigue AUTORIZADA. Liberada
 * la medida (en identidad), la MISMA clave emite UNA sola orden por el neto de la entrega, y reintentarla devuelve la
 * misma orden. Reversa y redondeo no aplican: la retención no genera asiento ni divide importes.
 */
class CU28RetencionPorMedidaTest extends EscenarioDeDesembolso {

    @Test
    @DisplayName(
            "Dada una entrega retenida por una medida de CU-134 · Cuando la emisión se reintenta con la misma clave y"
                    + " luego la medida se libera · Entonces mientras está retenida no hay orden ni evento de desembolso"
                    + " · Y liberada la misma clave emite una sola orden por el neto de la entrega")
    void retenidaYLuegoLiberada() {
        Caso c = caso(true, true);
        EntradaOrden libre = orden(c, "des-retencion-916");
        EntradaOrden retenida = new EntradaOrden(
                libre.entregaId(),
                libre.proveedorId(),
                libre.cuentaDestinoId(),
                libre.glosa(),
                libre.claveIdempotencia(),
                libre.saldoRetenido(),
                true);

        // Retenida, dos veces (un reintento del orquestador): las dos se rechazan y nada se mueve.
        for (int intento = 0; intento < 2; intento++) {
            assertThatThrownBy(() -> transaccion.execute(t -> desembolsoCU.emitir(retenida, c.ctx())))
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU28-08"));
        }
        assertThat(ordenes(c)).isZero();
        assertThat(eventosDeDesembolso(c)).isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM entregas.entrega_fondo WHERE id = ? AND estado = 'AUTORIZADA'",
                        c.entregaId()))
                .isEqualTo(1);

        // Liberada: la misma clave emite una sola orden, y el reintento devuelve esa misma orden.
        SalidaOrden primera = transaccion.execute(t -> desembolsoCU.emitir(libre, c.ctx()));
        SalidaOrden repetida = transaccion.execute(t -> desembolsoCU.emitir(libre, c.ctx()));
        assertThat(primera.esNueva()).isTrue();
        assertThat(repetida.esNueva()).isFalse();
        assertThat(repetida.ordenId()).isEqualTo(primera.ordenId());
        assertThat(ordenes(c)).isEqualTo(1);
        assertThat(eventosDeDesembolso(c)).isEqualTo(1);

        // Cuadre: la orden es exactamente el neto de la entrega, en su moneda.
        BigDecimal monto = (BigDecimal)
                dsl.fetchValue("SELECT monto FROM entregas.orden_desembolso WHERE id = ?", primera.ordenId());
        BigDecimal neto = (BigDecimal)
                dsl.fetchValue("SELECT monto_neto_a_entregar FROM entregas.entrega_fondo WHERE id = ?", c.entregaId());
        assertThat(monto).isEqualByComparingTo(neto);
    }

    private int ordenes(Caso c) {
        return contar("SELECT count(*)::int FROM entregas.orden_desembolso WHERE entrega_id = ?", c.entregaId());
    }

    private int eventosDeDesembolso(Caso c) {
        return contar(
                """
                SELECT count(*)::int FROM entregas.evento_dominio
                WHERE tipo = 'entregas.desembolso_ordenado' AND payload->>'entregaId' = ?
                """,
                c.entregaId().toString());
    }
}
