package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-64 por subasta · el cierre: gana la más alta, grupos pasa el cupo y la billetera paga; y sus fallas. */
class CU64CierreDeSubastaTest extends BaseDeSubasta {

    // ------------------------------------------------------------------ cerrar y pagar

    @Test
    @DisplayName("Dada una subasta sin pujas · Cuando vence · Entonces queda DESIERTA, grupos no se toca y nadie cobra")
    void sinPujasQuedaDesierta() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");

        vencerYCerrar(id);

        assertThat(estadoDeSubasta(id)).isEqualTo("DESIERTA");
        assertThat(gruposSubasta.fueTraspasada(id)).isFalse();
        assertThat(fondosDoble.disponible(c.saliente())).isEqualTo(bob("0.00"));
    }

    @Test
    @DisplayName(
            "Dada una subasta con la más alta de Bs 1.500 · Cuando vence · Entonces grupos pasa el cupo, el saliente cobra exactamente Bs 1.500, la subasta queda VENDIDA y repetir el cierre no paga dos veces")
    void alCierreGanaLaMasAltaYElSalienteCobra() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");
        UUID a = postorConSaldo("5000.00");
        UUID b = postorConSaldo("5000.00");
        pujar(id, a, "1200.00");
        pujar(id, b, "1500.00");
        var totalAntes = fondosDoble.totalDelSistema();

        vencerYCerrar(id);
        // El trabajo programado vuelve a pasar: no traspasa ni paga otra vez (si pagara, el saliente tendria 3.000).
        subastaCU.correrCierres(sistema());
        seguir(id);

        assertThat(estadoDeSubasta(id)).isEqualTo("VENDIDA");
        assertThat(gruposSubasta.fueTraspasada(id)).isTrue();
        assertThat(fondosDoble.disponible(c.saliente())).isEqualTo(bob("1500.00"));
        assertThat(fondosDoble.disponible(b)).isEqualTo(bob("3500.00"));
        assertThat(fondosDoble.disponible(a)).isEqualTo(bob("5000.00"));
        assertThat(fondosDoble.plataforma()).isEqualTo(bob("0.00"));
        assertThat(fondosDoble.retenidoVivo()).isEqualTo(bob("0.00"));
        assertThat(fondosDoble.totalDelSistema()).isEqualTo(totalAntes);
        assertThat(eventos("entregas.subasta_vendida", id)).isEqualTo(1);
        var pago = consultasCU.paraElPago(id, sistema()).orElseThrow();
        assertThat(pago.precio()).isEqualTo(bob("1500.00"));
        assertThat(pago.cargos()).isEqualTo(bob("0.00"));
        assertThat(pago.vendedorUsuarioId()).isEqualTo(c.saliente());
    }

    @Test
    @DisplayName(
            "Dado grupos caído al cerrar · Entonces la subasta queda ADJUDICADA sin pagar; cuando vuelve, el cierre la termina VENDIDA")
    void gruposCaidoAlCerrar() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");
        UUID a = postorConSaldo("5000.00");
        pujar(id, a, "1200.00");

        gruposSubasta.seCae(true);
        vencerYCerrar(id);
        assertThat(estadoDeSubasta(id)).isEqualTo("ADJUDICADA");
        assertThat(fondosDoble.disponible(c.saliente())).isEqualTo(bob("0.00"));

        gruposSubasta.seCae(false);
        seguir(id);
        assertThat(estadoDeSubasta(id)).isEqualTo("VENDIDA");
        assertThat(fondosDoble.disponible(c.saliente())).isEqualTo(bob("1200.00"));
    }

    @Test
    @DisplayName(
            "Dada la billetera caída al pagar · Entonces queda LIQUIDANDO con el cupo ya traspasado; cuando vuelve, se paga UNA vez")
    void billeteraCaidaAlPagar() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");
        UUID a = postorConSaldo("5000.00");
        pujar(id, a, "1200.00");

        fondosDoble.fallaAlPagar(1);
        vencerYCerrar(id);
        assertThat(estadoDeSubasta(id)).isEqualTo("LIQUIDANDO");
        assertThat(gruposSubasta.fueTraspasada(id)).isTrue();
        assertThat(fondosDoble.disponible(c.saliente())).isEqualTo(bob("0.00"));

        seguir(id);
        seguir(id);
        assertThat(estadoDeSubasta(id)).isEqualTo("VENDIDA");
        // Un solo pago: dos pagarian Bs 2.400.
        assertThat(fondosDoble.disponible(c.saliente())).isEqualTo(bob("1200.00"));
        assertThat(fondosDoble.retenidoVivo()).isEqualTo(bob("0.00"));
    }

    @Test
    @DisplayName(
            "Dado que grupos ya no puede pasar el cupo (AP-CU64-01) · Entonces la subasta FALLA, el saliente no cobra y al ganador se le devuelve todo")
    void gruposRechazaElTraspaso() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");
        UUID a = postorConSaldo("5000.00");
        pujar(id, a, "1200.00");

        gruposSubasta.rechazaElTraspaso(true);
        vencerYCerrar(id);

        assertThat(estadoDeSubasta(id)).isEqualTo("FALLIDA");
        assertThat(fondosDoble.disponible(a)).isEqualTo(bob("5000.00"));
        assertThat(fondosDoble.disponible(c.saliente())).isEqualTo(bob("0.00"));
    }

    @Test
    @DisplayName("Una puja que llega después del cierre se rechaza (05) y no retiene nada")
    void pujaTardia() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");
        UUID a = postorConSaldo("5000.00");
        AHORA.set(AHORA.get().plus(java.time.Duration.ofHours(25)));

        assertThatThrownBy(() -> pujar(id, a, "1200.00"))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU136-05"));
        assertThat(fondosDoble.retencionesNuevas.get()).isZero();
    }
}
