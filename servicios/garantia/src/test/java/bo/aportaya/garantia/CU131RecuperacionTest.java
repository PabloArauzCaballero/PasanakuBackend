package bo.aportaya.garantia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.garantia.aplicacion.CU23RecuperarRespaldo.EntradaRecuperacion;
import bo.aportaya.garantia.aplicacion.CU23RecuperarRespaldo.Resultado;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** H8.S1.M5 · Aporte tardio y recuperacion, sin doble cobro ni doble entrega. */
class CU131RecuperacionTest extends BaseDeRespaldo {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private record Cubierto(Caso caso, UUID reservaId, UUID coberturaId) {}

    private Cubierto cubierto() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");
        var cobertura = cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));
        return new Cubierto(c, reserva.reservaId(), cobertura.coberturaId());
    }

    private bo.aportaya.garantia.aplicacion.CU23RecuperarRespaldo.SalidaRecuperacion recuperar(
            Caso c, UUID pagoId, UUID obligacion, String monto) {
        return transaccion.execute(
                t -> recuperacionCU.recuperar(new EntradaRecuperacion(pagoId, obligacion, bob(monto)), c.ctx()));
    }

    @Test
    @DisplayName(
            "Dada una cobertura de Bs 1.000 (dos lineas) · Cuando entra el aporte tardio de Bs 500 de la primera · Entonces recupera Bs 500, la exposicion baja a 500, el disponible NO se repone y el asiento devuelve caja a la empresa")
    void aporteTardioRecuperaLaEmpresa() {
        var k = cubierto();
        UUID pago = fixturaDeRespaldo.pago(k.caso().obligacionA(), "500.00");

        var salida = recuperar(k.caso(), pago, k.caso().obligacionA(), "500.00");

        assertThat(salida.resultado()).isEqualTo(Resultado.RECUPERADA);
        assertThat(salida.recuperado()).isEqualTo(bob("500.00"));
        assertThat(salida.exposicionDespues()).isEqualTo(bob("500.00"));
        assertThat(numero("SELECT monto_recuperado FROM garantia.reserva_respaldo WHERE id = ?", k.reservaId()))
                .isEqualByComparingTo("500.00");
        assertThat(numero(
                        "SELECT monto_reservado - monto_aplicado - monto_liberado FROM garantia.reserva_respaldo WHERE id = ?",
                        k.reservaId()))
                .isEqualByComparingTo("7000.00"); // el disponible NO se repuso
        assertThat(dsl.fetchOne("SELECT estado FROM garantia.cobertura_respaldo WHERE id = ?", k.coberturaId())
                        .get(0, String.class))
                .isEqualTo("RECUPERADA_PARCIAL");
        var mayor = new MayorDeDoble();
        mayor.consumirOutbox(dsl, "garantia", k.reservaId(), k.coberturaId());
        assertThat(mayor.saldo("POZO_GRUPO")).isEqualByComparingTo("500.00");
        assertThat(mayor.saldo("CAJA_EMPRESA")).isEqualByComparingTo("-7500.00");
        assertThat(mayor.sumaDeSaldos()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName(
            "Dado el mismo pago entregado dos veces · Cuando se recupera · Entonces el segundo intento no duplica nada (idempotencia por pago)")
    void elMismoPagoRecuperaUnaSolaVez() {
        var k = cubierto();
        UUID pago = fixturaDeRespaldo.pago(k.caso().obligacionA(), "500.00");

        var primera = recuperar(k.caso(), pago, k.caso().obligacionA(), "500.00");
        var segunda = recuperar(k.caso(), pago, k.caso().obligacionA(), "500.00");

        assertThat(primera.esNueva()).isTrue();
        assertThat(segunda.resultado()).isEqualTo(Resultado.YA_RECUPERADA);
        assertThat(segunda.esNueva()).isFalse();
        assertThat(contar("SELECT count(*)::int FROM garantia.recuperacion_respaldo WHERE pago_id = ?", pago))
                .isEqualTo(1);
        assertThat(numero("SELECT monto_recuperado FROM garantia.reserva_respaldo WHERE id = ?", k.reservaId()))
                .isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName(
            "Dadas las dos lineas recuperadas · Cuando llega un tercer pago de la misma obligacion · Entonces AGOTADA: no se cobra dos veces; y la cobertura queda RECUPERADA_TOTAL con exposicion cero")
    void recuperacionTotalYSinDobleCobro() {
        var k = cubierto();
        UUID pagoA = fixturaDeRespaldo.pago(k.caso().obligacionA(), "500.00");
        UUID pagoB = fixturaDeRespaldo.pago(k.caso().obligacionB(), "500.00");
        UUID pagoRepetido = fixturaDeRespaldo.pago(k.caso().obligacionA(), "500.00");

        recuperar(k.caso(), pagoA, k.caso().obligacionA(), "500.00");
        var total = recuperar(k.caso(), pagoB, k.caso().obligacionB(), "500.00");
        var agotada = recuperar(k.caso(), pagoRepetido, k.caso().obligacionA(), "500.00");

        assertThat(total.exposicionDespues()).isEqualTo(bob("0.00"));
        assertThat(dsl.fetchOne("SELECT estado FROM garantia.cobertura_respaldo WHERE id = ?", k.coberturaId())
                        .get(0, String.class))
                .isEqualTo("RECUPERADA_TOTAL");
        assertThat(agotada.resultado()).isEqualTo(Resultado.AGOTADA);
        assertThat(agotada.recuperado()).isEqualTo(bob("0.00"));
        assertThat(agotada.excedente()).isEqualTo(bob("500.00"));
        assertThat(numero("SELECT monto_recuperado FROM garantia.reserva_respaldo WHERE id = ?", k.reservaId()))
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName(
            "Dado un pago mayor a lo adelantado · Cuando se recupera · Entonces vuelve solo lo adelantado y el excedente no es de la empresa")
    void pagoConExcedente() {
        var k = cubierto();
        UUID pago = fixturaDeRespaldo.pago(k.caso().obligacionA(), "520.00");

        var salida = recuperar(k.caso(), pago, k.caso().obligacionA(), "520.00");

        assertThat(salida.recuperado()).isEqualTo(bob("500.00"));
        assertThat(salida.excedente()).isEqualTo(bob("20.00"));
    }

    @Test
    @DisplayName(
            "Dado un pago de un centavo · Cuando se recupera · Entonces recupera un centavo y deja la cobertura en RECUPERADA_PARCIAL (limite inferior)")
    void unCentavo() {
        var k = cubierto();
        UUID pago = fixturaDeRespaldo.pago(k.caso().obligacionA(), "0.01");

        var salida = recuperar(k.caso(), pago, k.caso().obligacionA(), "0.01");

        assertThat(salida.recuperado()).isEqualTo(bob("0.01"));
        assertThat(salida.exposicionDespues()).isEqualTo(bob("999.99"));
    }

    @Test
    @DisplayName(
            "Dado un pago de una obligacion que nunca fue cubierta por la empresa · Cuando se consulta · Entonces SIN_COBERTURA y no se escribe nada")
    void obligacionNuncaCubierta() {
        var k = cubierto();
        UUID ajena = fixturaDeRespaldo.otraObligacion(fixtura, k.caso().escenario(), "300.00");
        UUID pago = fixturaDeRespaldo.pago(ajena, "300.00");

        var salida = recuperar(k.caso(), pago, ajena, "300.00");

        assertThat(salida.resultado()).isEqualTo(Resultado.SIN_COBERTURA);
        assertThat(contar("SELECT count(*)::int FROM garantia.recuperacion_respaldo WHERE pago_id = ?", pago))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dada una cobertura con una recuperacion · Cuando se intenta reversar · Entonces se rechaza AP-CU23-11 (hay plata de terceros de por medio)")
    void noSeReversaConRecuperaciones() {
        var k = cubierto();
        UUID pago = fixturaDeRespaldo.pago(k.caso().obligacionA(), "500.00");
        recuperar(k.caso(), pago, k.caso().obligacionA(), "500.00");

        assertThatThrownBy(() -> transaccion.execute(t -> reversaCU.reversar(
                        k.coberturaId(), clave("reversa"), k.caso().ctx())))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU23-11"));
    }

    @Test
    @DisplayName(
            "Dado un ciclo ya cerrado (reserva liberada) · Cuando llega un aporte tardio · Entonces igual recupera para la empresa")
    void recuperaDespuesDeCerrarElCiclo() {
        var k = cubierto();
        transaccion.execute(
                t -> reservaCU.liberar(k.reservaId(), clave("liberar"), k.caso().ctx()));
        UUID pago = fixturaDeRespaldo.pago(k.caso().obligacionB(), "500.00");

        var salida = recuperar(k.caso(), pago, k.caso().obligacionB(), "500.00");

        assertThat(salida.resultado()).isEqualTo(Resultado.RECUPERADA);
        assertThat(salida.exposicionDespues()).isEqualTo(bob("500.00"));
    }
}
