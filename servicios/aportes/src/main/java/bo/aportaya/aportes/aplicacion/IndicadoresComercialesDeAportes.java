package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.infraestructura.IndicadoresDeAportesRepositorio;
import bo.aportaya.plataforma.dominio.Indicador;
import bo.aportaya.plataforma.dominio.Indicador.Definicion;
import bo.aportaya.plataforma.dominio.Indicador.Medicion;
import bo.aportaya.plataforma.dominio.Indicador.Unidad;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.mensajeria.CalculadorDeIndicadores;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/** Tableros · NEGOCIO de {@code aportes}: el paso 4 del embudo, participaciones con su primer aporte en el periodo. */
@Component
public class IndicadoresComercialesDeAportes implements CalculadorDeIndicadores {

    private final IndicadoresDeAportesRepositorio repositorio;

    public IndicadoresComercialesDeAportes(IndicadoresDeAportesRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<Calculado> calcular(DSLContext dsl, PeriodoDeTablero periodo) {
        long primeros = repositorio.primerAporte(
                dsl, periodo.rango().inicio(), periodo.rango().fin());
        return List.of(new Calculado(
                Indicador.publicar(
                        new Definicion("EMBUDO_PRIMER_APORTE", Unidad.CONTEO, 0, false),
                        Medicion.conteo(primeros),
                        null),
                "Primer aporte pagado"));
    }
}
