package bo.aportaya.entregas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.entregas.dominio.puertos.FondosDelComprador.Resultado;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link FondosPorHttp} · INVALIDO: sin sesion, sin credencial, respuestas que no son el contrato y el libro caido. En todos
 * los casos el resultado es NO_DISPONIBLE y la compra queda detenida y reanudable (falla cerrado).
 */
class FondosPorHttpInvalidoTest extends BaseDeFondosPorHttp {

    // ------------------------------------------------------------------ invalido

    @Test
    @DisplayName("INVALIDO · sin sesion, o con la sesion de OTRA persona, no sale ninguna peticion al libro")
    void sinSesionDelComprador() {
        assertThat(adaptador().retener(COMPRADOR, OFERTA, bob("5500.00"), "k").resultado())
                .isEqualTo(Resultado.NO_DISPONIBLE);

        sesionDe(OTRA_PERSONA);
        assertThat(adaptador().retener(COMPRADOR, OFERTA, bob("5500.00"), "k").resultado())
                .isEqualTo(Resultado.NO_DISPONIBLE);

        assertThat(recibidos).as("no se retiene bajo una identidad ajena").isEmpty();
    }

    @Test
    @DisplayName("INVALIDO · sin credencial de servicio no se paga: falla cerrado y no sale ninguna peticion")
    void sinCredencialDeServicio() {
        sesionDe(COMPRADOR);

        for (CredencialDeServicio sin : List.of(CredencialDeServicio.ninguna(), CredencialDeServicio.fija("  "))) {
            assertThat(adaptador(sin)
                            .pagarAlVendedor(RETENCION, VENDEDOR, bob("5450.00"), bob("50.00"), "k")
                            .resultado())
                    .isEqualTo(Resultado.NO_DISPONIBLE);
        }
        assertThat(recibidos).isEmpty();
    }

    @Test
    @DisplayName("INVALIDO · 401, 403, 404, 422 de negocio, 500 y cuerpo ilegible en cada operacion son NO_DISPONIBLE")
    void respuestasQueNoSonElContrato() {
        sesionDe(COMPRADOR);
        String pago = "POST /billetera/retenciones/" + RETENCION + "/pago-a-tercero";
        String cierre = "POST /billetera/retenciones/" + RETENCION + "/cierre";
        for (var r : List.of(
                new Respuesta(401, "{}"),
                new Respuesta(403, "{\"codigo\":\"AP-SEG-01\"}"),
                new Respuesta(404, "{}"),
                new Respuesta(422, "{\"codigo\":\"AP-CU13-03\",\"mensaje\":\"ya ejecutada\"}"),
                new Respuesta(500, "{\"codigo\":\"AP-SYS-01\"}"),
                new Respuesta(200, "esto no es json"),
                new Respuesta(200, "{}"))) {
            respuestas.put(pago, r);
            respuestas.put(cierre, r);
            respuestas.put("POST /billetera/retenciones", r);
            var fondos = adaptador();

            assertThat(fondos.pagarAlVendedor(RETENCION, VENDEDOR, bob("5450.00"), bob("50.00"), "kp")
                            .resultado())
                    .as("pagar con %s", r)
                    .isEqualTo(Resultado.NO_DISPONIBLE);
            assertThat(fondos.liberar(RETENCION, "kl")).as("liberar con %s", r).isEqualTo(Resultado.NO_DISPONIBLE);
            assertThat(fondos.retener(COMPRADOR, OFERTA, bob("5500.00"), "kr").resultado())
                    .as("retener con %s", r)
                    .isEqualTo(Resultado.NO_DISPONIBLE);
        }
    }

    @Test
    @DisplayName("INVALIDO · con el libro caido las tres operaciones responden NO_DISPONIBLE y ninguna lanza")
    void libroCaido() {
        sesionDe(COMPRADOR);
        var fondos = adaptador();
        servidor.stop(0);

        assertThat(fondos.retener(COMPRADOR, OFERTA, bob("5500.00"), "k").resultado())
                .isEqualTo(Resultado.NO_DISPONIBLE);
        assertThat(fondos.pagarAlVendedor(RETENCION, VENDEDOR, bob("5450.00"), bob("50.00"), "k")
                        .resultado())
                .isEqualTo(Resultado.NO_DISPONIBLE);
        assertThat(fondos.liberar(RETENCION, "k")).isEqualTo(Resultado.NO_DISPONIBLE);
    }
}
