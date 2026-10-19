package bo.aportaya.garantia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.garantia.aplicacion.IngresoAlFondo;
import bo.aportaya.garantia.aplicacion.IngresoAlFondo.AporteAplicado;
import bo.aportaya.garantia.aplicacion.IngresoAlFondo.Resultado;
import bo.aportaya.garantia.infraestructura.FondoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tableros · H1.S2.M1c — el fondo de garantia anota el recargo por mora que {@code nucleo-financiero} ya movio a su
 * billetera: un {@code movimiento_fondo} RECARGO_MORA y el saldo disponible, una sola vez por evento.
 */
class CUIngresoAlFondoTest extends BaseDeGarantia {

    private final IngresoAlFondo ingreso =
            new IngresoAlFondo(new Datos(dsl), new FondoRepositorio(), Reloj.delSistema());

    private record Fondo(UUID grupoId, UUID fondoId) {}

    private Fondo fondoCon(String saldo) {
        var escenario = fixtura.escenario(fixtura.usuario());
        UUID politica = fixtura.politica(escenario.grupoId(), "100.00", "50000.00", "50000.00", 3, "100000.00", 15);
        return new Fondo(escenario.grupoId(), fixtura.fondo(escenario.grupoId(), politica, saldo));
    }

    private AporteAplicado alFondo(UUID grupo, String monto, Moneda moneda) {
        return new AporteAplicado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                grupo,
                Dinero.de(monto, moneda),
                "FONDO_GARANTIA");
    }

    private Resultado anotar(AporteAplicado e) {
        return transaccion.execute(t -> ingreso.anotar(e));
    }

    private BigDecimal saldoDisponible(UUID fondo) {
        return (BigDecimal) dsl.fetchOne("SELECT saldo_disponible FROM garantia.fondo_garantia WHERE id = ?", fondo)
                .get(0);
    }

    private BigDecimal totalAportado(UUID fondo) {
        return (BigDecimal) dsl.fetchOne("SELECT total_aportado FROM garantia.fondo_garantia WHERE id = ?", fondo)
                .get(0);
    }

    private int recargosDelFondo(UUID fondo) {
        return contar(
                "SELECT count(*)::int FROM garantia.movimiento_fondo WHERE fondo_id = ? AND tipo = 'RECARGO_MORA'",
                fondo);
    }

    private int marcas(UUID evento) {
        return contar("SELECT count(*)::int FROM garantia.evento_consumido WHERE id_evento = ?", evento);
    }

    @Test
    @DisplayName("cuadre: un recargo de Bs 25 suma 25 al saldo disponible del fondo del grupo, deja un movimiento"
            + " RECARGO_MORA con el saldo resultante, y NO cuenta como aporte del participante")
    void anotaElRecargo() {
        var f = fondoCon("200.00");
        var e = alFondo(f.grupoId(), "25.00", Moneda.BOB);

        assertThat(anotar(e)).isEqualTo(Resultado.ANOTADO);

        assertThat(saldoDisponible(f.fondoId())).isEqualByComparingTo("225.00");
        assertThat(totalAportado(f.fondoId())).isEqualByComparingTo("200.00");
        assertThat(recargosDelFondo(f.fondoId())).isEqualTo(1);
        var mov = dsl.fetchOne(
                """
                SELECT monto, saldo_resultante, referencia_tipo, referencia_id FROM garantia.movimiento_fondo
                 WHERE fondo_id = ? AND tipo = 'RECARGO_MORA'
                """,
                f.fondoId());
        assertThat((BigDecimal) mov.get(0)).isEqualByComparingTo("25.00");
        assertThat((BigDecimal) mov.get(1)).isEqualByComparingTo("225.00");
        assertThat(mov.get(2)).isEqualTo("TRANSACCION_BILLETERA");
        assertThat(mov.get(3)).isEqualTo(e.transaccionId());
    }

    @Test
    @DisplayName("evento duplicado: el mismo aporte_aplicado dos veces suma UNA vez")
    void duplicado() {
        var f = fondoCon("200.00");
        var e = alFondo(f.grupoId(), "25.00", Moneda.BOB);

        anotar(e);
        assertThat(anotar(e)).isEqualTo(Resultado.DUPLICADO);

        assertThat(saldoDisponible(f.fondoId())).isEqualByComparingTo("225.00");
        assertThat(recargosDelFondo(f.fondoId())).isEqualTo(1);
    }

    @Test
    @DisplayName("compatibilidad: un aporte al pozo del grupo (destino GRUPO) no toca el fondo ni marca el evento")
    void noEsDelFondo() {
        var f = fondoCon("200.00");
        var e = new AporteAplicado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                f.grupoId(),
                Dinero.de("500.00", Moneda.BOB),
                "GRUPO");

        assertThat(anotar(e)).isEqualTo(Resultado.NO_ES_DEL_FONDO);

        assertThat(saldoDisponible(f.fondoId())).isEqualByComparingTo("200.00");
        assertThat(marcas(e.eventoId())).isZero();
    }

    @Test
    @DisplayName(
            "rechaza anotar en un grupo sin fondo propio: no marca el evento, el reproceso lo aplica cuando exista")
    void grupoSinFondo() {
        var escenario = fixtura.escenario(fixtura.usuario());
        var e = alFondo(escenario.grupoId(), "25.00", Moneda.BOB);

        assertThatThrownBy(() -> anotar(e)).isInstanceOf(IllegalStateException.class);

        assertThat(marcas(e.eventoId())).isZero();
    }

    @Test
    @DisplayName("rechaza un ingreso en otra moneda: no se suman dólares a un fondo en bolivianos sin tipo de cambio")
    void otraMoneda() {
        var f = fondoCon("200.00");
        var e = alFondo(f.grupoId(), "25.00", Moneda.USD);

        assertThatThrownBy(() -> anotar(e)).hasMessageContaining("tipo de cambio");

        assertThat(saldoDisponible(f.fondoId())).isEqualByComparingTo("200.00");
        assertThat(marcas(e.eventoId())).isZero();
    }

    private int ajustesDelFondo(UUID fondo) {
        return contar(
                "SELECT count(*)::int FROM garantia.movimiento_fondo WHERE fondo_id = ? AND tipo = 'AJUSTE_CONTABLE'",
                fondo);
    }

    private Resultado revertir(IngresoAlFondo.TransaccionReversada r) {
        return transaccion.execute(t -> ingreso.revertir(r));
    }

    @Test
    @DisplayName("compensa: el reverso de un recargo anotado le resta los Bs 25 al fondo con un AJUSTE_CONTABLE que"
            + " referencia el reverso, y el saldo vuelve a 200")
    void reversoDelRecargo() {
        var f = fondoCon("200.00");
        var e = alFondo(f.grupoId(), "25.00", Moneda.BOB);
        anotar(e);
        UUID reverso = UUID.randomUUID();

        var r = new IngresoAlFondo.TransaccionReversada(
                UUID.randomUUID(), UUID.randomUUID(), e.transaccionId(), reverso);
        assertThat(revertir(r)).isEqualTo(Resultado.REVERTIDO);

        assertThat(saldoDisponible(f.fondoId())).isEqualByComparingTo("200.00");
        assertThat(ajustesDelFondo(f.fondoId())).isEqualTo(1);
        var mov = dsl.fetchOne(
                """
                SELECT monto, saldo_resultante, referencia_tipo, referencia_id FROM garantia.movimiento_fondo
                 WHERE fondo_id = ? AND tipo = 'AJUSTE_CONTABLE'
                """,
                f.fondoId());
        assertThat((BigDecimal) mov.get(0)).isEqualByComparingTo("25.00");
        assertThat((BigDecimal) mov.get(1)).isEqualByComparingTo("200.00");
        assertThat(mov.get(2)).isEqualTo("REVERSO_TRANSACCION");
        assertThat(mov.get(3)).isEqualTo(reverso);
        // El RECARGO_MORA original sigue ahi: append-only, se corrige con otro movimiento.
        assertThat(recargosDelFondo(f.fondoId())).isEqualTo(1);
    }

    @Test
    @DisplayName("evento duplicado: el mismo reverso dos veces resta UNA vez")
    void reversoDuplicado() {
        var f = fondoCon("200.00");
        var e = alFondo(f.grupoId(), "25.00", Moneda.BOB);
        anotar(e);
        var r = new IngresoAlFondo.TransaccionReversada(
                UUID.randomUUID(), UUID.randomUUID(), e.transaccionId(), UUID.randomUUID());

        revertir(r);
        assertThat(revertir(r)).isEqualTo(Resultado.DUPLICADO);

        assertThat(saldoDisponible(f.fondoId())).isEqualByComparingTo("200.00");
        assertThat(ajustesDelFondo(f.fondoId())).isEqualTo(1);
    }

    @Test
    @DisplayName("compatibilidad: el reverso de una transacción que no es un recargo anotado no toca ningún fondo ni"
            + " marca el evento")
    void reversoAjeno() {
        var r = new IngresoAlFondo.TransaccionReversada(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThat(revertir(r)).isEqualTo(Resultado.NO_ES_DEL_FONDO);
        assertThat(marcas(r.eventoId())).isZero();
    }
}
