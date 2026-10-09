package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H9.S1.M4-M5 · La compraventa del derecho: comprador no elegible, vigencia, carrera con la
 * entrega, aborto, compras colgadas y lo que la base exige por su cuenta.
 *
 * <p>Complementa a {@link CU130CompraTest}; los dobles de grupos y nucleo-financiero son los mismos
 * de tres niveles (regla 65).
 */
class CU130CompraBordesTest extends BaseDeCompraDeTurno {

    @Test
    @DisplayName(
            "INVALIDO · COMPRADOR NO ELEGIBLE · Dado un no miembro, un miembro inactivo, el propio vendedor o grupos caido · Cuando compra · Entonces se rechaza y la oferta sigue PUBLICADA")
    void compradorNoElegible() {
        var c = caso();
        var oferta = publicar(c);
        var forastero = contextoDe(fixtura.usuario());
        var inactivo = fixtura.miembro(c.e().grupoId());
        gruposDoble.miembro(c.e().grupoId(), inactivo.usuarioId(), inactivo.participanteId(), false);

        rechaza(() -> compraCU.comprar(oferta.ofertaId(), "k1-" + c.e().turnoId(), forastero), "AP-CU130-08");
        rechaza(
                () -> compraCU.comprar(oferta.ofertaId(), "k2-" + c.e().turnoId(), contextoDe(inactivo.usuarioId())),
                "AP-CU130-06");
        rechaza(() -> compraCU.comprar(oferta.ofertaId(), "k3-" + c.e().turnoId(), c.ctxVendedor()), "AP-CU130-06");
        gruposDoble.seCae(true);
        rechaza(() -> compraCU.comprar(oferta.ofertaId(), "k4-" + c.e().turnoId(), c.ctxComprador()), "AP-CU130-08");
        gruposDoble.seCae(false);

        assertThat(estadoDeOferta(oferta.ofertaId())).isEqualTo("PUBLICADA");
        assertThat(contar("SELECT count(*)::int FROM entregas.cesion_derecho WHERE turno_id = ?", c.e().turnoId()))
                .isZero();
    }

    @Test
    @DisplayName(
            "INVALIDO · VIGENCIA PASADA · Dada una oferta vencida que el barrido todavia no marco · Cuando se compra · Entonces AP-CU130-05: vencimiento y compra no se cruzan")
    void vigenciaPasada() {
        var c = caso();
        var oferta = publicar(c);
        dslFixtura.execute(
                "UPDATE entregas.oferta_turno SET vigente_hasta = now() - interval '1 second' WHERE id = ?",
                oferta.ofertaId());

        rechaza(() -> compraCU.comprar(oferta.ofertaId(), "k-" + c.e().turnoId(), c.ctxComprador()), "AP-CU130-05");

        assertThat(fondosDoble.retencionesNuevas.get()).isZero();
    }

    @Test
    @DisplayName(
            "CARRERA CON LA ENTREGA · Dado que el pozo se fondea despues de retener y antes del titulo · Cuando se reanuda · Entonces AP-CU130-02, se libera lo retenido, la cesion es FALLIDA y el pozo va al vendedor")
    void fondeoGanaAlTitulo() {
        var c = caso();
        var oferta = publicar(c);
        var reserva = pasosDeCesion.reservar(
                oferta.ofertaId(),
                c.comprador().usuarioId(),
                c.comprador().participanteId(),
                "compra-" + c.e().turnoId(),
                c.ctxComprador());
        var ref = fondosDoble.retener(
                c.comprador().usuarioId(),
                bob("5500.00"),
                "cesion:" + reserva.cesion().id() + ":retener");
        pasosDeCesion.retenida(reserva.cesion().id(), ref.referencia(), c.ctxComprador());
        // El fondeo pasa por encima: el derecho se entrega al titular de siempre.
        fondearElPozo(c);

        rechaza(() -> compraCU.reanudar(reserva.cesion().id(), c.ctxComprador()), "AP-CU130-02");

        assertThat(estadoDeCesion(reserva.cesion().id())).isEqualTo("FALLIDA");
        assertThat(fondosDoble.disponible(c.comprador().usuarioId())).isEqualTo(bob("10000.00"));
        assertThat(fondosDoble.retenidoVivo().esCero()).isTrue();
        assertThat(dsl.fetchOne(
                                "SELECT beneficiario_participante_id FROM entregas.entrega_fondo WHERE turno_id = ?",
                                c.e().turnoId())
                        .get(0, UUID.class))
                .isEqualTo(c.e().participanteId());
    }

