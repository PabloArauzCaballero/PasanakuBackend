package bo.aportaya.garantia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.garantia.aplicacion.CU23ReservarRespaldo.EntradaReserva;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** H7.S1.M2 · Reservar capacidad empresarial por ciclo, contra PostgreSQL real. */
class CU131RespaldoTest extends BaseDeRespaldo {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    @Test
    @DisplayName(
            "Dada capacidad para Bs 10.000 · Cuando se reservan Bs 8.000 para el ciclo 1 · Entonces la reserva queda identificada, la capacidad comprometida sube y el libro tiene un movimiento")
    void reservaIdentificadaPorCiclo() {
        fixturaDeRespaldo.capacidad("10000.00");
        var c = caso();
        BigDecimal antes = fixturaDeRespaldo.comprometido();

        var salida = reservar(c, "8000.00");

        assertThat(salida.esNueva()).isTrue();
        assertThat(salida.capacidadDisponible()).isEqualTo(bob("2000.00"));
        assertThat(fixturaDeRespaldo.comprometido()).isEqualByComparingTo(antes.add(new BigDecimal("8000.00")));
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.movimiento_reserva WHERE reserva_respaldo_id = ? AND tipo = 'RESERVA'",
                        salida.reservaId()))
                .isEqualTo(1);
        assertThat(numero("SELECT monto_reservado FROM garantia.reserva_respaldo WHERE id = ?", salida.reservaId()))
                .isEqualByComparingTo("8000.00");
        // Asiento propuesto que viaja en el evento: reserva contra caja, y cuadra.
        var mayor = new MayorDeDoble();
        mayor.consumirOutbox(dsl, "garantia", salida.reservaId());
        assertThat(mayor.saldo("RESERVA_RESPALDO")).isEqualByComparingTo("8000.00");
        assertThat(mayor.saldo("CAJA_EMPRESA")).isEqualByComparingTo("-8000.00");
        assertThat(mayor.sumaDeSaldos()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName(
            "Dada la misma clave dos veces · Cuando se reserva · Entonces devuelve la misma reserva y no compromete capacidad otra vez (idempotencia)")
    void idempotente() {
        fixturaDeRespaldo.capacidad("10000.00");
        var c = caso();
        var clave = clave("reserva");
        var entrada = new EntradaReserva(c.escenario().grupoId(), 1, bob("3000.00"), c.usuario(), clave);

        var primera = transaccion.execute(t -> reservaCU.reservar(entrada, c.ctx()));
        BigDecimal despuesDeLaPrimera = fixturaDeRespaldo.comprometido();
        var segunda = transaccion.execute(t -> reservaCU.reservar(entrada, c.ctx()));

        assertThat(segunda.reservaId()).isEqualTo(primera.reservaId());
        assertThat(segunda.esNueva()).isFalse();
        assertThat(fixturaDeRespaldo.comprometido()).isEqualByComparingTo(despuesDeLaPrimera);
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.movimiento_reserva WHERE reserva_respaldo_id = ?",
                        primera.reservaId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dada una clave ya usada · Cuando se reutiliza con otro monto · Entonces se rechaza AP-CU23-06")
    void claveReutilizadaConOtroContenido() {
        fixturaDeRespaldo.capacidad("10000.00");
        var c = caso();
        var clave = clave("reserva");
        transaccion.execute(t -> reservaCU.reservar(
                new EntradaReserva(c.escenario().grupoId(), 1, bob("1000.00"), c.usuario(), clave), c.ctx()));

        assertThatThrownBy(() -> transaccion.execute(t -> reservaCU.reservar(
                        new EntradaReserva(c.escenario().grupoId(), 1, bob("2000.00"), c.usuario(), clave), c.ctx())))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU23-06"));
    }

    @Test
    @DisplayName(
            "Dada capacidad de Bs 5.000 · Cuando se piden exactamente Bs 5.000 · Entonces se reserva (limite) y un centavo mas ya no entra")
    void limiteDeLaCapacidad() {
        fixturaDeRespaldo.capacidad("5000.00");
        var c = caso();
        var otro = caso();

        var justo = reservar(c, "5000.00");

        assertThat(justo.capacidadDisponible()).isEqualTo(bob("0.00"));
        assertThatThrownBy(() -> reservar(otro, "0.01")).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo("AP-CU23-05"));
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.reserva_respaldo WHERE grupo_id = ?",
                        otro.escenario().grupoId()))
                .isZero();
    }

    @Test
    @DisplayName("Dada moneda sin capacidad cargada · Cuando se reserva · Entonces se deniega por omision")
    void sinCapacidadSeDeniega() {
        var c = caso();

        assertThatThrownBy(() -> transaccion.execute(t -> reservaCU.reservar(
                        new EntradaReserva(
                                c.escenario().grupoId(),
                                1,
                                Dinero.de("100.00", Moneda.USD),
                                c.usuario(),
                                clave("reserva")),
                        c.ctx())))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU23-05"));
    }

    @Test
    @DisplayName(
            "Dada una reserva y otra para el mismo grupo y ciclo con otra clave · Cuando se intenta · Entonces la base lo rechaza (una reserva por ciclo)")
    void unaReservaPorCiclo() {
        fixturaDeRespaldo.capacidad("10000.00");
        var c = caso();
        reservar(c, "1000.00");

        assertThatThrownBy(() -> reservar(c, "1000.00"))
                .satisfies(e -> assertThat(raizDe(e)).contains("uq_reserva_respaldo_grupo_id_ciclo_numero"));
    }

    @Test
    @DisplayName(
            "Dada una capacidad · Cuando alguien comprometa mas que el tope por fuera del caso de uso · Entonces la base lo rechaza (R-GAR-08)")
    void laBaseImpideSobreasignar() {
        fixturaDeRespaldo.capacidad("100.00");

        String error = rechazaLaBase(
                "UPDATE garantia.capacidad_respaldo SET monto_comprometido = monto_tope + 0.01 WHERE ambito = 'GENERAL' AND moneda = 'BOB'");

        assertThat(error).contains("ck_capacidad_respaldo_comprometido");
    }

    @Test
    @DisplayName(
            "Dado el libro de movimientos · Cuando se intenta editar o borrar · Entonces la base lo rechaza (append-only)")
    void libroInmutable() {
        fixturaDeRespaldo.capacidad("10000.00");
        var c = caso();
        var salida = reservar(c, "1000.00");

        assertThat(rechazaLaBase(
                        "UPDATE garantia.movimiento_reserva SET monto = 1 WHERE reserva_respaldo_id = ?",
                        salida.reservaId()))
                .isNotEmpty();
        assertThat(rechazaLaBase(
                        "DELETE FROM garantia.movimiento_reserva WHERE reserva_respaldo_id = ?", salida.reservaId()))
                .isNotEmpty();
        assertThat(numero(
                        "SELECT monto FROM garantia.movimiento_reserva WHERE reserva_respaldo_id = ?",
                        salida.reservaId()))
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName(
            "Dada una reserva con parte aplicada · Cuando se libera el ciclo · Entonces vuelve a la capacidad solo lo no usado, una sola vez")
    void liberarDevuelveLoNoUsado() {
        fixturaDeRespaldo.capacidad("10000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");
        cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));
        BigDecimal conReserva = fixturaDeRespaldo.comprometido();

        var primera = transaccion.execute(t -> reservaCU.liberar(reserva.reservaId(), clave("liberar"), c.ctx()));
        var segunda = transaccion.execute(t -> reservaCU.liberar(reserva.reservaId(), clave("liberar"), c.ctx()));

        assertThat(primera.liberado()).isEqualTo(bob("7000.00"));
        assertThat(primera.esNueva()).isTrue();
        assertThat(segunda.esNueva()).isFalse();
        assertThat(fixturaDeRespaldo.comprometido())
                .isEqualByComparingTo(conReserva.subtract(new BigDecimal("7000.00")));
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.movimiento_reserva WHERE reserva_respaldo_id = ? AND tipo = 'LIBERACION'",
                        reserva.reservaId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado el libro · Cuando se compara con los contadores de la reserva · Entonces cuadran (consulta de verificacion R-GAR-09 sin filas)")
    void cuadreDesdeElLibro() {
        fixturaDeRespaldo.capacidad("10000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");
        cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));

        int descuadradas = contar(
                """
                SELECT count(*)::int FROM garantia.reserva_respaldo r
                  LEFT JOIN LATERAL (
                        SELECT SUM(CASE WHEN m.tipo IN ('RESERVA', 'AMPLIACION') THEN m.monto ELSE 0 END) AS reservado,
                               SUM(CASE WHEN m.tipo = 'APLICACION' THEN m.monto WHEN m.tipo = 'REVERSA_APLICACION' THEN -m.monto ELSE 0 END) AS aplicado,
                               SUM(CASE WHEN m.tipo = 'RECUPERACION' THEN m.monto ELSE 0 END) AS recuperado,
                               SUM(CASE WHEN m.tipo = 'LIBERACION' THEN m.monto ELSE 0 END) AS liberado
                          FROM garantia.movimiento_reserva m WHERE m.reserva_respaldo_id = r.id) l ON TRUE
                 WHERE r.id = ?
                   AND (r.monto_reservado <> COALESCE(l.reservado, 0) OR r.monto_aplicado <> COALESCE(l.aplicado, 0)
                     OR r.monto_recuperado <> COALESCE(l.recuperado, 0) OR r.monto_liberado <> COALESCE(l.liberado, 0))
                """,
                reserva.reservaId());

        assertThat(descuadradas).isZero();
        assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName(
            "Dada una reserva con faltante sin cubrir · Cuando se amplia dentro de la capacidad · Entonces sube lo reservado y lo comprometido (contingencia)")
    void ampliar() {
        fixturaDeRespaldo.capacidad("10000.00");
        var c = caso();
        var reserva = reservar(c, "800.00");
        BigDecimal antes = fixturaDeRespaldo.comprometido();

        var ampliada = transaccion.execute(
                t -> reservaCU.ampliar(reserva.reservaId(), bob("200.00"), clave("ampliar"), c.ctx()));

        assertThat(ampliada.reservadoDespues()).isEqualTo(bob("1000.00"));
        assertThat(fixturaDeRespaldo.comprometido()).isEqualByComparingTo(antes.add(new BigDecimal("200.00")));
        assertThat(numero("SELECT monto_reservado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Dada la clave en blanco · Cuando se construye · Entonces se rechaza (invalido)")
    void claveEnBlanco() {
        assertThatThrownBy(() -> new ClaveIdempotencia(" ")).isInstanceOf(RuntimeException.class);
    }
}
