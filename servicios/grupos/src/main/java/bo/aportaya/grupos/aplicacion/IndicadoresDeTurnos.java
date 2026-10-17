package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.infraestructura.IndicadoresDeTurnosRepositorio;
import bo.aportaya.plataforma.dominio.Indicador;
import bo.aportaya.plataforma.dominio.Indicador.Definicion;
import bo.aportaya.plataforma.dominio.Indicador.Medicion;
import bo.aportaya.plataforma.dominio.Indicador.Unidad;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.mensajeria.CalculadorDeIndicadores;
import java.math.BigDecimal;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Tableros · turnos normales y turnos que cierran el ciclo. La muestra de los dos es el total de turnos cobrados del
 * periodo: sin ninguno no hay valor; con alguno, «0 turnos de cierre» es un cero real y se publica.
 */
@Component
public class IndicadoresDeTurnos implements CalculadorDeIndicadores {

    private final IndicadoresDeTurnosRepositorio repositorio;

    public IndicadoresDeTurnos(IndicadoresDeTurnosRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<Calculado> calcular(DSLContext dsl, PeriodoDeTablero periodo) {
        var t = repositorio.cobrados(
                dsl, periodo.rango().inicio(), periodo.rango().fin());
        return List.of(
                conteo("TURNOS_NORMALES", "Turnos normales", t.normales(), t.total()),
                conteo("TURNOS_CIERRE_CICLO", "Turnos que cierran ciclo", t.deCierre(), t.total()));
    }

    private static Calculado conteo(String codigo, String nombre, long cuantos, long muestra) {
        return new Calculado(
                Indicador.publicar(
                        new Definicion(codigo, Unidad.CONTEO, 0, false),
                        new Medicion(BigDecimal.valueOf(cuantos), null, null, muestra),
                        null),
                nombre);
    }
}
