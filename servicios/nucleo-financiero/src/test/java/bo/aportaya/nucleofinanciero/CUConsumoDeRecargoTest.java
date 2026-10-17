package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes.Resultado;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tableros · H1.S2.M1 — el recargo por mora cobrado va al FONDO DE GARANTIA, no al pozo del grupo (D1). La billetera
 * del participante baja, la de sistema {@code FONDO_GARANTIA} sube, la del grupo no se mueve, y el mayor asienta DEBE
 * usuarios / HABER {@code RESERVA_RESPALDO}.
 */
class CUConsumoDeRecargoTest extends BaseDeRecargo {

    private int asientos(UUID transaccion) {
        return contar("SELECT count(*) FROM nucleo_financiero.asiento_contable WHERE origen_id = ?", transaccion);
    }

    @Test
    @DisplayName(
            "cuadre: un recargo de Bs 25 cobrado baja la billetera del participante, sube la del fondo, NO mueve la"
                    + " del grupo, y el mayor asienta DEBE usuarios / HABER reserva del fondo por 25")
    void elRecargoVaAlFondo() {
        var a = aporte("100.00");
        UUID fondo = fondo();
        BigDecimal fondoAntes = saldoDelLibro(fondo);
        BigDecimal reservaAntes = saldoDelMayor(reservaRespaldo);
        BigDecimal gruposAntes = saldoDelMayor(MAPEO.get("SALDO_GRUPOS.BOB"));

        var salida = procesar(recargo(a, "25.00"));

        assertThat(salida.resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("75.00");
        assertThat(saldoDelLibro(fondo).subtract(fondoAntes)).isEqualByComparingTo("25.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("0.00");
        // El mayor: la reserva del fondo sube 25 y la de grupos no se mueve.
        assertThat(saldoDelMayor(reservaRespaldo).subtract(reservaAntes)).isEqualByComparingTo("25.00");
        assertThat(saldoDelMayor(MAPEO.get("SALDO_GRUPOS.BOB")).subtract(gruposAntes))
                .isEqualByComparingTo("0.00");
        var partidas = dsl.fetch(
                """
                SELECT c.codigo, m.debe, m.haber, m.moneda FROM nucleo_financiero.movimiento_contable m
                  JOIN nucleo_financiero.cuenta_contable c ON c.id = m.cuenta_id WHERE m.asiento_id = ? ORDER BY m.debe DESC
                """,
                salida.asientoId().orElseThrow());
        assertThat(partidas).hasSize(2);
        assertThat(partidas.get(0).get(0)).isEqualTo(MAPEO.get("SALDO_USUARIOS.BOB"));
        assertThat((BigDecimal) partidas.get(0).get(1)).isEqualByComparingTo("25.00");
        assertThat(partidas.get(1).get(0)).isEqualTo(reservaRespaldo);
        assertThat((BigDecimal) partidas.get(1).get(2)).isEqualByComparingTo("25.00");
        assertThat(partidas).allSatisfy(p -> assertThat(p.get(3)).isEqualTo("BOB"));
        // La transaccion del libro se llama por lo que es, y el hecho dice adonde fue.
        assertThat(dsl.fetchOne(
                                "SELECT tipo FROM nucleo_financiero.transaccion_billetera WHERE id = ?",
                                salida.transaccionId().orElseThrow())
                        .get(0))
                .isEqualTo("RECARGO_MORA");
        assertThat(dsl.fetchOne(
                                """
                                SELECT payload->>'destino' FROM nucleo_financiero.evento_dominio
                                 WHERE tipo = 'nucleo_financiero.aporte_aplicado' AND agregado_id = ?
                                """,
                                a.obligacion())
                        .get(0))
                .isEqualTo("FONDO_GARANTIA");
        System.out.println("ASIENTO | recargo pago | DEBE " + MAPEO.get("SALDO_USUARIOS.BOB") + " 25.00 | HABER "
                + reservaRespaldo + " 25.00 | participante 100.00 -> " + saldoDelLibro(a.billetera())
                + " | fondo +" + saldoDelLibro(fondo).subtract(fondoAntes) + " | grupo "
                + saldoDelLibro(a.billeteraDelGrupo()));
    }

    @Test
    @DisplayName("evento duplicado: el mismo recargo dos veces (mismo id de evento) mueve el dinero UNA vez")
    void duplicado() {
        var a = aporte("100.00");
        var e = recargo(a, "25.00");

        var primero = procesar(e);
        var segundo = procesar(e);

        assertThat(primero.resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(segundo.resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("75.00");
        assertThat(asientos(primero.transaccionId().orElseThrow())).isEqualTo(1);
    }

    @Test
    @DisplayName("reintento: el productor republica el mismo pago con OTRO id de evento · un solo asiento, misma"
            + " transaccion")
    void reintento() {
        var a = aporte("100.00");
        var e = recargo(a, "25.00");

        var primero = procesar(e);
        var segundo = procesar(reenviado(e));

        assertThat(segundo.resultado()).isEqualTo(Resultado.YA_APLICADO);
        assertThat(segundo.transaccionId()).isEqualTo(primero.transaccionId());
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("75.00");
        assertThat(asientos(primero.transaccionId().orElseThrow())).isEqualTo(1);
    }

    @Test
    @DisplayName("compatibilidad: un evento SIN tipo (productor anterior) sigue yendo al grupo, como siempre")
    void sinTipoVaAlGrupo() {
        var a = aporte("100.00");

        procesar(cobrado(a, "25.00"));

        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("rechaza un recargo sin mapeo contable del fondo (AP-CU24-02): no se mueve nada ni se marca el evento")
    void sinMapeoDelFondo() {
        var a = aporte("100.00");
        var sinReserva = new HashMap<>(MAPEO);
        sinReserva.remove("RESERVA_RESPALDO.BOB");
        var consumoSinReserva = armar(libroReal, sinReserva);
        var e = recargo(a, "25.00");

        assertThatThrownBy(() -> transaccion.execute(t -> consumoSinReserva.procesar(e)))
                .satisfies(ex -> assertThat(codigo(ex)).isEqualTo("AP-CU24-02"));
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("100.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("0.00");
        assertThat(marcas(e.eventoId())).isZero();
    }

    @Test
    @DisplayName("rechaza un recargo sin billetera activa del fondo (AP-CU12-02): el recargo no cae en el pozo, no se"
            + " marca, y el reproceso lo aplica cuando el fondo existe")
    void sinBilleteraDelFondo() {
        var a = aporte("100.00");
        UUID fondo = fondo();
        var e = recargo(a, "25.00");
        dslFixtura.execute("UPDATE nucleo_financiero.cuenta_billetera SET estado = 'CONGELADA' WHERE id = ?", fondo);
        try {
            assertThatThrownBy(() -> procesar(e))
                    .satisfies(ex -> assertThat(codigo(ex)).isEqualTo("AP-CU12-02"));
            assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("100.00");
            assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("0.00");
            assertThat(marcas(e.eventoId())).isZero();
        } finally {
            dslFixtura.execute("UPDATE nucleo_financiero.cuenta_billetera SET estado = 'ACTIVA' WHERE id = ?", fondo);
        }

        assertThat(procesar(e).resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("compensa: reversar (CU-14) el recargo cobrado devuelve los Bs 25 al participante y se los descuenta"
            + " a la billetera del fondo, con una transaccion REVERSO; la original queda intacta")
    void reversaDelRecargo() {
        var a = aporte("100.00");
        UUID fondo = fondo();
        BigDecimal fondoAntes = saldoDelLibro(fondo);
        var aplicado = procesar(recargo(a, "25.00"));
        UUID original = aplicado.transaccionId().orElseThrow();

        var salida = transaccion.execute(x -> reversoCU.ejecutar(
                new bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.EntradaReverso(
                        "rev-recargo-" + original,
                        original,
                        "ERROR_OPERATIVO",
                        "El recargo se cobro sobre una cuota que estaba en gracia",
                        fixtura.usuario()),
                contextoDe(a.usuario())));

        assertThat(salida.generaObligacionDeRestitucion()).isFalse();
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("100.00");
        assertThat(saldoDelLibro(fondo).subtract(fondoAntes)).isEqualByComparingTo("0.00");
        assertThat(contar(
                        "SELECT count(*) FROM nucleo_financiero.transaccion_billetera WHERE id = ? AND tipo = 'REVERSO'",
                        salida.transaccionReversoId()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*) FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id = ?",
                        original))
                .isEqualTo(2);
    }
}
