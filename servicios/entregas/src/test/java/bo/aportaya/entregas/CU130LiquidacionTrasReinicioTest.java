package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.entregas.dominio.ReglasDeOferta;
import bo.aportaya.entregas.dominio.puertos.FondosDelComprador;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H9.S1.M5 · Liquidar el pago y cambiar de titular: LEDGER, TITULARIDAD y EVENTOS coinciden tras un reinicio, en
 * cualquiera de los tres puntos donde el proceso puede morir.
 *
 * <p>Un reinicio es exactamente esto: el proceso muere y el siguiente solo conoce la base y lo que nucleo guardo.
 * Los puntos de muerte son los de la saga: (1) despues de dar el titulo y antes de pagar, (2) despues de que
 * nucleo pago y antes de anotarlo, (3) sin reinicio (camino feliz, de referencia). nucleo-financiero es un DOBLE
 * ({@link DoblesDelMercado.Fondos}, que cumple {@link ContratoDeFondosDelComprador}); lo que queda verificado es
 * la saga de ESTE servicio. La prueba contra el productor real queda pendiente de {@code pagarAlVendedor} en nucleo.
 */
class CU130LiquidacionTrasReinicioTest extends BaseDeCompraDeTurno {

    private static final String PRECIO = "5500.00";

    private int eventos(String tipo, UUID turno) {
        return contar(
                "SELECT count(*)::int FROM entregas.evento_dominio WHERE tipo = ? AND payload->>'turnoId' = ?",
                tipo,
                turno.toString());
    }

    /** Lo que tiene que ser cierto cuando la cesion quedo liquidada, no importa por donde se llego. */
    private void coincide(Caso c, UUID cesionId, UUID ofertaId) {
        // LEDGER (nucleo): el comprador pago 5.500 una vez; el vendedor cobro 5.450; la plataforma 50; nada retenido.
        assertThat(fondosDoble.disponible(c.comprador().usuarioId())).isEqualTo(bob("4500.00"));
        assertThat(fondosDoble.disponible(c.vendedor())).isEqualTo(bob("5450.00"));
        assertThat(fondosDoble.plataforma()).isEqualTo(bob("50.00"));
        assertThat(fondosDoble.retenidoVivo().esCero()).isTrue();
        assertThat(fondosDoble.totalDelSistema()).isEqualTo(bob("10000.00"));
        // TITULARIDAD: la cesion y la oferta lo dicen, y la referencia del pago es la que anoto la cesion.
        assertThat(estadoDeCesion(cesionId)).isEqualTo("LIQUIDADA");
        assertThat(estadoDeOferta(ofertaId)).isEqualTo("VENDIDA");
        var fila = dsl.fetchOne("SELECT liquidacion_ref FROM entregas.cesion_derecho WHERE id = ?", cesionId);
        assertThat(fila.get(0, UUID.class)).isNotNull();
        // EVENTOS: uno solo de cesion, con esa misma referencia y ese precio; ninguno de falla.
        assertThat(eventos("entregas.derecho_cedido", c.e().turnoId())).isEqualTo(1);
        assertThat(eventos("entregas.cesion_fallida", c.e().turnoId())).isZero();
        var evento = dsl.fetchOne(
                "SELECT payload->>'liquidacionRef', payload->>'precio' FROM entregas.evento_dominio WHERE tipo = 'entregas.derecho_cedido' AND payload->>'turnoId' = ?",
                c.e().turnoId().toString());
        assertThat(evento.get(0, String.class))
                .isEqualTo(fila.get(0, UUID.class).toString());
        assertThat(new BigDecimal(evento.get(1, String.class))).isEqualByComparingTo(PRECIO);
    }

    @Test
    @DisplayName(
            "REFERENCIA · Dada una compra sin caidas · Cuando se liquida · Entonces ledger, titularidad y eventos coinciden")
    void sinReinicio() {
        var c = caso();
        var oferta = publicar(c);

        var r = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());

        assertThat(r.liquidada()).isTrue();
        coincide(c, r.cesionId(), oferta.ofertaId());
        assertThat(fondosDoble.pagosNuevos.get()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "REINICIO 1 · Dado que el proceso murio con el titulo dado y sin pagar · Cuando el siguiente reanuda (y otra vez, y otra) · Entonces el vendedor cobra una vez y todo coincide")
    void muereAntesDePagar() {
        var c = caso();
        var oferta = publicar(c);
        fondosDoble.fallaAlPagar(1);
        var enCurso = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());
        assertThat(enCurso.estado()).isEqualTo("TITULO_ASIGNADO");
        assertThat(fondosDoble.pagosNuevos.get()).isZero();

        var primera = compraCU.reanudar(enCurso.cesionId(), c.ctxComprador());
        var segunda = compraCU.reanudar(enCurso.cesionId(), c.ctxComprador());
        var tercera = compraCU.reanudar(enCurso.cesionId(), c.ctxComprador());

        assertThat(primera.liquidada() && segunda.liquidada() && tercera.liquidada())
                .isTrue();
        assertThat(fondosDoble.pagosNuevos.get()).isEqualTo(1);
        assertThat(fondosDoble.retencionesNuevas.get()).isEqualTo(1);
        coincide(c, enCurso.cesionId(), oferta.ofertaId());
    }

    @Test
    @DisplayName(
            "REINICIO 2 · Dado que nucleo PAGO y el proceso murio antes de anotarlo · Cuando el siguiente reanuda · Entonces nucleo devuelve el MISMO pago, la cesion lo anota y no se paga dos veces")
    void muereDespuesDePagarYAntesDeAnotar() {
        var c = caso();
        var oferta = publicar(c);
        fondosDoble.fallaAlPagar(1);
        var enCurso = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());
        var vista = pasosDeCesion.leer(enCurso.cesionId(), c.ctxComprador());
        // El proceso "anterior" llego a pagar en nucleo con la clave de la cesion y murio antes de liquidar localmente.
        FondosDelComprador.Referencia yaPagado = fondosDoble.pagarAlVendedor(
                vista.cesion().retencionRef(),
                vista.oferta().vendedorUsuarioId(),
                ReglasDeOferta.paraElVendedor(
                        vista.oferta().precio(), vista.oferta().cargos()),
                vista.oferta().cargos(),
                "cesion:" + enCurso.cesionId() + ":pagar");
        assertThat(yaPagado.resultado()).isEqualTo(FondosDelComprador.Resultado.OK);
        assertThat(estadoDeCesion(enCurso.cesionId())).isEqualTo("TITULO_ASIGNADO");

        var fin = compraCU.reanudar(enCurso.cesionId(), c.ctxComprador());

        assertThat(fin.liquidada()).isTrue();
        assertThat(fondosDoble.pagosNuevos.get()).isEqualTo(1);
        coincide(c, enCurso.cesionId(), oferta.ofertaId());
        var anotada = dsl.fetchOne(
                        "SELECT liquidacion_ref FROM entregas.cesion_derecho WHERE id = ?", enCurso.cesionId())
                .get(0, UUID.class);
        assertThat(anotada).isEqualTo(yaPagado.referencia());
    }
}
