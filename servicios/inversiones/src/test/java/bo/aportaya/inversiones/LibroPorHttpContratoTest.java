package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular.LibroNoDisponible;
import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular.SaldoInsuficiente;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueba de contrato del lado CONSUMIDOR de {@code nucleo-financiero}, en los tres niveles:
 * correcto, limite e invalido. El productor real NO cumple todavia lo que este adaptador le
 * pide (motivo de retencion de inversion, debito y credito de inversion, saldo con titular y
 * lo comprometido en pozos): por eso es una prueba del consumidor contra un servidor HTTP
 * local que habla el contrato propuesto ({@code evidencia/carril-D/contrato-requerido-nucleo.md}).
 * La prueba del lado productor es del carril del nucleo y esta PENDIENTE.
 */
class LibroPorHttpContratoTest extends BaseDelLibroHttp {

    // ------------------------------------------------------------------ correcto
    @Test
    @DisplayName(
            "correcto · el saldo trae titular, disponible, retenido y lo comprometido en pozos, y el invertible los descuenta")
    void saldo() {
        UUID cuenta = UUID.randomUUID();
        UUID titular = UUID.randomUUID();
        responder(
                200,
                "{\"cuentaId\":\"" + cuenta + "\",\"titularId\":\"" + titular
                        + "\",\"disponible\":{\"monto\":\"5000.00\",\"moneda\":\"BOB\"},"
                        + "\"retenido\":{\"monto\":\"0.00\",\"moneda\":\"BOB\"},\"comprometidoEnPozos\":{\"monto\":\"4500.00\",\"moneda\":\"BOB\"}}");

        var s = libro.saldo(cuenta);

        assertThat(s.titularId()).isEqualTo(titular);
        assertThat(s.invertible().monto()).isEqualByComparingTo("500.00");
        assertThat(recibidas.get(0).ruta()).isEqualTo("/billetera/" + cuenta + "/saldo");
        assertThat(recibidas.get(0).autorizacion())
                .as("sin sesion no hay token que propagar")
                .isNull();
    }

