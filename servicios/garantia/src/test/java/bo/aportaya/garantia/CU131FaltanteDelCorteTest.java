package bo.aportaya.garantia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.Resultado;
import bo.aportaya.garantia.aplicacion.CU23CubrirFaltanteDelCorte;
import bo.aportaya.garantia.aplicacion.CU23CubrirFaltanteDelCorte.Pedido;
import bo.aportaya.garantia.dominio.puertos.RecaudoDelPeriodo;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Amenaza: quien pide la cobertura inventa el importe, o el periodo o el turno de otro grupo.
 *
 * <p>Control: el faltante lo afirma {@code aportes} (aca, un DOBLE del puerto en tres niveles,
 * regla 65: el adaptador HTTP real se prueba en {@code RecaudoPorHttpTest}); el periodo tiene que
 * ser del grupo que lo declara; y la base exige que el turno sea de ese grupo y ese periodo
 * (R-GAR-12). Esta clase es el test negativo de regla 90.6.
 */
class CU131FaltanteDelCorteTest extends BaseDeRespaldo {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    /** El doble de aportes: lo que devuelve lo decide cada prueba. */
    private CU23CubrirFaltanteDelCorte conAportes(Function<UUID, Optional<RecaudoDelPeriodo.Recaudo>> respuesta) {
        return new CU23CubrirFaltanteDelCorte(respuesta::apply, respaldoCU);
    }

    /** Pozo 6.000, 5.000 confirmados: aportes dice que faltan 1.000, de dos obligaciones. */
    private RecaudoDelPeriodo.Recaudo recaudoDelEjemplo(Caso c) {
        return new RecaudoDelPeriodo.Recaudo(
                c.escenario().periodoId(),
                c.escenario().grupoId(),
                OffsetDateTime.now(),
                bob("6000.00"),
                bob("5000.00"),
                bob("0.00"),
                List.of(
                        new RecaudoDelPeriodo.Pendiente(c.obligacionA(), bob("500.00")),
                        new RecaudoDelPeriodo.Pendiente(c.obligacionB(), bob("500.00"))));
    }

    private Pedido pedido(Caso c, Dinero faltanteEsperado) {
        return new Pedido(
                c.escenario().grupoId(),
                c.escenario().periodoId(),
                c.turno(),
                faltanteEsperado,
                new ClaveIdempotencia("corte-" + c.turno()));
    }

    private int coberturas(Caso c) {
        return contar("SELECT count(*)::int FROM garantia.cobertura_respaldo WHERE turno_id = ?", c.turno());
    }

    @Test
    @DisplayName(
            "CORRECTO · Dado un pedido con solo identificadores · Cuando aportes confirma pozo 6.000 y 5.000 confirmados · Entonces cubre Bs 1.000 con las lineas que dijo aportes")
    void elFaltanteSaleDeAportes() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");

        var salida = transaccion.execute(
                t -> conAportes(p -> Optional.of(recaudoDelEjemplo(c))).cubrir(pedido(c, null), c.ctx()));

