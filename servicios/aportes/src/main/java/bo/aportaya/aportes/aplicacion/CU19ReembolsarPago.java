package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.dominio.MotivoDeReembolso;
import bo.aportaya.aportes.dominio.SaldoDeLaObligacion;
import bo.aportaya.aportes.dominio.TipoDeDisputa;
import bo.aportaya.aportes.infraestructura.AuditoriaRepositorio;
import bo.aportaya.aportes.infraestructura.DisputaPagoRepositorio;
import bo.aportaya.aportes.infraestructura.ObligacionRepositorio;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-19 · Reembolsar un pago y atender una disputa.
 *
 * <p>Devolver plata exige **cuatro ojos**: quien aprueba no puede ser quien solicita.
 * Un reembolso es la via mas corta para sacar dinero del sistema con una excusa
 * verosimil, y por eso la separacion de funciones no es formalismo.
 *
 * <p>Al reembolsar, la obligacion **vuelve a estar pendiente por ese importe**. Es lo
 * que hace que el grupo no pierda: si la cuota queda dada por pagada, el faltante lo
 * absorben los demas participantes sin enterarse.
 */
@Service
public class CU19ReembolsarPago {

    private final Datos datos;
    private final PagoRepositorio pagos;
    private final DisputaPagoRepositorio disputas;
    private final ObligacionRepositorio obligaciones;
    private final Consumidos consumidos;
    private final Outbox outbox;
    private final AuditoriaRepositorio auditoria;
    private final Reloj reloj;
    private final Duration plazoDeDisputa;

    public CU19ReembolsarPago(
            Datos datos,
            PagoRepositorio pagos,
            DisputaPagoRepositorio disputas,
            ObligacionRepositorio obligaciones,
            Consumidos consumidos,
            Outbox outbox,
            AuditoriaRepositorio auditoria,
            Reloj reloj,
            @Value("${aportaya.reembolso.plazo-de-disputa}") Duration plazoDeDisputa) {
        this.datos = datos;
        this.pagos = pagos;
        this.disputas = disputas;
        this.obligaciones = obligaciones;
        this.consumidos = consumidos;
        this.outbox = outbox;
        this.auditoria = auditoria;
        this.reloj = reloj;
        this.plazoDeDisputa = plazoDeDisputa;
    }

    private PertenenciaDelParticipante pertenencia;

    /** Por setter: el constructor no cambia (las pruebas de integracion lo arman a mano). */
    @org.springframework.beans.factory.annotation.Autowired
    void conPertenencia(PertenenciaDelParticipante pertenencia) {
        this.pertenencia = pertenencia;
    }

    /**
     * F1.L1.M07 · si el pago es de una obligacion de quien pide el reembolso. Se pregunta ANTES de
     * {@link #solicitar}, fuera de su transaccion: la pertenencia la sabe {@code grupos} (invariante 6).
     */
    public boolean esPagoDelSolicitante(UUID pagoId, ContextoSesion ctx) {
        return pertenencia.pagoEsDelUsuario(pagoId, ctx);
    }

    @Transactional
    public SalidaSolicitud solicitar(EntradaReembolso entrada, ContextoSesion ctx) {
        // El motivo se valida antes de abrir nada: la base solo admite cuatro, y
        // enterarse en el INSERT convierte una regla de negocio en un error 500.
        String motivo = MotivoDeReembolso.exigir(entrada.motivo());
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            // F1.L1.M09 · R-BIL-28: la misma clave sobre el mismo pago es la misma solicitud. El candado serializa
            // dos reintentos simultaneos; el indice unico es el respaldo si algo lo saltea.
            pagos.bloquearIdempotenciaDeReembolso(dsl, entrada.pagoId(), entrada.claveIdempotencia());
            var previo = pagos.reembolsoPorClave(dsl, entrada.pagoId(), entrada.claveIdempotencia());
            if (previo.isPresent()) {
                return repetido(dsl, previo.get(), entrada, ctx);
            }

            var pago = pagos.ver(dsl, entrada.pagoId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(19, 1), "Ese pago no existe."));

