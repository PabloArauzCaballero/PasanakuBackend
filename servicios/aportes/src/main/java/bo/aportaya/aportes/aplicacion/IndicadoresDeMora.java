package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.infraestructura.IndicadoresDeAportesRepositorio;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Indicador;
import bo.aportaya.plataforma.dominio.Indicador.Definicion;
import bo.aportaya.plataforma.dominio.Indicador.Medicion;
import bo.aportaya.plataforma.dominio.Indicador.Motivo;
import bo.aportaya.plataforma.dominio.Indicador.Unidad;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.CalculadorDeIndicadores;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Tableros · tasa de mora y pendientes cobrados (reconstruibles por fecha) y dinero en mora (foto del estado actual:
 * solo para el periodo que termina ayer; uno mas viejo sale SIN_HISTORIA y no se publica, en vez de inventar el saldo
 * que habia entonces).
 */
@Component
public class IndicadoresDeMora implements CalculadorDeIndicadores {

    private static final Moneda MONEDA = Moneda.BOB;

    private final IndicadoresDeAportesRepositorio repositorio;
    private final Reloj reloj;

    public IndicadoresDeMora(IndicadoresDeAportesRepositorio repositorio, Reloj reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Override
    public List<Calculado> calcular(DSLContext dsl, PeriodoDeTablero periodo) {
        var desde = periodo.rango().inicio();
        var hasta = periodo.rango().fin();
        var tasa = repositorio.mora(dsl, desde, hasta, MONEDA.name());
        var tarde = repositorio.cobradoTarde(dsl, desde, hasta, MONEDA.name());

        var definicionMora = new Definicion("DINERO_EN_MORA", Unidad.DINERO, 0, false);
        Indicador enMora = hasta.isBefore(reloj.hoy().minusDays(1))
                ? Indicador.sinValor(definicionMora, Motivo.SIN_HISTORIA)
                : enMora(definicionMora, repositorio.dineroEnMora(dsl, reloj.hoy(), MONEDA.name()));

        return List.of(
                new Calculado(
                        Indicador.publicar(
                                new Definicion("TASA_MORA", Unidad.PORCENTAJE, 0, false),
                                Medicion.tasa(tasa.enMora(), tasa.exigibles()),
                                null),
                        "Tasa de mora"),
                new Calculado(enMora, "Dinero en mora"),
                new Calculado(
                        Indicador.publicar(
                                new Definicion("PENDIENTES_COBRADOS_PROPIO", Unidad.DINERO, 0, false),
                                Medicion.de(Dinero.de(tarde.total(), MONEDA), tarde.casos()),
                                null),
                        "Pendientes cobrados (pagó tarde)"));
    }

    private static Indicador enMora(Definicion d, IndicadoresDeAportesRepositorio.Suma s) {
        return Indicador.publicar(d, Medicion.de(Dinero.de(s.total(), MONEDA), s.casos()), null);
    }
}
