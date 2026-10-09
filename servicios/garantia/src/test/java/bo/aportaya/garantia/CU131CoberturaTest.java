package bo.aportaya.garantia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.EntradaCobertura;
import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.Linea;
import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.Resultado;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** H8.S1.M2/M3/M6 · Faltante al corte, cobertura empresarial y falta extraordinaria de caja. */
class CU131CoberturaTest extends BaseDeRespaldo {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    @Test
    @DisplayName(
            "Dado pozo Bs 6.000 y Bs 5.000 confirmados · Cuando se cubre con la reserva · Entonces solicita Bs 1.000, queda aplicado con exposicion y responsable, y los asientos cuadran")
    void ejemploDelPlan() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");

        var salida = cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));

        assertThat(salida.resultado()).isEqualTo(Resultado.APLICADA);
        assertThat(salida.cubierto()).isEqualTo(bob("1000.00"));
        assertThat(salida.disponibleDespues()).isEqualTo(bob("7000.00"));
        assertThat(salida.exposicionDespues()).isEqualTo(bob("1000.00"));
        // El libro registra la exposicion resultante y quien responde por ella.
        var mov = dsl.fetchOne(
                "SELECT exposicion_resultante, disponible_resultante, responsable_id, registrado_por FROM garantia.movimiento_reserva WHERE reserva_respaldo_id = ? AND tipo = 'APLICACION'",
                reserva.reservaId());
        assertThat(mov.get(0, java.math.BigDecimal.class)).isEqualByComparingTo("1000.00");
        assertThat(mov.get(1, java.math.BigDecimal.class)).isEqualByComparingTo("7000.00");
        assertThat(mov.get(2, UUID.class)).isEqualTo(c.usuario());
        assertThat(mov.get(3, UUID.class)).isEqualTo(c.usuario());
        // Dos lineas, una por obligacion faltante, que suman el faltante (R-GAR-11).
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.cobertura_respaldo_linea WHERE cobertura_respaldo_id = ?",
                        salida.coberturaId()))
                .isEqualTo(2);
        // Asientos propuestos: contra el doble del mayor.
        var mayor = new MayorDeDoble();
        mayor.consumirOutbox(dsl, "garantia", reserva.reservaId(), salida.coberturaId());
        assertThat(mayor.asientos()).isEqualTo(2);
        assertThat(mayor.saldo("RESERVA_RESPALDO")).isEqualByComparingTo("7000.00");
        assertThat(mayor.saldo("POZO_GRUPO")).isEqualByComparingTo("1000.00");
        assertThat(mayor.saldo("CAJA_EMPRESA")).isEqualByComparingTo("-8000.00");
        assertThat(mayor.sumaDeSaldos()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName(
            "Dado el mismo pedido dos veces · Cuando se cubre · Entonces hay una sola cobertura y un solo movimiento (idempotencia)")
    void idempotente() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");
        var pedido = pedidoDelEjemplo(c, "cobertura-" + c.turno());

        var primera = cubrir(c, pedido);
        var segunda = cubrir(c, pedido);

        assertThat(segunda.coberturaId()).isEqualTo(primera.coberturaId());
        assertThat(segunda.esNueva()).isFalse();
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.movimiento_reserva WHERE reserva_respaldo_id = ? AND tipo = 'APLICACION'",
                        reserva.reservaId()))
                .isEqualTo(1);
        assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName(
            "Dado un turno ya cubierto · Cuando llega otro pedido del mismo turno con otra clave y el mismo faltante · Entonces devuelve la cobertura existente; con otro faltante se rechaza")
    void mismoTurnoOtraClave() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        reservar(c, "8000.00");
        var primera = cubrir(c, pedidoDelEjemplo(c, "cobertura-a-" + c.turno()));

        var otraClave = cubrir(c, pedidoDelEjemplo(c, "cobertura-b-" + c.turno()));
        var otroFaltante = new EntradaCobertura(
                c.escenario().grupoId(),
                c.escenario().periodoId(),
                c.turno(),
                bob("6000.00"),
                bob("5000.00"),
                bob("500.00"),
                List.of(new Linea(c.obligacionA(), bob("500.00"))),
                OffsetDateTime.now(),
                new ClaveIdempotencia("cobertura-c-" + c.turno()));

        assertThat(otraClave.coberturaId()).isEqualTo(primera.coberturaId());
        assertThat(otraClave.esNueva()).isFalse();
        assertThatThrownBy(() -> cubrir(c, otroFaltante)).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo("AP-CU23-06"));
    }

    @Test
    @DisplayName(
            "Dada una reserva de Bs 800 y un faltante de Bs 1.000 · Cuando se cubre · Entonces INSUFICIENTE, no se cubre nada, se emite el incidente y la reserva queda intacta")
    void faltaExtraordinariaDeCaja() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "800.00");

        var salida = cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));

        assertThat(salida.resultado()).isEqualTo(Resultado.INSUFICIENTE);
        assertThat(salida.sinCubrir()).isEqualTo(bob("200.00"));
        assertThat(salida.coberturaId()).isNull();
        assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("0");
        assertThat(contar("SELECT count(*)::int FROM garantia.cobertura_respaldo WHERE turno_id = ?", c.turno()))
                .isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.evento_dominio WHERE tipo = 'garantia.respaldo_insuficiente' AND agregado_id = ?",
                        c.turno()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada la contingencia (ampliacion de la reserva) tras un INSUFICIENTE · Cuando se reintenta con la misma clave · Entonces ahora se cubre, una sola vez")
    void contingenciaYReintento() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "800.00");
        var pedido = pedidoDelEjemplo(c, "cobertura-" + c.turno());
        assertThat(cubrir(c, pedido).resultado()).isEqualTo(Resultado.INSUFICIENTE);

        transaccion.execute(t -> reservaCU.ampliar(reserva.reservaId(), bob("200.00"), clave("ampliar"), c.ctx()));
        var reintento = cubrir(c, pedido);

        assertThat(reintento.resultado()).isEqualTo(Resultado.APLICADA);
        assertThat(reintento.disponibleDespues()).isEqualTo(bob("0.00"));
        assertThat(contar("SELECT count(*)::int FROM garantia.cobertura_respaldo WHERE turno_id = ?", c.turno()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un grupo sin reserva vigente · Cuando se cubre · Entonces SIN_RESERVA y no se escribe ninguna cobertura")
    void sinReserva() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();

        var salida = cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));

        assertThat(salida.resultado()).isEqualTo(Resultado.SIN_RESERVA);
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.cobertura_respaldo WHERE grupo_id = ?",
                        c.escenario().grupoId()))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dado un faltante exactamente igual a lo disponible · Cuando se cubre · Entonces cubre (limite) y el siguiente centavo ya no")
    void limiteExacto() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        reservar(c, "1000.00");
        var otroTurno = fixturaDeRespaldo.turno(c.escenario());

        var justo = cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));
        var extra = new EntradaCobertura(
                c.escenario().grupoId(),
                periodoDe(otroTurno),
                otroTurno,
                bob("6000.00"),
                bob("5999.99"),
                bob("0.00"),
                List.of(new Linea(c.obligacionA(), bob("0.01"))),
                OffsetDateTime.now(),
                new ClaveIdempotencia("cobertura-" + otroTurno));

        assertThat(justo.resultado()).isEqualTo(Resultado.APLICADA);
        assertThat(justo.disponibleDespues()).isEqualTo(bob("0.00"));
        assertThat(cubrir(c, extra).resultado()).isEqualTo(Resultado.INSUFICIENTE);
    }

    @Test
    @DisplayName(
            "Dadas lineas que no suman el faltante, repetidas o con un faltante cero · Cuando se cubre · Entonces se rechaza AP-CU23-09 antes de escribir")
    void pedidosInvalidos() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        reservar(c, "8000.00");
        var base = pedidoDelEjemplo(c, "x");

        var noSuman = new EntradaCobertura(
                base.grupoId(),
                base.periodoId(),
                base.turnoId(),
                base.pozo(),
                base.confirmado(),
                base.cubiertoMutual(),
                List.of(new Linea(c.obligacionA(), bob("500.00")), new Linea(c.obligacionB(), bob("499.99"))),
                base.corte(),
                new ClaveIdempotencia("inv-1-" + c.turno()));
        var repetidas = new EntradaCobertura(
                base.grupoId(),
                base.periodoId(),
                base.turnoId(),
                base.pozo(),
                base.confirmado(),
                base.cubiertoMutual(),
                List.of(new Linea(c.obligacionA(), bob("500.00")), new Linea(c.obligacionA(), bob("500.00"))),
                base.corte(),
                new ClaveIdempotencia("inv-2-" + c.turno()));
        var sinFaltante = new EntradaCobertura(
                base.grupoId(),
                base.periodoId(),
                base.turnoId(),
                base.pozo(),
                base.pozo(),
                base.cubiertoMutual(),
                List.of(),
                base.corte(),
                new ClaveIdempotencia("inv-3-" + c.turno()));

        for (var pedido : List.of(noSuman, repetidas, sinFaltante)) {
            assertThatThrownBy(() -> cubrir(c, pedido)).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                            e.codigo().valor())
                    .isEqualTo("AP-CU23-09"));
        }
        assertThat(contar("SELECT count(*)::int FROM garantia.cobertura_respaldo WHERE turno_id = ?", c.turno()))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dada una cobertura aplicada · Cuando se reversa · Entonces nace el movimiento y el asiento inverso, la reserva recupera su disponible y el turno puede cubrirse de nuevo")
    void reversaConContraAsiento() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");
        var cobertura = cubrir(c, pedidoDelEjemplo(c, "cobertura-1-" + c.turno()));

        var primera = transaccion.execute(t -> reversaCU.reversar(cobertura.coberturaId(), clave("reversa"), c.ctx()));
        var segunda = transaccion.execute(t -> reversaCU.reversar(cobertura.coberturaId(), clave("reversa"), c.ctx()));

        assertThat(primera.esNueva()).isTrue();
        assertThat(segunda.esNueva()).isFalse();
        assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("0");
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.movimiento_reserva WHERE reserva_respaldo_id = ? AND tipo = 'REVERSA_APLICACION'",
                        reserva.reservaId()))
                .isEqualTo(1);
        var mayor = new MayorDeDoble();
        mayor.consumirOutbox(dsl, "garantia", reserva.reservaId(), cobertura.coberturaId());
        assertThat(mayor.saldo("POZO_GRUPO")).isEqualByComparingTo("0");
        assertThat(mayor.saldo("RESERVA_RESPALDO")).isEqualByComparingTo("8000.00");
        // El turno se puede volver a cubrir despues de la reversa.
        var otra = cubrir(c, pedidoDelEjemplo(c, "cobertura-2-" + c.turno()));
        assertThat(otra.resultado()).isEqualTo(Resultado.APLICADA);
        assertThat(otra.coberturaId()).isNotEqualTo(cobertura.coberturaId());
    }

    @Test
    @DisplayName(
            "Dada una reserva y las lineas de la cobertura · Cuando alguien inserta una linea de mas por fuera del caso de uso · Entonces la base lo rechaza al confirmar (R-GAR-11)")
    void labaseExigeQueLasLineasSumenElFaltante() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        reservar(c, "8000.00");
        var cobertura = cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));
        UUID tercera = fixturaDeRespaldo.otraObligacion(fixtura, c.escenario(), "100.00");

        String error = rechazaLaBase("SET CONSTRAINTS ALL IMMEDIATE; INSERT INTO garantia.cobertura_respaldo_linea "
                + "(id, cobertura_respaldo_id, obligacion_id, monto_cubierto, monto_recuperado) VALUES (gen_random_uuid(), '"
                + cobertura.coberturaId() + "', '" + tercera + "', 100.00, 0)");

        assertThat(error).contains("R-GAR-11");
    }
}
