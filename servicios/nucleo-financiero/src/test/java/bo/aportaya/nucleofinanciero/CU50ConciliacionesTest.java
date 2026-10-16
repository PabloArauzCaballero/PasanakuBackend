package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU50ConciliarCustodia.SalidaConciliacion;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarTesoreria;
import bo.aportaya.nucleofinanciero.infraestructura.TesoreriaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-50 desde el backoffice (carril A · H8): Tesoreria concilia y Auditoria lista, contra la base real.
 *
 * <p>Tres niveles: lo correcto (el saldo emitido lo calcula el servidor; la pagina con cursor), el
 * limite (reintento con transito distinto de cero devuelve la MISMA respuesta) y lo invalido (otros
 * saldos el mismo dia, otra moneda, custodia inexistente, dia sin cierres, cursor a medias).
 */
class CU50ConciliacionesTest extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";

    /** Se arma por prueba: {@code dsl} lo deja listo el {@code @BeforeAll} de la base. */
    private static ConsultarTesoreria consultas() {
        return new ConsultarTesoreria(new Datos(dsl), new TesoreriaRepositorio());
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static LocalDate hoy() {
        return OffsetDateTime.now(ZoneOffset.UTC).toLocalDate();
    }

    /** El operador de Tesoreria: rol BACKOFFICE en la sesion, como lo deja el filtro de seguridad. */
    private static ContextoSesion operador(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    /** Una custodia BOB y los saldos diarios sellados de {@code fechas}: sin ellos no hay que conciliar. */
    private UUID custodiaConCierres(LocalDate... fechas) {
        fixtura.tipoDeCambioDeHoy();
        UUID cuenta = fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal("1000.00"));
        for (LocalDate f : fechas) {
            fixtura.cierreDelDia(cuenta, f, new BigDecimal("1000.00"), 1);
        }
        return custodia.cuentaDeCustodia();
    }

    private SalidaConciliacion conciliar(UUID cc, LocalDate fecha, String banco, String transito, ContextoSesion ctx) {
        return transaccion.execute(
                t -> conciliacionCU.conciliarDesdeTesoreria(cc, fecha, bob(banco), bob(transito), ctx));
    }

    private static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    @Test
    @DisplayName(
            "Dada una custodia en BOB con cierres del día · Cuando Tesorería concilia desde el backoffice con el saldo del extracto y el tránsito · Entonces el saldo de dinero electrónico registrado es el que calcula el servidor, no uno enviado por el cliente")
    void elServidorCalculaElEmitido() {
        UUID cc = custodiaConCierres(hoy());
        ContextoSesion ctx = operador(fixtura.usuario());

        // Lo que el servidor debe sumar, medido aparte y en la misma transaccion que la conciliacion.
        var medido = transaccion.execute(t -> {
            BigDecimal esperado = dsl.fetchOne(
                            """
                            SELECT COALESCE(sum(saldo_total), 0) FROM nucleo_financiero.cuenta_billetera
                             WHERE tipo IN ('USUARIO', 'GRUPO', 'FONDO_GARANTIA') AND moneda = 'BOB'
                            """)
                    .get(0, BigDecimal.class);
            var salida = conciliacionCU.conciliarDesdeTesoreria(cc, hoy(), bob("1000.00"), bob("25.00"), ctx);
            return new Object[] {esperado, salida};
        });
        BigDecimal esperado = (BigDecimal) medido[0];
        SalidaConciliacion salida = (SalidaConciliacion) medido[1];

        BigDecimal registrado = dsl.fetchOne(
                        "SELECT saldo_dinero_electronico FROM nucleo_financiero.conciliacion_custodia WHERE id = ?",
                        salida.conciliacionId())
                .get(0, BigDecimal.class);
        assertThat(registrado).isEqualByComparingTo(esperado);
        // diferencia = custodia + transito - emitido, sobre el emitido del servidor.
        assertThat(salida.diferencia().monto()).isEqualByComparingTo(new BigDecimal("1025.00").subtract(esperado));
        assertThat(salida.estado()).isIn("CUADRADA", "DESCUADRADA");
    }

    @Test
    @DisplayName(
            "reintento: los mismos saldos del mismo operador devuelven la misma conciliacion, con transito incluido")
    void reintentoConTransito() {
        UUID cc = custodiaConCierres(hoy());
        ContextoSesion ctx = operador(fixtura.usuario());

        SalidaConciliacion primera = conciliar(cc, hoy(), "1000.00", "25.00", ctx);
        SalidaConciliacion segunda = conciliar(cc, hoy(), "1000.00", "25.00", ctx);

        assertThat(segunda.conciliacionId()).isEqualTo(primera.conciliacionId());
        // Limite: la columna generada de la tabla no suma el transito; el reintento no puede leerla.
        assertThat(segunda.diferencia().monto())
                .isEqualByComparingTo(primera.diferencia().monto());
        assertThat(segunda.ratioCobertura()).isEqualByComparingTo(primera.ratioCobertura());
        assertThat(segunda.cumpleEncaje()).isEqualTo(primera.cumpleEncaje());
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.conciliacion_custodia WHERE cuenta_custodia_id = ?",
                        cc))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza otros saldos para el mismo dia con AP-CU50-03 y no escribe una segunda verdad")
    void rechazaOtrosSaldos() {
        UUID cc = custodiaConCierres(hoy());
        ContextoSesion ctx = operador(fixtura.usuario());
        conciliar(cc, hoy(), "1000.00", "0.00", ctx);

        assertThatThrownBy(() -> conciliar(cc, hoy(), "999.00", "0.00", ctx))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU50-03"));
        // Otro operador con los MISMOS saldos tampoco es un reintento: es una segunda conciliacion.
        assertThatThrownBy(() -> conciliar(cc, hoy(), "1000.00", "0.00", operador(fixtura.usuario())))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU50-03"));
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.conciliacion_custodia WHERE cuenta_custodia_id = ?",
                        cc))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza saldos en otra moneda que la de la custodia con AP-CU50-05")
    void rechazaOtraMoneda() {
        UUID cc = custodiaConCierres(hoy());
        ContextoSesion ctx = operador(fixtura.usuario());

        assertThatThrownBy(() -> transaccion.execute(t -> conciliacionCU.conciliarDesdeTesoreria(
                        cc, hoy(), Dinero.de("1000.00", Moneda.USD), bob("0.00"), ctx)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU50-05"));
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.conciliacion_custodia WHERE cuenta_custodia_id = ?",
                        cc))
                .isZero();
    }

    @Test
    @DisplayName("rechaza una cuenta de custodia que no existe con AP-CU50-06")
    void rechazaCustodiaInexistente() {
        custodiaConCierres(hoy());
        assertThatThrownBy(() -> conciliar(UUID.randomUUID(), hoy(), "1.00", "0.00", operador(fixtura.usuario())))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU50-06"));
    }

    @Test
    @DisplayName("rechaza un dia sin saldos diarios sellados con AP-CU50-01")
    void rechazaDiaSinCierres() {
        UUID cc = custodiaConCierres(hoy());
        LocalDate sinCierres = LocalDate.of(2001, 1, 1);
        assertThatThrownBy(() -> conciliar(cc, sinCierres, "1.00", "0.00", operador(fixtura.usuario())))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU50-01"));
    }

    @Test
    @DisplayName(
            "Dadas dos conciliaciones en días distintos · Cuando Auditoría lista las conciliaciones de a una · Entonces recibe la más reciente primero y un cursor que lleva a la anterior")
    void listaConCursor() {
        LocalDate ayer = hoy().minusDays(1);
        UUID cc = custodiaConCierres(hoy(), ayer);
        ContextoSesion ctx = operador(fixtura.usuario());
        UUID deAyer = conciliar(cc, ayer, "1000.00", "0.00", ctx).conciliacionId();
        UUID deHoy = conciliar(cc, hoy(), "1000.00", "0.00", ctx).conciliacionId();

        var primera = transaccion.execute(t -> consultas().conciliaciones(Optional.empty(), Optional.empty(), 1, ctx));
        assertThat(primera.items()).singleElement().satisfies(c -> {
            assertThat(c.conciliacionId()).isEqualTo(deHoy);
            assertThat(c.saldoCustodia().moneda()).isEqualTo(Moneda.BOB);
            assertThat(c.descuadresAbiertos()).isZero();
            assertThat(c.ejecutadaPor()).contains(ctx.usuarioId());
        });
        assertThat(primera.proximoAntesDeId()).contains(deHoy);

        var segunda = transaccion.execute(
                t -> consultas().conciliaciones(primera.proximoAntesDe(), primera.proximoAntesDeId(), 1, ctx));
        assertThat(segunda.items()).singleElement().satisfies(c -> assertThat(c.conciliacionId())
                .isEqualTo(deAyer));

        var tercera = transaccion.execute(
                t -> consultas().conciliaciones(segunda.proximoAntesDe(), segunda.proximoAntesDeId(), 1, ctx));
        assertThat(tercera.items()).isEmpty();
        assertThat(tercera.proximoAntesDeId()).isEmpty();
    }

    @Test
    @DisplayName("rechaza un cursor a medias en el listado de conciliaciones con AP-CU50-04")
    void rechazaCursorAMedias() {
        ContextoSesion ctx = operador(fixtura.usuario());
        assertThatThrownBy(() -> transaccion.execute(
                        t -> consultas().conciliaciones(Optional.of(hoy()), Optional.empty(), 10, ctx)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU50-04"));
    }
}
