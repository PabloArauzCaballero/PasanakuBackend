package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.DecisionDeIngreso;
import bo.aportaya.grupos.infraestructura.AdmisionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-68 · Admisión al grupo.
 *
 * <p>Expediente humano: el administrador propone, backoffice resuelve y ambas decisiones se conservan
 * inmutables ({@link #proponer}, {@link #resolver}, {@link #historial}).
 *
 * <p>Camino directo del organizador (flujo principal 4 del caso de uso): acepta o rechaza una solicitud
 * pendiente en una sola transaccion ({@link #pendientes}, {@link #decidir}). Aceptada, en la MISMA
 * transaccion se reserva el cupo libre, nace el participante en {@code ACEPTADO_PENDIENTE_FIRMA} y se
 * emite el evento. Se trabaja con rol de sistema porque la politica de fila reserva a otros las filas de
 * {@code participante} y {@code solicitud_ingreso}; por eso la autorizacion del organizador se comprueba
 * aca, contra el grupo, y no se delega en la base. Una solicitud ya resuelta por cualquiera de los dos
 * caminos no se vuelve a resolver.
 */
@Service
public class CU68AceptarIngreso {
    private final Datos datos;
    private final AdmisionRepositorio admision;
    private final Outbox outbox;
    private final Reloj reloj;
    private final OrganizadorDecideIngreso directo;

    @Autowired
    public CU68AceptarIngreso(
            Datos datos,
            AdmisionRepositorio admision,
            Outbox outbox,
            Reloj reloj,
            bo.aportaya.grupos.infraestructura.EmparejamientoRepositorio emparejamientos) {
        this.datos = datos;
        this.admision = admision;
        this.outbox = outbox;
        this.reloj = reloj;
        this.directo = new OrganizadorDecideIngreso(datos, outbox, emparejamientos);
    }

    /** Sin tope de participaciones: para las pruebas que no miran esa regla. */
    public CU68AceptarIngreso(Datos datos, AdmisionRepositorio admision, Outbox outbox, Reloj reloj) {
        this(datos, admision, outbox, reloj, new bo.aportaya.grupos.infraestructura.EmparejamientoRepositorio());
    }

    /** Solo el camino directo del organizador ({@link #pendientes}, {@link #decidir}); no usa el expediente. */
    public CU68AceptarIngreso(Datos datos, Outbox outbox) {
        this(datos, new AdmisionRepositorio(), outbox, Reloj.delSistema());
    }

    @Transactional
    public DecisionDeIngreso proponer(Entrada entrada, ContextoSesion ctx) {
        return decidir(entrada, ctx, false);
    }

    @Transactional
    public DecisionDeIngreso resolver(Entrada entrada, ContextoSesion ctx) {
        if (!"BACKOFFICE".equals(ctx.rol()) && !"ADMIN_PLATAFORMA".equals(ctx.rol())) {
            throw error("Solo backoffice puede resolver la admisión.");
        }
        return decidir(entrada, ctx, true);
    }

    private DecisionDeIngreso decidir(Entrada entrada, ContextoSesion ctx, boolean definitiva) {
        if (entrada.clave() == null
                || entrada.solicitudId() == null
                || entrada.revisionEsperada() < 0
                || entrada.motivo() == null
                || entrada.motivo().isBlank()
                || entrada.motivo().length() > 1000
                || entrada.decision() == null
                || !List.of("ACEPTAR", "RECHAZAR").contains(entrada.decision())) {
            throw error("La decisión requiere clave, revisión, resultado y motivo explícitos.");
        }
        String fase = definitiva ? "RESOLUCION" : "PROPUESTA";
        return datos.conContexto(ctx, dsl -> {
            var solicitud = admision.bloquear(dsl, entrada.solicitudId());
            if (!definitiva && !admision.esAdministrador(dsl, solicitud.grupoId(), ctx.usuarioId())) {
                throw error("Solo el administrador de este grupo puede proponer la admisión.");
            }
            if (solicitud.usuarioId().equals(ctx.usuarioId())) {
                throw error("Nadie puede decidir su propia admisión.");
            }
            var repetida = admision.porClave(dsl, entrada.clave());
            if (repetida.isPresent()) {
                var previa = repetida.get();
                if (!previa.solicitudId().equals(entrada.solicitudId())
                        || !previa.actorId().equals(ctx.usuarioId())
                        || !previa.fase().equals(fase)
                        || !previa.decision().equals(entrada.decision())
                        || !previa.motivo().equals(entrada.motivo())
                        || previa.revision() != entrada.revisionEsperada() + 1
                        || !Objects.equals(previa.propuestaId(), entrada.propuestaId())) {
                    throw error("La clave de idempotencia pertenece a otra decisión.");
                }
                return previa;
            }
            var anterior = admision.ultima(dsl, entrada.solicitudId());
            int revision = anterior.map(DecisionDeIngreso::revision).orElse(0);
            if (!"PENDIENTE".equals(solicitud.estado()) || revision != entrada.revisionEsperada()) {
                throw error("La solicitud cambió o ya fue resuelta; actualiza el expediente.");
            }
            if (definitiva) {
                var propuesta = anterior.orElseThrow(() -> error("Falta la propuesta del administrador."));
                if (!"PROPUESTA".equals(propuesta.fase())
                        || !propuesta.id().equals(entrada.propuestaId())
                        || propuesta.actorId().equals(ctx.usuarioId())) {
                    throw error("La resolución requiere la propuesta vigente y otro responsable humano.");
                }
            } else if (entrada.propuestaId() != null) {
                throw error("Una propuesta no puede suplantar una resolución.");
            }
            UUID participante = null;
            if (definitiva && "ACEPTAR".equals(entrada.decision())) {
                participante = admision.reservarAdmision(dsl, solicitud);
            }
            String evidencia = admision.evidencia(dsl, entrada.solicitudId());
            var motor = admision.recomendacion(evidencia);
            // Apartamiento: la persona decide en contra de la lectura del motor (acepta pese a alertas o SIN_DATOS,
            // o rechaza cuando se recomendaba aceptar). Se registra; no se castiga ni se bloquea.
            boolean apartamiento =
                    ("ACEPTAR".equals(entrada.decision()) && "REVISION_HUMANA".equals(motor.recomendacion()))
                            || ("RECHAZAR".equals(entrada.decision()) && "ACEPTAR".equals(motor.recomendacion()));
            var decision = new DecisionDeIngreso(
                    UUID.randomUUID(),
                    entrada.solicitudId(),
                    entrada.clave(),
                    fase,
                    entrada.decision(),
                    ctx.usuarioId(),
                    entrada.motivo(),
                    entrada.propuestaId(),
                    participante,
                    revision + 1,
                    evidencia,
                    motor.version(),
                    motor.recomendacion(),
                    apartamiento,
                    reloj.ahora()
                            .truncatedTo(java.time.temporal.ChronoUnit.MICROS)
                            .atOffset(ZoneOffset.UTC),
                    UUID.fromString(ctx.traza().id()));
            admision.guardar(dsl, decision);
            if (definitiva) {
                admision.resolver(dsl, solicitud.id(), decision);
            }
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            definitiva ? "grupos.admision_resuelta" : "grupos.admision_propuesta",
                            "solicitud_ingreso",
                            solicitud.id(),
                            Map.of(
                                    "decisionId",
                                    decision.id().toString(),
                                    "grupoId",
                                    solicitud.grupoId().toString(),
                                    "resultado",
                                    decision.decision()),
                            decision.correlacionId()));
            return decision;
        });
    }

    @Transactional(readOnly = true)
    public List<DecisionDeIngreso> historial(UUID solicitudId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            var solicitud = admision.ver(dsl, solicitudId);
            boolean backoffice = "BACKOFFICE".equals(ctx.rol()) || "ADMIN_PLATAFORMA".equals(ctx.rol());
            if (!backoffice && !admision.esAdministrador(dsl, solicitud.grupoId(), ctx.usuarioId())) {
                throw error("El expediente de decisión requiere permisos de revisión.");
            }
            return admision.historial(dsl, solicitudId);
        });
    }

    /** La cola del organizador: solo PENDIENTES, la mas antigua primero. */
    @Transactional(readOnly = true)
    public List<Solicitud> pendientes(UUID grupoId, ContextoSesion ctx) {
        return directo.pendientes(grupoId, ctx);
    }

    /**
     * La bandeja general del backoffice (H4.B.M1): solicitudes de TODOS los grupos, las mas nuevas
     * primero, con el codigo del grupo. Corre con contexto de sistema, igual que la cola del
     * organizador, porque la RLS de {@code solicitud_ingreso} solo deja pasar a BACKOFFICE,
     * CUMPLIMIENTO y AUDITOR y los roles sembrados del backoffice son otros: la puerta es el permiso
     * {@code SOLICITUD_INGRESO_VER} del controlador. Sin telefono ni correo (no hay catalogo de
     * propositos para la bitacora de lectura, R-SEG-02).
     *
     * <p>{@code estado} usa los nombres de la pantalla ({@code ACEPTADA} es {@code APROBADA} en la
     * base); uno que no existe no coincide con ninguna fila. {@code limite} se acota a 1..200.
     */
    @Transactional(readOnly = true)
    public List<FilaDeBandeja> bandeja(String estado, Integer limite, ContextoSesion ctx) {
        String enBase = estado == null ? null : "ACEPTADA".equals(estado) ? "APROBADA" : estado;
        int tope = limite == null ? FILAS_DE_LA_BANDEJA : Math.max(1, Math.min(limite, FILAS_DE_LA_BANDEJA));
        var sistema =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(sistema, dsl -> dsl.fetch(
                        """
                        SELECT s.id, s.usuario_id, s.grupo_id, g.codigo_publico, s.cupos_solicitados, s.mensaje,
                               s.puntaje_compatibilidad, s.estado, s.fecha_solicitud, s.nombre_visible,
                               s.enlace_grupo_id IS NOT NULL AS por_enlace
                          FROM grupos.solicitud_ingreso s
                          JOIN grupos.grupo g ON g.id = s.grupo_id
                         WHERE (?::varchar IS NULL OR s.estado = ?::varchar)
                         ORDER BY s.fecha_solicitud DESC, s.id
                         LIMIT ?
                        """,
                        enBase,
                        enBase,
                        tope)
                .map(r -> new FilaDeBandeja(
                        r.get("id", UUID.class),
                        r.get("usuario_id", UUID.class),
                        r.get("grupo_id", UUID.class),
                        r.get("codigo_publico", String.class),
                        r.get("cupos_solicitados", Integer.class),
                        r.get("mensaje", String.class),
                        r.get("puntaje_compatibilidad", java.math.BigDecimal.class),
                        "APROBADA".equals(r.get("estado", String.class)) ? "ACEPTADA" : r.get("estado", String.class),
                        r.get("fecha_solicitud", OffsetDateTime.class),
                        r.get("nombre_visible", String.class),
                        Boolean.TRUE.equals(r.get("por_enlace", Boolean.class)))));
    }

    @Transactional
    public Resultado decidir(
            UUID solicitudId, boolean aceptar, String motivo, java.math.BigDecimal reputacion, ContextoSesion ctx) {
        return directo.decidir(solicitudId, aceptar, motivo, reputacion, ctx);
    }

    /** Quien pidio entrar, para consultar su reputacion FUERA de la transaccion de decidir (invariante 6). */
    @Transactional(readOnly = true)
    public UUID solicitante(UUID solicitudId, ContextoSesion ctx) {
        return directo.solicitante(solicitudId, ctx);
    }

    public record Solicitud(
            UUID id,
            UUID usuarioId,
            int cuposSolicitados,
            String mensaje,
            java.math.BigDecimal puntaje,
            String estado,
            OffsetDateTime fecha,
            String nombreVisible,
            boolean porEnlace) {}

    /** Una fila de la bandeja del backoffice; {@code estado} ya viene con los nombres de la pantalla. */
    public record FilaDeBandeja(
            UUID id,
            UUID usuarioId,
            UUID grupoId,
            String grupoCodigo,
            int cuposSolicitados,
            String mensaje,
            java.math.BigDecimal puntaje,
            String estado,
            OffsetDateTime fecha,
            String nombreVisible,
            boolean porEnlace) {}

    private static final int FILAS_DE_LA_BANDEJA = 200;

    public record Resultado(UUID solicitudId, String estado, UUID participanteId, UUID cupoId) {}

    private static ErrorDeNegocio error(String mensaje) {
        return new ErrorDeNegocio(CodigoError.de(68, 6), mensaje);
    }

    public record Entrada(
            UUID solicitudId, UUID clave, String decision, String motivo, int revisionEsperada, UUID propuestaId) {}
}
