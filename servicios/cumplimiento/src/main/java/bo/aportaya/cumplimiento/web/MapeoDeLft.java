package bo.aportaya.cumplimiento.web;

import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejasLft;
import bo.aportaya.cumplimiento.web.generado.modelo.AlertaEnBandeja;
import bo.aportaya.cumplimiento.web.generado.modelo.CasoEnBandeja;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeAlertas;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeCasos;

/** De las bandejas de CU-44 al contrato de {@code /cumplimiento}: traduce, no decide. */
final class MapeoDeLft {

    private MapeoDeLft() {}

    static PaginaDeAlertas alertas(ConsultarBandejasLft.PaginaDeAlertas pagina) {
        var salida = new PaginaDeAlertas();
        salida.setItems(pagina.items().stream()
                .map(a -> {
                    var r = new AlertaEnBandeja();
                    r.setAlertaId(a.alertaId());
                    r.setReglaCodigo(a.reglaCodigo());
                    r.setTipologia(a.tipologia());
                    r.setSeveridad(AlertaEnBandeja.SeveridadEnum.fromValue(a.severidad()));
                    r.setEstado(AlertaEnBandeja.EstadoEnum.fromValue(a.estado()));
                    r.setDetectadaEn(a.detectadaEn());
                    r.setAsignadaA(a.asignadaA().orElse(null));
                    r.setCasoId(a.casoId().orElse(null));
                    r.setCerradaEn(a.cerradaEn().orElse(null));
                    return r;
                })
                .toList());
        salida.setProximoFecha(pagina.proximoFecha().orElse(null));
        salida.setProximoId(pagina.proximoId().orElse(null));
        return salida;
    }

    static PaginaDeCasos casos(ConsultarBandejasLft.PaginaDeCasos pagina) {
        var salida = new PaginaDeCasos();
        salida.setItems(pagina.items().stream()
                .map(c -> {
                    var r = new CasoEnBandeja();
                    r.setCasoId(c.casoId());
                    r.setCodigo(c.codigo());
                    r.setOrigen(CasoEnBandeja.OrigenEnum.fromValue(c.origen()));
                    r.setEstado(CasoEnBandeja.EstadoEnum.fromValue(c.estado()));
                    r.setPrioridad(CasoEnBandeja.PrioridadEnum.fromValue(c.prioridad()));
                    r.setAnalistaId(c.analistaId());
                    r.setRevisadoPor(c.revisadoPor().orElse(null));
                    r.setAbiertoEn(c.abiertoEn());
                    r.setPlazoLimite(c.plazoLimite());
                    r.setVencido(c.vencido());
                    r.setDecision(c.decision()
                            .map(CasoEnBandeja.DecisionEnum::fromValue)
                            .orElse(null));
                    r.setCerradoEn(c.cerradoEn().orElse(null));
                    return r;
                })
                .toList());
        salida.setProximoPlazo(pagina.proximoPlazo().orElse(null));
        salida.setProximoId(pagina.proximoId().orElse(null));
        return salida;
    }
}
