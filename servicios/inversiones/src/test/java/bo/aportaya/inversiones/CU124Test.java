package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.aplicacion.VistaRescate;
import bo.aportaya.inversiones.infraestructura.RescateRepositorio;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H10.S1.M6 · solicitar un rescate: informa el corte, queda pendiente hasta la confirmacion,
 * no hay rescate anticipado donde el producto no lo permite y se rechaza la doble disponibilidad.
 *
 * <p>PostgreSQL real; el aliado y el libro son los dobles declarados.
 */
class CU124Test extends BaseDeRescates {

    // ------------------------------------------------------------------ DPF
    @Test
    @DisplayName(
            "Dado un DPF que NO admite cancelacion anticipada · Cuando pide rescate antes del vencimiento · Entonces AP-CU124-01 y no queda ningun rescate")
    void dpfSinRescateAnticipado() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        AHORA.set(LUNES.plusSeconds(10L * 86400));

        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, null), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU124-01"));

        assertThat(contar(
                        "select count(*)::int from inversiones.rescate_inversion where posicion_inversion_id = ?",
                        posicion))
                .isZero();
        assertThat(contar(
                        "select count(*)::int from inversiones.posicion_inversion where id = ? and estado = 'ABIERTA'",
                        posicion))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un DPF que si admite la cancelacion · Cuando pide rescate anticipado · Entonces el tipo es ANTICIPADO y se liquida con penalizacion e impuesto del contrato")
    void dpfRescateAnticipadoPermitido() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF_ANTICIPABLE, "10000.00");
        AHORA.set(LUNES.plusSeconds(90L * 86400));
        // A mano: 10000 x 4,5 % x 90 / 360 = 112,50 ; penalizacion sintetica 50 % = 56,25 ; retencion 5,625 -> 5,62.
        aliado.liquidacionDpf("56.25", "5.62", 90);

        VistaRescate r = cu124.solicitar(pedir(posicion, null), ctx);

        assertThat(r.tipo()).isEqualTo("ANTICIPADO");
        assertThat(r.estado()).isEqualTo("LIQUIDADO");
        assertThat(r.netoAcreditar()).isEqualByComparingTo("10050.63");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("20050.63"); // 20000 - 10000 + 10050,63
    }

    @Test
    @DisplayName(
            "Dado un deposito ya rescatado · Cuando se pide otra vez con la misma clave o con otra · Entonces la misma clave devuelve el rescate original y otra clave se rechaza porque la posicion ya esta cerrada")
    void dpfSeRescataUnaVez() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        AHORA.set(LUNES.plusSeconds(180L * 86400));
        aliado.liquidacionDpf("197.26", "19.73", 180);
        var entrada = pedir(posicion, null);

        VistaRescate primero = cu124.solicitar(entrada, ctx);
        VistaRescate repetido = cu124.solicitar(entrada, ctx);

        assertThat(primero.tipo()).isEqualTo("VENCIMIENTO");
        assertThat(repetido.rescateId()).isEqualTo(primero.rescateId());
        assertThat(libro.veces("ACREDITAR:" + primero.rescateId()))
                .as("el credito se pidio al libro una sola vez")
                .isEqualTo(1);
        assertThat(libro.total(cuenta)).isEqualByComparingTo("20177.53");
        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, null), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU124-03"));
        assertThat(contar(
                        "select count(*)::int from inversiones.rescate_inversion where posicion_inversion_id = ?",
                        posicion))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza por R-INV-05: un DPF por cuotas y un segundo rescate vigente del mismo deposito")
    void dpfNoAceptaCuotas() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, "1.000000"), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU124-04"));

        // A nivel base: dos rescates VENCIMIENTO vigentes del mismo deposito no pueden coexistir.
        tx.en(() -> datos.conContexto(
                ctx,
                d -> rescates.crear(
                        d,
                        posicion,
                        usuario,
                        "VENCIMIENTO",
                        null,
                        UUID.randomUUID().toString(),
                        "h",
                        null,
                        java.time.OffsetDateTime.now())));
        assertThatThrownBy(() -> tx.en(() -> datos.conContexto(
                        ctx,
                        d -> rescates.crear(
                                d,
                                posicion,
                                usuario,
                                "VENCIMIENTO",
                                null,
                                UUID.randomUUID().toString(),
                                "h",
                                null,
                                java.time.OffsetDateTime.now()))))
                .isInstanceOf(RescateRepositorio.DobleDisponibilidad.class);
    }
}
