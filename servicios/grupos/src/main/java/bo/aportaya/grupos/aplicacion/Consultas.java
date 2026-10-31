package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.CalendarioDeAportes;
import bo.aportaya.grupos.dominio.CondicionesDeIngreso;
import bo.aportaya.grupos.dominio.PaqueteDeSorteo;
import bo.aportaya.grupos.dominio.PoliticaDelGrupo;
import bo.aportaya.grupos.infraestructura.CalendarioDeAportesRepositorio;
import bo.aportaya.grupos.infraestructura.ConsultasRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las tres respuestas que este servicio le debe a los otros.
 *
 * <p>No son casos de uso de la boveda: son lo que hace posible que
 * {@code nucleo-financiero} transfiera a un alias, sepa si puede cerrar una billetera y
 * que {@code transparencia} pueda verificar un sorteo, todo **sin leer este esquema**.
 */
@Service
public class Consultas {

    private final Datos datos;
    private final ConsultasRepositorio consultas;
    private final CalendarioDeAportesRepositorio calendarios;

    public Consultas(Datos datos, ConsultasRepositorio consultas, CalendarioDeAportesRepositorio calendarios) {
        this.datos = datos;
        this.consultas = consultas;
        this.calendarios = calendarios;
    }

    /**
     * El calendario de aportes de un periodo (H8.S1.M1): titulares, importes y fechas, para que `aportes`
     * genere sus obligaciones sin leer este esquema. Se lee con rol de sistema (la politica de fila no deja
     * ver cupos ajenos) y la autorizacion de quien pregunta ya la hizo el permiso del endpoint.
     */
    @Transactional(readOnly = true)
    public Optional<CalendarioDeAportes> calendarioDeAportes(UUID periodoId, ContextoSesion ctx) {
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(interno, dsl -> calendarios.deLosCupos(dsl, periodoId));
    }

