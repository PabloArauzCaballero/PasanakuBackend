package bo.aportaya.transparencia.web;

import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import bo.aportaya.transparencia.aplicacion.ConsultarPerfilEnGrupo;
import bo.aportaya.transparencia.dominio.PerfilDeReputacionEnGrupo;
import bo.aportaya.transparencia.web.generado.ReputacionApi;
import bo.aportaya.transparencia.web.generado.modelo.InsigniaVigente;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Perfil del integrante (2026-10-21): la reputacion de alguien para quien comparte su grupo. Es parte de
 * {@link ReputacionApi} (el generador agrupa por {@code /reputacion}) y vive en su propia clase para no sumarle otra
 * responsabilidad a {@link ReputacionController}.
 */
abstract class RutasDePerfilEnGrupo implements ReputacionApi {

    private final SesionDeLaPeticion sesion;
    private ConsultarPerfilEnGrupo perfiles;

    protected RutasDePerfilEnGrupo(SesionDeLaPeticion sesion) {
        this.sesion = sesion;
    }

    /**
     * Por setter y no por constructor: ReputacionController ya recibe nueve dependencias. Spring lo inyecta antes de
     * atender cualquier peticion.
     */
    @Autowired
    void conPerfiles(ConsultarPerfilEnGrupo perfiles) {
        this.perfiles = perfiles;
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<bo.aportaya.transparencia.web.generado.modelo.PerfilDeReputacionEnGrupo>
            consultarPerfilEnGrupo(UUID usuarioId, UUID grupoId) {
        Traza.marcarCasoDeUso("CU-71", usuarioId.toString());
        return ResponseEntity.ok(aSalida(perfiles.ejecutar(grupoId, usuarioId, sesion.actual())));
    }

    static bo.aportaya.transparencia.web.generado.modelo.PerfilDeReputacionEnGrupo aSalida(
            PerfilDeReputacionEnGrupo perfil) {
        var insignias = perfil.insignias().stream()
                .map(i -> new InsigniaVigente(i.codigo(), i.nombre(), i.descripcion(), i.otorgadaEn()))
                .toList();
        return perfil.factores()
                .map(f -> new bo.aportaya.transparencia.web.generado.modelo.PerfilDeReputacionEnGrupo(
                                true, f.nivelConfianza(), insignias)
                        // Null cuando quien pregunta no es el dueño (ConsultarPerfilEnGrupo): el campo se omite.
                        .puntaje(f.puntaje() == null ? null : f.puntaje().toPlainString())
                        .indicePuntualidad(f.indicePuntualidad().toPlainString())
                        .gruposCompletados(f.gruposCompletados())
                        .gruposAbandonados(f.gruposAbandonados())
                        .incumplimientosAbiertos(f.incumplimientosAbiertos())
                        .antiguedadMeses(f.antiguedadMeses()))
                .orElseGet(() -> new bo.aportaya.transparencia.web.generado.modelo.PerfilDeReputacionEnGrupo(
                        false, "SIN_HISTORIAL", insignias));
    }
}
