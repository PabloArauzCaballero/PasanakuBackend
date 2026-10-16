package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H9.S1.M5 · el hecho que `nucleo-financiero` consulta para pagar al vendedor, contra PostgreSQL real: quien vende, el
 * precio exacto y el estado salen de la oferta guardada, no de lo que diga quien llama.
 */
class CU130OfertaParaPagoTest extends BaseDeMercado {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    /** La identidad de servicio de entregas: otro usuario y otro rol que el vendedor y el comprador. */
    private ContextoSesion comoServicio() {
        return ContextoSesion.de(
                UUID.randomUUID(),
                "SERVICIO_ENTREGAS",
                new Traza(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("CORRECTO · la oferta publicada devuelve su vendedor, el precio exacto, sus cargos y el estado")
    void devuelveVendedorPrecioYEstado() {
        var c = caso();
        var publicada = publicar(c);

        var oferta =
                ofertasCU.verParaElPago(publicada.ofertaId(), comoServicio()).orElseThrow();

        assertThat(oferta.ofertaId()).isEqualTo(publicada.ofertaId());
        assertThat(oferta.vendedorUsuarioId()).isEqualTo(c.vendedor());
        assertThat(oferta.precio()).isEqualTo(bob("5500.00"));
        assertThat(oferta.cargos().esNegativo()).isFalse();
        assertThat(oferta.cargos().esMayorQue(oferta.precio())).isFalse();
        assertThat(oferta.estado()).isEqualTo("PUBLICADA");
    }

    @Test
    @DisplayName("LIMITE · el estado que informa es el que tiene la oferta ahora: cancelada se informa CANCELADA")
    void informaElEstadoActual() {
        var c = caso();
        var publicada = publicar(c);
        ofertasCU.cancelar(publicada.ofertaId(), c.ctxVendedor());

        var oferta =
                ofertasCU.verParaElPago(publicada.ofertaId(), comoServicio()).orElseThrow();

        assertThat(oferta.estado()).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("INVALIDO · una oferta que no existe se informa vacia: nucleo no paga")
    void ofertaInexistente() {
        assertThat(ofertasCU.verParaElPago(UUID.randomUUID(), comoServicio())).isEmpty();
    }
}
