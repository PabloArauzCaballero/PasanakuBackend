package bo.aportaya.erp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.erp.aplicacion.CU101Presupuestar.EntradaPresupuesto;
import bo.aportaya.erp.aplicacion.CU101Presupuestar.Partida;
import bo.aportaya.erp.aplicacion.ConsultarListadosDeErp.Filtro;
import bo.aportaya.erp.aplicacion.FilasDeErp;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · A6 · el listado de partidas presupuestarias, contra PostgreSQL real.
 *
 * <p>Ejercicios del rango 2550, que ninguna otra prueba de erp reparte, y siempre filtrado por el
 * ejercicio propio: lo que otras pruebas dejan en la base no entra en las aserciones.
 */
class CU101ListadoTest extends BaseDeErp {

    private static final AtomicInteger ANIO = new AtomicInteger(2550);

    private int anio;
    private UUID ejercicioId;
    private ContextoSesion ctx;

    @BeforeEach
    void escenario() {
        anio = ANIO.incrementAndGet();
        ctx = contextoDe(fixtura.usuario());
        ejercicioId =
                transaccion.execute(t -> periodoCU.abrirEjercicio(anio, ctx)).ejercicioId();
    }

    private UUID periodo(int mes) {
        return dsl.fetchOne(
                        "SELECT id FROM erp.periodo_contable WHERE ejercicio_fiscal_id = ? AND mes = ?",
                        ejercicioId,
                        (short) mes)
                .get(0, UUID.class);
    }

    private UUID presupuestoEnTresMeses() {
        UUID centro = fixtura.centroDeCosto("CC-L-" + anio, "AREA");
        UUID cuenta = fixtura.cuenta("5102-" + anio, "EGRESO", "DEUDORA");
        var entrada = new EntradaPresupuesto(
                centro,
                ejercicioId,
                "Presupuesto listado " + anio,
                "BOB",
                List.of(
                        new Partida(cuenta, periodo(1), new BigDecimal("1000.10")),
                        new Partida(cuenta, periodo(2), new BigDecimal("2000.20")),
                        new Partida(cuenta, periodo(3), new BigDecimal("3000.30"))));
        return transaccion.execute(t -> presupuestoCU.crear(entrada, ctx)).presupuestoId();
    }

    private Filtro<LocalDate> filtro(List<String> estados, Optional<LocalDate> antesDe, Optional<UUID> id, int n) {
        return new Filtro<>(estados, antesDe, id, n);
    }

    @Test
    @DisplayName("Dado un presupuesto con partidas en tres meses del ejercicio · Cuando Contabilidad las recorre de a"
            + " dos · Entonces van del mes mas reciente al mas antiguo, con lo presupuestado y lo ejecutado en cadena y"
            + " su moneda, sin repetir ninguna")
    void partidasPaginadas() {
        UUID presupuesto = presupuestoEnTresMeses();

        var primera = transaccion.execute(t -> listadosCU.partidas(
                Optional.of(ejercicioId), filtro(List.of(), Optional.empty(), Optional.empty(), 2), ctx));
        var segunda = transaccion.execute(t -> listadosCU.partidas(
                Optional.of(ejercicioId),
                filtro(List.of(), primera.proximoAntesDe(), primera.proximoAntesDeId(), 2),
                ctx));

        assertThat(primera.items()).extracting(FilasDeErp.Partida::mes).containsExactly(3, 2);
        assertThat(segunda.items()).extracting(FilasDeErp.Partida::mes).containsExactly(1);
        assertThat(segunda.proximoAntesDe()).isEmpty();
        var marzo = primera.items().get(0);
        assertThat(marzo.presupuestoId()).isEqualTo(presupuesto);
        assertThat(marzo.estado()).isEqualTo("BORRADOR");
        assertThat(marzo.anio()).isEqualTo(anio);
        assertThat(marzo.presupuestado().toString()).isEqualTo("3000.30");
        assertThat(marzo.ejecutado().toString()).isEqualTo("0.00");
        assertThat(marzo.presupuestado().moneda().name()).isEqualTo("BOB");
    }

    @Test
    @DisplayName("Dado un ejercicio sin presupuestos · Cuando Contabilidad pide sus partidas · Entonces la pagina viene"
            + " vacia y sin cursor")
    void ejercicioSinPresupuesto() {
        var pagina = transaccion.execute(t -> listadosCU.partidas(
                Optional.of(ejercicioId), filtro(List.of(), Optional.empty(), Optional.empty(), 25), ctx));

        assertThat(pagina.items()).isEmpty();
        assertThat(pagina.proximoAntesDe()).isEmpty();
        assertThat(pagina.proximoAntesDeId()).isEmpty();
    }

    @Test
    @DisplayName("Dado un presupuesto en borrador con tres partidas · Cuando Contabilidad pide una pagina de tres o"
            + " filtra por APROBADO · Entonces la pagina llena trae cursor, la siguiente viene vacia y el filtro"
            + " no muestra el borrador")
    void paginaExactaYFiltro() {
        presupuestoEnTresMeses();

        var llena = transaccion.execute(t -> listadosCU.partidas(
                Optional.of(ejercicioId), filtro(List.of(), Optional.empty(), Optional.empty(), 3), ctx));
        var despues = transaccion.execute(t -> listadosCU.partidas(
                Optional.of(ejercicioId), filtro(List.of(), llena.proximoAntesDe(), llena.proximoAntesDeId(), 3), ctx));
        var aprobadas = transaccion.execute(t -> listadosCU.partidas(
                Optional.of(ejercicioId), filtro(List.of("APROBADO"), Optional.empty(), Optional.empty(), 3), ctx));

        assertThat(llena.items()).hasSize(3);
        assertThat(llena.proximoAntesDe()).isPresent();
        assertThat(despues.items()).isEmpty();
        assertThat(aprobadas.items()).isEmpty();
    }

    @Test
    @DisplayName("rechaza el listado de partidas con cursor a medias o estado desconocido: AP-CU101-04")
    void rechazaFiltroInvalido() {
        assertThatThrownBy(() -> transaccion.execute(t -> listadosCU.partidas(
                        Optional.empty(),
                        filtro(List.of(), Optional.of(LocalDate.of(anio, 1, 1)), Optional.empty(), 5),
                        ctx)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU101-04"));
        assertThatThrownBy(() -> transaccion.execute(t -> listadosCU.partidas(
                        Optional.empty(), filtro(List.of("VIGENTE"), Optional.empty(), Optional.empty(), 5), ctx)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU101-04"));
    }
}
