package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.SustituirAdministrador;
import bo.aportaya.grupos.dominio.DecisionDeIngreso;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.DecisionIngreso;
import bo.aportaya.grupos.web.generado.modelo.EntradaDecisionIngreso;
import bo.aportaya.grupos.web.generado.modelo.EntradaSustitucionAdministrador;
import bo.aportaya.grupos.web.generado.modelo.SustitucionAdministrador;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Mapea los contratos públicos del expediente humano. */
@Component
public class AdmisionDelGrupo {
    private final CU68AceptarIngreso admision;
    private final SustituirAdministrador sustitucion;
    private final HechosDeOtrosServicios afuera;

    public AdmisionDelGrupo(
            CU68AceptarIngreso admision, SustituirAdministrador sustitucion, HechosDeOtrosServicios afuera) {
        this.admision = admision;
        this.sustitucion = sustitucion;
        this.afuera = afuera;
    }

    /**
     * Sustituye al administrador. Que el entrante sea organizador habilitado se pregunta ANTES de la transaccion
     * (invariante 6) a quien lo sabe; sin respuesta afirmativa no se sustituye (denegar por omision).
     */
    public SustitucionAdministrador sustituir(
            UUID grupoId, UUID clave, EntradaSustitucionAdministrador cuerpo, ContextoSesion ctx) {
        if (afuera.organizadorHabilitadoDelUsuario(cuerpo.getNuevoAdministradorId())
                .isEmpty()) {
            throw new bo.aportaya.plataforma.dominio.ErrorDeNegocio(
                    bo.aportaya.plataforma.dominio.CodigoError.de(68, 14),
                    "El nuevo administrador debe estar habilitado como organizador.");
        }
        var s = sustitucion.ejecutar(
                new SustituirAdministrador.Entrada(
                        grupoId, cuerpo.getNuevoAdministradorId(), clave, cuerpo.getMotivo()),
                ctx);
        var salida = new SustitucionAdministrador();
        salida.setId(s.id());
        salida.setGrupoId(s.grupoId());
        salida.setSalienteParticipanteId(s.salienteParticipanteId());
        salida.setEntranteParticipanteId(s.entranteParticipanteId());
        salida.setMotivo(s.motivo());
        salida.setObligacionesConservadas(s.obligacionesConservadas());
        salida.setOcurridaEn(s.ocurridaEn());
        return salida;
    }

    public DecisionIngreso decidir(
            UUID solicitud, UUID clave, EntradaDecisionIngreso cuerpo, ContextoSesion ctx, boolean resolver) {
        var entrada = new CU68AceptarIngreso.Entrada(
                solicitud,
                clave,
                cuerpo.getDecision().getValue(),
                cuerpo.getMotivo(),
                cuerpo.getRevisionEsperada(),
                cuerpo.getPropuestaId());
        return salida(resolver ? admision.resolver(entrada, ctx) : admision.proponer(entrada, ctx));
    }

    public List<DecisionIngreso> historial(UUID solicitud, ContextoSesion ctx) {
        return admision.historial(solicitud, ctx).stream()
                .map(AdmisionDelGrupo::salida)
                .toList();
    }

    private static DecisionIngreso salida(DecisionDeIngreso decision) {
        var salida = new DecisionIngreso();
        salida.setId(decision.id());
        salida.setSolicitudId(decision.solicitudId());
        salida.setFase(decision.fase());
        salida.setDecision(decision.decision());
        salida.setActorId(decision.actorId());
        salida.setMotivo(decision.motivo());
        salida.setPropuestaId(decision.propuestaId());
        salida.setParticipanteId(decision.participanteId());
        salida.setRevision(decision.revision());
        salida.setEvidenciaAlgoritmo(decision.evidenciaAlgoritmo());
        salida.setVersionMotor(decision.versionMotor());
        salida.setRecomendacionAlgoritmo(
                decision.recomendacionAlgoritmo() == null
                        ? null
                        : DecisionIngreso.RecomendacionAlgoritmoEnum.fromValue(decision.recomendacionAlgoritmo()));
        salida.setApartamiento(decision.apartamiento());
        salida.setOcurridaEn(decision.ocurridaEn());
        salida.setCorrelacionId(decision.correlacionId());
        return salida;
    }
}