    @Test
    @DisplayName(
            "ABORTAR · Dada una compra con el saldo retenido y sin pagar · Cuando se aborta · Entonces se libera el saldo, la oferta reabre; ya liquidada no se deshace a ciegas")
    void abortar() {
        var c = caso();
        var oferta = publicar(c);
        fondosDoble.fallaAlPagar(5);
        var enCurso = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());
        assertThat(enCurso.estado()).isEqualTo("TITULO_ASIGNADO");

        var abortada = compraCU.abortar(enCurso.cesionId(), "Se arrepintio", c.ctxComprador());

        assertThat(abortada.estado()).isEqualTo("FALLIDA");
        assertThat(estadoDeOferta(oferta.ofertaId())).isEqualTo("PUBLICADA");
        assertThat(fondosDoble.disponible(c.comprador().usuarioId())).isEqualTo(bob("10000.00"));

        fondosDoble.fallaAlPagar(0);
        var otra = fixtura.miembro(c.e().grupoId());
        gruposDoble.miembro(c.e().grupoId(), otra.usuarioId(), otra.participanteId(), true);
        fondosDoble.saldo(otra.usuarioId(), bob("9000.00"));
        var vendida = compraCU.comprar(oferta.ofertaId(), "compra-2-" + c.e().turnoId(), contextoDe(otra.usuarioId()));
        assertThat(vendida.liquidada()).isTrue();
        rechaza(() -> compraCU.abortar(vendida.cesionId(), "tarde", c.ctxComprador()), "AP-CU130-09");
    }

    @Test
    @DisplayName(
            "COLGADAS · Dada una compra detenida mas alla de la reserva maxima · Cuando corre la recuperacion · Entonces se aborta y la oferta vuelve a estar a la venta")
    void colgadas() {
        var c = caso();
        var oferta = publicar(c);
        fondosDoble.seCae(true);
        var enCurso = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());
        dslFixtura.execute(
                "UPDATE entregas.cesion_derecho SET creada_en = now() - interval '1 hour' WHERE id = ?",
                enCurso.cesionId());
        fondosDoble.seCae(false);

        int abortadas = compraCU.abortarColgadas(c.ctxVendedor());

        assertThat(abortadas).isGreaterThanOrEqualTo(1);
        assertThat(estadoDeCesion(enCurso.cesionId())).isEqualTo("FALLIDA");
        assertThat(estadoDeOferta(oferta.ofertaId())).isEqualTo("PUBLICADA");
    }

    @Test
    @DisplayName(
            "La base rechaza una cesion liquidada sin referencias y una segunda cesion viva del mismo turno (R-DES-05)")
    void laBaseExigeLoSuyo() {
        var c = caso();
        var oferta = publicar(c);
        var r = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());

        assertThat(rechazaLaBase(
                        "UPDATE entregas.cesion_derecho SET liquidacion_ref = NULL WHERE id = ?", r.cesionId()))
                .contains("ck_cesion_derecho_liquidada");
        assertThat(rechazaLaBase(
                        """
                        INSERT INTO entregas.cesion_derecho (id, oferta_turno_id, turno_id, participante_origen_id, participante_destino_id, comprador_usuario_id, moneda, monto_precio, estado, clave_idempotencia, creada_en, version)
                        SELECT gen_random_uuid(), oferta_turno_id, turno_id, participante_origen_id, participante_destino_id, comprador_usuario_id, moneda, monto_precio, 'VALIDADA', 'otra-' || gen_random_uuid(), now(), 0
                        FROM entregas.cesion_derecho WHERE id = ?""",
                        r.cesionId()))
                .contains("uq_cesion_derecho_turno_viva");
        assertThat(rechazaLaBase(
                        "UPDATE entregas.cesion_derecho SET participante_destino_id = participante_origen_id WHERE id = ?",
                        r.cesionId()))
                .contains("ck_cesion_derecho_partes");
    }
}
