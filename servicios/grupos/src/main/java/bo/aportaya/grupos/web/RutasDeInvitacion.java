package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.CanjeInvitacion;
import bo.aportaya.grupos.web.generado.modelo.DetalleEnlaceInvitacion;
import bo.aportaya.grupos.web.generado.modelo.EntradaAceptacionInvitacion;
import bo.aportaya.grupos.web.generado.modelo.EntradaEnlaceInvitacion;
import bo.aportaya.grupos.web.generado.modelo.EntradaInvitacion;
import bo.aportaya.grupos.web.generado.modelo.EntradaPostulacion;
import bo.aportaya.grupos.web.generado.modelo.SalidaAceptacionInvitacion;
import bo.aportaya.grupos.web.generado.modelo.SalidaCanjeInvitacion;
import bo.aportaya.grupos.web.generado.modelo.SalidaInvitacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/** Resuelve identidad fuera de la transacción; el canje local es recuperable con la misma clave. */
abstract class RutasDeInvitacion extends RutasDeSolicitudes {
    private final InvitacionesWeb enlaces;
    private final CU69Invitar invitaciones;
    private final CanjearInvitacion canjes;
    private final Consultas consultas;
    private final HechosDeOtrosServicios afuera;
    private final SesionDeLaPeticion sesion;
    private final BigDecimal afinidad;

    protected RutasDeInvitacion(
            AdmisionDelGrupo admision,
            SesionDeLaPeticion sesion,
            CU68AceptarIngreso cu68Decision,
            InvitacionesWeb enlaces,
            CU69Invitar invitaciones,
            CanjearInvitacion canjes,
            Consultas consultas,
            HechosDeOtrosServicios afuera,
            BigDecimal afinidad) {
        super(admision, sesion, cu68Decision, consultas, afuera);
        this.enlaces = enlaces;
        this.invitaciones = invitaciones;
        this.canjes = canjes;
        this.consultas = consultas;
        this.afuera = afuera;
        this.sesion = sesion;
        this.afinidad = afinidad;
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<SalidaInvitacion> invitarAlGrupo(
            UUID grupoId, UUID idempotencyKey, EntradaInvitacion cuerpo) {
        // El secreto viaja en el cuerpo: que ningun intermediario lo guarde.
        return ResponseEntity.status(201)
                .header("Cache-Control", "no-store")
                .body(enlaces.invitar(grupoId, idempotencyKey, cuerpo));
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<DetalleEnlaceInvitacion> consultarInvitacionPorEnlace(EntradaEnlaceInvitacion cuerpo) {
        return ResponseEntity.ok(enlaces.consultar(cuerpo));
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaAceptacionInvitacion> aceptarInvitacionPorEnlace(
            UUID idempotencyKey, EntradaAceptacionInvitacion cuerpo) {
        return ResponseEntity.status(201).body(enlaces.aceptar(cuerpo));
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<Void> revocarInvitacionDelGrupo(UUID grupoId, UUID invitacionId) {
        var ctx = sesion.actual();
        var invitacion = invitaciones.consultar(invitacionId, ctx);
        if (!grupoId.equals(invitacion.grupoId()) || !ctx.usuarioId().equals(invitacion.emisorId()))
            throw new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion ya no es valida.");
        // Primero el enlace (lo que importa para la seguridad, idempotente y fuera de la transaccion);
        // despues el estado local. Si lo segundo falla, reintentar completa lo que falta.
        if (!afuera.revocarTokenDeInvitacion(invitacion.tokenId()))
            throw new ErrorDeNegocio(CodigoError.de(69, 5), "No se pudo revocar esa invitacion.");
        invitaciones.revocar(invitacionId, ctx);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaCanjeInvitacion> canjearInvitacion(
            UUID grupoId, UUID invitacionId, UUID idempotencyKey, CanjeInvitacion cuerpo) {
        var ctx = sesion.actual();
        var invitacion = invitaciones.consultar(invitacionId, ctx);
        if (!grupoId.equals(invitacion.grupoId()))
            throw new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion ya no es valida.");
        var postulacion = new EntradaPostulacion(cuerpo.getCuposSolicitados()).mensaje(cuerpo.getMensaje());
        var entrada = MapeoDePostulacion.entrada(grupoId, postulacion, consultas, afuera, afinidad, ctx);
        var recibo = afuera.consumirInvitacion(invitacion.tokenId(), grupoId, idempotencyKey, cuerpo.getToken());
        if (!idempotencyKey.equals(recibo.clave()))
            throw new ErrorDeNegocio(CodigoError.de(69, 5), "No se confirmó el canje.");
        UUID solicitud = canjes.ejecutar(invitacionId, recibo, entrada, ctx);
        return ResponseEntity.ok(
                new SalidaCanjeInvitacion(solicitud, SalidaCanjeInvitacion.EstadoEnum.PENDIENTE_BACKOFFICE));
    }
}
