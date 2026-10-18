package bo.aportaya.auditoria.web;

import bo.aportaya.auditoria.aplicacion.CU130GobernarFichaDeTabla;
import bo.aportaya.auditoria.dominio.FichaDeGobierno;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoDeDatos;
import bo.aportaya.auditoria.web.generado.modelo.EntradaFichaDeGobierno;
import bo.aportaya.auditoria.web.generado.modelo.EstadoDeRevision;
import bo.aportaya.auditoria.web.generado.modelo.EstadoDeTabla;
import bo.aportaya.auditoria.web.generado.modelo.OpcionDeRetencion;
import bo.aportaya.auditoria.web.generado.modelo.TablaDelCatalogo;
import java.util.Optional;

/** CU-130 · traduce la ficha de gobierno entre el contrato y el dominio. Sin decisiones. */
final class MapeoDeCatalogo {

    private MapeoDeCatalogo() {}

    /** Superpone a cada tabla su ficha guardada, o la de omision, y agrega las politicas de retencion. */
    static CatalogoDeDatos conGobierno(CatalogoDeDatos catalogo, CU130GobernarFichaDeTabla.Vista vista) {
        for (TablaDelCatalogo t : catalogo.getTablas()) {
            FichaDeGobierno ficha = vista.fichas()
                    .getOrDefault(FichaDeGobierno.clave(t.getEsquema(), t.getTabla()), FichaDeGobierno.porOmision());
            t.setGobierno(aContrato(ficha));
        }
        catalogo.setPoliticasDeRetencion(vista.politicas().stream()
                .map(p -> new OpcionDeRetencion(p.id(), p.entidad(), p.mesesRetencionActiva(), p.accionAlVencer()))
                .toList());
        return catalogo;
    }

    static bo.aportaya.auditoria.web.generado.modelo.FichaDeGobierno aContrato(FichaDeGobierno f) {
        var c = new bo.aportaya.auditoria.web.generado.modelo.FichaDeGobierno(
                EstadoDeTabla.fromValue(f.estado()),
                EstadoDeRevision.fromValue(f.estadoRevision()),
                f.criticaParaAuditoria(),
                f.version());
        f.responsable().ifPresent(c::setResponsable);
        f.politicaRetencionId().ifPresent(c::setPoliticaRetencionId);
        f.notaRevision().ifPresent(c::setNotaRevision);
        f.revisadoPor().ifPresent(c::setRevisadoPor);
        f.actualizadoEn().ifPresent(c::setActualizadoEn);
        return c;
    }

    static FichaDeGobierno aDominio(EntradaFichaDeGobierno e) {
        return new FichaDeGobierno(
                e.getEstado().getValue(),
                e.getEstadoRevision().getValue(),
                Optional.ofNullable(e.getResponsable()).map(String::strip).filter(s -> !s.isEmpty()),
                Optional.ofNullable(e.getPoliticaRetencionId()),
                Boolean.TRUE.equals(e.getCriticaParaAuditoria()),
                Optional.ofNullable(e.getNotaRevision()).map(String::strip).filter(s -> !s.isEmpty()),
                Optional.empty(),
                Optional.empty(),
                e.getVersion());
    }
}
