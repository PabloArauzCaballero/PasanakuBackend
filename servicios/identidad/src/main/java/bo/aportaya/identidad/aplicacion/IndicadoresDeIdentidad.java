package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.IndicadoresDeIdentidadRepositorio;
import bo.aportaya.plataforma.dominio.Indicador;
import bo.aportaya.plataforma.dominio.Indicador.Definicion;
import bo.aportaya.plataforma.dominio.Indicador.Medicion;
import bo.aportaya.plataforma.dominio.Indicador.Unidad;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.mensajeria.CalculadorDeIndicadores;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/** Tableros · NEGOCIO: usuarios nuevos (paso 1 del embudo) y KYC aprobados (paso 2), por fecha del hecho. */
@Component
public class IndicadoresDeIdentidad implements CalculadorDeIndicadores {

    private final IndicadoresDeIdentidadRepositorio repositorio;

    public IndicadoresDeIdentidad(IndicadoresDeIdentidadRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<Calculado> calcular(DSLContext dsl, PeriodoDeTablero periodo) {
        var desde = periodo.rango().inicio();
        var hasta = periodo.rango().fin();
        return List.of(
                conteo("USUARIOS_NUEVOS", "Usuarios nuevos", repositorio.registrados(dsl, desde, hasta)),
                conteo("EMBUDO_KYC_APROBADO", "KYC aprobados", repositorio.kycAprobados(dsl, desde, hasta)));
    }

    private static Calculado conteo(String codigo, String nombre, long cuantos) {
        return new Calculado(
                Indicador.publicar(new Definicion(codigo, Unidad.CONTEO, 0, false), Medicion.conteo(cuantos), null),
                nombre);
    }
}
