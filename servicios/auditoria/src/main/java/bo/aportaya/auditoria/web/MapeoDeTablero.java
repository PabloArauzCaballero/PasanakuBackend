package bo.aportaya.auditoria.web;

import bo.aportaya.auditoria.aplicacion.CU98PublicarTablero;
import bo.aportaya.auditoria.web.generado.modelo.Indicador;
import bo.aportaya.auditoria.web.generado.modelo.IndicadorSerieInner;
import bo.aportaya.auditoria.web.generado.modelo.SalidaTablero;
import java.math.BigDecimal;
import java.util.List;

/**
 * CU-98 · del resultado del caso de uso al contrato, sin logica. Una sola regla importa: **el valor sale como
 * cadena, con la escala que trae de la base**. Pasarlo a `number` lo convertiria en un doble en el navegador, y el
 * numero del tablero dejaria de ser el numero de la tabla.
 */
final class MapeoDeTablero {

    private MapeoDeTablero() {}

    static SalidaTablero aContrato(CU98PublicarTablero.SalidaTablero salida) {
        SalidaTablero cuerpo = new SalidaTablero(
                salida.periodo(),
                salida.provisorio(),
                SalidaTablero.DimensionEnum.fromValue(salida.dimension()),
                salida.indicadores().stream().map(MapeoDeTablero::aContrato).toList());
        salida.dimensionId().ifPresent(cuerpo::setDimensionId);
        return cuerpo;
    }

    private static Indicador aContrato(CU98PublicarTablero.Indicador indicador) {
        Indicador cuerpo = new Indicador(
                indicador.codigo(),
                indicador.nombre(),
                indicador.unidad(),
                Indicador.FamiliaEnum.fromValue(indicador.familia()),
                indicador.duenoFamilia(),
                indicador.definicionVersion(),
                indicador.suprimidoPorPrivacidad(),
                indicador.calculadoEn());

        indicador.valor().map(MapeoDeTablero::comoCadena).ifPresent(cuerpo::setValor);
        indicador.casos().ifPresent(cuerpo::setCasos);
        cuerpo.setMinimoCasos(indicador.minimoCasos());
        indicador.meta().map(MapeoDeTablero::comoCadena).ifPresent(cuerpo::setMeta);
        indicador.cumpleMeta().ifPresent(cuerpo::setCumpleMeta);
        indicador
                .variacionPeriodoAnterior()
                .map(MapeoDeTablero::comoCadena)
                .ifPresent(cuerpo::setVariacionPeriodoAnterior);
        cuerpo.setSerie(mapearSerie(indicador.serie()));
        return cuerpo;
    }

    private static List<IndicadorSerieInner> mapearSerie(List<CU98PublicarTablero.PuntoDeSerie> serie) {
        return serie.stream()
                .map(punto -> new IndicadorSerieInner(punto.periodo(), comoCadena(punto.valor())))
                .toList();
    }

    /**
     * `toPlainString` y no `toString`: para un valor chico o muy grande, `toString`
     * usa notacion cientifica —`1E+3`— y en un tablero eso no se lee, se adivina.
     */
    private static String comoCadena(BigDecimal valor) {
        return valor.toPlainString();
    }
}
