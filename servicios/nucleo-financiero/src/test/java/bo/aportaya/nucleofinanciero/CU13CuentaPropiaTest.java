package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CuentaPropia;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril G · la billetera operativa de la SESION, contra la base real (la fila de otra persona no se ve).
 *
 * <p>El adaptador de {@code entregas} retiene el saldo de quien compra un turno y necesita su
 * {@code cuentaBilleteraId}: lo pregunta aca con el token de esa persona.
 */
class CU13CuentaPropiaTest extends BaseDeBilletera {

    private static CuentaPropia cuentaPropia;

    @BeforeAll
    static void armar() {
        cuentaPropia = new CuentaPropia(new Datos(dsl), new CuentaBilleteraRepositorio());
    }

    private UUID deLaSesion(UUID usuario, Moneda moneda) {
        return transaccion.execute(t -> cuentaPropia.deLaSesion(moneda, contextoDe(usuario)));
    }

    private static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    @Test
    @DisplayName(
            "Dado una persona con una billetera BOB activa · Cuando pregunta por su billetera propia · Entonces recibe la suya y no la de otra persona")
    void devuelveLaPropia() {
        UUID ana = fixtura.usuario();
        UUID beto = fixtura.usuario();
        UUID cuentaAna = fixtura.billetera(ana, "ESTANDAR", new BigDecimal("10.00"));
        UUID cuentaBeto = fixtura.billetera(beto, "ESTANDAR", BigDecimal.ZERO);

        assertThat(deLaSesion(ana, Moneda.BOB)).isEqualTo(cuentaAna).isNotEqualTo(cuentaBeto);
        assertThat(deLaSesion(beto, Moneda.BOB)).isEqualTo(cuentaBeto);
    }

    @Test
    @DisplayName("rechaza: sin billetera en esa moneda no hay cuenta que devolver (AP-CU13-01)")
    void sinBilleteraEnEsaMoneda() {
        UUID ana = fixtura.usuario();
        fixtura.billetera(ana, "ESTANDAR", BigDecimal.ZERO);

        assertThatThrownBy(() -> deLaSesion(ana, Moneda.USD))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-01"));
        assertThatThrownBy(() -> deLaSesion(fixtura.usuario(), Moneda.BOB))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-01"));
    }

    @Test
    @DisplayName("rechaza: una billetera que no esta ACTIVA no es operativa y no se ofrece para retener")
    void cuentaNoActiva() {
        UUID ana = fixtura.usuario();
        UUID cuenta = fixtura.billetera(ana, "ESTANDAR", BigDecimal.ZERO);
        dslFixtura.execute("UPDATE nucleo_financiero.cuenta_billetera SET estado = 'LIMITADA' WHERE id = ?", cuenta);

        assertThatThrownBy(() -> deLaSesion(ana, Moneda.BOB))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-01"));
    }
}
