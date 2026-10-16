package bo.aportaya.notificaciones.web;

import bo.aportaya.notificaciones.aplicacion.ConsultarMensajeria;
import bo.aportaya.notificaciones.web.generado.modelo.AcuseDeEntrega;
import bo.aportaya.notificaciones.web.generado.modelo.EnvioEnLista;
import bo.aportaya.notificaciones.web.generado.modelo.PaginaDeEnvios;
import bo.aportaya.notificaciones.web.generado.modelo.PaginaDePlantillas;
import bo.aportaya.notificaciones.web.generado.modelo.PlantillaEnLista;

/** De la auditoria de mensajeria de la aplicacion al contrato de plantillas y envios. */
final class MapeoDeMensajeria {

    private MapeoDeMensajeria() {}

    static PaginaDePlantillas plantillas(ConsultarMensajeria.Pagina<ConsultarMensajeria.Plantilla> pagina) {
        var salida = new PaginaDePlantillas();
        salida.setItems(
                pagina.items().stream().map(MapeoDeMensajeria::plantilla).toList());
        salida.setProximoAntesDe(pagina.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(pagina.proximoAntesDeId().orElse(null));
        return salida;
    }

    static PaginaDeEnvios envios(ConsultarMensajeria.Pagina<ConsultarMensajeria.Envio> pagina) {
        var salida = new PaginaDeEnvios();
        salida.setItems(pagina.items().stream().map(MapeoDeMensajeria::envio).toList());
        salida.setProximoAntesDe(pagina.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(pagina.proximoAntesDeId().orElse(null));
        return salida;
    }

    private static PlantillaEnLista plantilla(ConsultarMensajeria.Plantilla p) {
        var r = new PlantillaEnLista();
        r.setPlantillaId(p.plantillaId());
        r.setCodigo(p.codigo());
        r.setEvento(p.evento());
        r.setCanal(PlantillaEnLista.CanalEnum.fromValue(p.canal()));
        r.setCategoriaProveedor(PlantillaEnLista.CategoriaProveedorEnum.fromValue(p.categoriaProveedor()));
        r.setEstadoAprobacion(PlantillaEnLista.EstadoAprobacionEnum.fromValue(p.estadoAprobacion()));
        r.setActiva(p.activa());
        r.setVersionId(p.versionId());
        r.setVersion(p.version());
        r.setIdioma(p.idioma());
        r.setVigenteDesde(p.vigenteDesde());
        r.setVigenteHasta(p.vigenteHasta().orElse(null));
        return r;
    }

    private static EnvioEnLista envio(ConsultarMensajeria.Envio e) {
        var r = new EnvioEnLista();
        r.setEnvioId(e.envioId());
        r.setNotificacionId(e.notificacionId());
        r.setProveedorId(e.proveedorId());
        r.setProveedor(e.proveedor());
        r.setVersionPlantillaId(e.versionPlantillaId());
        r.setCanal(EnvioEnLista.CanalEnum.fromValue(e.canal()));
        r.setEstado(EnvioEnLista.EstadoEnum.fromValue(e.estado()));
        r.setOrden(e.orden());
        r.setIntentos(e.intentos());
        r.setMaxIntentos(e.maxIntentos());
        r.setCosto(e.costo().toPlainString());
        r.setMoneda(e.moneda());
        r.setCodigoError(e.codigoError().orElse(null));
        r.setEncoladoEn(e.encoladoEn());
        r.setEnviadoEn(e.enviadoEn().orElse(null));
        r.setEntregadoEn(e.entregadoEn().orElse(null));
        r.setLeidoEn(e.leidoEn().orElse(null));
        r.setProximoReintentoEn(e.proximoReintentoEn().orElse(null));
        r.setUltimoAcuse(e.ultimoAcuse().map(MapeoDeMensajeria::acuse).orElse(null));
        return r;
    }

    private static AcuseDeEntrega acuse(ConsultarMensajeria.Acuse a) {
        var r = new AcuseDeEntrega();
        r.setTipoEvento(AcuseDeEntrega.TipoEventoEnum.fromValue(a.tipoEvento()));
        r.setFechaHoraProveedor(a.fechaHoraProveedor());
        r.setRecibidoEn(a.recibidoEn());
        r.setCodigoError(a.codigoError().orElse(null));
        return r;
    }
}
