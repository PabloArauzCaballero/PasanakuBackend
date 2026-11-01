package bo.aportaya.auditoria.web;

import bo.aportaya.auditoria.aplicacion.ConsultarHistorialDeReportes;
import bo.aportaya.auditoria.web.generado.modelo.DefinicionDeReporteEnLista;
import bo.aportaya.auditoria.web.generado.modelo.DefinicionDeReporteEnListaParametrosEsperadosInner;
import bo.aportaya.auditoria.web.generado.modelo.EjecucionDeReporteEnLista;

/** Del catalogo y el historial de reportes (CU-58) al contrato. */
final class MapeoDeReportes {

    private MapeoDeReportes() {}

    static DefinicionDeReporteEnLista definicion(ConsultarHistorialDeReportes.Definicion d) {
        var r = new DefinicionDeReporteEnLista();
        r.setDefinicionId(d.definicionId());
        r.setTipo(d.tipo());
        r.setNombre(d.nombre());
        r.setDescripcion(d.descripcion());
        r.setPermisoRequerido(d.permisoRequerido());
        r.setContieneDatosSensibles(d.sensible());
        r.setParametrosEsperados(d.esperados().stream()
                .map(e -> new DefinicionDeReporteEnListaParametrosEsperadosInner(e.nombre(), e.tipo(), e.obligatorio()))
                .toList());
        return r;
    }

    static EjecucionDeReporteEnLista ejecucion(ConsultarHistorialDeReportes.Ejecucion e) {
        var r = new EjecucionDeReporteEnLista();
        r.setEjecucionId(e.ejecucionId());
        r.setDefinicionId(e.definicionId());
        r.setSolicitadoPor(e.solicitadoPor());
        r.setEstado(EjecucionDeReporteEnLista.EstadoEnum.fromValue(e.estado()));
        r.setFilasGeneradas(e.filas());
        r.setDuracionMs(e.duracionMs());
        r.setHashResultado(e.hash().orElse(null));
        r.setIniciadaEn(e.iniciadaEn());
        r.setFinalizadaEn(e.finalizadaEn().orElse(null));
        return r;
    }
}
