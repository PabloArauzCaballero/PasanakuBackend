package bo.aportaya.garantia.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Un asiento propuesto que no cuadra no sale del servicio. */
class AsientoPropuestoTest {

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName("Dada una aplicacion de Bs 1.000 · Cuando se arma el asiento · Entonces debe y haber suman lo mismo")
    void cuadra() {
        var a = AsientoPropuesto.aplicacion(UUID.randomUUID(), bob("1000.00"));

        Dinero debe = a.partidas().stream()
                .map(AsientoPropuesto.Partida::debe)
                .reduce(Dinero::mas)
                .orElseThrow();
        Dinero haber = a.partidas().stream()
                .map(AsientoPropuesto.Partida::haber)
                .reduce(Dinero::mas)
                .orElseThrow();
        assertThat(debe).isEqualTo(haber).isEqualTo(bob("1000.00"));
        assertThat(a.origenTipo()).isEqualTo("COBERTURA");
    }

    @Test
    @DisplayName("Dado el contra-asiento de una aplicacion · Cuando se compara · Entonces invierte cuenta por cuenta")
    void reversaInvierte() {
        UUID id = UUID.randomUUID();
        var original = AsientoPropuesto.aplicacion(id, bob("250.00"));
        var reversa = AsientoPropuesto.reversaDeAplicacion(id, bob("250.00"));

        for (int i = 0; i < original.partidas().size(); i++) {
            var o = original.partidas().get(i);
            var r = reversa.partidas().stream()
                    .filter(p -> p.cuenta() == o.cuenta())
                    .findFirst()
                    .orElseThrow();
            assertThat(r.debe()).isEqualTo(o.haber());
            assertThat(r.haber()).isEqualTo(o.debe());
        }
    }

    @Test
    @DisplayName("Dadas partidas que no suman igual · Cuando se construye · Entonces se rechaza")
    void descuadrado() {
        var cero = bob("0.00");
        assertThatThrownBy(() -> new AsientoPropuesto(
                        "AJUSTE",
                        UUID.randomUUID(),
                        List.of(
                                new AsientoPropuesto.Partida(AsientoPropuesto.Cuenta.CAJA_EMPRESA, bob("10.00"), cero),
                                new AsientoPropuesto.Partida(
                                        AsientoPropuesto.Cuenta.RESERVA_RESPALDO, cero, bob("9.99")))))
                .isInstanceOf(ErrorDeDominio.class);
    }

    @Test
    @DisplayName(
            "Dado un asiento · Cuando viaja en el evento · Entonces los importes son cadenas decimales con su moneda")
    void cargaConCadenas() {
        var carga = AsientoPropuesto.reserva(UUID.randomUUID(), bob("8000.00")).comoCarga();

        assertThat(carga.get("partidas").toString()).contains("8000.00").contains("BOB");
    }
}
