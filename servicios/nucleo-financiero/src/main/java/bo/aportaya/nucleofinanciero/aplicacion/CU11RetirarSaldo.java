package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.dominio.EstadoDeRetiro;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiro;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRetiroRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-11 · Retirar saldo.
 *
 * <p>**Retencion primero, pago despues. Nunca al reves.** Si se instruyera el pago
 * antes de retener, entre las dos cosas la persona podria gastar el mismo saldo en
 * otra operacion y el retiro saldria contra un disponible que ya no existe. Retener
 * primero cuesta un paso mas y cierra ese hueco entero.
 *
 * <p>El proveedor se instruye **fuera de la transaccion** (invariante 6): una llamada
 * de red adentro mantiene abierta la transaccion tanto como tarde el proveedor, y si
 * el timeout llega, se revierte una retencion que el proveedor quiza ya acepto.
 */
@Service
public class CU11RetirarSaldo {

    private final Datos datos;
    private final OrdenRetiroRepositorio ordenes;
    private final ProveedorDeRetiro proveedor;
    private final ResolucionDeRetiro resolucion;
    private final SolicitudDeRetiro solicitud;

    // H4.S2.M6 · metricas de negocio, no tecnicas: cuantos retiros se piden, se autorizan (automatico o por
    // aprobacion) y se rechazan (en cualquiera de sus motivos). `withdrawal_*_total`.

    public CU11RetirarSaldo(
            Datos datos,
            CuentaBilleteraRepositorio cuentas,
            OrdenRetiroRepositorio ordenes,
            CU13RetenerSaldo retenciones,
            CU40EvaluarLimites limites,
            LibroDeBilletera libro,
            Outbox outbox,
            Reloj reloj,
            @Value("${aportaya.custodia.cuenta-puente}") UUID cuentaPuenteDeCustodia,
            ProveedorDeRetiro proveedor,
            MeterRegistry metricas) {
        this.datos = datos;
        this.ordenes = ordenes;
        this.proveedor = proveedor;
        Counter retirosSolicitados = Counter.builder("withdrawal_requested_total")
                .description("Retiros solicitados, cualquiera sea su estado inicial")
                .register(metricas);
        Counter retirosAutorizados = Counter.builder("withdrawal_approved_total")
                .description("Retiros que llegaron a AUTORIZADA, automatico o por aprobacion")
                .register(metricas);
        Counter retirosRechazados = Counter.builder("withdrawal_failed_total")
                .description("Retiros rechazados: MFA, limites, proveedor, revision o aprobador")
                .register(metricas);
        this.solicitud = new SolicitudDeRetiro(
                datos,
                cuentas,
                ordenes,
                retenciones,
                limites,
                outbox,
                reloj,
                retirosSolicitados,
                retirosAutorizados,
                retirosRechazados);
        this.resolucion = new ResolucionDeRetiro(
                datos,
                cuentas,
                ordenes,
                retenciones,
                libro,
                outbox,
                reloj,
                cuentaPuenteDeCustodia,
                retirosAutorizados,
                retirosRechazados);
    }

    @Transactional
    public SalidaRetiro solicitar(EntradaRetiro entrada, ContextoSesion ctx) {
        return solicitud.solicitar(entrada, ctx);
    }

