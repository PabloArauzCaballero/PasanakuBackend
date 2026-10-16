package bo.aportaya.cumplimiento.web;

import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejaDeReclamos;
import bo.aportaya.cumplimiento.web.generado.modelo.FichaDeReclamo;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeReclamos;
import bo.aportaya.cumplimiento.web.generado.modelo.ReclamoEnBandeja;

/** De los registros de CU-52 al contrato de {@code /reclamos}: traduce, no decide. */
final class MapeoDeReclamos {

    private MapeoDeReclamos() {}

    static PaginaDeReclamos pagina(ConsultarBandejaDeReclamos.Pagina pagina) {
        var salida = new PaginaDeReclamos();
        salida.setItems(pagina.items().stream().map(MapeoDeReclamos::fila).toList());
        salida.setProximoLimite(pagina.proximoLimite().orElse(null));
        salida.setProximoId(pagina.proximoId().orElse(null));
        return salida;
    }

    static FichaDeReclamo ficha(ConsultarBandejaDeReclamos.Ficha ficha) {
        var salida = new FichaDeReclamo();
        salida.setReclamo(fila(ficha.reclamo()));
        salida.setDescripcion(ficha.descripcion());
        salida.setResultado(
                ficha.resultado().map(FichaDeReclamo.ResultadoEnum::fromValue).orElse(null));
        salida.setRespuesta(ficha.respuesta().orElse(null));
        salida.setFechaRespuesta(ficha.fechaRespuesta().orElse(null));
        salida.setDevolucionId(ficha.devolucionId().orElse(null));
        return salida;
    }

    private static ReclamoEnBandeja fila(ConsultarBandejaDeReclamos.Fila f) {
        var r = new ReclamoEnBandeja();
        r.setReclamoId(f.reclamoId());
        r.setCodigo(f.codigo());
        r.setCategoria(ReclamoEnBandeja.CategoriaEnum.fromValue(f.categoria()));
        r.setProducto(f.producto());
        r.setCanalIngreso(ReclamoEnBandeja.CanalIngresoEnum.fromValue(f.canalIngreso()));
        r.setMontoReclamado(f.montoReclamado().orElse(null));
        r.setEstado(ReclamoEnBandeja.EstadoEnum.fromValue(f.estado()));
        r.setFechaIngreso(f.fechaIngreso());
        r.setPlazoRespuesta(f.plazoRespuesta());
        r.setPlazoProrrogadoHasta(f.plazoProrrogadoHasta().orElse(null));
        r.setDiasHabilesPlazo(f.diasHabilesPlazo());
        r.setLimiteVigente(f.limiteVigente());
        r.setVencido(f.vencido());
        r.setResponsableId(f.responsableId().orElse(null));
        return r;
    }
}
