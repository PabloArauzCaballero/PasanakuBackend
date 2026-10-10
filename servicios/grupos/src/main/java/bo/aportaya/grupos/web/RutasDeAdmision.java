package bo.aportaya.grupos.web;

import bo.aportaya.grupos.web.generado.GruposApi;
import bo.aportaya.grupos.web.generado.modelo.DecisionIngreso;
import bo.aportaya.grupos.web.generado.modelo.EntradaDecisionIngreso;
import bo.aportaya.grupos.web.generado.modelo.EntradaSustitucionAdministrador;
import bo.aportaya.grupos.web.generado.modelo.SustitucionAdministrador;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
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

    /**
     * El expediente de decisiones de una solicitud. Tres puertas, y el caso de uso decide sobre el RECURSO:
     * {@code PARTICIPANTE} (cuenta de participante), {@code GRUPO_ADMINISTRAR} (el administrador, que es quien
     * propone y por eso lee su expediente) y {@code SOLICITUD_INGRESO_VER} (backoffice: ADMIN_PLATAFORMA y SOPORTE
     * lo traen sembrado). Antes solo la primera abria la ruta y {@code CU68AceptarIngreso.historial}, que ya admite
     * al backoffice y al administrador, nunca llegaba a ejecutarse para ellos (403 de la guardia, hallado en la
     * corrida real del carril G). La guardia solo deja pasar a quien PUEDE llegar a ser admitido: un participante
     * cualquiera, un administrador de OTRO grupo o quien no tiene ninguno de los tres codigos sigue recibiendo la
     * negativa.
     */
    @Override
    @Permiso("PARTICIPANTE")
    @PermisoAlternativo({"SOLICITUD_INGRESO_VER", "GRUPO_ADMINISTRAR"})
    public ResponseEntity<List<DecisionIngreso>> historialAdmision(UUID solicitudId) {
        return ResponseEntity.ok(admision.historial(solicitudId, sesion.actual()));
    }
}
