package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera.Pata;
import bo.aportaya.nucleofinanciero.infraestructura.ReversoRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.SolicitudReversoRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.SolicitudReversoRepositorio.Solicitud;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
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
 * CU-14 · Reversar una transaccion.
 *
 * <p>**Reversar es escribir el espejo, no borrar el original.** El libro es
 * append-only (R-AUD-01) y la unica correccion valida es el movimiento inverso. La
 * transaccion original conserva sus movimientos sin un solo cambio: lo que paso, paso,
 * y el extracto tiene que poder contar la historia completa — el cobro y su
 * devolucion, no un cobro que se desvanecio.
 *
 * <p>Cuatro ojos reales (P-13, R-BIL-27): son dos actos de dos personas, cada una con su
 * propio token. {@link #solicitar} guarda a quien lo pide; {@link #autorizar}, en otra
 * peticion, lo ejecuta con quien autoriza. Ningun actor sale del cuerpo de la peticion: un
 * reverso es la operacion con la que se puede sacar plata de cualquier cuenta sin que el
 * titular intervenga (R-SEG-04).
 */
@Service
public class CU14ReversarTransaccion {

    private static final int MOTIVO_MINIMO = 20;

    private final Datos datos;
    private final ReversoRepositorio reversos;
    private final SolicitudReversoRepositorio solicitudes;
    private final CuentaBilleteraRepositorio cuentas;
    private final LibroDeBilletera libro;
    private final Outbox outbox;
    private final Reloj reloj;
    private final CargoDelReverso cargo;

    public CU14ReversarTransaccion(
            Datos datos,
            ReversoRepositorio reversos,
            SolicitudReversoRepositorio solicitudes,
            CuentaBilleteraRepositorio cuentas,
            LibroDeBilletera libro,
            Outbox outbox,
            Reloj reloj,
            CargoDelReverso cargo) {
        this.datos = datos;
        this.reversos = reversos;
        this.solicitudes = solicitudes;
        this.cuentas = cuentas;
        this.libro = libro;
        this.outbox = outbox;
        this.reloj = reloj;
        this.cargo = cargo;
    }

    /** Primer acto: registra la solicitud, con quien la pide tomado de la sesion. No mueve saldo. */
    @Transactional
    public SalidaSolicitud solicitar(EntradaReverso entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        if (entrada.motivo() == null || entrada.motivo().trim().length() < MOTIVO_MINIMO) {
            // Un motivo de tres palabras no explica nada seis meses despues, que es
            // justo cuando alguien pregunta por que se le saco plata de la cuenta.
            throw new ErrorDeNegocio(
                    CodigoError.de(14, 2), "El motivo del reverso tiene que explicar que paso, no resumirlo.");
        }
        return datos.conContexto(ctx, dsl -> {
            var previa = solicitudes.porClave(dsl, entrada.claveIdempotencia());
            if (previa.isPresent()) {
                return repetida(previa.get(), entrada, ctx);
            }
            exigirReversable(dsl, entrada.transaccionOriginalId());
            var creada = solicitudes.crear(
                    dsl,
                    entrada.transaccionOriginalId(),
                    ctx.usuarioId(),
                    entrada.claveIdempotencia(),
                    entrada.tipo(),
                    entrada.motivo(),
                    ahora);
            if (creada.isEmpty()) {
                // Un reintento concurrente con la misma clave gano la carrera, o ya hay otra solicitud viva.
                return solicitudes
                        .porClave(dsl, entrada.claveIdempotencia())
                        .map(s -> repetida(s, entrada, ctx))
                        .orElseThrow(() -> new ErrorDeNegocio(
                                CodigoError.de(14, 1), "Esa transaccion ya tiene un reverso pendiente de autorizar."));
            }
            return new SalidaSolicitud(creada.get(), "SOLICITADA");
        });
    }

    /** Quien pidio la solicitud, para que la capa web lo compare con quien autoriza (CuatroOjos). */
    @Transactional(readOnly = true)
    public UUID solicitadaPor(UUID solicitudId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> solicitudes
                .ver(dsl, solicitudId)
                .map(Solicitud::solicitadaPor)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(14, 5), "Esa solicitud de reverso no existe.")));
    }

    /**
     * Segundo acto: otra persona autoriza y el reverso se ejecuta, una sola vez.
     *
     * <p>Quien autoriza es {@code ctx.usuarioId()}, del token. La capa web ya lo comparo con
     * {@code CuatroOjos}; aca se vuelve a exigir porque el caso de uso no confia en quien lo
     * llama, y la base lo exige una tercera vez ({@code ck_solicitud_reverso_cuatro_ojos}).
     */
    @Transactional
    public SalidaReverso autorizar(UUID solicitudId, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            var solicitud = solicitudes
                    .bloquear(dsl, solicitudId)
                    .orElseThrow(
                            () -> new ErrorDeNegocio(CodigoError.de(14, 5), "Esa solicitud de reverso no existe."));
            // AP-CU14-02 · R-SEG-04 · R-BIL-27.
            if (solicitud.solicitadaPor().equals(ctx.usuarioId()) || ctx.esSistema()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(14, 2), "Quien autoriza un reverso no puede ser quien lo solicita.");
            }
            if (!solicitud.pendiente()) {
                throw new ErrorDeNegocio(CodigoError.de(14, 5), "Esa solicitud de reverso ya fue resuelta.");
            }
            var salida = ejecutar(dsl, solicitud, ctx.usuarioId(), ctx, ahora);
            if (!solicitudes.marcarEjecutada(dsl, solicitud.id(), ctx.usuarioId(), salida.reversoId(), ahora)) {
                throw new ErrorDeNegocio(CodigoError.de(14, 5), "Otra autorizacion resolvio esa solicitud primero.");
            }
            return salida;
        });
    }

    private SalidaSolicitud repetida(Solicitud previa, EntradaReverso entrada, ContextoSesion ctx) {
        if (!previa.transaccionOriginalId().equals(entrada.transaccionOriginalId())
                || !previa.solicitadaPor().equals(ctx.usuarioId())) {
            throw new ErrorDeNegocio(CodigoError.de(14, 1), "La clave de idempotencia ya identifica otra solicitud.");
        }
        return new SalidaSolicitud(previa.id(), previa.estado());
    }

    private ReversoRepositorio.Original exigirReversable(DSLContext dsl, UUID transaccionId) {
        var original = reversos.original(dsl, transaccionId)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(14, 1), "Esa transaccion no existe."));
        // AP-CU14-01 · R-BIL-15.
        if (!original.esReversable()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(14, 1), "Esa transaccion esta " + original.estado() + ": no se reversa.");
        }
        if (reversos.yaFueReversada(dsl, original.id())) {
            throw new ErrorDeNegocio(CodigoError.de(14, 1), "Esa transaccion ya fue reversada.");
        }
        return original;
    }

    private SalidaReverso ejecutar(
            DSLContext dsl, Solicitud solicitud, UUID autorizadaPor, ContextoSesion ctx, OffsetDateTime ahora) {
        var original = exigirReversable(dsl, solicitud.transaccionOriginalId());

        // Se bloquean las cuentas que el reverso va a tocar, en orden de
        // identificador: es el mismo cuidado que en la transferencia.
        original.patas().stream()
                .map(ReversoRepositorio.Movimiento::cuentaId)
                .distinct()
                .sorted()
                .forEach(cuenta -> cuentas.bloquear(dsl, cuenta));

        // AP-CU14-03: si el espejo dejaria una cuenta en negativo, no se fuerza.
        // Se registra una obligacion de restitucion y el saldo NO baja de cero —
        // el error fue nuestro y la deuda queda a la vista, no escondida en un
        // saldo negativo que la persona no entiende.
        List<Pata> espejo = new ArrayList<>();
        boolean generaRestitucion = false;
        for (var pata : original.patas()) {
            if ("CREDITO".equals(pata.sentido())) {
                var cuenta = cuentas.ver(dsl, pata.cuentaId()).orElseThrow();
                if (pata.monto().esMayorQue(cuenta.disponible()) && !cuenta.permiteNegativo()) {
                    generaRestitucion = true;
                }
            }
            espejo.add(new Pata(pata.cuentaId(), pata.sentidoInverso(), pata.monto(), "Reverso de: " + pata.glosa()));
        }

        if (generaRestitucion) {
            UUID reversoId = reversos.registrar(
                    dsl,
                    original.id(),
                    null,
                    autorizadaPor,
                    solicitud.tipo(),
                    solicitud.motivo(),
                    original.montoTotal(),
                    ahora);
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "nucleo_financiero.restitucion_requerida",
                            "reverso_transaccion",
                            reversoId,
                            Map.of(
                                    "transaccionOriginalId", original.id().toString(),
                                    "monto", original.montoTotal().toString(),
                                    "motivo", solicitud.motivo()),
                            UUID.fromString(ctx.traza().id())));
            return new SalidaReverso(reversoId, null, true);
        }

        // La clave del libro sale de la solicitud: autorizar dos veces no puede asentar dos veces.
        UUID transaccionReverso = libro.registrar(
                dsl,
                "REVERSO",
                "AJUSTE",
                original.id(),
                "API",
                original.montoTotal(),
                "reverso:" + solicitud.id(),
                Optional.of(autorizadaPor),
                espejo,
                ahora);

        UUID reversoId = reversos.registrar(
                dsl,
                original.id(),
                transaccionReverso,
                autorizadaPor,
                solicitud.tipo(),
                solicitud.motivo(),
                original.montoTotal(),
                ahora);
        cargo.revertir(dsl, original, transaccionReverso, ctx);

        outbox.emitir(
                dsl,
                new EventoDominio(
                        "nucleo_financiero.transaccion_reversada",
                        "reverso_transaccion",
                        reversoId,
                        Map.of(
                                "transaccionOriginalId", original.id().toString(),
                                "transaccionReversoId", transaccionReverso.toString(),
                                "tipo", solicitud.tipo()),
                        UUID.fromString(ctx.traza().id())));

        return new SalidaReverso(reversoId, transaccionReverso, false);
    }

    /** Sin autorizador: quien autoriza es otra peticion, con otro token. */
    public record EntradaReverso(String claveIdempotencia, UUID transaccionOriginalId, String tipo, String motivo) {}

    public record SalidaSolicitud(UUID solicitudId, String estado) {}

    public record SalidaReverso(UUID reversoId, UUID transaccionReversoId, boolean generaObligacionDeRestitucion) {}
}
