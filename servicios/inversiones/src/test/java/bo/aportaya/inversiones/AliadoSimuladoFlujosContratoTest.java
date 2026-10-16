package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.dominio.DevengoDeDpf;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoRechazo;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.Estado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Los flujos con estado del aliado simulado REAL, vistos desde el adaptador: el interes de un
 * DPF calculado por el servidor Python contra el del servicio y contra el hecho a mano, el
 * rescate anticipado y el ciclo de un fondo (valor publicado, cuotas, rescate demorado,
 * doble disponibilidad).
 */
class AliadoSimuladoFlujosContratoTest extends ServidorDelAliadoSimulado {

    private final UUID titular = UUID.randomUUID();

    @Test
    @DisplayName(
            "DPF a vencimiento: el interes del servidor Python, el del servicio y el hecho a mano son el mismo (197,26 / 19,73 / 10177,53)")
    void interesDeDpfCoincideEntreImplementaciones() throws Exception {
        UUID ref = UUID.randomUUID();
        var suscripcion = aliado().suscribir(ref, "DPF-DEMO-180", new BigDecimal("10000.00"), titular);
        control("/control/reloj", "{\"segundos\":" + 180L * 86400 + "}");

        UUID rescate = UUID.randomUUID();
        var r = aliado().rescatar(rescate, suscripcion.posicionExterna().orElseThrow(), Optional.empty());

        assertThat(r.estado()).isEqualTo(Estado.CONFIRMADO);
        Map<String, String> d = r.detalle();
        BigDecimal propio = DevengoDeDpf.interes(new BigDecimal("10000.00"), new BigDecimal("0.040000"), 180, 365);
        assertThat(new BigDecimal(d.get("interesBruto")))
                .isEqualByComparingTo(propio)
                .isEqualByComparingTo("197.26");
        assertThat(new BigDecimal(d.get("retencion")))
                .isEqualByComparingTo(DevengoDeDpf.retencion(propio, new BigDecimal("0.100000")))
                .isEqualByComparingTo("19.73");
        assertThat(r.monto().orElseThrow()).isEqualByComparingTo("10177.53");
        assertThat(d.get("modo")).isEqualTo("VENCIMIENTO");
    }

    @Test
    @DisplayName(
            "Rescate anticipado de un DPF que no lo permite: el aliado lo rechaza (RESCATE_ANTICIPADO_NO_PERMITIDO)")
    void rescateAnticipadoNoPermitido() {
        var s = aliado().suscribir(UUID.randomUUID(), "DPF-DEMO-180", new BigDecimal("1000.00"), titular);
        assertThatThrownBy(() ->
                        aliado().rescatar(UUID.randomUUID(), s.posicionExterna().orElseThrow(), Optional.empty()))
                .isInstanceOfSatisfying(
                        AliadoRechazo.class, e -> assertThat(e.codigo()).isEqualTo("RESCATE_ANTICIPADO_NO_PERMITIDO"));
    }

    @Test
    @DisplayName(
            "Fondo de punta a punta: valor publicado, cuotas truncadas, rescate PENDIENTE, doble disponibilidad rechazada y confirmacion al pasar el tiempo")
    void fondoDePuntaAPunta() throws Exception {
        // El reloj del simulador arranca el lunes 12/10/2026 08:00 en La Paz (antes del corte de las 15:00).
        for (int i = 0; i <= 2; i++) {
            control(
                    "/control/valor-cuota",
                    "{\"producto\":\"FONDO-DEMO-ABIERTO\",\"fecha\":\""
                            + LocalDate.of(2026, 10, 12).plusDays(i) + "\",\"valor\":\"100.500000\"}");
        }
        var s = aliado().suscribir(UUID.randomUUID(), "FONDO-DEMO-ABIERTO", new BigDecimal("1000.00"), titular);
        assertThat(s.estado()).isEqualTo(Estado.CONFIRMADO);
        assertThat(s.cuotas().orElseThrow()).isEqualByComparingTo("9.950248"); // 1000 / 100,5 truncado a seis decimales
        assertThat(s.detalle()).containsEntry("valorCuota", "100.500000");

        UUID uno = UUID.randomUUID();
        var r = aliado().rescatar(uno, s.posicionExterna().orElseThrow(), Optional.of(new BigDecimal("7.000000")));
        assertThat(r.estado()).isEqualTo(Estado.PENDIENTE);
        assertThat(r.liquidaEn()).isPresent();
        assertThatThrownBy(() -> aliado().rescatar(
                                UUID.randomUUID(),
                                s.posicionExterna().orElseThrow(),
                                Optional.of(new BigDecimal("3.000000"))))
                .isInstanceOfSatisfying(
                        AliadoRechazo.class, e -> assertThat(e.codigo()).isEqualTo("DOBLE_DISPONIBILIDAD"));

        control(
                "/control/reloj",
                "{\"segundos\":" + (86400 + 3600) + "}"); // martes 09:00 La Paz: llega la hora de liquidacion
        var liquidado = aliado().consultar(uno).orElseThrow();
        assertThat(liquidado.estado()).isEqualTo(Estado.CONFIRMADO);
        assertThat(liquidado.monto().orElseThrow()).isEqualByComparingTo("703.50"); // 7 x 100,5
    }
}
