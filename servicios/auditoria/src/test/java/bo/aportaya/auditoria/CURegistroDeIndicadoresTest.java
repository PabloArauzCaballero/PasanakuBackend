package bo.aportaya.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.auditoria.aplicacion.RegistroDeIndicadores;
import bo.aportaya.auditoria.aplicacion.RegistroDeIndicadores.IndicadorCalculado;
import bo.aportaya.auditoria.aplicacion.RegistroDeIndicadores.Resultado;
import bo.aportaya.auditoria.infraestructura.IndicadorRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tableros · H4.S0.M3 — lo que un servicio dueño calculo entra a {@code indicador_kpi} y CU-98 lo publica. Cada
 * prueba usa su propio codigo: la tabla es append-only y el contenedor se comparte.
 */
class CURegistroDeIndicadoresTest extends BaseDeAuditoria {

    private final RegistroDeIndicadores registro =
            new RegistroDeIndicadores(new Datos(dsl), new IndicadorRepositorio(), Reloj.delSistema());

    private static String codigoNuevo() {
        return "T_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }

    private static IndicadorCalculado calculado(String codigo, String valor, String periodo) {
        return new IndicadorCalculado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                codigo,
                "Saldo cargado",
                new BigDecimal(valor),
                "BOB",
                periodo,
                6,
                false);
    }

    private Resultado registrar(IndicadorCalculado e) {
        return transaccion.execute(t -> registro.registrar(e));
    }

    private int filas(String codigo) {
        return ((Number) dslFixtura
                        .fetchOne("SELECT count(*) FROM auditoria.indicador_kpi WHERE codigo = ?", codigo)
                        .get(0))
                .intValue();
    }

    @Test
    @DisplayName("correcto: un indicador calculado se guarda atado a su definición y CU-98 lo publica con su valor")
    void seGuardaYSePublica() {
        String codigo = codigoNuevo();
        fixtura.definicion(codigo, "FINANZAS", "MAYOR_ES_MEJOR", 0, "v1");

        assertThat(registrar(calculado(codigo, "1500.0000", "2026-09"))).isEqualTo(Resultado.GUARDADO);

        var tablero = transaccion.execute(t -> tableroCU.ejecutar(
                new bo.aportaya.auditoria.aplicacion.CU98PublicarTablero.EntradaTablero(
                        "2026-09", "GLOBAL", Optional.empty(), 0),
                contexto()));
        var publicado = tablero.indicadores().stream()
                .filter(i -> i.codigo().equals(codigo))
                .findFirst()
                .orElseThrow();
        assertThat(publicado.valor()).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("1500"));
        assertThat(publicado.unidad()).isEqualTo("BOB");
    }

    @Test
    @DisplayName("evento duplicado: el mismo evento dos veces deja una sola fila")
    void duplicado() {
        String codigo = codigoNuevo();
        fixtura.definicion(codigo, "FINANZAS", "MAYOR_ES_MEJOR", 0, "v1");
        var e = calculado(codigo, "10.0000", "2026-09-30");

        registrar(e);
        assertThat(registrar(e)).isEqualTo(Resultado.DUPLICADO);
        assertThat(filas(codigo)).isEqualTo(1);
    }

    @Test
    @DisplayName("reintento: el mismo período recalculado (otro evento) no pisa el valor guardado — append-only")
    void noPisa() {
        String codigo = codigoNuevo();
        fixtura.definicion(codigo, "RIESGO", "MENOR_ES_MEJOR", 0, "v1");

        registrar(calculado(codigo, "10.0000", "2026"));
        assertThat(registrar(calculado(codigo, "99.0000", "2026"))).isEqualTo(Resultado.YA_ESTABA);

        assertThat(filas(codigo)).isEqualTo(1);
        assertThat((BigDecimal) dsl.fetchOne("SELECT valor FROM auditoria.indicador_kpi WHERE codigo = ?", codigo)
                        .get(0))
                .isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("rechaza un código sin definición vigente (AP-CU98-02): no guarda nada")
    void sinDefinicion() {
        String codigo = codigoNuevo();

        assertThat(registrar(calculado(codigo, "1.0000", "2026-09"))).isEqualTo(Resultado.SIN_DEFINICION);
        assertThat(filas(codigo)).isZero();
    }
}