    @Test
    @DisplayName(
            "correcto · retener manda motivo INVERSION_VOLUNTARIA, referencia de la orden y una clave de idempotencia UUID estable")
    void retener() {
        UUID cuenta = UUID.randomUUID();
        UUID orden = UUID.randomUUID();
        UUID retencion = UUID.randomUUID();
        responder(201, "{\"retencionId\":\"" + retencion + "\"}");

        UUID r1 = libro.retener(cuenta, bob("1000.00"), "RETENER:" + orden, orden);
        UUID r2 = libro.retener(cuenta, bob("1000.00"), "RETENER:" + orden, orden);

        assertThat(r1).isEqualTo(retencion).isEqualTo(r2);
        Peticion p = recibidas.get(0);
        assertThat(p.metodo()).isEqualTo("POST");
        assertThat(p.ruta()).isEqualTo("/billetera/retenciones");
        assertThat(p.cuerpo())
                .contains("\"motivo\":\"INVERSION_VOLUNTARIA\"")
                .contains("\"monto\":\"1000.00\"")
                .contains(orden.toString());
        // Limite: la misma instruccion lleva SIEMPRE la misma clave, y es un UUID (lo que exige el contrato del libro).
        assertThat(UUID.fromString(p.clave()))
                .isEqualTo(UUID.fromString(recibidas.get(1).clave()));
        assertThat(UUID.fromString(p.clave()))
                .isEqualTo(UUID.nameUUIDFromBytes(("RETENER:" + orden).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName(
            "correcto · liberar, debitar la inversion y acreditar el rescate pegan a las rutas del contrato propuesto")
    void liberarDebitarAcreditar() {
        UUID retencion = UUID.randomUUID();
        UUID transaccion = UUID.randomUUID();
        responder(200, "{\"transaccionId\":\"" + transaccion + "\",\"retencionId\":\"" + retencion + "\"}");

        var conCredencial = conCredencial(CREDENCIAL);
        conCredencial.liberar(retencion, "LIBERAR:x");
        assertThat(conCredencial.debitarInversion(retencion, bob("1000.00"), "DEBITAR:x"))
                .isEqualTo(transaccion);
        assertThat(conCredencial.acreditarRescate(UUID.randomUUID(), bob("407.20"), "ACREDITAR:x", UUID.randomUUID()))
                .isEqualTo(transaccion);

        assertThat(recibidas)
                .extracting(Peticion::ruta)
                .containsExactly(
                        "/billetera/retenciones/" + retencion + "/cierre",
                        "/billetera/inversiones/debitos",
                        "/billetera/inversiones/acreditaciones");
        assertThat(recibidas.get(0).cuerpo()).contains("\"desenlace\":\"LIBERADA\"");
    }

    // ------------------------------------------------------------------ invalido
    @Test
    @DisplayName(
            "invalido · 422 AP-CU13-01 es SALDO_INSUFICIENTE (definitivo); cualquier otro 422 es «no se sabe» y NO se interpreta como rechazo")
    void rechazos() {
        responder(422, "{\"codigo\":\"AP-CU13-01\",\"mensaje\":\"x\",\"trazaId\":\"t\"}");
        assertThatThrownBy(() -> libro.retener(UUID.randomUUID(), bob("1.00"), "k", UUID.randomUUID()))
                .isInstanceOf(SaldoInsuficiente.class);

        responder(422, "{\"codigo\":\"AP-CU13-03\",\"mensaje\":\"ya ejecutada\",\"trazaId\":\"t\"}");
        assertThatThrownBy(() -> libro.liberar(UUID.randomUUID(), "k")).isInstanceOf(LibroNoDisponible.class);
    }

    @Test
    @DisplayName("invalido · 5xx, cuerpo ilegible y un saldo sin titular o sin lo comprometido en pozos fallan CERRADO")
    void respuestasQueNoSirven() {
        responder(500, "{}");
        assertThatThrownBy(() -> libro.saldo(UUID.randomUUID())).isInstanceOf(LibroNoDisponible.class);
        responder(200, "esto no es json");
        assertThatThrownBy(() -> libro.saldo(UUID.randomUUID())).isInstanceOf(LibroNoDisponible.class);
        responder(
                200,
                "{\"disponible\":{\"monto\":\"5000.00\",\"moneda\":\"BOB\"},\"retenido\":{\"monto\":\"0.00\",\"moneda\":\"BOB\"}}");
        assertThatThrownBy(() -> libro.saldo(UUID.randomUUID()))
                .isInstanceOf(LibroNoDisponible.class)
                .hasMessageContaining("titular");
    }

    @Test
    @DisplayName(
            "invalido · si el libro tarda mas que el timeout, se corta y es «no disponible» (nunca un hilo colgado)")
    void timeout() {
        demoraMs = 1500;
        var impaciente = nuevo(Duration.ofMillis(300));
        long inicio = System.nanoTime();
        assertThatThrownBy(() -> impaciente.saldo(UUID.randomUUID())).isInstanceOf(LibroNoDisponible.class);
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofMillis(1400));
    }

    @Test
    @DisplayName(
            "invalido · el cortacircuitos se abre tras fallas repetidas y las siguientes llamadas ni salen (degradacion declarada)")
    void cortacircuitos() {
        responder(503, "{}");
        for (int i = 0; i < 40; i++) {
            try {
                libro.saldo(UUID.randomUUID());
            } catch (LibroNoDisponible esperada) {
                // cada falla cuenta para abrir el circuito
            }
        }
        int enviadas = recibidas.size();
        assertThatThrownBy(() -> libro.saldo(UUID.randomUUID()))
                .isInstanceOf(LibroNoDisponible.class)
                .hasMessageContaining("no disponible");
        assertThat(recibidas)
                .as("con el circuito abierto no se le pega al libro")
                .hasSize(enviadas);
        assertThat(enviadas).isLessThan(40);
    }

    @Test
    @DisplayName("limite · un 422 de saldo insuficiente NO abre el circuito: es una respuesta sana del libro")
    void rechazoDeNegocioNoAbreElCircuito() {
        responder(422, "{\"codigo\":\"AP-CU13-01\",\"mensaje\":\"x\",\"trazaId\":\"t\"}");
        Consumer<Integer> intentar =
                i -> assertThatThrownBy(() -> libro.retener(UUID.randomUUID(), bob("1.00"), "k" + i, UUID.randomUUID()))
                        .isInstanceOf(SaldoInsuficiente.class);
        for (int i = 0; i < 60; i++) {
            intentar.accept(i);
        }
        assertThat(recibidas).hasSize(60);
    }
}
