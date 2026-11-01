package bo.aportaya.transparencia.web;

import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import bo.aportaya.transparencia.aplicacion.ConsultarEtapasDePersonas;
import bo.aportaya.transparencia.aplicacion.ConsultarPuntaje;
import bo.aportaya.transparencia.aplicacion.ListarEtapas;
import bo.aportaya.transparencia.web.generado.modelo.EtapaDePersona;
import bo.aportaya.transparencia.web.generado.modelo.EtapaDelCatalogo;
import bo.aportaya.transparencia.web.generado.modelo.PuntajeDelUsuario;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

/**
 * El puntaje propio y las etapas (2026-10-30): mi puntaje con mi etapa, el catalogo de etapas y la
 * etapa de otras personas para grupos. Es parte de {@link
 * bo.aportaya.transparencia.web.generado.ReputacionApi} (el generador agrupa por {@code /reputacion})
 * y vive en su propia clase, como {@link RutasDePerfilEnGrupo}, para no sumarle otra
 * responsabilidad a {@link ReputacionController}.
 */
abstract class RutasDeEtapas extends RutasDePerfilEnGrupo {

    private final SesionDeLaPeticion sesion;
    private final ConsultarPuntaje puntajes;
    private final ListarEtapas etapas;
    private final ConsultarEtapasDePersonas etapasDePersonas;

    protected RutasDeEtapas(
            SesionDeLaPeticion sesion,
            ConsultarPuntaje puntajes,
            ListarEtapas etapas,
            ConsultarEtapasDePersonas etapasDePersonas) {
        super(sesion);
        this.sesion = sesion;
        this.puntajes = puntajes;
        this.etapas = etapas;
        this.etapasDePersonas = etapasDePersonas;
    }

    /**
     * Mi puntaje exacto, con mi etapa. Solo el propio (decision del dueño 2026-10-30, regla
     * 90.2.8): otro {@code usuarioId} es 403 antes de llegar al caso, aunque quien pregunta comparta
     * un grupo con esa persona. {@link ConsultarPuntaje} lo vuelve a exigir: la ruta no es la unica
     * forma de llamarlo.
     *
     * <p>Quien no tiene historial no tiene cero: se responde SIN_HISTORIAL. Confundir
     * «no se sabe» con «malo» le cerraria la puerta a todo el que empieza.
     */
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<PuntajeDelUsuario> consultarPuntaje(UUID usuarioId) {
        Traza.marcarCasoDeUso("CU-71", usuarioId.toString());
        if (!usuarioId.equals(sesion.actual().usuarioId())) {
            throw new AccessDeniedException("el puntaje exacto solo lo ve su dueño");
        }

        var puntaje = puntajes.ejecutar(usuarioId, sesion.actual());

        var respuesta = new PuntajeDelUsuario();
        respuesta.setTieneHistorial(puntaje.tieneHistorial());
        respuesta.setPuntaje(puntaje.puntaje().toPlainString());
        respuesta.setNivelDeConfianza(puntaje.nivelDeConfianza());
        if (puntaje.etapa() != null) {
            respuesta.setEtapa(MapeoDeReputacion.etapaDelPuntaje(puntaje.etapa()));
        }
        return ResponseEntity.ok(respuesta);
    }

    /**
     * El catalogo de etapas del modelo vigente, de la mas baja a la mas alta. No dice nada de
     * ninguna persona.
     *
     * <p>Lo leen tres poblaciones, y cada puerta tiene su motivo:
     * <ul>
     *   <li>{@code PARTICIPANTE}: la app dibuja el camino de etapas y la etapa minima al crear un grupo.
     *   <li>Backoffice: {@code SOPORTE} (el rol que ya opera la reputacion en este controlador),
     *       {@code SOLICITUD_INGRESO_VER} y {@code SOLICITUD_INGRESO_RESOLVER} (la bandeja de ingresos
     *       decide con la etapa minima del grupo) y {@code AUDITORIA_LEER}. Antes solo PARTICIPANTE: el
     *       backoffice recibia 403 y grupos, que reenvia su sesion, fallaba cerrado.
     *   <li>{@code SERVICIO_GRUPOS}: grupos valida la etapa minima y los desbloqueos con su identidad de
     *       servicio cuando quien decide no es la persona que entra.
     * </ul>
     */
    @Override
    @Permiso("PARTICIPANTE")
    @PermisoAlternativo({
        "SOPORTE",
        "SOLICITUD_INGRESO_VER",
        "SOLICITUD_INGRESO_RESOLVER",
        "AUDITORIA_LEER",
        "SERVICIO_GRUPOS"
    })
    public ResponseEntity<List<EtapaDelCatalogo>> listarEtapas() {
        Traza.marcarCasoDeUso("ETAPAS", "catalogo");

        return ResponseEntity.ok(etapas.ejecutar(sesion.actual()).stream()
                .map(MapeoDeReputacion::etapa)
                .toList());
    }

    /**
     * [CONTRATO · SOLO SERVICIO] La etapa de otras personas, sin su puntaje. Solo la identidad de
     * servicio de {@code grupos}: una sesion de persona —aunque comparta el grupo— recibe 403. El
     * tamaño (1 a 60) lo valida el contrato en la ruta, y el caso lo vuelve a exigir.
     */
    @Override
    @Permiso("SERVICIO_GRUPOS")
    public ResponseEntity<List<EtapaDePersona>> consultarEtapasDePersonas(List<UUID> usuarioId) {
        Traza.marcarCasoDeUso("ETAPAS", "de-personas");

        return ResponseEntity.ok(etapasDePersonas.ejecutar(usuarioId, sesion.actual()).stream()
                .map(MapeoDeReputacion::etapaDePersona)
                .toList());
    }
}
