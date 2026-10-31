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
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/** CU-69 · El grupo por dentro (2026-10-20): su detalle para los integrantes y la firma del reglamento. */
abstract class RutasDelGrupo extends RutasDeEnlaceDeGrupo {
    private final SesionDeLaPeticion sesion;
    private IntegrantesDelGrupo integrantes;
    private GruposDeBackoffice deBackoffice;

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

    /** Por setter, por la misma razon que {@link #conIntegrantes}. */
    @org.springframework.beans.factory.annotation.Autowired
    void conGruposDeBackoffice(GruposDeBackoffice deBackoffice) {
        this.deBackoffice = deBackoffice;
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

    /**
     * CU-69 · iniciar al 75 % (2026-10-30). {@code GRUPO_ADMINISTRAR} es la guardia por funcion (403 sin ese
     * permiso); que quien llama organice ESTE grupo lo decide el caso de uso sobre el recurso (AP-CU69-07).
     */
    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<bo.aportaya.grupos.web.generado.modelo.GrupoIniciado> iniciarGrupo(
            UUID grupoId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-69", grupoId.toString());
        return ResponseEntity.ok(integrantes.iniciar(grupoId, idempotencyKey, sesion.actual()));
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaFirmaDeReglamento> firmarReglamentoDelGrupo(
            UUID grupoId, EntradaFirmaDeReglamento cuerpo) {
        Traza.marcarCasoDeUso("CU-69", grupoId.toString());
        return ResponseEntity.ok(integrantes.firmar(grupoId, cuerpo, sesion.actual()));
    }

    /**
     * El backoffice consulta los grupos: quien opera la plataforma o quien la audita. Solo lectura; un participante,
     * un organizador o quien no tiene ninguno de los dos codigos recibe la negativa de la guardia.
     */
    @Override
    @Permiso("ADMIN_PLATAFORMA")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<java.util.List<bo.aportaya.grupos.web.generado.modelo.GrupoDeBackoffice>>
            listarGruposDeBackoffice(String estado, Integer limite) {
        Traza.marcarCasoDeUso("BACKOFFICE-GRUPOS", "listado");
        return ResponseEntity.ok(deBackoffice.listar(estado, limite, sesion.actual()));
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<bo.aportaya.grupos.web.generado.modelo.DetalleDeGrupoDeBackoffice> consultarGrupoDeBackoffice(
            UUID grupoId) {
        Traza.marcarCasoDeUso("BACKOFFICE-GRUPOS", grupoId.toString());
        return ResponseEntity.ok(deBackoffice.detalle(grupoId, sesion.actual()));
    }
}