        assertThat(salida.resultado()).isEqualTo(Resultado.APLICADA);
        assertThat(salida.cubierto()).isEqualTo(bob("1000.00"));
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.cobertura_respaldo_linea WHERE cobertura_respaldo_id = ?",
                        salida.coberturaId()))
                .isEqualTo(2);
        assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName(
            "LIMITE · Dado un faltanteEsperado exactamente igual al de aportes · Cuando se cubre dos veces · Entonces coincide, cubre y la segunda no duplica")
    void informadoIgualYReintento() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");
        var servicio = conAportes(p -> Optional.of(recaudoDelEjemplo(c)));

        var primera = transaccion.execute(t -> servicio.cubrir(pedido(c, bob("1000.00")), c.ctx()));
        var segunda = transaccion.execute(t -> servicio.cubrir(pedido(c, bob("1000.00")), c.ctx()));

        assertThat(segunda.coberturaId()).isEqualTo(primera.coberturaId());
        assertThat(segunda.esNueva()).isFalse();
        assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName(
            "INVALIDO · MONTO INFLADO · Dado un cliente que dice que faltan Bs 5.000 · Cuando aportes confirma Bs 1.000 · Entonces AP-CU23-13 y no se cubre ni un centavo")
    void montoInflado() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");

        assertThatThrownBy(() -> transaccion.execute(t ->
                        conAportes(p -> Optional.of(recaudoDelEjemplo(c))).cubrir(pedido(c, bob("5000.00")), c.ctx())))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU23-13"));

        assertThat(coberturas(c)).isZero();
        assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName(
            "INVALIDO · PERIODO DE OTRO GRUPO · Dado un pedido del grupo A con un periodo que aportes dice ser del grupo B · Entonces AP-CU23-13 y no se escribe nada")
    void periodoDeOtroGrupo() {
        fixturaDeRespaldo.capacidad("20000.00");
        var a = caso();
        var b = caso();
        reservar(a, "8000.00");
        // aportes dice que el periodo de B es del grupo B; el pedido lo reclama para A
        var recaudoDeB = recaudoDelEjemplo(b);
        var pedidoRobado = new Pedido(
                a.escenario().grupoId(),
                b.escenario().periodoId(),
                a.turno(),
                null,
                new ClaveIdempotencia("robo-" + a.turno()));

        assertThatThrownBy(() -> transaccion.execute(
                        t -> conAportes(p -> Optional.of(recaudoDeB)).cubrir(pedidoRobado, a.ctx())))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU23-13"));

        assertThat(coberturas(a)).isZero();
    }

    @Test
    @DisplayName(
            "INVALIDO · TURNO DE OTRO GRUPO · Dado un pedido con periodo y grupo propios pero el turno de otro grupo · Cuando se cubre · Entonces la base lo rechaza (R-GAR-12) y la reserva queda intacta")
    void turnoDeOtroGrupo() {
        fixturaDeRespaldo.capacidad("20000.00");
        var a = caso();
        var b = caso();
        var reservaA = reservar(a, "8000.00");
        var pedidoConTurnoAjeno = new Pedido(
                a.escenario().grupoId(),
                a.escenario().periodoId(),
                b.turno(),
                null,
                new ClaveIdempotencia("turno-ajeno-" + b.turno()));

        assertThatThrownBy(() -> transaccion.execute(
                        t -> conAportes(p -> Optional.of(recaudoDelEjemplo(a))).cubrir(pedidoConTurnoAjeno, a.ctx())))
                .satisfies(e -> assertThat(raizDe(e)).contains("R-GAR-12"));

        assertThat(coberturas(b)).isZero();
        assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reservaA.reservaId()))
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName(
            "INVALIDO · APORTES CAIDO · Dado que aportes no responde · Cuando se pide cubrir · Entonces AP-CU23-12 (deniega por omision), no se escribe nada y al volver aportes el reintento cubre")
    void aportesCaido() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");

        assertThatThrownBy(() -> transaccion.execute(
                        t -> conAportes(p -> Optional.empty()).cubrir(pedido(c, null), c.ctx())))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU23-12"));
        assertThat(coberturas(c)).isZero();
        assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("0");

        var recuperado = transaccion.execute(
                t -> conAportes(p -> Optional.of(recaudoDelEjemplo(c))).cubrir(pedido(c, null), c.ctx()));
        assertThat(recuperado.resultado()).isEqualTo(Resultado.APLICADA);
    }

    @Test
    @DisplayName(
            "INVALIDO · POZO YA COMPLETO · Dado que aportes confirma que no falta nada · Cuando se pide cubrir · Entonces AP-CU23-09: no hay faltante que cubrir")
    void sinFaltante() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        reservar(c, "8000.00");
        var completo = new RecaudoDelPeriodo.Recaudo(
                c.escenario().periodoId(),
                c.escenario().grupoId(),
                OffsetDateTime.now(),
                bob("6000.00"),
                bob("6000.00"),
                bob("0.00"),
                List.of());

        assertThatThrownBy(() -> transaccion.execute(
                        t -> conAportes(p -> Optional.of(completo)).cubrir(pedido(c, null), c.ctx())))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU23-09"));
    }
}
