package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiro;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** F1.L1.M04 · el puerto de desembolso sin proveedor contratado falla cerrado. Puro. */
class ProveedorDeRetiroNoConfiguradoTest {

    private final ProveedorDeRetiroNoConfigurado proveedor = new ProveedorDeRetiroNoConfigurado();

    @Test
    @DisplayName("invalido: instruir cualquier retiro falla con AP-CU11-15 PROVEEDOR_DE_DESEMBOLSO_NO_CONFIGURADO")
    void instruirFallaCerrado() {
        assertThatThrownBy(() -> proveedor.instruir(UUID.randomUUID(), Dinero.de("100.00", Moneda.BOB)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("PROVEEDOR_DE_DESEMBOLSO_NO_CONFIGURADO")
                .extracting(e -> ((ErrorDeNegocio) e).codigo().valor())
                .isEqualTo("AP-CU11-15");
    }

    @Test
    @DisplayName("limite: consultar una referencia no afirma nada (TIMEOUT), ni acepta ni rechaza")
    void consultarNoAfirma() {
        assertThat(proveedor.consultar("cualquiera")).isEqualTo(ProveedorDeRetiro.Estado.TIMEOUT);
    }
}
