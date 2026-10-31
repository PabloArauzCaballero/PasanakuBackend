package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU64TraspasoPorSubasta;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.TraspasoAdmisible;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.Dinero;
import bo.aportaya.grupos.web.generado.modelo.ElegibilidadParaSubasta;
import bo.aportaya.grupos.web.generado.modelo.EntradaTraspasoPorSubasta;
import bo.aportaya.grupos.web.generado.modelo.SalidaTraspasoPorSubasta;
import bo.aportaya.grupos.web.generado.modelo.SubastabilidadDelCupo;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/**
 * CU-64 por subasta (2026-10-30): lo que {@code entregas} le pregunta a {@code grupos} para abrir una subasta, aceptar
 * una puja y, al cierre, pasar el cupo al ganador. La subasta y la plata son de {@code entregas}.
 */
abstract class RutasDeSubastaDelCupo extends RutasDelGrupo {
    private final SesionDeLaPeticion sesion;
    private final Consultas consultas;
    private final HechosDeOtrosServicios afuera;
    private CU64TraspasoPorSubasta porSubasta;

    @SuppressWarnings("checkstyle:ParameterNumber")
    protected RutasDeSubastaDelCupo(
            AdmisionDelGrupo admision,
            SesionDeLaPeticion sesion,
            CU68AceptarIngreso cu68Decision,
            InvitacionesWeb enlaces,
            CU69Invitar invitaciones,
            CanjearInvitacion canjes,
            Consultas consultas,
            HechosDeOtrosServicios afuera,
            BigDecimal afinidad,
            CU69EnlaceDeGrupo enlacesDeGrupo) {
        super(
                admision,
                sesion,
                cu68Decision,
                enlaces,
                invitaciones,
                canjes,
                consultas,
                afuera,
                afinidad,
                enlacesDeGrupo);
        this.sesion = sesion;
        this.consultas = consultas;
        this.afuera = afuera;
    }

    /** Por setter, por la misma razon que {@link RutasDelGrupo#conIntegrantes}. */
    @org.springframework.beans.factory.annotation.Autowired
    void conTraspasoPorSubasta(CU64TraspasoPorSubasta porSubasta) {
        this.porSubasta = porSubasta;
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SubastabilidadDelCupo> consultarSubastabilidadDelCupo(UUID grupoId, UUID cupoId) {
        var ctx = sesion.actual();
        Traza.marcarCasoDeUso("CU-64", cupoId.toString());
        var titular = consultas.participanteDe(grupoId, ctx.usuarioId(), ctx);
        // La deuda que se mira es la del titular; si no es titular, no se le pregunta nada a aportes.
        boolean alDia = titular.map(p -> afuera.estadoDePagos(p).alDia()).orElse(false);
        var s = porSubasta.subastabilidad(grupoId, cupoId, titular, alDia, ctx);

        var respuesta = new SubastabilidadDelCupo();
        respuesta.setGrupoId(s.grupoId());
        respuesta.setCupoId(s.cupoId());
        respuesta.setParticipanteId(s.participanteId());
        respuesta.setNombreDelGrupo(s.nombreDelGrupo());
        respuesta.setPeriodicidad(s.periodicidad());
        respuesta.setSubastable(s.impedimento().isEmpty());
        respuesta.setMotivo(s.impedimento()
                .map(i -> SubastabilidadDelCupo.MotivoEnum.fromValue(i.name()))
                .orElse(null));
        var aporte = new Dinero();
        aporte.setMonto(s.montoAporte().setScale(2, RoundingMode.UNNECESSARY).toPlainString());
        aporte.setMoneda(Dinero.MonedaEnum.fromValue(s.moneda()));
        respuesta.setMontoAporte(aporte);
        return ResponseEntity.ok(respuesta);
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<ElegibilidadParaSubasta> consultarElegibilidadParaSubasta(UUID grupoId) {
        var ctx = sesion.actual();
        Traza.marcarCasoDeUso("CU-64", grupoId.toString());
        var politica = consultas
                .politicaDelGrupo(grupoId, ctx)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(68, 4), "Ese grupo no existe."));
        // La etapa minima y el desbloqueo, con la misma regla que cualquier ingreso (antes de retener la plata).
        boolean alcanza =
                EtapaDelIngreso.alcanzaParaPujar(afuera, consultas.condicionesDeIngreso(grupoId, ctx), ctx.usuarioId());
        var motivo = porSubasta.elegibilidad(
                grupoId,
                afuera.restriccion(ctx.usuarioId()).vigente(),
                TraspasoAdmisible.NivelDeKyc.suficiente(afuera.nivelDeKyc(ctx.usuarioId()), politica.kycMinimo()),
                alcanza,
                ctx);

        var respuesta = new ElegibilidadParaSubasta();
        respuesta.setElegible(motivo.isEmpty());
        respuesta.setMotivo(motivo.map(m -> ElegibilidadParaSubasta.MotivoEnum.fromValue(m.name()))
                .orElse(null));
        return ResponseEntity.ok(respuesta);
    }

    @Override
    @Permiso("SERVICIO_ENTREGAS")
    public ResponseEntity<SalidaTraspasoPorSubasta> traspasarCupoPorSubasta(
            UUID grupoId, UUID cupoId, UUID idempotencyKey, EntradaTraspasoPorSubasta cuerpo) {
        Traza.marcarCasoDeUso("CU-64", cupoId.toString());
        var t = porSubasta.traspasar(
                grupoId,
                cupoId,
                cuerpo.getSubastaId(),
                cuerpo.getGanadorUsuarioId(),
                cuerpo.getSalienteParticipanteId(),
                sesion.actual());

        var respuesta = new SalidaTraspasoPorSubasta();
        respuesta.setTraspasoId(t.traspasoId());
        respuesta.setParticipanteEntranteId(t.participanteEntranteId());
        return ResponseEntity.ok(respuesta);
    }
}
