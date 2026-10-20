package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.CalendarioDeAportes;
import bo.aportaya.grupos.web.generado.modelo.EntradaDecisionDeIngreso;
import bo.aportaya.grupos.web.generado.modelo.Participacion;
import bo.aportaya.grupos.web.generado.modelo.SalidaDecisionDeIngreso;
import bo.aportaya.grupos.web.generado.modelo.SolicitudDeIngreso;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

/** CU-68, camino directo del organizador: la cola de solicitudes pendientes y su decision, y mis participaciones. */
abstract class RutasDeSolicitudes extends RutasDeAdmision {
    private final CU68AceptarIngreso cu68Decision;
    private final Consultas consultas;
    private final HechosDeOtrosServicios afuera;
    private final SesionDeLaPeticion sesion;

    /** Interruptor de produccion; por campo para no ensanchar la cadena de constructores de los controladores. */
    @Value("${aportaya.grupos.admision.humana-obligatoria}")
    private boolean admisionHumanaObligatoria;

    protected RutasDeSolicitudes(
            AdmisionDelGrupo admision,
            SesionDeLaPeticion sesion,
            CU68AceptarIngreso cu68Decision,
            Consultas consultas,
            HechosDeOtrosServicios afuera) {
        super(admision, sesion);
        this.cu68Decision = cu68Decision;
        this.consultas = consultas;
        this.afuera = afuera;
        this.sesion = sesion;
    }

    /**
     * H8.S1.M1: el hecho que `aportes` pide para generar las obligaciones de un periodo. Lo dispara quien opera
     * la plataforma y su token viaja en la llamada entre servicios, asi que el permiso es el suyo; un
     * participante no lo ve aunque sea titular de un cupo, porque enumera a todos los titulares.
     */
    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<CalendarioDeAportes> consultarCalendarioDeAportes(UUID periodoId) {
        Traza.marcarCasoDeUso("CU-21", periodoId.toString());
        var calendario = consultas
                .calendarioDeAportes(periodoId, sesion.actual())
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(21, 8), "Ese periodo no existe."));
        return ResponseEntity.ok(MapeoDeCalendario.aContrato(calendario));
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<java.util.List<Participacion>> listarMisParticipaciones() {
        Traza.marcarCasoDeUso("CU-68", sesion.actual().usuarioId().toString());
        var lista = consultas.participacionesDe(sesion.actual()).stream()
                .map(p -> {
                    var o = new Participacion();
                    o.setGrupoId(p.grupoId());
                    o.setParticipanteId(p.participanteId());
                    o.setEstado(p.estado());
                    o.setNombreDelGrupo(p.nombreDelGrupo());
                    o.setMontoAporte(p.montoAporte());
                    o.setMoneda(p.moneda());
                    o.setEsOrganizador(p.esOrganizador());
                    return o;
                })
                .toList();
        return ResponseEntity.ok(lista);
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<java.util.List<SolicitudDeIngreso>> listarSolicitudesDeIngreso(UUID grupoId) {
        Traza.marcarCasoDeUso("CU-68", grupoId.toString());
        var lista = cu68Decision.pendientes(grupoId, sesion.actual()).stream()
                .map(s -> {
                    var o = new SolicitudDeIngreso();
                    o.setSolicitudId(s.id());
                    o.setUsuarioId(s.usuarioId());
                    o.setCuposSolicitados(s.cuposSolicitados());
                    o.setMensaje(s.mensaje());
                    o.setPuntaje(s.puntaje() == null ? null : s.puntaje().toPlainString());
                    o.setEstado(SolicitudDeIngreso.EstadoEnum.fromValue(s.estado()));
                    o.setFechaSolicitud(s.fecha());
                    o.setNombreVisible(s.nombreVisible());
                    o.setTelefono(s.telefono());
                    o.setCorreo(s.correo());
                    o.setPorEnlace(s.porEnlace());
                    return o;
                })
                .toList();
        return ResponseEntity.ok(lista);
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<SalidaDecisionDeIngreso> decidirSolicitudDeIngreso(
            UUID solicitudId, UUID idempotencyKey, EntradaDecisionDeIngreso cuerpo) {
        Traza.marcarCasoDeUso("CU-68", solicitudId.toString());
        if (admisionHumanaObligatoria) {
            // Interruptor aportaya.grupos.admision.humana-obligatoria: el organizador decidiendo solo saltea a
            // backoffice. Su decision es una PROPUESTA (proponerAdmision) que resuelve backoffice (resolverAdmision).
            throw new ErrorDeNegocio(
                    CodigoError.de(68, 15),
                    "Este grupo exige admision humana: tu decision es una propuesta (proponerAdmision) y la resuelve backoffice.");
        }
        var ctx = sesion.actual();
        // Primero se comprueba que quien decide es el organizador y se sabe quien pidio entrar; la
        // reputacion se pregunta afuera, ANTES de abrir la transaccion de decidir (invariante 6).
        var solicitante = cu68Decision.solicitante(solicitudId, ctx);
        var reputacion = afuera.reputacion(solicitante).puntaje();
        var r = cu68Decision.decidir(
                solicitudId,
                cuerpo.getDecision() == EntradaDecisionDeIngreso.DecisionEnum.ACEPTAR,
                cuerpo.getMotivo(),
                reputacion,
                ctx);
        var salida = new SalidaDecisionDeIngreso();
        salida.setSolicitudId(r.solicitudId());
        salida.setEstado(SalidaDecisionDeIngreso.EstadoEnum.fromValue(r.estado()));
        salida.setParticipanteId(r.participanteId());
        salida.setCupoId(r.cupoId());
        return ResponseEntity.ok(salida);
    }
}
