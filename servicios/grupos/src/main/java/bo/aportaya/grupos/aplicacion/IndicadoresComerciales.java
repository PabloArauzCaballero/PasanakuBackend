package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.infraestructura.IndicadoresComercialesRepositorio;
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
 * Tableros · NEGOCIO de {@code grupos}: activos, paso 3 del embudo, grupos nuevos y completos, dias de llenado,
 * retencion a 60 dias y volumen comprometido. Activos y volumen son foto del estado actual: solo para el periodo que
 * termina ayer; uno mas viejo sale SIN_HISTORIA (no se publica).
 */
@Component
public class IndicadoresComerciales implements CalculadorDeIndicadores {

    private final IndicadoresComercialesRepositorio repositorio;
    private final Reloj reloj;

    public IndicadoresComerciales(IndicadoresComercialesRepositorio repositorio, Reloj reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Override
    public List<Calculado> calcular(DSLContext dsl, PeriodoDeTablero periodo) {
        var desde = periodo.rango().inicio();
        var hasta = periodo.rango().fin();
        boolean vigente = !hasta.isBefore(reloj.hoy().minusDays(1));
        var llenado = repositorio.diasDeLlenado(dsl, desde, hasta);
        var retencion = repositorio.retencion(dsl, desde, hasta);

        Definicion activos = new Definicion("USUARIOS_ACTIVOS", Unidad.CONTEO, 0, false);
        Definicion volumen = new Definicion("VOLUMEN_COMPROMETIDO", Unidad.DINERO, 0, false);
        Indicador activosHoy = vigente
                ? Indicador.publicar(activos, Medicion.conteo(repositorio.activos(dsl)), null)
                : Indicador.sinValor(activos, Motivo.SIN_HISTORIA);
        Indicador volumenHoy;
        if (vigente) {
            var v = repositorio.volumenComprometido(dsl, Moneda.BOB.name());
            volumenHoy = Indicador.publicar(volumen, Medicion.de(Dinero.de(v.total(), Moneda.BOB), v.casos()), null);
        } else {
            volumenHoy = Indicador.sinValor(volumen, Motivo.SIN_HISTORIA);
        }

        return List.of(
                new Calculado(activosHoy, "Usuarios activos"),
                conteo("EMBUDO_EN_GRUPO", "Entraron a su primer grupo", repositorio.primerIngreso(dsl, desde, hasta)),
                conteo("GRUPOS_NUEVOS", "Grupos nuevos", repositorio.creados(dsl, desde, hasta)),
                conteo("GRUPOS_COMPLETOS", "Grupos completos", repositorio.conformados(dsl, desde, hasta)),
                new Calculado(
                        Indicador.publicar(
                                new Definicion("DIAS_LLENADO_GRUPO", Unidad.DIAS, 0, false),
                                new Medicion(llenado.total(), null, null, llenado.casos()),
                                null),
                        "Días hasta llenar un grupo"),
                new Calculado(
                        Indicador.publicar(
                                new Definicion("RETENCION_60D", Unidad.PORCENTAJE, 0, false),
                                Medicion.tasa(retencion.numerador(), retencion.denominador()),
                                null),
                        "Retención a 60 días"),
                new Calculado(volumenHoy, "Volumen comprometido"));
    }

    private static Calculado conteo(String codigo, String nombre, long cuantos) {
        return new Calculado(
                Indicador.publicar(new Definicion(codigo, Unidad.CONTEO, 0, false), Medicion.conteo(cuantos), null),
                nombre);
    }
}
