package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.InvitacionAdmisible;
import bo.aportaya.grupos.dominio.InvitacionRegistrada;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios.ConsumoInvitacion;
import bo.aportaya.grupos.infraestructura.InvitacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-69 · Invitar a un contacto.
 *
 * <p>Si el destinatario esta suprimido **no se envia nada y se responde como si
 * hubiera salido bien**. Decir «esa persona pidio no recibir mensajes» ya cuenta algo
 * de ella a quien no tiene por que saberlo, y quien invita no necesita esa
 * informacion para nada.
 *
 * <p>La aceptacion de un solo uso y la creacion de la participacion viven juntas
 * en {@link CU69Enlace}.
 */
@Service
public class CU69Invitar {

    private static final Duration VIGENCIA = Duration.ofDays(7);

    private final Datos datos;
    private final InvitacionRepositorio invitaciones;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Ids ids;

    public CU69Invitar(Datos datos, InvitacionRepositorio invitaciones, Outbox outbox, Reloj reloj, Ids ids) {
        this.datos = datos;
        this.invitaciones = invitaciones;
        this.outbox = outbox;
        this.reloj = reloj;
        this.ids = ids;
    }

    @Transactional
    public Resultado invitar(EntradaInvitacion entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        // La API autorizó GRUPO_ADMINISTRAR; la comprobación de pertenencia
        // sigue abajo. La fila de participante está reservada por RLS al
        // proceso interno incluso cuando pertenece al emisor.
        ContextoSesion interno = interno(ctx);
        return datos.conContexto(interno, dsl -> {
            if (entrada.tokenId() != null) {
                dsl.execute("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", "invitar:" + entrada.tokenId());
                var anterior = dsl.fetchOne("SELECT * FROM grupos.invitacion WHERE token_id=?", entrada.tokenId());
                if (anterior != null) {
                    if (!ctx.usuarioId().equals(anterior.get("emisor_id", UUID.class))
                            || !entrada.grupoId().equals(anterior.get("grupo_id", UUID.class))
                            || !entrada.telefonoInvitado().equals(anterior.get("telefono_invitado", String.class))
                            || !entrada.canal().equals(anterior.get("canal", String.class))
                            || !java.util.Objects.equals(
                                    entrada.nombreSugerido(), anterior.get("nombre_sugerido", String.class)))
                        throw new ErrorDeNegocio(CodigoError.de(69, 6), "La clave corresponde a otra invitacion.");
                    return new Resultado(
                            Optional.of(anterior.get("id", UUID.class)), "Invitacion disponible para compartir.");
                }
            }
            // Reemision: una sola invitacion viva por numero y grupo (indice uq_invitacion_activa).
            // La anterior se CONSERVA: emitir otra exige revocarla antes, de forma explicita. Las
            // vencidas se cierran aca para que no estorben.
            dsl.execute(
                    "SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
                    "invitar-destino:" + entrada.grupoId() + ":" + entrada.telefonoInvitado());
            invitaciones.expirarVencidas(dsl, entrada.grupoId(), entrada.telefonoInvitado(), ahora);
            if (invitaciones.hayVigente(dsl, entrada.grupoId(), entrada.telefonoInvitado(), ahora)) {
                throw new ErrorDeNegocio(
                        CodigoError.de(69, 9),
                        "Ya hay una invitacion vigente para ese numero. Revocala antes de emitir otra.");
            }
            var impedimento = InvitacionAdmisible.impedimento(
                    invitaciones.hayCuposLibres(dsl, entrada.grupoId()),
                    entrada.destinatarioSuprimido(),
                    entrada.yaEsParticipante(),
                    0,
                    entrada.topeDeReenvios(),
                    invitaciones.emisorHabilitado(dsl, entrada.grupoId(), ctx.usuarioId()));

            if (impedimento.isPresent()) {
                var motivo = impedimento.get();
                if (motivo.seRespondeComoExito()) {
                    // Nada se envia y nada se escribe, pero la respuesta no lo delata.
                    return new Resultado(Optional.empty(), motivo.mensaje());
                }
                throw new ErrorDeNegocio(CodigoError.de(69, motivo.numero()), motivo.mensaje());
            }

            UUID invitacion = invitaciones.crear(
                    dsl,
                    entrada.grupoId(),
                    entrada.telefonoInvitado(),
                    entrada.nombreSugerido(),
                    ctx.usuarioId(),
                    // El token lo emite `identidad`, que es quien posee
                    // token_verificacion; aca solo se recibe su identificador. La
                    // clave foranea cruza esquemas y la verifica el motor.
                    entrada.tokenId(),
                    entrada.canal(),
                    ahora,
                    entrada.expiraEn());

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "grupos.invitacion_enviada",
                            "invitacion",
                            invitacion,
                            Map.of("grupoId", entrada.grupoId().toString()),
                            UUID.fromString(ctx.traza().id())));

            return new Resultado(Optional.of(invitacion), "Invitacion disponible para compartir.");
        });
    }

    /** Aceptar consume el token: la segunda vez no queda nada que consumir. */
    @Transactional
    public void aceptar(UUID invitacionId, ConsumoInvitacion recibo, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        datos.conContexto(ctx, dsl -> {
            if (recibo == null
                    || recibo.consumidoEn() == null
                    || recibo.consumidoEn().isAfter(ahora)
                    || !ctx.usuarioId().equals(recibo.usuarioId())
                    || invitaciones.aceptar(
                                    dsl, invitacionId, recibo.tokenId(), recibo.grupoId(), ahora, recibo.consumidoEn())
                            == 0) {
                throw new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion ya no es valida.");
            }
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "grupos.invitacion_aceptada",
                            "invitacion",
                            invitacionId,
                            Map.of("invitacionId", invitacionId.toString()),
                            UUID.fromString(ctx.traza().id())));
            return null;
        });
    }

    /**
     * Revoca la invitacion en la base propia. El enlace ya se revoco en {@code identidad} (fuera de la
     * transaccion): repetirlo es seguro, y una invitacion ya revocada responde igual.
     */
    @Transactional
    public void revocar(UUID invitacionId, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        datos.conContexto(ctx, dsl -> {
            dsl.execute("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", "invitar-id:" + invitacionId);
            var invitacion = invitaciones
                    .porId(dsl, invitacionId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion no existe."));
            if (!ctx.usuarioId().equals(invitacion.emisorId())) {
                throw new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion ya no es valida.");
            }
            if ("REVOCADA".equals(invitacion.estado())) {
                return null;
            }
            if (invitaciones.revocar(dsl, invitacionId, ahora) == 0) {
                throw new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion ya no es valida.");
            }
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "grupos.invitacion_revocada",
                            "invitacion",
                            invitacionId,
                            Map.of("grupoId", invitacion.grupoId().toString()),
                            UUID.fromString(ctx.traza().id())));
            return null;
        });
    }

    /** Insistir tres veces es recordar; insistir diez es acoso. */
    @Transactional
    public void reenviar(UUID invitacionId, int topeDeReenvios, ContextoSesion ctx) {
        datos.conContexto(ctx, dsl -> {
            var invitacion = invitaciones
                    .porId(dsl, invitacionId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion no existe."));
            if (!ctx.usuarioId().equals(invitacion.emisorId())
                    || !"ENVIADA".equals(invitacion.estado())
                    || !reloj.ahora().isBefore(invitacion.expiraEn().toInstant()))
                throw new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion ya no es valida.");
            if (invitacion.envios() >= topeDeReenvios) {
                throw new ErrorDeNegocio(CodigoError.de(69, 4), InvitacionAdmisible.Motivo.TOPE_REENVIOS.mensaje());
            }
            invitaciones.reenviar(dsl, invitacionId);
            return null;
        });
    }

    public record EntradaInvitacion(
            UUID grupoId,
            String telefonoInvitado,
            String nombreSugerido,
            String canal,
            boolean destinatarioSuprimido,
            boolean yaEsParticipante,
            int topeDeReenvios,
            UUID tokenId,
            OffsetDateTime expiraEn) {}

    @Transactional(readOnly = true)
    public InvitacionRegistrada consultar(UUID invitacionId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> invitaciones
                .porId(dsl, invitacionId)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion ya no es valida.")));
    }

    @Transactional(readOnly = true)
    public void comprobarEmisor(UUID grupoId, ContextoSesion ctx) {
        datos.conContexto(interno(ctx), dsl -> {
            if (!invitaciones.emisorHabilitado(dsl, grupoId, ctx.usuarioId()))
                throw new ErrorDeNegocio(CodigoError.de(69, 7), "No podes invitar a este grupo.");
            return null;
        });
    }

    /** Proceso interno acotado: la fila de participante está reservada por RLS incluso para su dueño. */
    private static ContextoSesion interno(ContextoSesion ctx) {
        return ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
    }

    public record Resultado(Optional<UUID> invitacionId, String mensaje) {}
}
