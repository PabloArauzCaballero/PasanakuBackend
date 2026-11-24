package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.HechoDeAcceso;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Deja cada acto de gobierno en la bitacora y pide el aviso (GU-07, decision P-5), en la MISMA transaccion
 * que el acto: si el acto revierte, ni la bitacora ni el aviso existieron (outbox, regla 98.3.3).
 *
 * <p>Dos eventos por acto (contrato en CU-08 § Contrato de eventos):
 *
 * <ol>
 *   <li>el hecho ({@link HechoDeAcceso.Accion#tipoDeEvento()}), que {@code auditoria} escribe en
 *       {@code comun.bitacora_evento};
 *   <li>{@code identidad.aviso_de_acceso_solicitado}, el pedido de aviso por correo a la persona afectada y a
 *       los oficiales de cumplimiento vigentes. Lleva identificadores: el correo lo resuelve
 *       {@code notificaciones} por su canal verificado, nunca viaja en el evento.
 * </ol>
 */
public final class BitacoraDeAccesos {

    /** El evento notificable del catalogo de notificaciones que este pedido dispara. */
    static final String EVENTO_NOTIFICABLE = "ACCESO_DE_OPERADOR_CAMBIADO";

    /** Quien, ademas del afectado, se entera de todo cambio de acceso (P-5). */
    static final String ROL_QUE_SE_ENTERA = "OFICIAL_CUMPLIMIENTO";

    private final Outbox outbox;
    private final GobiernoDeAccesosRepositorio gobierno;

    public BitacoraDeAccesos(Outbox outbox, GobiernoDeAccesosRepositorio gobierno) {
        this.outbox = outbox;
        this.gobierno = gobierno;
    }

    public void registrar(DSLContext dsl, HechoDeAcceso hecho, UUID agregadoId, ContextoSesion ctx) {
        UUID correlacion = correlacionDe(ctx);
        HechoDeAcceso.Accion accion = hecho.accion();
        outbox.emitir(
                dsl,
                new EventoDominio(
                        accion.tipoDeEvento(),
                        accion.esDeAsignacion() ? "asignacion_rol" : "usuario",
                        agregadoId,
                        hecho.carga(),
                        correlacion));

        Set<String> destinatarios = new LinkedHashSet<>();
        destinatarios.add(hecho.afectadoId().toString());
        gobierno.titularesVigentesDelRol(dsl, ROL_QUE_SE_ENTERA, hecho.ocurridoEn())
                .forEach(oficial -> destinatarios.add(oficial.toString()));
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "identidad.aviso_de_acceso_solicitado",
                        "usuario",
                        hecho.afectadoId(),
                        Map.of(
                                "version", HechoDeAcceso.VERSION,
                                "eventoNotificable", EVENTO_NOTIFICABLE,
                                "eventoOrigen", accion.tipoDeEvento(),
                                "accion", accion.name(),
                                "afectadoId", hecho.afectadoId().toString(),
                                "canal", "CORREO",
                                "destinatarios", List.copyOf(destinatarios)),
                        correlacion));
    }

    static UUID correlacionDe(ContextoSesion ctx) {
        return ctx.traza() != null ? UUID.fromString(ctx.traza().id()) : UUID.randomUUID();
    }
}