            // AP-CU19-01: no se reembolsa lo que todavia no entro. Devolver contra un
            // pago sin conciliar es regalar plata que quiza nunca llegue.
            if (!"ACREDITADO".equals(pago.estado()) && !"CONCILIADO".equals(pago.estado())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(19, 1), "Ese pago esta " + pago.estado() + ": todavia no se puede reembolsar.");
            }

            // F1.L1.M12 · AP-CU19-02: se devuelve en la moneda en que se pago, sin conversion implicita.
            if (entrada.monto().moneda() != pago.monto().moneda()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(19, 2),
                        "El reembolso tiene que ser en " + pago.monto().moneda() + ", la moneda del pago.");
            }

            // AP-CU19-02: contra lo YA reembolsado, no contra el monto original. Dos
            // reembolsos parciales que juntos superan el pago devuelven de mas.
            Dinero yaDevuelto = pagos.reembolsadoDe(dsl, pago.id(), pago.monto().moneda());
            Dinero disponible = pago.monto().menos(yaDevuelto);
            if (entrada.monto().esMayorQue(disponible)) {
                throw new ErrorDeNegocio(
                        CodigoError.de(19, 2),
                        "El monto excede el pago: quedan " + disponible + " por reembolsar.",
                        Map.of("pagado", pago.monto().toString(), "yaReembolsado", yaDevuelto.toString()));
            }

            UUID reembolsoId = pagos.solicitarReembolso(
                    dsl, pago.id(), entrada.monto(), motivo, ctx.usuarioId(), entrada.claveIdempotencia(), ahora);

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "aportes.reembolso_solicitado",
                            "reembolso",
                            reembolsoId,
                            Map.of(
                                    "pagoId",
                                    pago.id().toString(),
                                    "monto",
                                    entrada.monto().toString()),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaSolicitud(reembolsoId, "SOLICITADO", disponible.menos(entrada.monto()));
        });
    }

    /**
     * El reintento devuelve la solicitud original, con lo que queda disponible hoy. Si la clave se usa para otra
     * cosa (otro monto, otra persona), no es un reintento: es un error del cliente.
     */
    private SalidaSolicitud repetido(
            org.jooq.DSLContext dsl, PagoRepositorio.Reembolso previo, EntradaReembolso entrada, ContextoSesion ctx) {
        if (!previo.monto().equals(entrada.monto()) || !previo.solicitadoPor().equals(ctx.usuarioId())) {
            throw new ErrorDeNegocio(CodigoError.de(19, 2), "La clave de idempotencia ya identifica otra solicitud.");
        }
        var pago = pagos.ver(dsl, previo.pagoId()).orElseThrow();
        Dinero disponible = pago.monto()
                .menos(pagos.reembolsadoDe(dsl, pago.id(), pago.monto().moneda()));
        return new SalidaSolicitud(previo.id(), previo.estado(), disponible);
    }

    /** Aprobar y ejecutar. Quien aprueba no puede ser quien solicito. */
    @Transactional
    public SalidaEjecucion aprobar(UUID reembolsoId, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var reembolso = pagos.verReembolso(dsl, reembolsoId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(19, 3), "Ese reembolso no existe."));

            // AP-CU19-04 · R-SEG-04.
            if (reembolso.solicitadoPor().equals(ctx.usuarioId())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(19, 4), "Quien aprueba un reembolso no puede ser quien lo solicito.");
            }
            if (!pagos.ejecutarReembolso(dsl, reembolsoId, ctx.usuarioId(), ahora)) {
                // F1.L1.M09 · el reintento de quien ya lo aprobo recibe la misma respuesta, sin volver a mover nada.
                if (ctx.usuarioId().equals(pagos.ejecutadoPor(dsl, reembolsoId).orElse(null))) {
                    var pago = pagos.ver(dsl, reembolso.pagoId()).orElseThrow();
                    var obligacion = obligaciones.ver(dsl, pago.obligacionId()).orElseThrow();
                    return new SalidaEjecucion(reembolsoId, "EJECUTADO", obligacion.id(), obligacion.estado());
                }
                throw new ErrorDeNegocio(CodigoError.de(19, 3), "Ese reembolso ya no esta solicitado.");
            }

            var pago = pagos.ver(dsl, reembolso.pagoId()).orElseThrow();
            var obligacion = obligaciones.bloquear(dsl, pago.obligacionId()).orElseThrow();

            // La obligacion vuelve a estar pendiente por ese importe: si quedara dada
            // por pagada, el faltante lo absorben los demas del grupo sin enterarse.
            var saldoDespues = new SaldoDeLaObligacion.Estado(
                    obligacion.saldo().esperado(),
                    obligacion.saldo().pagado().menos(reembolso.monto()),
                    obligacion.saldo().condonado(),
                    obligacion.saldo().cubiertoPorGarantia());
            boolean vencida = obligacion.finDeGracia().isBefore(reloj.hoy());
            String estadoNuevo = SaldoDeLaObligacion.estadoSegunSaldo(saldoDespues, vencida);
            obligaciones.revertirPago(dsl, obligacion.id(), reembolso.monto(), estadoNuevo);

            // H5/H7.S6 (carril PR4-seguridad): la aprobacion de un reembolso es una
            // operacion critica — deja rastro append-only con quien aprobo, sobre que
            // reembolso, y el estado nuevo. Sin montos de terceros ni tokens: solo lo
            // que un auditor necesita para reconstruir el hecho.
            auditoria.registrar(
                    dsl,
                    "reembolso",
                    reembolsoId,
                    "APROBACION",
                    ctx.usuarioId(),
                    ctx.rol(),
                    UUID.fromString(ctx.traza().id()),
                    null,
                    "{\"estado\":\"EJECUTADO\",\"obligacionId\":\"%s\"}".formatted(obligacion.id()),
                    null,
                    ahora);

            // El movimiento de dinero lo hace nucleo-financiero (invariante 12).
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "aportes.reembolso_ejecutado",
                            "reembolso",
                            reembolsoId,
                            Map.of(
                                    "pagoId", pago.id().toString(),
                                    "obligacionId", obligacion.id().toString(),
                                    "monto", reembolso.monto().toString()),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaEjecucion(reembolsoId, "EJECUTADO", obligacion.id(), estadoNuevo);
        });
    }

    /**
     * Registra una disputa del proveedor, con su plazo **guardado**.
     *
     * <p>El plazo no se recalcula al mirar el tablero (invariante 8): moverlo cada vez
     * que cambie la politica es un argumento que el proveedor no acepta.
     */
    @Transactional
    public SalidaDisputa registrarDisputa(EntradaDisputa entrada, ContextoSesion ctx) {
        String tipo = TipoDeDisputa.exigir(entrada.tipo());
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            // AP-CU19-06: el proveedor reenvia. Una disputa abierta por pago basta.
            UUID idMensaje = UUID.nameUUIDFromBytes(
                    entrada.referenciaDelProveedor().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (!consumidos.registrar(dsl, idMensaje, "proveedor-pago")) {
                return new SalidaDisputa(null, ahora, false);
            }
            if (disputas.hayDisputaAbierta(dsl, entrada.pagoId())) {
                return new SalidaDisputa(null, ahora, false);
            }

            OffsetDateTime limite = ahora.plus(plazoDeDisputa);
            UUID disputaId = disputas.abrirDisputa(
                    dsl, entrada.pagoId(), tipo, entrada.descripcion(), entrada.montoDisputado(), "[]", ahora, limite);

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "aportes.disputa_abierta",
                            "disputa_pago",
                            disputaId,
                            Map.of(
                                    "pagoId", entrada.pagoId().toString(),
                                    "fechaLimiteRespuesta", limite.toString()),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaDisputa(disputaId, limite, true);
        });
    }

    /** {@code claveIdempotencia}: la {@code Idempotency-Key} de la peticion (F1.L1.M09, R-BIL-28). */
    public record EntradaReembolso(UUID pagoId, Dinero monto, String motivo, String claveIdempotencia) {}

    public record SalidaSolicitud(UUID reembolsoId, String estado, Dinero disponibleRestante) {}

    public record SalidaEjecucion(UUID reembolsoId, String estado, UUID obligacionId, String estadoObligacion) {}

    public record EntradaDisputa(
            UUID pagoId, String tipo, String descripcion, Dinero montoDisputado, String referenciaDelProveedor) {}

    public record SalidaDisputa(UUID disputaId, OffsetDateTime fechaLimiteRespuesta, boolean esNueva) {}
}
