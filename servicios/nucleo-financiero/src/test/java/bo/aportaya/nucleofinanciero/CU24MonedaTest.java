package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU24RegistrarAsiento.EntradaAsiento;
import bo.aportaya.nucleofinanciero.dominio.OrigenAsiento;
import bo.aportaya.nucleofinanciero.dominio.Partida;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H2.S1.M1 · El mayor habla monedas: cada cuenta es de UNA, un movimiento es de la moneda de su cuenta y un asiento
 * no mezcla monedas (R-CTB-10, R-AUD-12). Sin tipo de cambio registrado no se suman BOB con USD (91.1.2).
 */
class CU24MonedaTest extends BaseDeCU24 {

    private record Cuenta(UUID id, String codigo) {}

    private Cuenta cuenta(String prefijo, String tipo, String naturaleza, String moneda) {
        String codigo = prefijo + "." + UUID.randomUUID().toString().substring(0, 8);
        return new Cuenta(fixtura.cuentaDeMovimiento(codigo, tipo, naturaleza, moneda), codigo);
    }

    private UUID asiento(Cuenta debe, Cuenta haber, String monto, Moneda moneda) {
        BigDecimal importe = new BigDecimal(monto);
        return transaccion
                .execute(e -> registrar.ejecutar(
                        dsl,
                        new EntradaAsiento(
                                OrigenAsiento.AJUSTE,
                                UUID.randomUUID(),
                                List.of(
                                        new Partida(debe.codigo(), importe, BigDecimal.ZERO, moneda),
                                        new Partida(haber.codigo(), BigDecimal.ZERO, importe, moneda)),
                                "prueba de moneda"),
                        comoSistema()))
                .asientoId();
    }

    private String monedaDe(UUID asientoId) {
        return dsl.fetch(
                        "SELECT DISTINCT moneda FROM nucleo_financiero.movimiento_contable WHERE asiento_id = ?",
                        asientoId)
                .getValues(0, String.class)
                .toString();
    }

    @Test
    @DisplayName("cuadre: un asiento en dolares cuadra en USD y el saldo de cada cuenta sale del mayor en su moneda")
    void asientoEnDolares() {
        Cuenta caja = cuenta("1.9", "ACTIVO", "DEUDORA", "USD");
        Cuenta ingreso = cuenta("4.9", "INGRESO", "ACREEDORA", "USD");

        UUID id = asiento(caja, ingreso, "250.50", Moneda.USD);

        assertThat(monedaDe(id)).isEqualTo("[USD]");
        assertThat(fixtura.saldoDe(caja.id())).isEqualByComparingTo("250.50");
        assertThat(fixtura.saldoDe(ingreso.id())).isEqualByComparingTo("250.50");
        var sumas = dsl.fetchOne(
                "SELECT SUM(debe), SUM(haber) FROM nucleo_financiero.movimiento_contable WHERE asiento_id = ?", id);
        assertThat((BigDecimal) sumas.get(0)).isEqualByComparingTo((BigDecimal) sumas.get(1));
        System.out.println(
                "ASIENTO | CU-24 USD | debe 1.9=" + sumas.get(0) + " haber 4.9=" + sumas.get(1) + " moneda=USD");
    }

