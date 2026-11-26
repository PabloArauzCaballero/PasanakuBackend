package bo.aportaya.aportes.infraestructura.pasarela.simulada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.dominio.InterpreteDeWebhook.PayloadIlegible;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El contrato del interprete del doble, en tres niveles (regla 65). */
class InterpreteDeWebhookJsonTest {

    private final InterpreteDeWebhookJson interprete = new InterpreteDeWebhookJson(new ObjectMapper());

    @Test
    @DisplayName("correcto: lee evento, idEvento, referencia y monto con decimales exactos")
    void correcto() throws Exception {
        var e = interprete.interpretar(
                "{\"evento\":\"pago.acreditado\",\"idEvento\":\"e1\",\"referenciaProveedor\":\"r1\",\"monto\":\"0.10\",\"moneda\":\"BOB\"}");

        assertThat(e.idEvento()).isEqualTo("e1");
        assertThat(e.tipo()).isEqualTo("pago.acreditado");
        assertThat(e.referenciaProveedor()).isEqualTo("r1");
        assertThat(e.monto()).contains(Dinero.de("0.10", Moneda.BOB));
    }

    @Test
    @DisplayName("limite: campos ausentes o nulos quedan en null/vacio (el caso de uso decide), nunca excepcion")
    void limite() throws Exception {
        var vacio = interprete.interpretar("{}");
        assertThat(vacio.idEvento()).isNull();
        assertThat(vacio.referenciaProveedor()).isNull();
        assertThat(vacio.monto()).isEmpty();

        var nulos = interprete.interpretar("{\"idEvento\":null,\"monto\":null,\"moneda\":null}");
        assertThat(nulos.idEvento()).isNull();
        assertThat(nulos.monto()).isEmpty();
    }

    @Test
    @DisplayName(
            "invalido: moneda desconocida o monto no numerico => sin monto; cuerpo que no es objeto JSON => PayloadIlegible")
    void invalido() throws Exception {
        assertThat(interprete
                        .interpretar("{\"monto\":\"10.00\",\"moneda\":\"XXX\"}")
                        .monto())
                .isEmpty();
        assertThat(interprete
                        .interpretar("{\"monto\":\"abc\",\"moneda\":\"BOB\"}")
                        .monto())
                .isEmpty();
        assertThatThrownBy(() -> interprete.interpretar("no es json")).isInstanceOf(PayloadIlegible.class);
        assertThatThrownBy(() -> interprete.interpretar("[1,2]")).isInstanceOf(PayloadIlegible.class);
        assertThatThrownBy(() -> interprete.interpretar("")).isInstanceOf(PayloadIlegible.class);
    }
}
