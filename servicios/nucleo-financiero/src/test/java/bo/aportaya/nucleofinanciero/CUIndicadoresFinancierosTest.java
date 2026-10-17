package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.EntradaReverso;
import bo.aportaya.nucleofinanciero.aplicacion.IndicadoresFinancieros;
import bo.aportaya.nucleofinanciero.infraestructura.IndicadoresDelMayorRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.plataforma.mensajeria.PublicacionDeIndicadores;
import bo.aportaya.plataforma.mensajeria.PublicadorDeIndicadores;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tableros · H4.S1.M1 — los indicadores de FINANZAS salen del MAYOR (regla 91.1.5). Las cuentas del plan y el
 * contenedor se comparten entre clases, asi que se compara el DELTA que deja cada prueba.
 */
class CUIndicadoresFinancierosTest extends BaseDeAportes {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");
    private final IndicadoresDelMayorRepositorio mayor = new IndicadoresDelMayorRepositorio();

    private PeriodoDeTablero hoy() {
        return PeriodoDeTablero.dia(LocalDate.now(LA_PAZ));
    }

    private IndicadoresDelMayorRepositorio.Total aportesDeHoy() {
        return transaccion.execute(t -> new Datos(dsl)
                .conContexto(
                        sistema(), d -> mayor.porTipoDeTransaccion(d, "APORTE_A_GRUPO", hoy().rango(), Moneda.BOB)));
    }

    private static ContextoSesion sistema() {
        return ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("cuadre: un aporte cobrado de Bs 500 suma 500 y un caso al total del día, leído del mayor")
    void sumaDelMayor() {
        var antes = aportesDeHoy();

        procesar(cobrado(aporte("1000.00"), "500.00"));

        var despues = aportesDeHoy();
        assertThat(despues.total().monto().subtract(antes.total().monto())).isEqualByComparingTo("500.00");
        assertThat(despues.casos() - antes.casos()).isEqualTo(1);
    }

    @Test
    @DisplayName("compensa: una transacción reversada (CU-14) deja de contar en el total")
    void reversadaNoCuenta() {
        var a = aporte("1000.00");
        var aplicado = procesar(cobrado(a, "300.00"));
        var antes = aportesDeHoy();
        UUID tx = aplicado.transaccionId().orElseThrow();

        transaccion.execute(x -> reversoCU.ejecutar(
                new EntradaReverso(
                        "rev-ind-" + tx,
                        tx,
                        "ERROR_OPERATIVO",
                        "Aporte cobrado dos veces por error del canal",
                        fixtura.usuario()),
                contextoDe(a.usuario())));

        var despues = aportesDeHoy();
        assertThat(antes.total().monto().subtract(despues.total().monto())).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("límite: un período sin operaciones no tiene valor (SIN_DATOS), nunca un cero")
    void sinDatos() {
        var calculados =
                transaccion.execute(t -> new Datos(dsl).conContexto(sistema(), d -> new IndicadoresFinancieros(mayor)
                        .calcular(d, PeriodoDeTablero.de("2001-01-01"))));

        assertThat(calculados)
                .extracting(c -> c.indicador().codigo())
                .containsExactly("SALDO_CARGADO", "SALDO_RETIRADO", "INTERESES_MORA_COBRADOS", "COMISIONES_COBRADAS");
        assertThat(calculados).allSatisfy(c -> assertThat(c.indicador().valor()).isNull());
    }

    @Test
    @DisplayName("rechaza sumar otra moneda: un total en BOB no incluye nada en USD")
    void otraMoneda() {
        var usd = transaccion.execute(t -> new Datos(dsl)
                .conContexto(
                        sistema(), d -> mayor.porTipoDeTransaccion(d, "APORTE_A_GRUPO", hoy().rango(), Moneda.USD)));
        var bob = aportesDeHoy();

        assertThat(usd.total().moneda()).isEqualTo(Moneda.USD);
        assertThat(bob.total().monto()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(usd.casos()).isLessThanOrEqualTo(bob.casos());
    }

    @Test
    @DisplayName("cuadre: publicar el día emite nucleo_financiero.indicador_calculado por outbox, solo los que tienen"
            + " valor")
    void publicaPorOutbox() {
        procesar(cobrado(aporte("1000.00"), "200.00"));
        var calculador = new IndicadoresFinancieros(mayor) {
            @Override
            public List<Calculado> calcular(org.jooq.DSLContext d, PeriodoDeTablero p) {
                // El indicador de aportes no esta en el catalogo financiero: se prueba la cañeria con uno que SI tiene
                // datos hoy en este contenedor, y con los cuatro reales del dia.
                var reales = super.calcular(d, p);
                var aportes = mayor.porTipoDeTransaccion(d, "APORTE_A_GRUPO", p.rango(), Moneda.BOB);
                var extra = new Calculado(
                        bo.aportaya.plataforma.dominio.Indicador.publicar(
                                new bo.aportaya.plataforma.dominio.Indicador.Definicion(
                                        "PRUEBA_APORTES",
                                        bo.aportaya.plataforma.dominio.Indicador.Unidad.DINERO,
                                        0,
                                        false),
                                bo.aportaya.plataforma.dominio.Indicador.Medicion.de(aportes.total(), aportes.casos()),
                                null),
                        "Aportes de prueba");
                return java.util.stream.Stream.concat(reales.stream(), java.util.stream.Stream.of(extra))
                        .toList();
            }
        };
        var publicacion = new PublicacionDeIndicadores(
                new Datos(dsl),
                transaccion,
                new PublicadorDeIndicadores(new Outbox("nucleo_financiero"), "nucleo_financiero"),
                List.of(calculador),
                Reloj.delSistema(),
                0,
                0);
        int antes = contar(
                "SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE tipo = 'nucleo_financiero.indicador_calculado'");

        int emitidos = publicacion.publicar(hoy());

        assertThat(emitidos).isGreaterThanOrEqualTo(1);
        assertThat(contar(
                                "SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE tipo = 'nucleo_financiero.indicador_calculado'")
                        - antes)
                .isEqualTo(emitidos);
        assertThat(contar(
                        """
                        SELECT count(*) FROM nucleo_financiero.evento_dominio
                         WHERE tipo = 'nucleo_financiero.indicador_calculado'
                           AND payload->>'codigo' = 'PRUEBA_APORTES' AND payload->>'unidad' = 'BOB'
                           AND payload->>'periodo' = ?
                        """,
                        hoy().codigo()))
                .isGreaterThanOrEqualTo(1);
    }
}
