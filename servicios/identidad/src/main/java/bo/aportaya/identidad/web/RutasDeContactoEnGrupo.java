package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.ConsultarContactoEnGrupo;
import bo.aportaya.identidad.web.generado.IdentidadApi;
import bo.aportaya.identidad.web.generado.modelo.ContactoEnGrupo;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/**
 * Perfil del integrante (2026-10-21): el telefono de alguien, solo para quien comparte su grupo. Vive bajo
 * {@code /identidad} (prefijo reservado de este servicio en scripts/modelo.py) y por eso es parte de {@link IdentidadApi},
 * que atiende {@link VerificacionesController}; queda en su propia clase para no mezclarlo con el portal de riesgo.
 */
abstract class RutasDeContactoEnGrupo implements IdentidadApi {

    private final ConsultarContactoEnGrupo contactos;
    private final SesionDeLaPeticion sesion;

    protected RutasDeContactoEnGrupo(ConsultarContactoEnGrupo contactos, SesionDeLaPeticion sesion) {
        this.contactos = contactos;
        this.sesion = sesion;
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<ContactoEnGrupo> consultarContactoEnGrupo(UUID grupoId, UUID usuarioId) {
        Traza.marcarCasoDeUso("CU-69", grupoId.toString());
        return ResponseEntity.ok(new ContactoEnGrupo(contactos.ejecutar(grupoId, usuarioId, sesion.actual())));
    }
}
