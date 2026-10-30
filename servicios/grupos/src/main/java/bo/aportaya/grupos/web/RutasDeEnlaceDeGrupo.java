package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.DetalleEnlaceDelGrupo;
import bo.aportaya.grupos.web.generado.modelo.EntradaCodigoDeEnlace;
import bo.aportaya.grupos.web.generado.modelo.EntradaPostulacion;
import bo.aportaya.grupos.web.generado.modelo.EntradaSolicitudPorEnlace;
import bo.aportaya.grupos.web.generado.modelo.SalidaEnlaceDelGrupo;
import bo.aportaya.grupos.web.generado.modelo.SalidaSolicitudPorEnlace;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/**
 * CU-69 · El enlace del grupo (2026-10-19). Lo que viene de otros servicios —los hechos de la postulacion y el
 * nombre visible de quien pide— se pregunta ANTES de abrir la transaccion de pedir (invariante 6).
 */
abstract class RutasDeEnlaceDeGrupo extends RutasDeInvitacion {
    private final CU69EnlaceDeGrupo enlacesDeGrupo;
    private final Consultas consultas;
    private final HechosDeOtrosServicios afuera;
    private final SesionDeLaPeticion sesion;
    private final BigDecimal afinidad;

    @SuppressWarnings("checkstyle:ParameterNumber")
    protected RutasDeEnlaceDeGrupo(
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
        super(admision, sesion, cu68Decision, enlaces, invitaciones, canjes, consultas, afuera, afinidad);
        this.enlacesDeGrupo = enlacesDeGrupo;
        this.consultas = consultas;
        this.afuera = afuera;
        this.sesion = sesion;
        this.afinidad = afinidad;
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<SalidaEnlaceDelGrupo> generarEnlaceDelGrupo(UUID grupoId) {
        Traza.marcarCasoDeUso("CU-69", grupoId.toString());
        var g = enlacesDeGrupo.generar(grupoId, sesion.actual());
        // El codigo es un secreto para compartir: que ningun intermediario lo guarde.
        return ResponseEntity.status(201)
                .header("Cache-Control", "no-store")
                .body(new SalidaEnlaceDelGrupo(g.enlaceId(), g.codigo(), g.expiraEn()));
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<Void> anularEnlaceDelGrupo(UUID grupoId) {
        Traza.marcarCasoDeUso("CU-69", grupoId.toString());
        enlacesDeGrupo.anular(grupoId, sesion.actual());
        return ResponseEntity.noContent().build();
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<DetalleEnlaceDelGrupo> consultarEnlaceDelGrupo(EntradaCodigoDeEnlace cuerpo) {
        var d = enlacesDeGrupo.consultar(cuerpo.getCodigo(), sesion.actual());
        return ResponseEntity.ok(new DetalleEnlaceDelGrupo(
                d.grupoId(),
                d.nombre(),
                d.montoAporte().toPlainString(),
                d.moneda(),
                d.periodicidad(),
                d.cuposLibres(),
                d.expiraEn()));
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaSolicitudPorEnlace> solicitarIngresoPorEnlace(EntradaSolicitudPorEnlace cuerpo) {
        var ctx = sesion.actual();
        UUID grupoId = enlacesDeGrupo.grupoDe(cuerpo.getCodigo(), ctx);
        Traza.marcarCasoDeUso("CU-69", grupoId.toString());
        var postulacion = new EntradaPostulacion(1).mensaje(cuerpo.getMensaje());
        var entrada = MapeoDePostulacion.entrada(grupoId, postulacion, consultas, afuera, afinidad, ctx);
        // Pedir de nuevo devuelve la solicitud PENDIENTE que ya habia: ese reintento no vuelve a mirar la arena.
        if (enlacesDeGrupo.pendienteDe(grupoId, ctx).isEmpty()) {
            MapeoDePostulacion.exigirArena(grupoId, consultas, afuera, ctx);
        }
        var contacto = afuera.contactoDeLaSesion().orElse(null);
        var s = enlacesDeGrupo.solicitar(cuerpo.getCodigo(), entrada, contacto, ctx);
        return ResponseEntity.status(201)
                .body(new SalidaSolicitudPorEnlace(
                        s.solicitudId(), s.grupoId(), SalidaSolicitudPorEnlace.EstadoEnum.PENDIENTE));
    }
}
