package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAsientos;
import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAsientos.Resultado;
import bo.aportaya.nucleofinanciero.infraestructura.AsientoRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaContableRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D5 / H2.S1.M4 · El mayor registra los asientos que garantia PROPONE en sus eventos ({@code AsientoPropuesto.comoCarga}):
 * una vez por evento, con la marca de consumo y el asiento en la misma transaccion, y la reversa como contra-asiento
 * enlazado. El productor (garantia) es un doble de CONTRATO: el evento se arma con la forma exacta de {@code comoCarga}.
 */
class CU24EventosTest extends BaseDeEventosDeAsientos {

    @Test
    @DisplayName(
            "Dado un evento de garantía con un asiento propuesto · Cuando se consume dos veces · Entonces existe un solo asiento que cuadra en su moneda")
    void criterio5() {
        UUID cobertura = UUID.randomUUID();
        var e = evento(
                UUID.randomUUID(),
                "garantia.cobertura_empresarial_aplicada",
                carga("COBERTURA", cobertura, "POZO_GRUPO", "RESERVA_RESPALDO", "1500.75", "USD"));

        var primera = procesar(e);
        var segunda = procesar(e);

        assertThat(primera.resultado()).isEqualTo(Resultado.REGISTRADO);
        assertThat(segunda.resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(asientos(cobertura)).isEqualTo(1);
        assertThat(marcas(e.eventoId())).isEqualTo(1);
        var sumas = dsl.fetchOne(
                "SELECT SUM(debe), SUM(haber), min(moneda), count(*) FROM nucleo_financiero.movimiento_contable WHERE asiento_id = ?",
                primera.asientoId().orElseThrow());
        assertThat(sumas.get(0).toString()).isEqualTo(sumas.get(1).toString());
        assertThat(sumas.get(2)).isEqualTo("USD");
        System.out.println("ASIENTO | evento cobertura_empresarial_aplicada | debe=" + sumas.get(0) + " haber="
                + sumas.get(1) + " moneda=" + sumas.get(2) + " lineas=" + sumas.get(3)
                + " | consumido 2 veces -> asientos=" + asientos(cobertura));
    }

    @Test
    @DisplayName(
            "Dado un evento de reversa de una cobertura ya asentada · Cuando se consume · Entonces nace un contra-asiento enlazado al original y el original queda intacto")
    void criterio6() {
        UUID cobertura = UUID.randomUUID();
        var aplicada = procesar(evento(
                UUID.randomUUID(),
                "garantia.cobertura_empresarial_aplicada",
                carga("COBERTURA", cobertura, "POZO_GRUPO", "RESERVA_RESPALDO", "800.00", "BOB")));
        var reversaEvento = evento(
                UUID.randomUUID(),
                "garantia.cobertura_empresarial_reversada",
                carga("COBERTURA", cobertura, "RESERVA_RESPALDO", "POZO_GRUPO", "800.00", "BOB"));

        var reversa = procesar(reversaEvento);
        var otraVez = procesar(reversaEvento);

        assertThat(reversa.resultado()).isEqualTo(Resultado.REVERSADO);
        assertThat(otraVez.resultado()).isEqualTo(Resultado.DUPLICADO);
        var fila = dsl.fetchOne(
                "SELECT asiento_reversa_id, estado FROM nucleo_financiero.asiento_contable WHERE id = ?",
                reversa.asientoId().orElseThrow());
        assertThat((UUID) fila.get(0)).isEqualTo(aplicada.asientoId().orElseThrow());
        assertThat((String) fila.get(1)).isEqualTo("REVERSADO");
        assertThat(dsl.fetchOne(
                                "SELECT estado FROM nucleo_financiero.asiento_contable WHERE id = ?",
                                aplicada.asientoId().get())
                        .get(0))
                .as("el original no se edita")
                .isEqualTo("CONFIRMADO");
        assertThat(asientos(cobertura))
                .as("original + contra-asiento, nada borrado")
                .isEqualTo(2);
        System.out.println("REVERSA | cobertura " + cobertura + " | original="
                + aplicada.asientoId().get() + " contra-asiento="
                + reversa.asientoId().get() + " asientos=" + asientos(cobertura));
    }

    @Test
    @DisplayName("rechaza una cuenta logica sin mapeo con AP-CU24-02: el evento no se asienta ni se marca consumido")
    void cuentaLogicaSinMapeo() {
        UUID origen = UUID.randomUUID();
        var e = evento(
                UUID.randomUUID(),
                "garantia.respaldo_reservado",
                carga("AJUSTE", origen, "CUENTA_QUE_CONTABILIDAD_NO_DEFINIO", "CAJA_EMPRESA", "10.00", "BOB"));
        assertThatThrownBy(() -> procesar(e))
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU24-02"));
        assertThat(asientos(origen)).isZero();
        assertThat(marcas(e.eventoId())).isZero();
        // Y la moneda cuenta: el mapeo de BOB no sirve para USD si no esta definido.
        sinMapeoParaLaMoneda();
    }

    private void sinMapeoParaLaMoneda() {
        var sinUsd = new ConsumoDeAsientos(
                new Datos(dsl),
                registrar,
                new AsientoRepositorio(),
                new CuentaContableRepositorio(),
                (l, m) -> Optional.empty());
        var e = evento(
                UUID.randomUUID(),
                "garantia.respaldo_reservado",
                carga("AJUSTE", UUID.randomUUID(), "RESERVA_RESPALDO", "CAJA_EMPRESA", "10.00", Moneda.USD.name()));
        assertThatThrownBy(() -> transaccion.execute(t -> sinUsd.procesar(e)))
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU24-02"));
    }

    @Test
    @DisplayName(
            "fuera de orden: la reversa antes que la aplicacion se rechaza sin efectos y se asienta cuando la aplicacion llega")
    void fueraDeOrden() {
        UUID cobertura = UUID.randomUUID();
        var reversa = evento(
                UUID.randomUUID(),
                "garantia.cobertura_empresarial_reversada",
                carga("COBERTURA", cobertura, "RESERVA_RESPALDO", "POZO_GRUPO", "50.00", "BOB"));
        assertThatThrownBy(() -> procesar(reversa))
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU24-06"));
        assertThat(asientos(cobertura)).isZero();
        assertThat(marcas(reversa.eventoId())).as("sin marca: se reintenta").isZero();

        procesar(evento(
                UUID.randomUUID(),
                "garantia.cobertura_empresarial_aplicada",
                carga("COBERTURA", cobertura, "POZO_GRUPO", "RESERVA_RESPALDO", "50.00", "BOB")));
        assertThat(procesar(reversa).resultado()).isEqualTo(Resultado.REVERSADO);
        assertThat(asientos(cobertura)).isEqualTo(2);
    }

    @Test
    @DisplayName("rechaza una reversa cuyas partidas no son el inverso del original, con AP-CU24-05")
    void reversaQueNoCoincide() {
        UUID cobertura = UUID.randomUUID();
        procesar(evento(
                UUID.randomUUID(),
                "garantia.cobertura_empresarial_aplicada",
                carga("COBERTURA", cobertura, "POZO_GRUPO", "RESERVA_RESPALDO", "100.00", "BOB")));
        var distinta = evento(
                UUID.randomUUID(),
                "garantia.cobertura_empresarial_reversada",
                carga("COBERTURA", cobertura, "RESERVA_RESPALDO", "POZO_GRUPO", "99.99", "BOB"));
        assertThatThrownBy(() -> procesar(distinta))
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU24-05"));
        assertThat(asientos(cobertura)).isEqualTo(1);
        assertThat(marcas(distinta.eventoId())).isZero();
    }

    @Test
    @DisplayName("rechaza un asiento propuesto que mezcla monedas o no se puede leer (AP-CU24-04 / AP-CU24-07)")
    void eventoIncorrecto() {
        var mezcla = carga("AJUSTE", UUID.randomUUID(), "CAJA_EMPRESA", "RESERVA_RESPALDO", "10.00", "BOB");
        ((com.fasterxml.jackson.databind.node.ObjectNode)
                        mezcla.get("asiento").get("partidas").get(1))
                .put("moneda", "USD");
        assertThatThrownBy(() -> procesar(evento(UUID.randomUUID(), "garantia.respaldo_reservado", mezcla)))
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU24-04"));

        var ilegible = carga("AJUSTE", UUID.randomUUID(), "CAJA_EMPRESA", "RESERVA_RESPALDO", "10.001", "BOB");
        assertThatThrownBy(() -> procesar(evento(UUID.randomUUID(), "garantia.respaldo_reservado", ilegible)))
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU24-07"));
        var origenRaro = carga("NO_EXISTE", UUID.randomUUID(), "CAJA_EMPRESA", "RESERVA_RESPALDO", "10.00", "BOB");
        assertThatThrownBy(() -> procesar(evento(UUID.randomUUID(), "garantia.respaldo_reservado", origenRaro)))
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU24-07"));
    }

    @Test
    @DisplayName("cuadre: un evento sin asiento propuesto no registra nada")
    void sinAsiento() {
        var e = evento(
                UUID.randomUUID(),
                "garantia.respaldo_insuficiente",
                JSON.createObjectNode().put("grupoId", "x"));
        assertThat(procesar(e).resultado()).isEqualTo(Resultado.SIN_ASIENTO);
        assertThat(marcas(e.eventoId())).isZero();
    }
}
