package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.DetalleDelGrupo;
import bo.aportaya.grupos.web.generado.modelo.EntradaFirmaDeReglamento;
import bo.aportaya.grupos.web.generado.modelo.SalidaFirmaDeReglamento;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/** CU-69 · El grupo por dentro (2026-10-20): su detalle para los integrantes y la firma del reglamento. */
abstract class RutasDelGrupo extends RutasDeEnlaceDeGrupo {
    private final SesionDeLaPeticion sesion;
    private IntegrantesDelGrupo integrantes;

    @SuppressWarnings("checkstyle:ParameterNumber")
    protected RutasDelGrupo(
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
    }

    /**
     * Por setter y no por constructor: GruposController ya esta al limite de largo del archivo (testBarrido, 300
     * lineas) y sumarle un parametro lo pasaba. Spring lo inyecta antes de atender cualquier peticion.
     */
    @org.springframework.beans.factory.annotation.Autowired
    void conIntegrantes(IntegrantesDelGrupo integrantes) {
        this.integrantes = integrantes;
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<bo.aportaya.grupos.web.generado.modelo.MisLimites> consultarMisLimites() {
        Traza.marcarCasoDeUso("CU-69", sesion.actual().usuarioId().toString());
        return ResponseEntity.ok(integrantes.misLimites(sesion.actual()));
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<DetalleDelGrupo> consultarDetalleDelGrupo(UUID grupoId) {
        Traza.marcarCasoDeUso("CU-69", grupoId.toString());
        return ResponseEntity.ok(integrantes.detalle(grupoId, sesion.actual()));
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<bo.aportaya.grupos.web.generado.modelo.PertenenciaCompartida> consultarSiCompartenGrupo(
            UUID grupoId, UUID usuarioId) {
        Traza.marcarCasoDeUso("CU-69", grupoId.toString());
        return ResponseEntity.ok(integrantes.comparten(grupoId, usuarioId, sesion.actual()));
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaFirmaDeReglamento> firmarReglamentoDelGrupo(
            UUID grupoId, EntradaFirmaDeReglamento cuerpo) {
        Traza.marcarCasoDeUso("CU-69", grupoId.toString());
        return ResponseEntity.ok(integrantes.firmar(grupoId, cuerpo, sesion.actual()));
    }
}
