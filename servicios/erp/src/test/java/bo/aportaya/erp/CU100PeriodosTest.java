package bo.aportaya.erp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.erp.aplicacion.CU100AbrirCerrarPeriodo;
import bo.aportaya.erp.aplicacion.CU100AbrirCerrarPeriodo.EjercicioConPeriodos;
import bo.aportaya.erp.infraestructura.PeriodoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · H4 · los ejercicios y el periodo vigente, contra PostgreSQL real.
 *
 * <p>Con reloj fijo y anios del rango 2800, que ninguna otra prueba de erp reparte: el resultado no depende del dia en
 * que se corre. Lo que importa: el vigente es el que contiene HOY en America/La_Paz —no en UTC—, y si
 * ninguno lo contiene la respuesta lo dice en vez de elegir uno.
 */
class CU100PeriodosTest extends BaseDeErp {

    private CU100AbrirCerrarPeriodo conReloj(String instante) {
        return new CU100AbrirCerrarPeriodo(
                new Datos(dsl), new PeriodoRepositorio(), new Outbox("erp"), Reloj.fijo(Instant.parse(instante)));
    }

    private ContextoSesion contador() {
        return contextoDe(fixtura.usuario());
    }

    @Test
    @DisplayName("Dado un ejercicio abierto y el reloj en la noche del 31 de marzo en La Paz · Cuando se consulta el"
            + " periodo vigente · Entonces es marzo, aunque en UTC ya sea primero de abril")
    void vigenteEnLaZonaDeBolivia() {
        transaccion.execute(t -> periodoCU.abrirEjercicio(2800, contador()));

        var vigente = transaccion.execute(t -> conReloj("2800-04-01T02:00:00Z").vigente(contador()));

        assertThat(vigente.anio()).isEqualTo(2800);
        assertThat(vigente.mes()).isEqualTo(3);
        assertThat(vigente.estado()).isEqualTo("ABIERTO");
        assertThat(vigente.desde()).isBeforeOrEqualTo(LocalDate.of(2800, 3, 31));
        assertThat(vigente.hasta()).isAfterOrEqualTo(LocalDate.of(2800, 3, 31));
    }

    @Test
    @DisplayName("Dados tres ejercicios · Cuando Contabilidad los recorre de a dos · Entonces van del mas reciente al"
            + " mas antiguo, cada uno con sus doce periodos, sin repetir ninguno")
    void ejerciciosPaginados() {
        for (int anio = 2811; anio <= 2813; anio++) {
            int este = anio;
            transaccion.execute(t -> periodoCU.abrirEjercicio(este, contador()));
        }

        var primera = transaccion.execute(t -> periodoCU.ejercicios(Optional.of(2814), 2, contador()));
        var segunda = transaccion.execute(t -> periodoCU.ejercicios(primera.proximoAntesDeAnio(), 2, contador()));

        assertThat(primera.items()).extracting(EjercicioConPeriodos::anio).containsExactly(2813, 2812);
        assertThat(primera.proximoAntesDeAnio()).contains(2812);
        assertThat(segunda.items().get(0).anio()).isEqualTo(2811);
        assertThat(primera.items().get(0).periodos())
                .hasSize(12)
                .extracting(p -> p.mes())
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

    @Test
    @DisplayName("Dado un dia que ningun periodo contiene · Cuando se consulta el periodo vigente · Entonces responde"
            + " SIN_PERIODO_VIGENTE y no elige uno cualquiera")
    void sinPeriodoParaHoy() {
        assertThatThrownBy(() -> transaccion.execute(
                        t -> conReloj("2898-06-15T15:00:00Z").vigente(contador())))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU100-04"));
    }

    @Test
    @DisplayName("Dado un periodo vigente ya cerrado · Cuando se consulta el periodo vigente · Entonces vuelve con"
            + " estado CERRADO: el estado se informa, no se salta al mes siguiente")
    void vigenteCerradoSeInforma() {
        var ejercicio = transaccion.execute(t -> periodoCU.abrirEjercicio(2820, contador()));
        var enero = dsl.fetchOne(
                        "SELECT id FROM erp.periodo_contable WHERE ejercicio_fiscal_id = ? AND mes = 1",
                        ejercicio.ejercicioId())
                .get(0, java.util.UUID.class);
        transaccion.execute(t -> periodoCU.cerrarPeriodo(
                new CU100AbrirCerrarPeriodo.EntradaCierre(enero, "Cierre sin movimientos"), contador()));

        var vigente = transaccion.execute(t -> conReloj("2820-01-20T15:00:00Z").vigente(contador()));

        assertThat(vigente.periodoId()).isEqualTo(enero);
        assertThat(vigente.estado()).isEqualTo("CERRADO");
    }
}
