package bo.aportaya.grupos.web;

import bo.aportaya.grupos.web.generado.GruposApi;
import bo.aportaya.grupos.web.generado.modelo.DecisionIngreso;
import bo.aportaya.grupos.web.generado.modelo.EntradaDecisionIngreso;
import bo.aportaya.grupos.web.generado.modelo.EntradaSustitucionAdministrador;
import bo.aportaya.grupos.web.generado.modelo.SustitucionAdministrador;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/** Rutas del expediente; el único controlador GruposController las expone. */
abstract class RutasDeAdmision implements GruposApi {
    private final AdmisionDelGrupo admision;
    private final SesionDeLaPeticion sesion;

    protected RutasDeAdmision(AdmisionDelGrupo admision, SesionDeLaPeticion sesion) {
        this.admision = admision;
        this.sesion = sesion;
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<DecisionIngreso> proponerAdmision(
            UUID solicitudId, UUID idempotencyKey, EntradaDecisionIngreso cuerpo) {
        return ResponseEntity.ok(admision.decidir(solicitudId, idempotencyKey, cuerpo, sesion.actual(), false));
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<DecisionIngreso> resolverAdmision(
            UUID solicitudId, UUID idempotencyKey, EntradaDecisionIngreso cuerpo) {
        return ResponseEntity.ok(admision.decidir(solicitudId, idempotencyKey, cuerpo, sesion.actual(), true));
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SustitucionAdministrador> sustituirAdministrador(
            UUID grupoId, UUID idempotencyKey, EntradaSustitucionAdministrador cuerpo) {
        return ResponseEntity.ok(admision.sustituir(grupoId, idempotencyKey, cuerpo, sesion.actual()));
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<List<DecisionIngreso>> historialAdmision(UUID solicitudId) {
        return ResponseEntity.ok(admision.historial(solicitudId, sesion.actual()));
    }
}
