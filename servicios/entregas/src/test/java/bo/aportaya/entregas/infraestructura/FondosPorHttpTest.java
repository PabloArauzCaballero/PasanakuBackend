package bo.aportaya.entregas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.entregas.dominio.puertos.FondosDelComprador.Referencia;
import bo.aportaya.entregas.dominio.puertos.FondosDelComprador.Resultado;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link FondosPorHttp} · CORRECTO y LIMITE: lo que sale (ruta, cuerpo, quien firma, clave de idempotencia) y como se lee
 * lo que vuelve. Los casos invalidos estan en {@link FondosPorHttpInvalidoTest}; el productor real lo ejerce
 * {@code despliegue/ensayos/integracion} contra el stack compose.
 */
class FondosPorHttpTest extends BaseDeFondosPorHttp {

    // ------------------------------------------------------------------ correcto

    @Test
    @DisplayName("CORRECTO · retener pregunta la billetera con el token del comprador y retiene nombrando la oferta")
    void retenerCorrecto() {
        sesionDe(COMPRADOR);

        Referencia r = adaptador().retener(COMPRADOR, OFERTA, bob("5500.00"), "cesion:1:retener");

        assertThat(r.resultado()).isEqualTo(Resultado.OK);
        assertThat(r.referencia()).isEqualTo(RETENCION);
        assertThat(recibidos).hasSize(2);
        Pedido cuenta = recibidos.get(0);
        assertThat(cuenta.metodo()).isEqualTo("GET");
        assertThat(cuenta.consulta()).isEqualTo("moneda=BOB");
        assertThat(cuenta.autorizacion()).isEqualTo("Bearer " + TOKEN_DEL_COMPRADOR);
        Pedido retencion = recibidos.get(1);
        assertThat(retencion.autorizacion()).isEqualTo("Bearer " + TOKEN_DEL_COMPRADOR);
        assertThat(UUID.fromString(retencion.clave())).isNotNull();
        assertThat(retencion.cuerpo())
                .contains("\"cuentaBilleteraId\":\"" + CUENTA + "\"")
                .contains("\"motivo\":\"COMPRA_EN_MERCADO\"")
                .contains("\"referenciaTipo\":\"OFERTA_MERCADO\"")
                .contains("\"referenciaId\":\"" + OFERTA + "\"")
                .contains("\"monto\":\"5500.00\"");
    }

    @Test
    @DisplayName(
            "CORRECTO · pagar al vendedor va con la credencial de SERVICIO, sin cuerpo: el libro deriva a quien y cuanto")
    void pagarCorrecto() {
        sesionDe(COMPRADOR);

        Referencia r = adaptador().pagarAlVendedor(RETENCION, VENDEDOR, bob("5450.00"), bob("50.00"), "cesion:1:pagar");

        assertThat(r.resultado()).isEqualTo(Resultado.OK);
        assertThat(r.referencia()).isEqualTo(TRANSACCION);
        Pedido pago = recibidos.get(0);
        assertThat(pago.ruta()).isEqualTo("/billetera/retenciones/" + RETENCION + "/pago-a-tercero");
        assertThat(pago.autorizacion())
                .as("la sesion de la persona NO puede pagar: el libro exige la identidad de servicio")
                .isEqualTo("Bearer " + TOKEN_DE_SERVICIO);
        assertThat(pago.cuerpo())
                .as("el beneficiario y el reparto no los dicta quien llama")
                .isEmpty();
    }

    @Test
    @DisplayName("CORRECTO · liberar cierra la retencion como LIBERADA con el token de la persona")
    void liberarCorrecto() {
        sesionDe(COMPRADOR);

        assertThat(adaptador().liberar(RETENCION, "cesion:1:liberar")).isEqualTo(Resultado.OK);

        Pedido cierre = recibidos.get(0);
        assertThat(cierre.ruta()).isEqualTo("/billetera/retenciones/" + RETENCION + "/cierre");
        assertThat(cierre.cuerpo()).contains("\"desenlace\":\"LIBERADA\"");
        assertThat(cierre.autorizacion()).isEqualTo("Bearer " + TOKEN_DEL_COMPRADOR);
    }

