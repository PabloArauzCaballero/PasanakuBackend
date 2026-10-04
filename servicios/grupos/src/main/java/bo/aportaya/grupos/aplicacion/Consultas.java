package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.PaqueteDeSorteo;
import bo.aportaya.grupos.dominio.PoliticaDelGrupo;
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

    public Consultas(Datos datos, ConsultasRepositorio consultas) {
        this.datos = datos;
        this.consultas = consultas;
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

    public record Participacion(UUID grupoId, UUID participanteId, String estado) {}

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
    public Optional<PaqueteDeSorteo> paqueteDelSorteo(UUID sorteoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.paqueteDelSorteo(dsl, sorteoId));
    }
}
