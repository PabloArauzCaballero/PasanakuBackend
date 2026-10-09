package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.SnapshotDeSorteo;
import bo.aportaya.grupos.infraestructura.SorteoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.SorteoVerificable;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-60 · Sortear los turnos, en dos actos: compromiso y revelacion.
 *
 * <p>El compromiso publica **solo el hash**. Ese hash ata la semilla, las entropias de los participantes y un
 * <b>snapshot</b> del plantel, el calendario y las reglas del grupo, congelados en la misma transaccion: quien
 * sortea no puede cambiar quienes entran, ni cuando se cobra, ni la semilla, despues de publicarlo. El
 * resultado se calcula SIEMPRE sobre el snapshot, no sobre lo que haya en la base el dia de revelar.
 *
 * <p>Hay un solo resultado vigente: revelar de nuevo no sortea otra vez (el unico camino que "reintenta" es
 * {@link #resultadoVigente}, que devuelve el original), y un sorteo que no verifica queda ANULADO y no se
 * rehace solo (la base admite un sorteo por grupo).
 *
 * <p>La revelacion va entera en una transaccion: **o hay turnos completos o no hay ninguno**.
 */
@Service
public class CU60Sortear {

    private static final String ALGORITMO = "FISHER_YATES_SHA256";

    private final Datos datos;
    private final SorteoRepositorio sorteos;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Ids ids;

    public CU60Sortear(Datos datos, SorteoRepositorio sorteos, Outbox outbox, Reloj reloj, Ids ids) {
        this.datos = datos;
        this.sorteos = sorteos;
        this.outbox = outbox;
        this.reloj = reloj;
        this.ids = ids;
    }

    /** Fase 1: se publica el hash y nada mas. La semilla queda sellada junto al snapshot hasta revelar. */
    @Transactional
    public Compromiso comprometer(
            UUID grupoId, List<String> entropias, Optional<OffsetDateTime> fechaPrevista, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        // Azar criptografico: una semilla que se puede adivinar es un sorteo que se
        // puede adivinar, y el compromiso no protegeria de nada.
        String semilla = ids.nuevo().toString() + ids.nuevo();

        return datos.conContexto(ctx, dsl -> {
            // Dos compromisos simultaneos del mismo grupo se serializan aca: el segundo ve al primero.
            dsl.execute("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", "sorteo:" + grupoId);
            // AP-CU60-01: no se sortea un grupo a medio armar.
            if (!sorteos.estaConformado(dsl, grupoId)) {
                throw new ErrorDeNegocio(
                        CodigoError.de(60, 1), "Todavia quedan cupos libres: el grupo no esta conformado.");
            }
            // AP-CU60-05: cada participante con su reglamento aceptado.
            if (!sorteos.todosAceptaronElReglamento(dsl, grupoId)) {
                throw new ErrorDeNegocio(CodigoError.de(60, 5), "Falta que alguien acepte el reglamento del grupo.");
            }
            // AP-CU60-02: el compromiso se publica una sola vez.
            if (sorteos.yaHuboSorteo(dsl, grupoId)) {
                throw new ErrorDeNegocio(CodigoError.de(60, 2), "Este grupo ya tiene su sorteo.");
            }
            var snapshot = new SnapshotDeSorteo(
                    sorteos.plantelOcupado(dsl, grupoId),
                    sorteos.calendarioDelGrupo(dsl, grupoId),
                    sorteos.reglasDelGrupo(dsl, grupoId));
            List<String> completas = conSnapshot(entropias, snapshot);
            String hash = SorteoVerificable.hashDelCompromiso(semilla, completas);
            UUID correlacion = UUID.fromString(ctx.traza().id());
            UUID sorteoId = sorteos.comprometer(
                    dsl, grupoId, hash, ALGORITMO, ctx.usuarioId(), ahora, fechaPrevista, completas);
            sorteos.guardarSnapshot(dsl, sorteoId, grupoId, snapshot, semilla, correlacion, ahora);
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "grupos.sorteo_comprometido",
                            "sorteo_turnos",
                            sorteoId,
                            Map.of(
                                    "grupoId", grupoId.toString(),
                                    "hashSemilla", hash,
                                    "hashSnapshot", snapshot.hash()),
                            correlacion));
            return new Compromiso(sorteoId, hash, semilla);
        });
    }

    /**
     * Fase 2: se verifica y recien entonces se crean los turnos.
     *
     * <p>Con {@code semilla} nula la revelacion usa la semilla sellada en el compromiso (el camino HTTP:
     * ningun cliente puede probar semillas). Los periodos y el monto que lleguen como argumento son
     * heredados: manda el calendario y la regla congelados.
     */
    @Transactional
    public Revelacion revelar(
            UUID sorteoId,
            String semilla,
            List<String> entropias,
            List<UUID> periodosEnOrden,
            BigDecimal montoEstimado,
            Optional<OffsetDateTime> fechaPrevista,
            ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var sorteo = sorteos.bloquear(dsl, sorteoId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(60, 2), "Ese sorteo no existe."));
            if ("REVELADO".equals(sorteo.estado())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(60, 7), "Este sorteo ya fue revelado: su resultado vigente es unico.");
            }
            if ("ANULADO".equals(sorteo.estado())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(60, 8),
                        "Este sorteo fue anulado y no se repite sin una decision de backoffice.");
            }
            var congelado = sorteos.snapshotDe(dsl, sorteoId);

            // AP-CU60-03: no se revela antes de la fecha comprometida. El compromiso
            // pierde sentido si quien lo publico puede adelantarlo cuando le conviene.
            if (fechaPrevista.map(ahora::isBefore).orElse(false)) {
                throw new ErrorDeNegocio(CodigoError.de(60, 3), "Todavia no llego la fecha comprometida para revelar.");
            }
            String semillaUsada = semilla != null
                    ? semilla
                    : congelado.map(c -> c.semillaSellada()).orElse(null);
            if (semillaUsada == null) {
                throw new ErrorDeNegocio(CodigoError.de(60, 4), "No hay semilla con la cual revelar este sorteo.");
            }
            List<String> entropiasUsadas = entropias == null
                    ? sorteo.entropias()
                    : congelado.map(c -> conSnapshot(entropias, c.snapshot())).orElse(entropias);

            if (!SorteoVerificable.verificarCompromiso(semillaUsada, entropiasUsadas, sorteo.hash())) {
                if (semilla == null) {
                    // La semilla sellada es la comprometida: lo que no cierra son las entropias que llegaron.
                    throw new ErrorDeNegocio(CodigoError.de(60, 4), "Las entropias no coinciden con el compromiso.");
                }
                // Jamas se publica un resultado cuyo hash no cierra. El sorteo se anula; el anterior queda visible.
                sorteos.anular(dsl, sorteoId, ahora);
                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "grupos.sorteo_anulado",
                                "sorteo_turnos",
                                sorteoId,
                                Map.of("grupoId", sorteo.grupoId().toString(), "motivo", "hash_no_verifica"),
                                UUID.fromString(ctx.traza().id())));
                return new Revelacion(sorteoId, List.of(), false, null);
            }

            List<UUID> cupos;
            List<UUID> periodos;
            BigDecimal monto;
            if (congelado.isPresent()) {
                var snapshot = congelado.get().snapshot();
                // El plantel y el calendario son los del compromiso: si algo cambio, no se sortea sobre otra cosa.
                exigirSinCambios(dsl, sorteo.grupoId(), snapshot);
                cupos = snapshot.cuposEnOrden();
                periodos = snapshot.periodos().isEmpty()
                        ? sorteos.calendarioDelGrupo(dsl, sorteo.grupoId()).stream()
                                .map(SnapshotDeSorteo.PeriodoCongelado::periodoId)
                                .toList()
                        : snapshot.periodosEnOrden();
                monto = montoCongelado(snapshot.reglas()).orElse(montoEstimado);
            } else {
                cupos = sorteos.cuposPorNumero(dsl, sorteo.grupoId());
                periodos = periodosEnOrden;
                monto = montoEstimado;
            }
            if (periodos == null || periodos.size() != cupos.size()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(60, 9), "Falta el calendario del grupo: cada cupo necesita su periodo.");
            }
            List<UUID> enOrden = SorteoVerificable.barajarDeterminista(semillaUsada, cupos);
            sorteos.crearTurnos(dsl, sorteo.grupoId(), periodos, enOrden, monto);
            sorteos.revelar(dsl, sorteoId, semillaUsada, comoJson(enOrden), ahora);

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "grupos.sorteo_revelado",
                            "sorteo_turnos",
                            sorteoId,
                            Map.of("grupoId", sorteo.grupoId().toString(), "semilla", semillaUsada),
                            UUID.fromString(ctx.traza().id())));

            return new Revelacion(sorteoId, enOrden, true, semillaUsada);
        });
    }

    /** El resultado ya revelado, tal cual se publico; vacio si todavia no hay uno. Un reintento nunca sortea. */
    @Transactional(readOnly = true)
    public Optional<Revelacion> resultadoVigente(UUID sorteoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> sorteos.ver(dsl, sorteoId)
                .filter(s -> "REVELADO".equals(s.estado()))
                .map(s -> new Revelacion(
                        sorteoId, s.resultado().stream().map(UUID::fromString).toList(), true, s.semillaPublica())));
    }

    /** El grupo al que pertenece un sorteo, para que quien llega por la ruta de OTRO grupo no lo toque. */
    @Transactional(readOnly = true)
    public Optional<UUID> grupoDelSorteo(UUID sorteoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> sorteos.ver(dsl, sorteoId).map(SorteoRepositorio.Sorteo::grupoId));
    }

    private void exigirSinCambios(DSLContext dsl, UUID grupoId, SnapshotDeSorteo snapshot) {
        var vivo = new SnapshotDeSorteo(sorteos.plantelOcupado(dsl, grupoId), snapshot.periodos(), snapshot.reglas());
        boolean calendarioIgual = snapshot.periodos().isEmpty()
                || new SnapshotDeSorteo(snapshot.roster(), sorteos.calendarioDelGrupo(dsl, grupoId), snapshot.reglas())
                        .periodosCanonico()
                        .equals(snapshot.periodosCanonico());
        if (!vivo.rosterCanonico().equals(snapshot.rosterCanonico()) || !calendarioIgual) {
            throw new ErrorDeNegocio(
                    CodigoError.de(60, 6),
                    "El plantel o el calendario cambiaron despues del compromiso: no se sortea sobre otra cosa.");
        }
    }

    private static List<String> conSnapshot(List<String> entropias, SnapshotDeSorteo snapshot) {
        List<String> completas = new ArrayList<>(entropias == null ? List.of() : entropias);
        completas.add(snapshot.entropia());
        return List.copyOf(completas);
    }

    /** Aporte por cupo, del texto canonico de reglas: el monto estimado del turno sale de lo congelado. */
    private static Optional<BigDecimal> montoCongelado(String reglas) {
        BigDecimal aporte = null;
        BigDecimal cupos = null;
        for (String linea : reglas.split("\n")) {
            if (linea.startsWith("monto=")) aporte = new BigDecimal(linea.substring(6));
            if (linea.startsWith("cupos=")) cupos = new BigDecimal(linea.substring(6));
        }
        return aporte == null || cupos == null ? Optional.empty() : Optional.of(aporte.multiply(cupos));
    }

    private String comoJson(List<UUID> cupos) {
        return cupos.stream().map(c -> "\"" + c + "\"").collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    /** La semilla viaja solo hacia quien llama al caso de uso; {@code toString} no la muestra. */
    public record Compromiso(UUID sorteoId, String hashSemilla, String semilla) {
        @Override
        public String toString() {
            return "Compromiso[sorteoId=" + sorteoId + ", hashSemilla=" + hashSemilla + ", semilla=SELLADA]";
        }
    }

    /** {@code semilla} es la revelada (publica una vez revelado); nula si el sorteo no verifico. */
    public record Revelacion(UUID sorteoId, List<UUID> cuposEnOrden, boolean verificado, String semilla) {}
}
