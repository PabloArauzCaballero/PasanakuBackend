package bo.aportaya.garantia.aplicacion;

import bo.aportaya.garantia.infraestructura.IndicadoresDeGarantiaRepositorio;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Indicador;
import bo.aportaya.plataforma.dominio.Indicador.Definicion;
import bo.aportaya.plataforma.dominio.Indicador.Medicion;
import bo.aportaya.plataforma.dominio.Indicador.Unidad;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.mensajeria.CalculadorDeIndicadores;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Tableros · impagos cubiertos y recuperado en el periodo (flujos), y la recuperacion acumulada y la cobertura del
 * fondo al cierre (saldos, reconstruibles a cualquier fecha porque cada movimiento guarda su resultante). En BOB: los
 * fondos sembrados y el plan minimo son en bolivianos.
 */
@Component
public class IndicadoresDeGarantia implements CalculadorDeIndicadores {

    /** Desde cuando se acumula: antes no hay operacion (el escenario mas viejo sembrado es de 2025). */
    private static final LocalDate ORIGEN = LocalDate.of(2000, 1, 1);

    private final IndicadoresDeGarantiaRepositorio repositorio;

    public IndicadoresDeGarantia(IndicadoresDeGarantiaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<Calculado> calcular(DSLContext dsl, PeriodoDeTablero periodo) {
        LocalDate desde = periodo.rango().inicio();
        LocalDate hasta = periodo.rango().fin();
        var cubiertoEnPeriodo = repositorio.cubierto(dsl, desde, hasta);
        var recuperadoEnPeriodo = repositorio.recuperado(dsl, desde, hasta);
        var cubiertoAcumulado = repositorio.cubierto(dsl, ORIGEN, hasta);
        var recuperadoAcumulado = repositorio.recuperado(dsl, ORIGEN, hasta);
        var fondos = repositorio.saldoDeLosFondos(dsl, hasta);
        BigDecimal pendiente = cubiertoAcumulado.total().subtract(recuperadoAcumulado.total());

        return List.of(
                dinero("IMPAGOS_CUBIERTOS", "Impagos cubiertos", cubiertoEnPeriodo),
                dinero("PENDIENTES_COBRADOS_RECUPERADO", "Recuperado de lo cubierto", recuperadoEnPeriodo),
                new Calculado(
                        Indicador.publicar(
                                new Definicion("RECUPERACION_CUBIERTO", Unidad.PORCENTAJE, 0, false),
                                new Medicion(
                                        recuperadoAcumulado.total(),
                                        cubiertoAcumulado.total(),
                                        null,
                                        cubiertoAcumulado.casos()),
                                null),
                        "Recuperación de lo cubierto"),
                new Calculado(
                        Indicador.publicar(
                                new Definicion("COBERTURA_FONDO", Unidad.RAZON, 0, false),
                                new Medicion(fondos.total(), pendiente, null, cubiertoAcumulado.casos()),
                                null),
                        "Cobertura del fondo"));
    }

    private static Calculado dinero(String codigo, String nombre, IndicadoresDeGarantiaRepositorio.Suma s) {
        return new Calculado(
                Indicador.publicar(
                        new Definicion(codigo, Unidad.DINERO, 0, false),
                        Medicion.de(Dinero.de(s.total(), Moneda.BOB), s.casos()),
                        null),
                nombre);
    }
}
