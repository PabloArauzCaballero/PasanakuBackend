package bo.aportaya.garantia;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.garantia.aplicacion.CU23CubrirIncumplimiento.EntradaCobertura;
import bo.aportaya.garantia.aplicacion.CU25DeclararIncumplimiento.EntradaDeclaracion;
import bo.aportaya.garantia.aplicacion.IndicadoresDeGarantia;
import bo.aportaya.garantia.infraestructura.IndicadoresDeGarantiaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tableros · H4.S1.M2 — impagos cubiertos, recuperado, recuperacion acumulada y cobertura del fondo. El contenedor se
 * comparte: se mide el DELTA de cada prueba sobre el dia de hoy.
 */
class CUIndicadoresDeGarantiaTest extends BaseDeGarantia {

    private final IndicadoresDeGarantiaRepositorio repositorio = new IndicadoresDeGarantiaRepositorio();
    private final IndicadoresDeGarantia calculador = new IndicadoresDeGarantia(repositorio);

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private static LocalDate hoy() {
        return LocalDate.now(ZoneId.of("America/La_Paz"));
    }

    private static ContextoSesion sistema() {
        return ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
    }

    private IndicadoresDeGarantiaRepositorio.Suma cubiertoHoy() {
        return transaccion.execute(
                t -> new Datos(dsl).conContexto(sistema(), d -> repositorio.cubierto(d, hoy(), hoy())));
    }

    private List<bo.aportaya.plataforma.mensajeria.CalculadorDeIndicadores.Calculado> calcular(String periodo) {
        return transaccion.execute(
                t -> new Datos(dsl).conContexto(sistema(), d -> calculador.calcular(d, PeriodoDeTablero.de(periodo))));
    }

    /** Un expediente de Bs 500 y un fondo con saldo, con la politica que dice cuando activar la cobertura. */
    private void cubrir(String saldo, int diasMoraParaActivar) {
        UUID usuario = fixtura.usuario();
        var escenario = fixtura.escenario(usuario);
        ContextoSesion gestor = contextoDe(fixtura.usuario());
        UUID politica = fixtura.politica(
                escenario.grupoId(), "100.00", "5000.00", "5000.00", 3, "100000.00", diasMoraParaActivar);
        UUID fondo = fixtura.fondo(escenario.grupoId(), politica, saldo);
        fixtura.aportarAlFondo(fondo, escenario.participanteId(), saldo, saldo);
        var expediente = transaccion.execute(t -> expedienteCU.declarar(
                new EntradaDeclaracion(
                        "EXP-" + UUID.randomUUID().toString().substring(0, 8),
                        usuario,
                        escenario.participanteId(),
                        escenario.grupoId(),
                        escenario.periodoId(),
                        escenario.cupoId(),
                        escenario.obligacionId(),
                        "APORTE_IMPAGO",
                        "GRAVE",
                        "AUTOMATICO_VENCIMIENTO",
                        Dinero.de("500.00", Moneda.BOB),
                        30,
                        true,
                        "LOG_SISTEMA",
                        "Vencido y sin pago",
                        null,
                        null),
                gestor));
        transaccion.execute(t -> coberturaCU.cubrir(
                new EntradaCobertura(expediente.expedienteId(), Dinero.de("500.00", Moneda.BOB), 30, null), gestor));
    }

    @Test
    @DisplayName("cuadre: una cobertura aplicada de Bs 500 suma 500 y un caso a los impagos cubiertos del día")
    void cubiertoSuma() {
        var antes = cubiertoHoy();

        cubrir("5000.00", 15);

        var despues = cubiertoHoy();
        assertThat(despues.total().subtract(antes.total())).isEqualByComparingTo("500.00");
        assertThat(despues.casos() - antes.casos()).isEqualTo(1);
    }

    @Test
    @DisplayName("cuadre: con coberturas, la recuperación y la cobertura del fondo viajan con numerador y denominador")
    void tasasConNumeradorYDenominador() {
        cubrir("5000.00", 15);

        var calculados = calcular(hoy().toString());

        assertThat(calculados)
                .extracting(c -> c.indicador().codigo())
                .containsExactly(
                        "IMPAGOS_CUBIERTOS",
                        "PENDIENTES_COBRADOS_RECUPERADO",
                        "RECUPERACION_CUBIERTO",
                        "COBERTURA_FONDO");
        var recuperacion = calculados.get(2).indicador();
        assertThat(recuperacion.denominador()).isGreaterThan(BigDecimal.ZERO);
        assertThat(recuperacion.valor()).isNotNull();
        var cobertura = calculados.get(3).indicador();
        assertThat(cobertura.numerador()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(cobertura.denominador()).isNotNull();
    }

    @Test
    @DisplayName("límite: un período sin operaciones no tiene valor en ninguno (SIN_DATOS), nunca un cero")
    void sinDatos() {
        assertThat(calcular("2001-01-01"))
                .allSatisfy(c -> assertThat(c.indicador().valor()).isNull());
    }

    @Test
    @DisplayName("rechaza contar una cobertura RECHAZADA: la mora no alcanzó para activar el fondo y no suma")
    void rechazadaNoCuenta() {
        var antes = cubiertoHoy();

        cubrir("5000.00", 90);

        assertThat(cubiertoHoy().total().subtract(antes.total())).isEqualByComparingTo("0.00");
    }
}