    /** Consulta las ordenes EN_PROCESO que recorre la reconciliacion. */
    @Transactional(readOnly = true)
    public java.util.List<OrdenRetiroRepositorio.Orden> ordenesEnProceso(ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> ordenes.enProceso(dsl));
    }

    /** Instruye al proveedor fuera de la transaccion que cambia el estado. */
    public SalidaInstruccion instruirPago(UUID ordenId, ContextoSesion ctx) {
        var orden = datos.conContexto(ctx, dsl -> ordenes.ver(dsl, ordenId))
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(11, 1), "Esa orden no existe."));
        if (!EstadoDeRetiro.AUTORIZADA.name().equals(orden.estado())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(11, 8),
                    "Esa orden esta " + orden.estado() + ": solo se instruye una orden AUTORIZADA.");
        }

        var resultado = proveedor.instruir(ordenId, orden.neto());

        return switch (resultado.estado()) {
            case ACEPTADO, TIMEOUT -> {
                // TIMEOUT tambien queda EN_PROCESO: no se sabe todavia, y el job de
                // reconciliacion (H4.S2.M3) es quien lo resuelve mas tarde — nunca se
                // asume un rechazo que nadie confirmo.
                boolean ok =
                        datos.conContexto(ctx, dsl -> ordenes.pasarAEnProceso(dsl, ordenId, resultado.referencia()));
                if (!ok) {
                    throw new ErrorDeNegocio(
                            CodigoError.de(11, 8), "Esa orden ya no esta AUTORIZADA: otra instruccion la adelanto.");
                }
                yield new SalidaInstruccion(ordenId, EstadoDeRetiro.EN_PROCESO.name(), resultado.referencia());
            }
            case RECHAZADO -> {
                var salidaRechazo = rechazar(ordenId, "Proveedor rechazo la instruccion en firme.", ctx);
                yield new SalidaInstruccion(salidaRechazo.ordenRetiroId(), "RECHAZADA", resultado.referencia());
            }
        };
    }

    @Transactional
    public SalidaPago confirmarPago(UUID ordenId, ContextoSesion ctx) {
        return resolucion.confirmarPago(ordenId, ctx);
    }

    @Transactional
    public SalidaRechazo rechazar(UUID ordenId, String motivo, ContextoSesion ctx) {
        return resolucion.rechazar(ordenId, motivo, ctx);
    }

    @Transactional
    public SalidaAprobacion aprobar(UUID ordenId, ContextoSesion ctx) {
        return resolucion.aprobar(ordenId, ctx);
    }

    @Transactional
    public SalidaAprobacion rechazarRevision(UUID ordenId, ContextoSesion ctx) {
        return resolucion.rechazarRevision(ordenId, ctx);
    }

    public record EntradaRetiro(
            String claveIdempotencia,
            UUID cuentaBilleteraId,
            Dinero monto,
            Dinero costo,
            UUID instrumentoDestinoId,
            boolean mfaVerificado,
            // H2: si vino ALGO en evidenciaMfa/factorMfa, aunque no haya pasado la
            // verificacion — es lo que separa MFA_REQUERIDO (nada) de MFA_INVALIDO
            // (algo, pero no vale).
            boolean evidenciaMfaProvista,
            boolean requiereDobleAprobacion,
            Optional<UUID> cotizacionId) {

        /** Con evidencia de MFA pero sin cotizacion previa aceptada. */
        public EntradaRetiro(
                String claveIdempotencia,
                UUID cuentaBilleteraId,
                Dinero monto,
                Dinero costo,
                UUID instrumentoDestinoId,
                boolean mfaVerificado,
                boolean evidenciaMfaProvista,
                boolean requiereDobleAprobacion) {
            this(
                    claveIdempotencia,
                    cuentaBilleteraId,
                    monto,
                    costo,
                    instrumentoDestinoId,
                    mfaVerificado,
                    evidenciaMfaProvista,
                    requiereDobleAprobacion,
                    Optional.empty());
        }

        /** Sin cotizacion previa aceptada; quien paso el MFA es quien presento evidencia. */
        public EntradaRetiro(
                String claveIdempotencia,
                UUID cuentaBilleteraId,
                Dinero monto,
                Dinero costo,
                UUID instrumentoDestinoId,
                boolean mfaVerificado,
                boolean requiereDobleAprobacion) {
            this(
                    claveIdempotencia,
                    cuentaBilleteraId,
                    monto,
                    costo,
                    instrumentoDestinoId,
                    mfaVerificado,
                    mfaVerificado,
                    requiereDobleAprobacion,
                    Optional.empty());
        }

        /** Con cotizacion previa aceptada; quien paso el MFA es quien presento evidencia. */
        public EntradaRetiro(
                String claveIdempotencia,
                UUID cuentaBilleteraId,
                Dinero monto,
                Dinero costo,
                UUID instrumentoDestinoId,
                boolean mfaVerificado,
                boolean requiereDobleAprobacion,
                Optional<UUID> cotizacionId) {
            this(
                    claveIdempotencia,
                    cuentaBilleteraId,
                    monto,
                    costo,
                    instrumentoDestinoId,
                    mfaVerificado,
                    mfaVerificado,
                    requiereDobleAprobacion,
                    cotizacionId);
        }
    }

    public record SalidaRetiro(
            UUID ordenRetiroId, String estado, Dinero costoRetiro, Dinero montoNeto, UUID retencionId) {}

    public record SalidaPago(UUID ordenRetiroId, UUID transaccionId, Dinero saldoDespues) {}

    public record SalidaRechazo(UUID ordenRetiroId, String motivo, Dinero saldoDespues) {}

    public record SalidaInstruccion(UUID ordenRetiroId, String estado, String referenciaProveedor) {}

    public record SalidaAprobacion(UUID ordenRetiroId, String estado, UUID aprobadaPor) {}
}
