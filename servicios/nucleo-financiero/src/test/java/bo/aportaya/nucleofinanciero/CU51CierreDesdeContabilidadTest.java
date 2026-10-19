package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU51EjecutarCierreDiario;
import bo.aportaya.nucleofinanciero.aplicacion.CU51EjecutarCierreDiario.SalidaCierre;
import bo.aportaya.nucleofinanciero.aplicacion.CierreDiarioDesdeContabilidad;
import bo.aportaya.nucleofinanciero.dominio.puertos.RecaudoDelDia;
import bo.aportaya.nucleofinanciero.infraestructura.CierreDiarioRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * CU-51 desde el backoffice (carril A · H8): Contabilidad cierra el dia con el recaudo que informa
 * {@code aportes}, contra la base real.
 *
 * <p>{@code aportes} va DOBLADO (regla 65) detras del puerto {@link RecaudoDelDia}: el doble cumple el
 * contrato {@code GET /aportes/recaudo-diario} y se ejercita en sus tres niveles. Lo que queda verificado
 * contra el doble y no contra {@code aportes} real esta declarado en el reporte del carril.
 */
class CU51CierreDesdeContabilidadTest extends EscenarioDeCierreDiario {

    private static ContextoSesion contabilidad() {
        return ContextoSesion.de(
                fixtura.usuario(), "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    /** El doble de aportes: devuelve lo que se le diga, y cuenta cuantas veces se le pregunto. */
    private static final class AportesDoble implements RecaudoDelDia {
        private final Optional<Resumen> respuesta;
        int preguntas;

        AportesDoble(Optional<Resumen> respuesta) {
            this.respuesta = respuesta;
        }

        @Override
        public Optional<Resumen> consultar(LocalDate fecha) {
            // Invariante 6: la pregunta de red a aportes va FUERA de la transaccion del cierre.
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .isFalse();
            preguntas++;
            return respuesta.map(r -> new Resumen(fecha, r.porMoneda(), r.excepcionesAbiertas()));
        }
    }

    private static Optional<RecaudoDelDia.Resumen> enBob(String recaudado, String conciliado, int excepciones) {
        return Optional.of(new RecaudoDelDia.Resumen(
                null,
                List.of(new RecaudoDelDia.PorMoneda(
                        Moneda.BOB, new BigDecimal(recaudado), new BigDecimal(conciliado), BigDecimal.ZERO, 3)),
                excepciones));
    }

    /**
     * En produccion el proxy de Spring abre la transaccion de {@code ejecutarConRecaudo}; aca el caso de uso
     * se arma con {@code new}, asi que esta subclase hace lo mismo que el proxy y nada mas.
     */
    private static CierreDiarioDesdeContabilidad con(AportesDoble aportes) {
        var cu51 =
                new CU51EjecutarCierreDiario(
                        new Datos(dsl),
                        new CierreDiarioRepositorio(),
                        conciliaciones,
                        new CuentaBilleteraRepositorio(),
                        new Outbox("nucleo_financiero"),
                        Reloj.delSistema()) {
                    @Override
                    public SalidaCierre ejecutarConRecaudo(
                            LocalDate fecha,
                            BigDecimal recaudado,
                            BigDecimal conciliado,
                            BigDecimal excepciones,
                            int cantidadPagos,
                            int excepcionesAbiertas,
                            ContextoSesion ctx) {
                        return transaccion.execute(t -> super.ejecutarConRecaudo(
                                fecha, recaudado, conciliado, excepciones, cantidadPagos, excepcionesAbiertas, ctx));
                    }
                };
        return new CierreDiarioDesdeContabilidad(aportes, cu51, Reloj.delSistema());
    }

    /** Una conciliacion de custodia del dia, como la deja CU-50 (fixture sintetica). */
    private void custodiaDelDia(LocalDate fecha, String estado) {
        UUID cuenta = custodia.cuentaDeCustodia();
        dsl.execute(
                """
                INSERT INTO nucleo_financiero.conciliacion_custodia
                    (id, cuenta_custodia_id, fecha, saldo_dinero_electronico, saldo_custodia,
                     saldo_en_transito, cumple_encaje, estado, ejecutada_en)
                VALUES (gen_random_uuid(), ?, ?, 1000.00, 1000.00, 0, ?, ?, now())
                """,
                cuenta,
                fecha,
                "CUADRADA".equals(estado),
                estado);
    }

    private static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    @Test
    @DisplayName(
            "Dado un día terminado con la custodia cuadrada y aportes que informa Bs 1.000 recaudados sin excepciones · Cuando Contabilidad lo cierra desde el backoffice · Entonces el cierre queda cuadrado con los totales de aportes, no con cifras del cliente")
    void cierraConLasCifrasDeAportes() {
        LocalDate fecha = diaDe(301);
        custodiaDelDia(fecha, "CUADRADA");

        var salida = con(new AportesDoble(enBob("1000.00", "1000.00", 0))).cerrar(fecha, contabilidad());

        assertThat(salida.cuadrado()).isTrue();
        assertThat(salida.yaExistia()).isFalse();
        var fila = dsl.fetchOne(
                "SELECT total_recaudado, total_conciliado, cantidad_pagos FROM nucleo_financiero.cierre_diario WHERE fecha = ?",
                fecha);
        assertThat(fila.get(0, BigDecimal.class)).isEqualByComparingTo("1000.00");
        assertThat(fila.get(1, BigDecimal.class)).isEqualByComparingTo("1000.00");
        assertThat(fila.get(2, Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName(
            "Dado un día sin conciliación de custodia · Cuando Contabilidad lo cierra · Entonces el cierre queda NO cuadrado: lo que no se verificó no cuadra")
    void sinConciliacionNoCuadra() {
        LocalDate fecha = diaDe(302);

        var salida = con(new AportesDoble(enBob("1000.00", "1000.00", 0))).cerrar(fecha, contabilidad());

        assertThat(salida.cuadrado()).isFalse();
    }

    @Test
    @DisplayName("reintento: cerrar dos veces el mismo dia devuelve el mismo cierre y escribe una sola fila")
    void reintento() {
        LocalDate fecha = diaDe(303);
        custodiaDelDia(fecha, "CUADRADA");
        var aportes = new AportesDoble(enBob("10.00", "10.00", 0));

        var primera = con(aportes).cerrar(fecha, contabilidad());
        var segunda = con(aportes).cerrar(fecha, contabilidad());

        assertThat(segunda.cierreId()).isEqualTo(primera.cierreId());
        assertThat(segunda.yaExistia()).isTrue();
        assertThat(contar("SELECT count(*)::int FROM nucleo_financiero.cierre_diario WHERE fecha = ?", fecha))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("cuadre: un dia sin pagos cierra con totales en cero y sin suponer moneda")
    void diaSinPagos() {
        LocalDate fecha = diaDe(304);
        custodiaDelDia(fecha, "CUADRADA");

        var salida = con(new AportesDoble(Optional.of(new RecaudoDelDia.Resumen(null, List.of(), 0))))
                .cerrar(fecha, contabilidad());

        assertThat(salida.cuadrado()).isTrue();
        assertThat(dsl.fetchOne("SELECT total_recaudado FROM nucleo_financiero.cierre_diario WHERE fecha = ?", fecha)
                        .get(0, BigDecimal.class))
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("rechaza cerrar el dia de hoy con AP-CU51-06 sin preguntarle nada a aportes")
    void rechazaDiaEnCurso() {
        var aportes = new AportesDoble(enBob("1.00", "1.00", 0));
        assertThatThrownBy(() -> con(aportes).cerrar(Reloj.delSistema().hoy(), contabilidad()))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU51-06"));
        assertThat(aportes.preguntas).isZero();
    }

    @Test
    @DisplayName("rechaza con AP-CU51-08 cuando aportes no informa el recaudo, y no escribe ningun cierre")
    void rechazaSinRecaudo() {
        LocalDate fecha = diaDe(305);
        assertThatThrownBy(() -> con(new AportesDoble(Optional.empty())).cerrar(fecha, contabilidad()))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU51-08"));
        assertThat(contar("SELECT count(*)::int FROM nucleo_financiero.cierre_diario WHERE fecha = ?", fecha))
                .isZero();
    }

    @Test
    @DisplayName("rechaza con AP-CU51-07 un dia con recaudo en dos monedas: el cierre no las suma")
    void rechazaMonedasMezcladas() {
        LocalDate fecha = diaDe(306);
        var dos = Optional.of(new RecaudoDelDia.Resumen(
                null,
                List.of(
                        new RecaudoDelDia.PorMoneda(Moneda.BOB, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, 1),
                        new RecaudoDelDia.PorMoneda(Moneda.USD, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, 1)),
                0));
        assertThatThrownBy(() -> con(new AportesDoble(dos)).cerrar(fecha, contabilidad()))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU51-07"));
        assertThat(contar("SELECT count(*)::int FROM nucleo_financiero.cierre_diario WHERE fecha = ?", fecha))
                .isZero();
    }

    @Test
    @DisplayName("cuadre: con excepciones abiertas segun aportes el dia cierra NO cuadrado aunque la custodia cuadre")
    void excepcionesAbiertas() {
        LocalDate fecha = diaDe(307);
        custodiaDelDia(fecha, "CUADRADA");

        var salida = con(new AportesDoble(enBob("1000.00", "980.00", 1))).cerrar(fecha, contabilidad());

        assertThat(salida.cuadrado()).isFalse();
        assertThat(salida.excepcionesAbiertas()).isEqualTo(1);
    }
}
