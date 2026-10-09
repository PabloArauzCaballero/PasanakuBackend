package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H9.S1.M4-M5 · La compraventa del derecho, de punta a punta, con la caida del dependido.
 *
 * <p>grupos y nucleo-financiero van como DOBLES de tres niveles (regla 65). Se verifica la saga de
 * este servicio y su persistencia; los contratos de los vecinos siguen siendo una propuesta.
 * Los rechazos, la carrera con la entrega, el aborto y las restricciones de la base estan en
 * {@link CU130CompraBordesTest}, separada por la regla de tamano de archivo.
 */
class CU130CompraTest extends BaseDeCompraDeTurno {

    @Test
    @DisplayName(
            "CORRECTO · Dada una oferta de Bs 5.500 con cargos de Bs 50 · Cuando el comprador compra · Entonces la cesion queda LIQUIDADA, el vendedor cobra 5.450, la plataforma 50, el comprador paga 5.500 y la plata del sistema no cambia")
    void compraCompleta() {
        var c = caso();
        var oferta = publicar(c);
        var antes = fondosDoble.totalDelSistema();

        var r = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());

        assertThat(r.liquidada()).isTrue();
        assertThat(estadoDeOferta(oferta.ofertaId())).isEqualTo("VENDIDA");
        assertThat(estadoDeCesion(r.cesionId())).isEqualTo("LIQUIDADA");
        var f = dsl.fetchOne(
                "SELECT retencion_ref, liquidacion_ref, titulo_asignado_en, liquidada_en FROM entregas.cesion_derecho WHERE id = ?",
                r.cesionId());
        assertThat(f.get(0, UUID.class)).isNotNull();
        assertThat(f.get(1, UUID.class)).isNotNull();
        assertThat(f.get(2)).isNotNull();
        assertThat(fondosDoble.disponible(c.comprador().usuarioId())).isEqualTo(bob("4500.00"));
        assertThat(fondosDoble.disponible(c.vendedor())).isEqualTo(bob("5450.00"));
        assertThat(fondosDoble.plataforma()).isEqualTo(bob("50.00"));
        assertThat(fondosDoble.retenidoVivo().esCero()).isTrue();
        assertThat(fondosDoble.totalDelSistema()).isEqualTo(antes);
        assertThat(contar(
                        "SELECT count(*)::int FROM entregas.evento_dominio WHERE tipo = 'entregas.derecho_cedido' AND agregado_id = ?",
                        r.cesionId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada la misma clave dos veces · Cuando compra · Entonces hay una cesion, una retencion y un pago (idempotencia ejecutada)")
    void idempotente() {
        var c = caso();
        var oferta = publicar(c);

        var primera = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());
        var segunda = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());

        assertThat(segunda.cesionId()).isEqualTo(primera.cesionId());
        assertThat(segunda.liquidada()).isTrue();
        assertThat(fondosDoble.retencionesNuevas.get()).isEqualTo(1);
        assertThat(fondosDoble.pagosNuevos.get()).isEqualTo(1);
        assertThat(contar("SELECT count(*)::int FROM entregas.cesion_derecho WHERE turno_id = ?", c.e().turnoId()))
                .isEqualTo(1);
        assertThat(fondosDoble.disponible(c.vendedor())).isEqualTo(bob("5450.00"));
    }

    @Test
    @DisplayName(
            "Dado el derecho ya cedido · Cuando se fondea el pozo del turno · Entonces la entrega se programa para el COMPRADOR y el cupo del vendedor no cambia")
    void elPozoVaAlComprador() {
        var c = caso();
        var oferta = publicar(c);
        compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());

        fondearElPozo(c);

