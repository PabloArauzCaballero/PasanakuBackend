package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.dominio.SelloDelDia;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F1.L1.M13 (ANF-DINERO-006) · CU-51 contra la base: cada saldo diario se sella con sus saldos, su moneda y
 * el sello del dia anterior, y la verificacion de la cadena senala el primer dia adulterado.
 */
class CU51SelloEncadenadoTest extends EscenarioDeCierreDiario {

    private String selloDe(UUID cuenta, LocalDate fecha, String columna) {
        var fila = dsl.fetchOne(
                """
                SELECT hash_registro, hash_anterior FROM nucleo_financiero.saldo_diario_billetera
                 WHERE cuenta_billetera_id = ? AND fecha = ?
                """,
                cuenta,
                fecha);
        return fila.get("hash_anterior".equals(columna) ? 1 : 0, String.class);
    }

    private java.util.Optional<LocalDate> ruptura(UUID cuenta, ContextoSesion ctx) {
        return transaccion.execute(t -> cierreDiarioCU.primeraRuptura(cuenta, ctx));
    }

    @Test
    @DisplayName(
            "cuadre: dos cierres seguidos quedan encadenados, el sello cubre el saldo y la cadena no tiene ruptura")
    void dosCierresEncadenados() {
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal("250.00"));
        ContextoSesion ctx = contextoDe(usuario);
        LocalDate ayer = diaDe(742);
        LocalDate hoy = diaDe(741);

        transaccion.execute(t -> cierreDiarioCU.ejecutar(entrada(ayer, 0, true), ctx));
        borrarCierres();
        transaccion.execute(t -> cierreDiarioCU.ejecutar(entrada(hoy, 0, true), ctx));

        String primero = selloDe(cuenta, ayer, "hash_registro");
        assertThat(primero)
                .isEqualTo(SelloDelDia.sellar(cuenta, ayer, "BOB", new BigDecimal("250.00"), BigDecimal.ZERO, null));
        assertThat(selloDe(cuenta, hoy, "hash_anterior")).isEqualTo(primero);
        assertThat(ruptura(cuenta, ctx)).isEmpty();
    }

    @Test
    @DisplayName("rechaza un saldo diario adulterado: la verificacion de la cadena senala ese dia")
    void rechazaUnEslabonAdulterado() {
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal("80.00"));
        ContextoSesion ctx = contextoDe(usuario);
        LocalDate primerDia = diaDe(752);
        LocalDate falso = diaDe(751);

        transaccion.execute(t -> cierreDiarioCU.ejecutar(entrada(primerDia, 0, true), ctx));
        String anterior = selloDe(cuenta, primerDia, "hash_registro");
        // Un dia insertado por fuera del cierre: dice 9999.00 y reusa un sello que no lo cubre.
        dsl.execute(
                """
                INSERT INTO nucleo_financiero.saldo_diario_billetera
                    (cuenta_billetera_id, fecha, saldo_disponible, saldo_retenido, hash_registro, hash_anterior, cerrado_en)
                VALUES (?, ?, 9999.00, 0.00, ?, ?, now())
                """,
                cuenta,
                falso,
                SelloDelDia.sellar(cuenta, falso, "BOB", new BigDecimal("80.00"), BigDecimal.ZERO, anterior),
                anterior);

        assertThat(ruptura(cuenta, ctx)).contains(falso);
    }
}