    @Transactional(readOnly = true)
    public Optional<UUID> usuarioDelAlias(String alias, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.usuarioDelAlias(dsl, alias));
    }

    @Transactional(readOnly = true)
    public int gruposActivosDe(UUID usuarioId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.gruposActivosDe(dsl, usuarioId));
    }

    @Transactional(readOnly = true)
    public Optional<UUID> grupoDelAcuerdo(UUID acuerdoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.grupoDelAcuerdo(dsl, acuerdoId));
    }

    @Transactional(readOnly = true)
    public Optional<UUID> grupoDelTurno(UUID turnoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.grupoDelTurno(dsl, turnoId));
    }

    @Transactional(readOnly = true)
    public boolean permitePermuta(UUID turnoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.permitePermuta(dsl, turnoId));
    }

    @Transactional(readOnly = true)
    public Optional<PoliticaDelGrupo> politicaDelGrupo(UUID grupoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.politicaDelGrupo(dsl, grupoId));
    }

    /**
     * Lo que el grupo pide a quien entra (etapa minima, cupos, aporte) y su estado, para comparar con la etapa del
     * entrante ANTES de la transaccion del ingreso (2026-10-30). Con rol de sistema: quien todavia no es integrante no
     * ve la fila del grupo por su politica, y una fila invisible no puede leerse como «el grupo no pide nada». Vacio
     * solo si el grupo no existe.
     */
    @Transactional(readOnly = true)
    public Optional<CondicionesDeIngreso> condicionesDeIngreso(UUID grupoId, ContextoSesion ctx) {
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(interno, dsl -> Optional.ofNullable(dsl.fetchOne(CONDICIONES, grupoId))
                .map(Consultas::condiciones));
    }

    /** Quien pidio entrar y las condiciones de ese grupo, para decidir una solicitud con su etapa a la vista. */
    @Transactional(readOnly = true)
    public Optional<Solicitante> solicitanteConCondiciones(UUID solicitudId, ContextoSesion ctx) {
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(interno, dsl -> Optional.ofNullable(dsl.fetchOne(
                        "SELECT usuario_id, grupo_id, estado FROM grupos.solicitud_ingreso WHERE id = ?", solicitudId))
                .flatMap(s -> Optional.ofNullable(dsl.fetchOne(CONDICIONES, s.get("grupo_id", UUID.class)))
                        .map(g -> new Solicitante(
                                s.get("usuario_id", UUID.class), condiciones(g), s.get("estado", String.class)))));
    }

    /**
     * {@code estadoSolicitud}: una solicitud que ya no esta PENDIENTE no se vuelve a decidir; repetir la misma decision
     * devuelve la de antes sin preguntar la etapa (regla 96.3.3).
     */
    public record Solicitante(UUID usuarioId, CondicionesDeIngreso condiciones, String estadoSolicitud) {
        public Solicitante(UUID usuarioId, CondicionesDeIngreso condiciones) {
            this(usuarioId, condiciones, "PENDIENTE");
        }

        public boolean pendiente() {
            return "PENDIENTE".equals(estadoSolicitud);
        }
    }

    private static final String CONDICIONES =
            "SELECT id, estado, reputacion_minima, cupos_totales, monto_aporte, moneda FROM grupos.grupo WHERE id = ?";

    private static CondicionesDeIngreso condiciones(org.jooq.Record g) {
        return new CondicionesDeIngreso(
                g.get("id", UUID.class),
                g.get("estado", String.class),
                g.get("reputacion_minima", java.math.BigDecimal.class),
                g.get("cupos_totales", Integer.class),
                g.get("monto_aporte", java.math.BigDecimal.class),
                g.get("moneda", String.class).trim());
    }

    @Transactional(readOnly = true)
    public boolean yaCobroSuTurno(UUID participanteId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.yaCobroSuTurno(dsl, participanteId));
    }

    @Transactional(readOnly = true)
    public boolean yaEsParticipante(UUID grupoId, UUID usuarioId, ContextoSesion ctx) {
        // El organizador no ve las filas ajenas de participante por RLS.
        // Esta consulta sólo devuelve un booleano a la emisión de invitaciones.
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(interno, dsl -> consultas.yaEsParticipante(dsl, grupoId, usuarioId));
    }

    @Transactional(readOnly = true)
    public Optional<UUID> participanteDe(UUID grupoId, UUID usuarioId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.participanteDe(dsl, grupoId, usuarioId));
    }

    /**
     * El participante ACTIVO de OTRA persona en un grupo, para quien administra (traspaso de cupo).
     * La politica de fila deja a cada titular ver solo lo suyo, asi que se lee con rol de sistema y
     * se devuelve unicamente un id, no la fila (B8). La autorizacion de quien pregunta ya la hizo el
     * permiso del endpoint.
     */
    @Transactional(readOnly = true)
    public Optional<UUID> participanteDeOtro(UUID grupoId, UUID usuarioId, ContextoSesion ctx) {
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(interno, dsl -> consultas.participanteDe(dsl, grupoId, usuarioId));
    }

    /** Los grupos en los que participa quien pregunta: lo suyo, por su propia politica de fila. */
    @Transactional(readOnly = true)
    public List<Participacion> participacionesDe(ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.participacionesDe(dsl, ctx.usuarioId()));
    }

    public record Participacion(
            UUID grupoId,
            UUID participanteId,
            String estado,
            String nombreDelGrupo,
            String montoAporte,
            String moneda,
            boolean esOrganizador) {}

    /** La admisibilidad de un pago: de quien es el participante y si el periodo sigue abierto. */
    @Transactional(readOnly = true)
    public Admisibilidad admisibilidadDePago(UUID participanteId, UUID periodoId, ContextoSesion ctx) {
        boolean esMio =
                datos.conContexto(ctx, dsl -> consultas.esElParticipanteDe(dsl, participanteId, ctx.usuarioId()));
        // El estado del periodo no es dato de nadie: se lee con rol de sistema y solo sale un booleano.
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        boolean abierto = datos.conContexto(interno, dsl -> consultas.periodoAdmitePago(dsl, periodoId));
        return new Admisibilidad(esMio, abierto);
    }

    public record Admisibilidad(boolean esDelUsuario, boolean periodoAbierto) {}

    /** Los periodos del grupo, en orden. Es contra ellos que el sorteo reparte turnos. */
    @Transactional(readOnly = true)
    public java.util.List<UUID> periodosDe(UUID grupoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.periodosDe(dsl, grupoId));
    }

    /** Lo que se estima que cobra cada turno: el aporte por la cantidad de cupos. */
    @Transactional(readOnly = true)
    public java.math.BigDecimal montoEstimado(UUID grupoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.montoEstimado(dsl, grupoId));
    }

    @Transactional(readOnly = true)
    /** Si quien llama administra ese grupo (es su administrador activo) o es backoffice. */
    public boolean puedeAdministrar(UUID grupoId, ContextoSesion ctx) {
        return "BACKOFFICE".equals(ctx.rol())
                || datos.conContexto(ctx, dsl -> consultas.esAdministradorDelGrupo(dsl, grupoId, ctx.usuarioId()));
    }

    public Optional<PaqueteDeSorteo> paqueteDelSorteo(UUID sorteoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.paqueteDelSorteo(dsl, sorteoId));
    }
}
