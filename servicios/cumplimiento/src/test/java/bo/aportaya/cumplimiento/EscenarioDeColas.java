package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento;
import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento.Pagina;
import bo.aportaya.cumplimiento.infraestructura.TableroDeCumplimientoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.function.Executable;

/**
 * Lo que comparten las pruebas de las colas del backoffice (carril A · H9), contra la base real.
 *
 * <p>Las filas se insertan como fixture SINTETICA (sin datos de personas reales). Varias tablas son
 * append-only y otras pruebas dejan filas: por eso se recorren TODAS las paginas con un limite chico y se
 * afirma sobre las filas propias, nunca sobre posiciones absolutas.
 */
abstract class EscenarioDeColas extends BaseDeCumplimiento {

    /** Lo que cada prueba inserto en tablas que SI admiten borrado: se limpia al terminar. */
    private final List<String[]> propiasParaBorrar = new ArrayList<>();

    protected UUID borrarAlTerminar(String tabla, UUID id) {
        propiasParaBorrar.add(new String[] {tabla, id.toString()});
        return id;
    }

    @AfterEach
    void borrarLasPropias() {
        for (String[] fila : propiasParaBorrar) {
            dslFixtura
                    .deleteFrom(DSL.table(DSL.name("cumplimiento", fila[0])))
                    .where(DSL.field("id", UUID.class).eq(UUID.fromString(fila[1])))
                    .execute();
        }
        propiasParaBorrar.clear();
    }

    protected static ConsultarTableroDeCumplimiento colas() {
        return new ConsultarTableroDeCumplimiento(new Datos(dsl), new TableroDeCumplimientoRepositorio());
    }

    /** Un operador del backoffice: rol BACKOFFICE en la sesion, como lo deja el filtro de seguridad. */
    protected static ContextoSesion operador() {
        return ContextoSesion.de(
                fixtura.usuario(), "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    /** Todas las paginas, de a {@code limite}, hasta que el cursor se termina. */
    protected static <T, C> List<T> todas(Function<PaginaPedida<C>, Pagina<T, C>> pedir, int limite) {
        var filas = new ArrayList<T>();
        Optional<C> antesDe = Optional.empty();
        Optional<UUID> antesDeId = Optional.empty();
        int vueltas = 0;
        do {
            var pedido = new PaginaPedida<>(antesDe, antesDeId, limite);
            var pagina = transaccion.execute(t -> pedir.apply(pedido));
            assertThat(pagina.items().size()).isLessThanOrEqualTo(limite);
            filas.addAll(pagina.items());
            antesDe = pagina.proximoAntesDe();
            antesDeId = pagina.proximoAntesDeId();
            assertThat(++vueltas).as("el cursor avanza y termina").isLessThan(10_000);
        } while (antesDeId.isPresent());
        return filas;
    }

    protected record PaginaPedida<C>(Optional<C> antesDe, Optional<UUID> antesDeId, int limite) {}

    /** Las filas propias, en el orden en que llegaron. */
    protected static <T> List<UUID> propias(List<T> filas, Function<T, UUID> id, List<UUID> mias) {
        return filas.stream().map(id).filter(mias::contains).toList();
    }

    protected static void rechazaCon(String codigo, Executable llamada) {
        assertThatThrownBy(llamada::execute)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo(codigo));
    }
}
