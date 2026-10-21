package bo.aportaya.auditoria.web;

import bo.aportaya.auditoria.aplicacion.CU130GobernarFichaDeTabla;
import bo.aportaya.auditoria.aplicacion.ConsultarCatalogoDeDatos;
import bo.aportaya.auditoria.dominio.FichaDeGobierno;
import bo.aportaya.auditoria.dominio.RevisionDeColumna;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoDeDatos;
import bo.aportaya.auditoria.web.generado.modelo.ClasificacionDeTabla;
import bo.aportaya.auditoria.web.generado.modelo.ColumnaDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.ConfianzaDeRevision;
import bo.aportaya.auditoria.web.generado.modelo.ConteoPorEstadoDeRevision;
import bo.aportaya.auditoria.web.generado.modelo.ConteoPorEstadoDeTabla;
import bo.aportaya.auditoria.web.generado.modelo.DecisionDeRevision;
import bo.aportaya.auditoria.web.generado.modelo.EntradaFichaDeGobierno;
import bo.aportaya.auditoria.web.generado.modelo.EntradaRevisionDeColumna;
import bo.aportaya.auditoria.web.generado.modelo.EsquemaDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.EstadoDeRevision;
import bo.aportaya.auditoria.web.generado.modelo.EstadoDeTabla;
import bo.aportaya.auditoria.web.generado.modelo.FilaDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.ModuloDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.OpcionDeRetencion;
import bo.aportaya.auditoria.web.generado.modelo.PaginaDeTablasDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.ResumenDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.TablaDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.TipoDeTabla;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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

    /** Catalogo de datos · H2 — el resumen: cifras, esquemas, modulos y politicas, sin una columna. */
    static ResumenDelCatalogo aContrato(ConsultarCatalogoDeDatos.Resumen r) {
        var porEstado = new ConteoPorEstadoDeTabla(
                r.porEstado().get("ACTIVA"),
                r.porEstado().get("PLANIFICADA"),
                r.porEstado().get("CANDIDATA_A_RETIRO"),
                r.porEstado().get("RETIRADA"),
                r.porEstado().get("DESHABILITADA"));
        var porRevision = new ConteoPorEstadoDeRevision(
                r.porRevision().get("DETECTADA"),
                r.porRevision().get("NECESITA_REVISION"),
                r.porRevision().get("APROBADA"),
                r.porRevision().get("RECHAZADA"));
        return new ResumenDelCatalogo(
                r.tablas(),
                r.tablasDeNegocio(),
                r.tablasDeInfraestructura(),
                r.columnas(),
                r.columnasRevisadas(),
                r.relaciones(),
                r.conDatosPersonales(),
                r.conDatosFinancieros(),
                r.criticasParaAuditoria(),
                r.sinResponsable(),
                porEstado,
                porRevision,
                r.esquemas().stream().map(MapeoDeCatalogo::aContrato).toList(),
                r.modulos().stream()
                        .map(m -> new ModuloDelCatalogo(m.codigo(), m.nombre(), m.tablas()))
                        .toList(),
                r.politicas().stream()
                        .map(p -> new OpcionDeRetencion(
                                p.id(), p.entidad(), p.mesesRetencionActiva(), p.accionAlVencer()))
                        .toList());
    }

    private static EsquemaDelCatalogo aContrato(ConsultarCatalogoDeDatos.Esquema e) {
        var c = new EsquemaDelCatalogo(
                e.nombre(),
                e.descripcion(),
                e.tablas(),
                e.tablasDeNegocio(),
                e.columnas(),
                e.relaciones(),
                e.appendOnly(),
                e.conDatosPersonales());
        e.servicio().ifPresent(c::setServicio);
        return c;
    }

    /** Catalogo de datos · H2 — una pagina de la lista. */
    static PaginaDeTablasDelCatalogo aContrato(ConsultarCatalogoDeDatos.Pagina p) {
        List<FilaDelCatalogo> items =
                p.items().stream().map(MapeoDeCatalogo::aContrato).toList();
        return new PaginaDeTablasDelCatalogo(items, p.pagina(), p.limite(), p.total(), p.totalPaginas());
    }

    private static FilaDelCatalogo aContrato(ConsultarCatalogoDeDatos.Fila f) {
        var c = new FilaDelCatalogo(
                f.tabla(),
                f.esquema(),
                TipoDeTabla.fromValue(f.tipo()),
                f.modulo(),
                f.nombreModulo(),
                f.appendOnly(),
                f.columnas(),
                f.columnasRevisadas(),
                f.relacionesSalientes(),
                f.relacionesEntrantes(),
                aContrato(f.clasificacion()),
                aContrato(f.gobierno()));
        f.servicio().ifPresent(c::setServicio);
        f.queEs().ifPresent(c::setQueEs);
        return c;
    }

    private static ClasificacionDeTabla aContrato(ConsultarCatalogoDeDatos.Clasificacion c) {
        return new ClasificacionDeTabla(c.personales(), c.financieros(), c.secretos(), c.derivados());
    }

    /** Catalogo de datos · H2 — la ficha de una tabla: la del recurso con su gobierno y sus revisiones encima. */
    /** La ficha del recurso pasada por el modelo generado: una tabla que no cumple el contrato falla aca. */
    static TablaDelCatalogo aContrato(ObjectMapper mapper, ConsultarCatalogoDeDatos.Ficha ficha) {
        try {
            return aContrato(mapper.treeToValue(ficha.tabla(), TablaDelCatalogo.class), ficha);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("La tabla del catalogo no cumple TablaDelCatalogo", e);
        }
    }

    static TablaDelCatalogo aContrato(TablaDelCatalogo t, ConsultarCatalogoDeDatos.Ficha ficha) {
        t.setGobierno(aContrato(ficha.gobierno()));
        for (ColumnaDelCatalogo c : t.getColumnas()) {
            RevisionDeColumna r =
                    ficha.revisiones().get(RevisionDeColumna.clave(t.getEsquema(), t.getTabla(), c.getNombre()));
            if (r != null) c.setRevision(aContrato(r));
        }
        return t;
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

    /** CU-131 · superpone a cada columna su revision guardada; sin ella, la columna no se reviso. */
    static CatalogoDeDatos conRevisiones(CatalogoDeDatos catalogo, Map<String, RevisionDeColumna> revisiones) {
        for (TablaDelCatalogo t : catalogo.getTablas()) {
            for (ColumnaDelCatalogo c : t.getColumnas()) {
                RevisionDeColumna r =
                        revisiones.get(RevisionDeColumna.clave(t.getEsquema(), t.getTabla(), c.getNombre()));
                if (r != null) c.setRevision(aContrato(r));
            }
        }
        return catalogo;
    }

    static bo.aportaya.auditoria.web.generado.modelo.RevisionDeColumna aContrato(RevisionDeColumna r) {
        var c = new bo.aportaya.auditoria.web.generado.modelo.RevisionDeColumna(
                DecisionDeRevision.fromValue(r.decision()), r.version());
        r.confianza().map(ConfianzaDeRevision::fromValue).ifPresent(c::setConfianza);
        r.motivo().ifPresent(c::setMotivo);
        r.revisadoPor().ifPresent(c::setRevisadoPor);
        r.revisadoEn().ifPresent(c::setRevisadoEn);
        return c;
    }

    static RevisionDeColumna aDominio(EntradaRevisionDeColumna e) {
        return new RevisionDeColumna(
                e.getDecision().getValue(),
                Optional.ofNullable(e.getConfianza()).map(ConfianzaDeRevision::getValue),
                Optional.ofNullable(e.getMotivo()).map(String::strip).filter(s -> !s.isEmpty()),
                Optional.empty(),
                Optional.empty(),
                e.getVersion());
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
