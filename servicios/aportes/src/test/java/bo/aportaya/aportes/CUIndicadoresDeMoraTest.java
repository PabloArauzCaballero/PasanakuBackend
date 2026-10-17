package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.aplicacion.IndicadoresDeMora;
import bo.aportaya.aportes.infraestructura.IndicadoresDeAportesRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tableros · H4.S1.M3 — tasa de mora, dinero en mora y lo cobrado tarde. Se mide el DELTA de cada prueba. */
class CUIndicadoresDeMoraTest extends BaseDeAportes {

    private final IndicadoresDeAportesRepositorio repositorio = new IndicadoresDeAportesRepositorio();

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private static ContextoSesion sistema() {
        return ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
    }

    private <T> T leer(java.util.function.Function<org.jooq.DSLContext, T> consulta) {
        return transaccion.execute(t -> new Datos(dsl).conContexto(sistema(), consulta));
    }

    private LocalDate finDeGracia(UUID obligacion) {
        return dsl.fetchOne("SELECT fecha_fin_gracia FROM aportes.obligacion_aporte WHERE id = ?", obligacion)
                .get(0, LocalDate.class);
    }

    private static LocalDate hoy() {
        return LocalDate.now(ZoneId.of("America/La_Paz"));
    }

    @Test
    @DisplayName("cuadre: una obligación de Bs 500 vencida y sin pagar suma 500 al dinero en mora y un caso a la tasa")
    void vencidaSinPagar() {
        var moraAntes = leer(d -> repositorio.dineroEnMora(d, hoy().plusDays(1), "BOB"));
        var o = fixtura.obligacion(fixtura.usuario(), "500.00", -10);
        LocalDate gracia = finDeGracia(o.id());

        var moraDespues = leer(d -> repositorio.dineroEnMora(d, hoy().plusDays(1), "BOB"));
        var tasa = leer(d -> repositorio.mora(d, gracia, gracia, "BOB"));

        assertThat(moraDespues.total().subtract(moraAntes.total())).isEqualByComparingTo("500.00");
        assertThat(tasa.enMora()).isPositive();
        assertThat(tasa.exigibles()).isGreaterThanOrEqualTo(tasa.enMora());
    }

    @Test
    @DisplayName("cuadre: pagar hoy una obligación vencida suma a lo cobrado tarde y la saca del dinero en mora")
    void pagoTarde() {
        UUID usuario = fixtura.usuario();
        var o = fixtura.obligacion(usuario, "500.00", -10);
        var tardeAntes = leer(d -> repositorio.cobradoTarde(d, hoy().minusDays(1), hoy().plusDays(1), "BOB"));
        var moraAntes = leer(d -> repositorio.dineroEnMora(d, hoy().plusDays(1), "BOB"));

        transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        "tarde-" + o.id(),
                        o.id(),
                        Dinero.de("500.00", Moneda.BOB),
                        Dinero.de("0.00", Moneda.BOB),
                        "BILLETERA_MOVIL",
                        "ref-tarde",
                        Optional.empty(),
                        false,
                        true),
                contextoDe(usuario)));

        var tardeDespues = leer(d -> repositorio.cobradoTarde(d, hoy().minusDays(1), hoy().plusDays(1), "BOB"));
        var moraDespues = leer(d -> repositorio.dineroEnMora(d, hoy().plusDays(1), "BOB"));
        assertThat(tardeDespues.total().subtract(tardeAntes.total())).isEqualByComparingTo("500.00");
        assertThat(moraAntes.total().subtract(moraDespues.total())).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("límite: un período viejo no publica dinero en mora (SIN_HISTORIA) y uno vacío no tiene valores")
    void sinHistoria() {
        var calculador = new IndicadoresDeMora(repositorio, Reloj.delSistema());

        var viejo = leer(d -> calculador.calcular(d, PeriodoDeTablero.de("2001-01")));

        assertThat(viejo)
                .extracting(c -> c.indicador().codigo())
                .containsExactly("TASA_MORA", "DINERO_EN_MORA", "PENDIENTES_COBRADOS_PROPIO");
        assertThat(viejo).allSatisfy(c -> assertThat(c.indicador().valor()).isNull());
        assertThat(viejo.get(1).indicador().motivo())
                .isEqualTo(bo.aportaya.plataforma.dominio.Indicador.Motivo.SIN_HISTORIA);
    }

    @Test
    @DisplayName("rechaza contar en mora una obligación que todavía no venció")
    void noVencida() {
        var antes = leer(d -> repositorio.dineroEnMora(d, hoy().plusDays(1), "BOB"));

        fixtura.obligacion(fixtura.usuario(), "500.00", 10);

        assertThat(leer(d -> repositorio.dineroEnMora(d, hoy().plusDays(1), "BOB"))
                        .total())
                .isEqualByComparingTo(antes.total());
    }
}