    @Test
    @DisplayName("rechaza un asiento que mezcla BOB y USD antes de tocar la base, con AP-CU24-04")
    void mezclaDeMonedas() {
        Cuenta bob = cuenta("1.8", "ACTIVO", "DEUDORA", "BOB");
        Cuenta usd = cuenta("4.8", "INGRESO", "ACREEDORA", "USD");
        assertThatThrownBy(() -> transaccion.execute(e -> registrar.ejecutar(
                        dsl,
                        new EntradaAsiento(
                                OrigenAsiento.AJUSTE,
                                UUID.randomUUID(),
                                List.of(
                                        new Partida(bob.codigo(), new BigDecimal("10.00"), BigDecimal.ZERO, Moneda.BOB),
                                        new Partida(
                                                usd.codigo(), BigDecimal.ZERO, new BigDecimal("10.00"), Moneda.USD)),
                                "mezcla"),
                        comoSistema())))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU24-04"));
        assertThat(fixtura.saldoDe(bob.id())).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("rechaza una partida en USD contra una cuenta en BOB, con AP-CU24-04 (R-CTB-10)")
    void partidaDeOtraMonedaQueSuCuenta() {
        Cuenta enBob = cuenta("1.7", "ACTIVO", "DEUDORA", "BOB");
        Cuenta otraEnBob = cuenta("4.7", "INGRESO", "ACREEDORA", "BOB");
        assertThatThrownBy(() -> asiento(enBob, otraEnBob, "5.00", Moneda.USD))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU24-04"));
    }

    @Test
    @DisplayName(
            "Dado un asiento que mezcla BOB y USD · Cuando se intenta confirmar · Entonces la base de datos lo rechaza (R-AUD-12)")
    void criterio4() {
        Cuenta bob = cuenta("1.6", "ACTIVO", "DEUDORA", "BOB");
        Cuenta usd = cuenta("4.6", "INGRESO", "ACREEDORA", "USD");
        // 10 BOB al debe y 10 USD al haber: para R-AUD-05 (suma sin moneda) CUADRA; solo R-AUD-12 lo ve.
        String mensaje = rechazaAlConfirmar(bob, usd);
        assertThat(mensaje).contains("R-AUD-12");
    }

    private String rechazaAlConfirmar(Cuenta bob, Cuenta usd) {
        UUID asientoId = UUID.randomUUID();
        try {
            transaccion.execute(estado -> {
                dsl.execute(
                        """
                        INSERT INTO nucleo_financiero.asiento_contable (id, fecha, glosa, origen_tipo, origen_id, estado)
                        VALUES (?, now(), 'mezcla', 'AJUSTE', gen_random_uuid(), 'CONFIRMADO')
                        """,
                        asientoId);
                dsl.execute(
                        """
                        INSERT INTO nucleo_financiero.movimiento_contable (id, asiento_id, cuenta_id, debe, haber, moneda, descripcion)
                        VALUES (gen_random_uuid(), ?, ?, 10.00, 0.00, 'BOB', 'debe')
                        """,
                        asientoId,
                        bob.id());
                dsl.execute(
                        """
                        INSERT INTO nucleo_financiero.movimiento_contable (id, asiento_id, cuenta_id, debe, haber, moneda, descripcion)
                        VALUES (gen_random_uuid(), ?, ?, 0.00, 10.00, 'USD', 'haber')
                        """,
                        asientoId,
                        usd.id());
                // Los triggers de cuadre son DIFERIDOS: se adelantan para observarlos antes de revertir.
                dsl.execute("SET CONSTRAINTS ALL IMMEDIATE");
                estado.setRollbackOnly();
                return null;
            });
            return "";
        } catch (RuntimeException e) {
            return raizDe(e);
        }
    }

    @Test
    @DisplayName("rechaza la base: un movimiento en USD sobre una cuenta en BOB (R-CTB-10)")
    void movimientoDeOtraMonedaEnLaBase() {
        Cuenta bob = cuenta("1.5", "ACTIVO", "DEUDORA", "BOB");
        UUID asientoId = UUID.randomUUID();
        String mensaje = rechazaLaBase(
                """
                WITH a AS (INSERT INTO nucleo_financiero.asiento_contable (id, fecha, glosa, origen_tipo, origen_id, estado)
                           VALUES (?, now(), 'x', 'AJUSTE', gen_random_uuid(), 'BORRADOR') RETURNING id)
                INSERT INTO nucleo_financiero.movimiento_contable (id, asiento_id, cuenta_id, debe, haber, moneda, descripcion)
                SELECT gen_random_uuid(), a.id, ?, 5.00, 0.00, 'USD', 'x' FROM a
                """,
                asientoId,
                bob.id());
        assertThat(mensaje).contains("R-CTB-10");
    }

    @Test
    @DisplayName("rechaza la base: cambiar de moneda una cuenta que ya tiene movimientos (R-CTB-10)")
    void cuentaNoCambiaDeMoneda() {
        Cuenta caja = cuenta("1.4", "ACTIVO", "DEUDORA", "BOB");
        Cuenta ingreso = cuenta("4.4", "INGRESO", "ACREEDORA", "BOB");
        asiento(caja, ingreso, "7.00", Moneda.BOB);
        String mensaje =
                rechazaLaBase("UPDATE nucleo_financiero.cuenta_contable SET moneda = 'USD' WHERE id = ?", caja.id());
        assertThat(mensaje).contains("R-CTB-10");
        // Sin movimientos si puede cambiar: la regla protege el historico, no la cuenta recien creada.
        Cuenta nueva = cuenta("1.3", "ACTIVO", "DEUDORA", "BOB");
        assertThat(rechazaLaBase(
                        "UPDATE nucleo_financiero.cuenta_contable SET moneda = 'USD' WHERE id = ?", nueva.id()))
                .isEmpty();
    }

    @Test
    @DisplayName("compensa: la reversa de un asiento en dolares conserva la moneda y deja el saldo en cero")
    void reversaEnDolares() {
        Cuenta caja = cuenta("1.2", "ACTIVO", "DEUDORA", "USD");
        Cuenta ingreso = cuenta("4.2", "INGRESO", "ACREEDORA", "USD");
        UUID original = asiento(caja, ingreso, "99.99", Moneda.USD);

        var reversa = transaccion.execute(e -> registrar.reversar(dsl, original, "monto equivocado", comoSistema()));

        assertThat(reversa.totalDebe().moneda()).isEqualTo(Moneda.USD);
        assertThat(monedaDe(reversa.asientoId())).isEqualTo("[USD]");
        assertThat(fixtura.saldoDe(caja.id())).isEqualByComparingTo("0.00");
        assertThat(fixtura.saldoDe(ingreso.id())).isEqualByComparingTo("0.00");
        assertThat(dsl.fetchOne("SELECT estado FROM nucleo_financiero.asiento_contable WHERE id = ?", original)
                        .get(0, String.class))
                .as("el original queda CONFIRMADO: append-only")
                .isEqualTo("CONFIRMADO");
        System.out.println("ASIENTO | CU-24 reversa USD | original=" + original + " reversa=" + reversa.asientoId());
    }
}
