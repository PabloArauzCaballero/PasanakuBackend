package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAsientos.Resultado;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H8.S1.M4 · El mayor registra el desembolso que el proveedor CONFIRMO: {@code entregas.desembolso_acreditado} trae el
 * asiento propuesto (POZO_GRUPO al debe, DESEMBOLSO_CONFIRMADO al haber, por el principal COMPLETO) y nucleo-financiero
 * lo asienta UNA vez. El productor (entregas) es un doble de CONTRATO: el evento se arma con la forma exacta de
 * {@code AsientoDeEntrega.comoCarga}. El recorrido contra entregas y Kafka reales vive en
 * {@code despliegue/ensayos/integracion}.
 */
class CU24EntregasEventosTest extends BaseDeEventosDeAsientos {

    @BeforeAll
    static void mapeoDelDesembolso() {
        for (String moneda : java.util.List.of("BOB", "USD")) {
            String codigo =
                    "L.DES." + moneda + "." + UUID.randomUUID().toString().substring(0, 6);
            fixtura.cuentaDeMovimiento(codigo, "ACTIVO", "DEUDORA", moneda);
            MAPEO.put("DESEMBOLSO_CONFIRMADO." + moneda, codigo);
        }
    }

    @Test
    @DisplayName(
            "evento duplicado: el desembolso acreditado de una entrega, entregado dos veces, se asienta UNA vez y cuadra por el principal completo")
    void elDesembolsoSeAsientaUnaVez() {
        UUID entrega = UUID.randomUUID();
        var e = evento(
                UUID.randomUUID(),
                "entregas.desembolso_acreditado",
                carga("ENTREGA", entrega, "POZO_GRUPO", "DESEMBOLSO_CONFIRMADO", "6000.00", "BOB"));

        var primera = procesar(e);
        var segunda = procesar(e);

        assertThat(primera.resultado()).isEqualTo(Resultado.REGISTRADO);
        assertThat(segunda.resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(asientos(entrega)).isEqualTo(1);
        assertThat(marcas(e.eventoId())).isEqualTo(1);
        var sumas = dsl.fetchOne(
                "SELECT SUM(debe), SUM(haber), count(*) FROM nucleo_financiero.movimiento_contable WHERE asiento_id = ?",
                primera.asientoId().orElseThrow());
        assertThat(sumas.get(0).toString())
                .isEqualTo("6000.00")
                .isEqualTo(sumas.get(1).toString());
        assertThat(((Number) sumas.get(2)).intValue()).isEqualTo(2);
        System.out.println("ASIENTO | evento desembolso_acreditado | debe=" + sumas.get(0) + " haber=" + sumas.get(1)
                + " | consumido 2 veces -> asientos=" + asientos(entrega));
    }

    @Test
    @DisplayName(
            "rechaza: un desembolso con una cuenta logica sin codigo en el plan (AP-CU24-02) no deja asiento ni marca de consumo")
    void sinMapeoNoSeAsienta() {
        UUID entrega = UUID.randomUUID();
        var e = evento(
                UUID.randomUUID(),
                "entregas.desembolso_acreditado",
                carga("ENTREGA", entrega, "POZO_GRUPO", "CUENTA_QUE_NO_EXISTE", "10.00", "BOB"));

        assertThatThrownBy(() -> procesar(e))
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU24-02"));

        assertThat(asientos(entrega)).isZero();
        assertThat(marcas(e.eventoId())).isZero();
    }
}