        var fila = dsl.fetchOne(
                "SELECT beneficiario_participante_id, cupo_id FROM entregas.entrega_fondo WHERE turno_id = ?",
                c.e().turnoId());
        assertThat(fila.get(0, UUID.class)).isEqualTo(c.comprador().participanteId());
        assertThat(fila.get(1, UUID.class)).isEqualTo(c.e().cupoId());
    }

    @Test
    @DisplayName(
            "Dado un comprador sin saldo · Cuando compra · Entonces AP-CU130-07, la cesion queda FALLIDA, la oferta vuelve a estar a la venta y no queda ninguna retencion huerfana; otro comprador puede comprar despues")
    void saldoInsuficiente() {
        var c = caso();
        fondosDoble.saldo(c.comprador().usuarioId(), bob("100.00"));
        var oferta = publicar(c);

        rechaza(
                () -> compraCU.comprar(oferta.ofertaId(), "compra-1-" + c.e().turnoId(), c.ctxComprador()),
                "AP-CU130-07");

        assertThat(estadoDeOferta(oferta.ofertaId())).isEqualTo("PUBLICADA");
        assertThat(fondosDoble.retencionesNuevas.get()).isZero();
        assertThat(fondosDoble.disponible(c.comprador().usuarioId())).isEqualTo(bob("100.00"));
        var otro = fixtura.miembro(c.e().grupoId());
        gruposDoble.miembro(c.e().grupoId(), otro.usuarioId(), otro.participanteId(), true);
        fondosDoble.saldo(otro.usuarioId(), bob("9000.00"));
        var r = compraCU.comprar(oferta.ofertaId(), "compra-2-" + c.e().turnoId(), contextoDe(otro.usuarioId()));
        assertThat(r.liquidada()).isTrue();
    }

    @Test
    @DisplayName(
            "CAIDA DEL DEPENDIDO · Dado que nucleo no responde al retener · Cuando compra · Entonces queda EN CURSO sin titulo ni pago; al volver nucleo, reanudar completa una sola retencion y un solo pago")
    void nucleoCaidoAlRetener() {
        var c = caso();
        var oferta = publicar(c);
        fondosDoble.seCae(true);

        var enCurso = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());

        assertThat(enCurso.liquidada()).isFalse();
        assertThat(enCurso.estado()).isEqualTo("VALIDADA");
        assertThat(estadoDeOferta(oferta.ofertaId())).isEqualTo("RESERVADA");
        assertThat(fondosDoble.retencionesNuevas.get()).isZero();

        fondosDoble.seCae(false);
        var fin = compraCU.reanudar(enCurso.cesionId(), c.ctxComprador());
        assertThat(fin.liquidada()).isTrue();
        assertThat(fondosDoble.retencionesNuevas.get()).isEqualTo(1);
        assertThat(fondosDoble.pagosNuevos.get()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "CAIDA DEL DEPENDIDO · Dado que nucleo cae DESPUES de dar el titulo y antes de pagar · Cuando se reanuda · Entonces el vendedor cobra una sola vez y el comprador no paga dos")
    void nucleoCaidoAlPagar() {
        var c = caso();
        var oferta = publicar(c);
        fondosDoble.fallaAlPagar(1);

        var primera = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());
        var segunda = compraCU.reanudar(primera.cesionId(), c.ctxComprador());
        var tercera = compraCU.reanudar(primera.cesionId(), c.ctxComprador());

        assertThat(primera.estado()).isEqualTo("TITULO_ASIGNADO");
        assertThat(primera.liquidada()).isFalse();
        assertThat(estadoDeOferta(oferta.ofertaId())).isEqualTo("VENDIDA");
        assertThat(segunda.liquidada()).isTrue();
        assertThat(tercera.liquidada()).isTrue();
        assertThat(fondosDoble.pagosNuevos.get()).isEqualTo(1);
        assertThat(fondosDoble.retencionesNuevas.get()).isEqualTo(1);
        assertThat(fondosDoble.disponible(c.comprador().usuarioId())).isEqualTo(bob("4500.00"));
        assertThat(fondosDoble.disponible(c.vendedor())).isEqualTo(bob("5450.00"));
    }

    @Test
    @DisplayName(
            "REINICIO · Dado que el proceso murio justo despues de retener (nucleo retuvo, la cesion no lo sabe) · Cuando se reanuda · Entonces nucleo devuelve la MISMA retencion y no se retiene dos veces")
    void reinicioTrasRetener() {
        var c = caso();
        var oferta = publicar(c);
        var reserva = pasosDeCesion.reservar(
                oferta.ofertaId(),
                c.comprador().usuarioId(),
                c.comprador().participanteId(),
                "compra-" + c.e().turnoId(),
                c.ctxComprador());
        // El proceso retiene en nucleo y muere antes de anotar FONDOS_RETENIDOS.
        fondosDoble.retener(
                c.comprador().usuarioId(),
                bob("5500.00"),
                "cesion:" + reserva.cesion().id() + ":retener");
        assertThat(estadoDeCesion(reserva.cesion().id())).isEqualTo("VALIDADA");

        var r = compraCU.reanudar(reserva.cesion().id(), c.ctxComprador());

        assertThat(r.liquidada()).isTrue();
        assertThat(fondosDoble.retencionesNuevas.get()).isEqualTo(1);
        assertThat(fondosDoble.disponible(c.comprador().usuarioId())).isEqualTo(bob("4500.00"));
    }
}
