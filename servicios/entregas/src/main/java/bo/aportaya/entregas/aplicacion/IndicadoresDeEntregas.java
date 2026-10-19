package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.infraestructura.IndicadoresDeEntregasRepositorio;
import bo.aportaya.plataforma.dominio.Indicador;
import bo.aportaya.plataforma.dominio.Indicador.Definicion;
import bo.aportaya.plataforma.dominio.Indicador.Medicion;
import bo.aportaya.plataforma.dominio.Indicador.Unidad;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.mensajeria.CalculadorDeIndicadores;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Tableros · entregas pendientes al cierre y tiempo medio hasta el desembolso. «Cero pendientes» es la buena noticia y
 * se publica (cero real); el tiempo medio sin ninguna entrega en el periodo no tiene valor.
 */
@Component
public class IndicadoresDeEntregas implements CalculadorDeIndicadores {

    private final IndicadoresDeEntregasRepositorio repositorio;

    public IndicadoresDeEntregas(IndicadoresDeEntregasRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<Calculado> calcular(DSLContext dsl, PeriodoDeTablero periodo) {
        var pendientes = repositorio.pendientes(dsl, periodo.rango().fin());
        var tiempo = repositorio.tiempoHastaDesembolso(
                dsl, periodo.rango().inicio(), periodo.rango().fin());
        return List.of(
                new Calculado(
                        Indicador.publicar(
                                new Definicion("ENTREGAS_PENDIENTES", Unidad.CONTEO, 0, true),
                                Medicion.conteo(pendientes.cuantas()),
                                null),
                        "Entregas pendientes"),
                new Calculado(
                        Indicador.publicar(
                                new Definicion("TIEMPO_TURNO_DESEMBOLSO", Unidad.DIAS, 0, false),
                                new Medicion(tiempo.dias(), null, null, tiempo.casos()),
                                null),
                        "Días del turno al desembolso"));
    }
}
