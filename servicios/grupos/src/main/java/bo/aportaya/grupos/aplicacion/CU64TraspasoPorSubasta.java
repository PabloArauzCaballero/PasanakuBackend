package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.TraspasoAdmisible;
import bo.aportaya.grupos.infraestructura.EmparejamientoRepositorio;
import bo.aportaya.grupos.infraestructura.SubastaDeCupoRepositorio;
import bo.aportaya.grupos.infraestructura.TraspasoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-64 por subasta (decision de Pablo, 2026-10-30): el cupo se traspasa al que mas ofrece, sin votacion.
 *
 * <p>La subasta y la plata viven en {@code entregas}. Aca queda lo que es de {@code grupos}: decir si un cupo se
 * puede subastar, si alguien puede pujar por entrar a este grupo, y pasar el cupo al ganador. El paso del cupo lo pide
 * {@code entregas} ANTES de pagarle al saliente (lo compensable primero, regla 98.3.5).
 */
@Service
public class CU64TraspasoPorSubasta {

    /** El codigo cerrado de {@code traspaso_cupo.motivo}: una subasta es una venta del cupo. */
    static final String MOTIVO = "VENTA";

    private final Datos datos;
    private final TraspasoRepositorio traspasos;
    private final SubastaDeCupoRepositorio subastas;
    private final EmparejamientoRepositorio emparejamientos;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU64TraspasoPorSubasta(
            Datos datos,
            TraspasoRepositorio traspasos,
            SubastaDeCupoRepositorio subastas,
            EmparejamientoRepositorio emparejamientos,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.traspasos = traspasos;
        this.subastas = subastas;
        this.emparejamientos = emparejamientos;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    public enum Impedimento {
        CUPO_NO_TRASPASABLE,
        SALIENTE_CON_DEUDA
    }

    public record Subastabilidad(
            UUID grupoId,
            UUID cupoId,
            UUID participanteId,
            Optional<Impedimento> impedimento,
            BigDecimal montoAporte,
            String moneda,
            String nombreDelGrupo,
            String periodicidad) {}

    public enum NoElegible {
        RESTRICCION_VIGENTE,
        KYC_INSUFICIENTE,
        REPUTACION_INSUFICIENTE,
        MAXIMO_DE_GRUPOS
    }

    public record Traspaso(UUID traspasoId, UUID participanteEntranteId) {}

    /** Solo contesta al titular del cupo: a cualquier otro, AP-CU64-06, sin decir de quien es. */
    @Transactional(readOnly = true)
    public Subastabilidad subastabilidad(
            UUID grupoId, UUID cupoId, Optional<UUID> titularQueLlama, boolean salienteAlDia, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            var cupo = traspasos
                    .estadoDelCupo(dsl, cupoId)
                    .filter(c -> c.grupoId().equals(grupoId))
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(64, 1), "Ese cupo no existe."));
            if (titularQueLlama.isEmpty() || !cupo.participanteId().equals(titularQueLlama)) {
                throw CU64TraspasarCupo.noEsTitular();
            }
            var aporte = subastas.aporteDelGrupo(dsl, grupoId).orElseThrow();
            Optional<Impedimento> impedimento = !"OCUPADO".equals(cupo.estado()) || cupo.turnoCobrado()
                    ? Optional.of(Impedimento.CUPO_NO_TRASPASABLE)
                    : salienteAlDia ? Optional.empty() : Optional.of(Impedimento.SALIENTE_CON_DEUDA);
            return new Subastabilidad(
                    grupoId,
                    cupoId,
                    titularQueLlama.get(),
                    impedimento,
                    aporte.monto(),
                    aporte.moneda(),
                    aporte.nombreDelGrupo(),
                    aporte.periodicidad());
        });
    }

    /**
     * Lo mismo que se le mira a quien se postula (CU-68), en el mismo orden: restriccion, KYC, reputacion (solo a quien
     * tiene historial) y maximo de grupos. Quien ya esta dentro del grupo no ocupa un lugar nuevo.
     */
    @Transactional(readOnly = true)
    public Optional<NoElegible> elegibilidad(
            UUID grupoId,
            boolean restriccionVigente,
            boolean kycSuficiente,
            boolean reputacionAlcanza,
            ContextoSesion ctx) {
        if (restriccionVigente) {
            return Optional.of(NoElegible.RESTRICCION_VIGENTE);
        }
        if (!kycSuficiente) {
            return Optional.of(NoElegible.KYC_INSUFICIENTE);
        }
        if (!reputacionAlcanza) {
            return Optional.of(NoElegible.REPUTACION_INSUFICIENTE);
        }
        return datos.conContexto(ctx, dsl -> {
            boolean yaEsta =
                    subastas.participanteActivo(dsl, grupoId, ctx.usuarioId()).isPresent();
            boolean sinLugar = !yaEsta
                    && emparejamientos.participacionesVivas(dsl, ctx.usuarioId())
                            >= emparejamientos.maximoDeParticipaciones();
            return sinLugar ? Optional.of(NoElegible.MAXIMO_DE_GRUPOS) : Optional.<NoElegible>empty();
        });
    }

    /**
     * El cupo pasa al ganador en una sola transaccion: alta (si hace falta), traspaso y evento. El candado del cupo
     * serializa dos cierres simultaneos; el reintento devuelve el mismo traspaso.
     */
    @Transactional
    public Traspaso traspasar(
            UUID grupoId,
            UUID cupoId,
            UUID subastaId,
            UUID ganadorUsuarioId,
            UUID salienteParticipanteId,
            ContextoSesion ctx) {
        var ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(
                ctx,
                dsl -> Datos.comoSistema(dsl, () -> {
                    var cupo = traspasos
                            .bloquearCupo(dsl, cupoId)
                            .filter(c -> c.grupoId().equals(grupoId))
                            .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(64, 1), "Ese cupo no existe."));

                    var yaDentro = subastas.participanteActivo(dsl, grupoId, ganadorUsuarioId);
                    if (yaDentro.isPresent() && cupo.participanteId().equals(yaDentro)) {
                        var previo = subastas.traspasoHacia(dsl, cupoId, yaDentro.get());
                        if (previo.isPresent()) {
                            return new Traspaso(previo.get(), yaDentro.get());
                        }
                    }
                    if (!cupo.participanteId().equals(Optional.of(salienteParticipanteId))
                            || !"OCUPADO".equals(cupo.estado())
                            || cupo.turnoCobrado()) {
                        var motivo = TraspasoAdmisible.Motivo.CUPO_NO_TRASPASABLE;
                        throw new ErrorDeNegocio(CodigoError.de(64, motivo.numero()), motivo.mensaje());
                    }

                    UUID entrante =
                            yaDentro.orElseGet(() -> subastas.altaDelGanador(dsl, grupoId, ganadorUsuarioId, ahora));
                    UUID traspaso = traspasos.registrar(
                            dsl, cupoId, salienteParticipanteId, entrante, MOTIVO, true, Optional.empty(), ahora);
                    traspasos.traspasar(dsl, cupoId, entrante, salienteParticipanteId, MOTIVO, ahora);

                    outbox.emitir(
                            dsl,
                            new EventoDominio(
                                    "grupos.cupo_traspasado",
                                    "traspaso_cupo",
                                    traspaso,
                                    Map.of(
                                            "grupoId", grupoId.toString(),
                                            "cupoId", cupoId.toString(),
                                            "motivo", "SUBASTA",
                                            "subastaId", subastaId.toString()),
                                    UUID.fromString(ctx.traza().id())));
                    return new Traspaso(traspaso, entrante);
                }));
    }
}
