package bo.aportaya.tarifas.web;

import bo.aportaya.tarifas.aplicacion.CU132RegistroDeParametros;
import bo.aportaya.tarifas.dominio.VersionDelParametro;
import bo.aportaya.tarifas.web.generado.modelo.ParametroFinanciero;
import bo.aportaya.tarifas.web.generado.modelo.VersionDeParametro;

/**
 * Del registro de parámetros al contrato de {@code /tarifas/parametros}. Las identidades que viajan son las de los
 * operadores internos que firmaron (propuso / aprobó), como UUID: el mismo patrón que {@code aprobadoPor} del
 * tarifario. Nunca un nombre ni un dato de un participante.
 */
final class MapeoDeParametros {

    private MapeoDeParametros() {}

    static ParametroFinanciero parametro(CU132RegistroDeParametros.EnCatalogo e) {
        var catalogo = e.parametro().catalogo();
        var r = new ParametroFinanciero();
        r.setCodigo(catalogo.codigo());
        r.setDescripcion(e.parametro().descripcion());
        r.setUnidad(
                catalogo.unidad().map(ParametroFinanciero.UnidadEnum::fromValue).orElse(null));
        r.setPorNivel(catalogo.porNivel());
        r.setVigentes(e.vigentes().stream().map(MapeoDeParametros::version).toList());
        r.setPropuestas(e.propuestas().stream().map(MapeoDeParametros::version).toList());
        return r;
    }

    static VersionDeParametro version(VersionDelParametro v) {
        var r = new VersionDeParametro();
        r.setId(v.id());
        r.setCodigo(v.codigo());
        r.setNivel(v.nivel().orElse(null));
        // Cadena decimal exacta: nunca number (regla 91.1.1).
        r.setValor(v.valor().toPlainString());
        r.setMoneda(v.moneda().orElse(null));
        r.setVigenteDesde(v.vigenteDesde());
        r.setEstado(VersionDeParametro.EstadoEnum.fromValue(v.estado()));
        r.setPropuestoPor(v.propuestoPor());
        // Nulo mientras espera la segunda firma o si se rechazó sin ella (R-TAR-14).
        r.setAprobadoPor(v.aprobadoPor().orElse(null));
        return r;
    }
}
