package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.CU13EjecutarConDestino.EntradaDebito;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-13 · lo que la BASE rechaza por su cuenta (R-BIL-24, R-BIL-25, R-BIL-26), sin pasar por la aplicacion. */
class CU13InversionBaseTest extends BaseDeInversiones {

    private UUID usuario;
    private UUID cuenta;
    private UUID orden;
    private UUID retencion;

    @BeforeEach
    void preparar() {
        usuario = fixtura.usuario();
        cuenta = fixtura.billetera(usuario, "ESTANDAR", new BigDecimal("1000.00"));
        sistemaFx.custodiaDeInversion("BOB");
        orden = UUID.randomUUID();
        retencion = retenerInversion(usuario, cuenta, "300.00", orden);
    }

    private void debitar() {
        transaccion.execute(t -> ejecucionCU.debitarInversion(
                new EntradaDebito("deb-u", retencion, Dinero.de("300.00", Moneda.BOB), Optional.of(usuario)),
                comoServicio("SERVICIO_INVERSIONES")));
    }

    @Test
    @DisplayName("rechaza la base: una segunda transaccion INVERSION para la misma orden (R-BIL-24)")
    void unicidadPorOrigenEnLaBase() {
        debitar();
        String mensaje = rechazaLaBase(
                """
                INSERT INTO nucleo_financiero.transaccion_billetera
                    (id, tipo, estado, moneda, monto_total, origen_tipo, origen_id, canal,
                     clave_idempotencia, hash_registro, ocurrida_en, registrada_en)
                VALUES (gen_random_uuid(), 'INVERSION', 'INICIADA', 'BOB', 1.00, 'ORDEN_INVERSION', '%s', 'API',
                        'otra-clave', repeat('b', 64), now(), now())
                """
                        .formatted(orden));
        assertThat(mensaje).contains("uq_tx_inversion_origen");
    }

    @Test
    @DisplayName(
            "rechaza la base: una retencion de inversion sin orden (R-BIL-26) y una segunda custodia en BOB (R-BIL-25)")
    void restriccionesNuevasEnLaBase() {
        String sinOrden = rechazaLaBase(
                """
                INSERT INTO nucleo_financiero.retencion_saldo
                    (id, cuenta_billetera_id, motivo, monto, estado, expira_en, creada_en)
                VALUES (gen_random_uuid(), '%s', 'INVERSION_VOLUNTARIA', 1.00, 'VIGENTE', now() + interval '1 day', now())
                """
                        .formatted(cuenta));
        assertThat(sinOrden).contains("ck_retencion_referencia_motivo");

        String segundaCustodia = rechazaLaBase(
                """
                INSERT INTO nucleo_financiero.cuenta_billetera
                    (id, numero_cuenta, tipo, moneda, estado, nivel_debida_diligencia, saldo_disponible,
                     saldo_retenido, permite_saldo_negativo, fecha_apertura, version)
                VALUES (gen_random_uuid(), 'SYS-DUP-%s', 'CUSTODIA_INVERSION', 'BOB', 'ACTIVA', 'SIMPLIFICADA', 0, 0,
                        false, now(), 0)
                """
                        .formatted(codigoCorto()));
        assertThat(segundaCustodia).contains("uq_cuenta_custodia_inversion_moneda");
    }
}