    // -------------------------------------------------------------------- limite

    @Test
    @DisplayName("LIMITE · la misma clave de la saga manda SIEMPRE la misma Idempotency-Key; otra clave, otra")
    void claveDeterminista() {
        sesionDe(COMPRADOR);
        var fondos = adaptador();

        fondos.pagarAlVendedor(RETENCION, VENDEDOR, bob("5450.00"), bob("50.00"), "cesion:7:pagar");
        fondos.pagarAlVendedor(RETENCION, VENDEDOR, bob("5450.00"), bob("50.00"), "cesion:7:pagar");
        fondos.pagarAlVendedor(RETENCION, VENDEDOR, bob("5450.00"), bob("50.00"), "cesion:8:pagar");

        assertThat(recibidos.get(0).clave()).isEqualTo(recibidos.get(1).clave());
        assertThat(recibidos.get(2).clave()).isNotEqualTo(recibidos.get(0).clave());
        assertThat(recibidos.get(0).clave())
                .isEqualTo(FondosPorHttp.llaveDe("cesion:7:pagar").toString());
    }

    @Test
    @DisplayName(
            "LIMITE · 422 del saldo CON lo disponible es SALDO_INSUFICIENTE; el mismo codigo sin detalle es no disponible")
    void saldoInsuficiente() {
        sesionDe(COMPRADOR);
        respuestas.put(
                "POST /billetera/retenciones",
                new Respuesta(
                        422,
                        "{\"codigo\":\"AP-CU13-01\",\"mensaje\":\"No hay disponible\",\"trazaId\":\"t\","
                                + "\"detalle\":{\"disponible\":\"10.00\",\"pedido\":\"5500.00\"}}"));
        assertThat(adaptador().retener(COMPRADOR, OFERTA, bob("5500.00"), "k1").resultado())
                .isEqualTo(Resultado.SALDO_INSUFICIENTE);

        respuestas.put(
                "POST /billetera/retenciones",
                new Respuesta(
                        422, "{\"codigo\":\"AP-CU13-01\",\"mensaje\":\"Esa billetera no es tuya\",\"trazaId\":\"t\"}"));
        var otraCuenta = adaptador().retener(COMPRADOR, OFERTA, bob("5500.00"), "k2");
        assertThat(otraCuenta.resultado()).isEqualTo(Resultado.NO_DISPONIBLE);
        assertThat(otraCuenta.referencia()).isNull();
    }

    @Test
    @DisplayName(
            "LIMITE · liberar una retencion que YA estaba liberada (reintento tras una caida) es OK; ya EJECUTADA, no")
    void liberarYaLiberada() {
        sesionDe(COMPRADOR);
        String cierre = "POST /billetera/retenciones/" + RETENCION + "/cierre";
        respuestas.put(
                cierre,
                new Respuesta(
                        422,
                        "{\"codigo\":\"AP-CU13-03\",\"mensaje\":\"Esa retencion ya esta LIBERADA: no admite mas operaciones.\","
                                + "\"trazaId\":\"t\",\"detalle\":{\"estado\":\"LIBERADA\"}}"));
        assertThat(adaptador().liberar(RETENCION, "cesion:1:liberar")).isEqualTo(Resultado.OK);

        respuestas.put(
                cierre,
                new Respuesta(
                        422,
                        "{\"codigo\":\"AP-CU13-03\",\"mensaje\":\"Esa retencion ya esta EJECUTADA: no admite mas operaciones.\","
                                + "\"trazaId\":\"t\",\"detalle\":{\"estado\":\"EJECUTADA\"}}"));
        assertThat(adaptador().liberar(RETENCION, "cesion:1:liberar")).isEqualTo(Resultado.NO_DISPONIBLE);
    }
}
