package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.infraestructura.IndicadoresDeInversionRepositorio;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Indicador;
import bo.aportaya.plataforma.dominio.Indicador.Definicion;
import bo.aportaya.plataforma.dominio.Indicador.Medicion;
import bo.aportaya.plataforma.dominio.Indicador.Unidad;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.mensajeria.CalculadorDeIndicadores;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/** Tableros · saldo invertido en el fondo de inversion al cierre del periodo (D12; dato sintetico con aliado simulado). */
@Component
public class IndicadoresDeInversion implements CalculadorDeIndicadores {

    private final IndicadoresDeInversionRepositorio repositorio;

    public IndicadoresDeInversion(IndicadoresDeInversionRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<Calculado> calcular(DSLContext dsl, PeriodoDeTablero periodo) {
        var s = repositorio.invertido(dsl, periodo.rango().fin(), Moneda.BOB.name());
        return List.of(new Calculado(
                Indicador.publicar(
                        new Definicion("SALDO_INVERTIDO", Unidad.DINERO, 0, false),
                        Medicion.de(Dinero.de(s.total(), Moneda.BOB), s.casos()),
                        null),
                "Saldo invertido"));
    }
}
