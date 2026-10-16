package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia;
import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo.EntradaRetencion;
import bo.aportaya.nucleofinanciero.aplicacion.LecturaDeSaldo;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H2.S1.M5 · La proyeccion no es la fuente de verdad. {@code cuenta_billetera.saldo_*} es una cache derivada
 * del libro; si queda atrasada (o alguien la toca) una decision de dinero se toma igual contra el LIBRO.
 * La cache se corrompe a proposito, con el dueño de la base, y se observa el rechazo.
 */
class CU12ProyeccionTest extends BaseDeBilletera {

    private UUID usuario;
    private UUID origen;
    private UUID destino;
    private ContextoSesion ctx;

    @BeforeEach
    void preparar() {
        EscenarioDeRetiro.catalogo();
        usuario = fixtura.usuario();
        origen = fixtura.billetera(usuario, "ESTANDAR", new BigDecimal("100.00"));
        destino = fixtura.billetera(fixtura.usuario(), "ESTANDAR", BigDecimal.ZERO);
        ctx = contextoDe(usuario);
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private void cacheDice(String disponible) {
        dslFixtura.execute(
                "UPDATE nucleo_financiero.cuenta_billetera SET saldo_disponible = ?::numeric WHERE id = ?",
                disponible,
                origen);
    }

    private BigDecimal cache() {
        return dslFixtura
                .fetchOne("SELECT saldo_disponible FROM nucleo_financiero.cuenta_billetera WHERE id = ?", origen)
                .get(0, BigDecimal.class);
    }

    private BigDecimal libro() {
        return dslFixtura
                .fetchOne(
                        "SELECT COALESCE(SUM(CASE WHEN sentido='CREDITO' THEN monto ELSE -monto END),0) FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id = ?",
                        origen)
                .get(0, BigDecimal.class);
    }

    @Test
    @DisplayName(
            "Dada una billetera cuyo saldo en caché dice Bs 9.999 y cuyo libro dice Bs 100 · Cuando se intenta transferir Bs 500 · Entonces se rechaza con AP-CU12-01 porque manda el libro")
    void criterio4() {
        cacheDice("9999.00");
        assertThat(cache()).isEqualByComparingTo("9999.00");
        assertThat(libro()).isEqualByComparingTo("100.00");

        assertThatThrownBy(() -> transaccion.execute(t -> transferenciaCU.ejecutar(
                        new EntradaTransferencia(
                                "proy-1",
                                origen,
                                destino,
                                Dinero.de("500.00", Moneda.BOB),
                                "prueba",
                                Optional.empty(),
                                Optional.empty()),
                        ctx)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU12-01"));

        assertThat(libro()).as("el libro no se movio").isEqualByComparingTo("100.00");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id=?",
                        destino))
                .isZero();
        System.out.println("PROYECCION | cache=9999.00 libro=100.00 transferir 500.00 -> AP-CU12-01, libro intacto");
    }

    @Test
    @DisplayName("rechaza apartar un saldo que el libro no cubre aunque la cache lo muestre (AP-CU13-01)")
    void retencionContraElLibro() {
        cacheDice("9999.00");
        assertThatThrownBy(() -> transaccion.execute(t -> retencionCU.retener(
                        EntradaRetencion.simple(origen, Dinero.de("500.00", Moneda.BOB), "DISPUTA"), ctx)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU13-01"));
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE cuenta_billetera_id=?",
                        origen))
                .isZero();
    }

    @Test
    @DisplayName("cuadre: la consulta de saldo informa lo que dice el libro, no la cache atrasada")
    void consultaDelLibro() {
        cacheDice("9999.00");
        var lectura = new LecturaDeSaldo(new Datos(dsl), new CuentaBilleteraRepositorio(), Reloj.delSistema());
        var salida = transaccion.execute(t -> lectura.ejecutar(origen, ctx));
        assertThat(salida.disponible().toString()).isEqualTo("100.00");
        assertThat(salida.titularId()).isEqualTo(usuario);
    }

    @Test
    @DisplayName("cuadre: una cache atrasada hacia abajo no bloquea lo que el libro cubre, y la base la resincroniza")
    void cacheBajaNoBloquea() {
        cacheDice("0.00");
        transaccion.execute(t -> transferenciaCU.ejecutar(
                new EntradaTransferencia(
                        "proy-2",
                        origen,
                        destino,
                        Dinero.de("60.00", Moneda.BOB),
                        "prueba",
                        Optional.empty(),
                        Optional.empty()),
                ctx));
        assertThat(libro()).isEqualByComparingTo("40.00");
        assertThat(cache()).as("el trigger recalculo la cache desde el libro").isEqualByComparingTo("40.00");
    }
}
