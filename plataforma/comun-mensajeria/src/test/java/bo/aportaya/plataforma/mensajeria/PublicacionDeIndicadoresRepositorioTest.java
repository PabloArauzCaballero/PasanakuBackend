package bo.aportaya.plataforma.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Indicador;
import bo.aportaya.plataforma.dominio.Indicador.Definicion;
import bo.aportaya.plataforma.dominio.Indicador.Medicion;
import bo.aportaya.plataforma.dominio.Indicador.Unidad;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tableros · el trabajo comun de publicacion de indicadores, contra PostgreSQL real: emite por outbox lo que tiene
 * valor, recorre los periodos cerrados del dia, rellena solo si se le pide, y un periodo que falla no arrastra a los
 * demas.
 */
class PublicacionDeIndicadoresRepositorioTest extends BaseDePruebaMensajeria {

    private static Datos datos;
    private static TransactionTemplate transaccion;

    @BeforeAll
    static void armar() {
        var c = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(c.getJdbcUrl(), c.getUsername(), c.getPassword());
        datos = new Datos(DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES));
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
    }

    /** Un calculador de prueba que anota que periodos le pidieron y falla en el que se le diga. */
    private static final class Anotador implements CalculadorDeIndicadores {
        final List<String> pedidos = new ArrayList<>();
        final String falla;

        Anotador(String falla) {
            this.falla = falla;
        }

        @Override
        public List<Calculado> calcular(DSLContext dsl, PeriodoDeTablero periodo) {
            pedidos.add(periodo.codigo());
            if (periodo.codigo().equals(falla)) {
                throw new IllegalStateException("fuente caida");
            }
            var conValor = Indicador.publicar(
                    new Definicion("PRUEBA_CON_VALOR", Unidad.DINERO, 0, false),
                    Medicion.de(Dinero.de("10.00", Moneda.BOB), 2),
                    null);
            var sinValor = Indicador.publicar(
                    new Definicion("PRUEBA_SIN_VALOR", Unidad.CONTEO, 0, false), Medicion.conteo(0), null);
            return List.of(new Calculado(conValor, "Con valor"), new Calculado(sinValor, "Sin valor"));
        }
    }

    private static PublicacionDeIndicadores publicacion(Anotador a, String hoy, int meses, int dias) {
        return new PublicacionDeIndicadores(
                datos,
                transaccion,
                new PublicadorDeIndicadores(new Outbox(ESQUEMA), ESQUEMA),
                List.of(a),
                Reloj.fijo(Instant.parse(hoy + "T16:00:00Z")),
                meses,
                dias);
    }

    private int emitidos(String periodo) {
        return dsl.fetchCount(
                DSL.table(DSL.name(ESQUEMA, "evento_dominio")),
                DSL.field("tipo")
                        .eq("nucleo_financiero.indicador_calculado")
                        .and(DSL.field("payload->>'periodo'").eq(periodo)));
    }

    @Test
    @DisplayName("correcto: publicar un período emite solo el indicador con valor, por outbox")
    void publicaSoloConValor() {
        assertThat(publicacion(new Anotador(""), "2026-10-16", 0, 0).publicar(PeriodoDeTablero.de("2026-09-30")))
                .isEqualTo(1);
        assertThat(emitidos("2026-09-30")).isEqualTo(1);
    }

    @Test
    @DisplayName("límite: el 1 de octubre el trabajo diario cierra el 30 de septiembre y el mes de septiembre")
    void diarioCierraDiaYMes() {
        var a = new Anotador("");
        publicacion(a, "2026-10-01", 0, 0).diario();

        assertThat(a.pedidos).containsExactly("2026-09-30", "2026-09");
        assertThat(emitidos("2026-09")).isEqualTo(1);
    }

    @Test
    @DisplayName("límite: sin relleno configurado no se calcula nada al arrancar; con relleno, los cerrados pedidos")
    void relleno() {
        var sinRelleno = new Anotador("");
        publicacion(sinRelleno, "2026-10-16", 0, 0).rellenar();
        assertThat(sinRelleno.pedidos).isEmpty();

        var conRelleno = new Anotador("");
        publicacion(conRelleno, "2026-10-16", 1, 1).rellenar();
        assertThat(conRelleno.pedidos).containsExactly("2026-10-15", "2026-09");
    }

    @Test
    @DisplayName("inválido: si un período falla, los demás se publican igual")
    void unoFallaLosDemasNo() {
        var a = new Anotador("2026-09-30");
        publicacion(a, "2026-10-01", 0, 0).diario();

        assertThat(a.pedidos).containsExactly("2026-09-30", "2026-09");
        assertThat(emitidos("2026-09-30")).isZero();
        assertThat(emitidos("2026-09")).isEqualTo(1);
    }
}
