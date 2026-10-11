package bo.aportaya.organizador.web;

import bo.aportaya.organizador.dominio.DecisionDeHabilitacion;
import bo.aportaya.organizador.dominio.ExpedienteDeHabilitacion;
import bo.aportaya.organizador.web.generado.modelo.ResumenDeHabilitacion;
import java.util.List;

/**
 * Traduce el expediente de habilitación al contrato. Lo que ve quien postuló no es lo que ve el
 * backoffice: el postulante conoce el resultado y el motivo de cada decisión, no quién la tomó ni la
 * evidencia interna con que se evaluó.
 */
final class MapeoDeHabilitacion {

    private MapeoDeHabilitacion() {}

    static ResumenDeHabilitacion resumen(ExpedienteDeHabilitacion e, boolean backoffice) {
        var salida = new ResumenDeHabilitacion();
        salida.setSolicitudId(e.solicitudId());
        salida.setUsuarioId(e.usuarioId());
        salida.setEstado(ResumenDeHabilitacion.EstadoEnum.fromValue(e.estado()));
        salida.setReputacionAlSolicitar(
                e.reputacionAlSolicitar() == null
                        ? null
                        : e.reputacionAlSolicitar().toPlainString());
        salida.setKycReforzadoId(backoffice ? e.kycReforzadoId() : null);
        salida.setMotivoRechazo(e.motivoRechazo());
        salida.setFechaSolicitud(e.fechaSolicitud());
        salida.setFechaResolucion(e.fechaResolucion());
        salida.setRevision(e.revision());
        if (backoffice) {
            boolean abierta = "PENDIENTE".equals(e.estado()) || "EN_REVISION".equals(e.estado());
            salida.setAcciones(List.of(
                    abierta
                            ? ResumenDeHabilitacion.AccionesEnum.RESOLVER
                            : ResumenDeHabilitacion.AccionesEnum.REVISAR));
        }
        return salida;
    }

    static bo.aportaya.organizador.web.generado.modelo.DecisionDeHabilitacion decision(
            DecisionDeHabilitacion d, boolean nueva, boolean backoffice) {
        var salida = new bo.aportaya.organizador.web.generado.modelo.DecisionDeHabilitacion();
        salida.setId(d.id());
        salida.setSolicitudId(d.solicitudId());
        salida.setFase(bo.aportaya.organizador.web.generado.modelo.DecisionDeHabilitacion.FaseEnum.fromValue(d.fase()));
        salida.setDecision(bo.aportaya.organizador.web.generado.modelo.DecisionDeHabilitacion.DecisionEnum.fromValue(
                d.decision()));
        salida.setActorId(backoffice ? d.actorId() : null);
        salida.setMotivo(d.motivo());
        salida.setRevision(d.revision());
        salida.setOrganizadorId(backoffice ? d.organizadorId() : null);
        salida.setEvidenciaRequisitos(backoffice ? d.evidenciaRequisitos() : null);
        salida.setOcurridaEn(d.ocurridaEn());
        salida.setNueva(nueva);
        return salida;
    }
}
