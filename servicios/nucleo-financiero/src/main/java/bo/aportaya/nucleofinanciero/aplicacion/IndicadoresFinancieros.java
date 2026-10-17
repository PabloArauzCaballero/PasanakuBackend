package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.IndicadoresDelMayorRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.IndicadoresDelMayorRepositorio.Total;
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

/**
 * Tableros · los indicadores de FINANZAS cuyo dueño es el mayor: saldo cargado, saldo retirado, intereses por mora
 * cobrados y comisiones cobradas. Las definiciones (formula, fuente, minimo de casos) viven en
 * {@code seeders/minimos/23-indicadores-tableros.json}; la privacidad por minimo de casos la aplica CU-98 al publicar.
 *
 * <p>En bolivianos: el plan de cuentas minimo solo tiene cuentas en BOB (ver application.yml, cuentas logicas).
 */
@Component
public class IndicadoresFinancieros implements CalculadorDeIndicadores {

    private static final Moneda MONEDA = Moneda.BOB;

    private final IndicadoresDelMayorRepositorio mayor;

    public IndicadoresFinancieros(IndicadoresDelMayorRepositorio mayor) {
        this.mayor = mayor;
    }

    @Override
    public List<Calculado> calcular(DSLContext dsl, PeriodoDeTablero periodo) {
        var rango = periodo.rango();
        return List.of(
                dinero("SALDO_CARGADO", "Saldo cargado", mayor.porTipoDeTransaccion(dsl, "RECARGA", rango, MONEDA)),
                dinero("SALDO_RETIRADO", "Saldo retirado", mayor.porTipoDeTransaccion(dsl, "RETIRO", rango, MONEDA)),
                dinero(
                        "INTERESES_MORA_COBRADOS",
                        "Intereses por mora cobrados",
                        mayor.porTipoDeTransaccion(dsl, "RECARGO_MORA", rango, MONEDA)),
                dinero("COMISIONES_COBRADAS", "Comisiones cobradas", mayor.ingresosPorComision(dsl, rango, MONEDA)));
    }

    private static Calculado dinero(String codigo, String nombre, Total total) {
        var definicion = new Definicion(codigo, Unidad.DINERO, 0, false);
        return new Calculado(Indicador.publicar(definicion, Medicion.de(total.total(), total.casos()), null), nombre);
    }
}
