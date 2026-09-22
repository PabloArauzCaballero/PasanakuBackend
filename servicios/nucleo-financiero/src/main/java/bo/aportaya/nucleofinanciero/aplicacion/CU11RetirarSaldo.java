package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo.EntradaRetencion;
import bo.aportaya.nucleofinanciero.aplicacion.CU40EvaluarLimites.EntradaLimites;
import bo.aportaya.nucleofinanciero.dominio.CondicionesDeRetiro;
import bo.aportaya.nucleofinanciero.dominio.CondicionesDeRetiro.Situacion;
import bo.aportaya.nucleofinanciero.dominio.CondicionesDeRetiro.Veredicto;
import bo.aportaya.nucleofinanciero.dominio.CostoDeOperacion;
import bo.aportaya.nucleofinanciero.dominio.EstadoDeRetiro;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiro;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera.Pata;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRetiroRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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

    private static final String CONCEPTO = "RETIRO";
    private static final String MOTIVO_RETENCION = "COMISION_PENDIENTE";

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final OrdenRetiroRepositorio ordenes;
    private final CU13RetenerSaldo retenciones;
    private final CU40EvaluarLimites limites;
    private final LibroDeBilletera libro;
    private final Outbox outbox;
    private final Reloj reloj;
    private final UUID cuentaPuenteDeCustodia;
    private final ProveedorDeRetiro proveedor;

    // H4.S2.M6 · metricas de negocio, no tecnicas: cuantos retiros se piden, se
    // autorizan (automatico o por aprobacion) y se rechazan (en cualquiera de sus
    // motivos). `withdrawal_*_total`, tageadas por `desenlace` para no multiplicar
    // contadores — un dashboard suma o separa segun le convenga, sin tocar el codigo.
    private final Counter retirosSolicitados;
    private final Counter retirosAutorizados;
    private final Counter retirosRechazados;

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
        this.cuentas = cuentas;
        this.ordenes = ordenes;
        this.retenciones = retenciones;
        this.limites = limites;
        this.libro = libro;
        this.outbox = outbox;
        this.reloj = reloj;
        this.cuentaPuenteDeCustodia = cuentaPuenteDeCustodia;
        this.proveedor = proveedor;
        this.retirosSolicitados = Counter.builder("withdrawal_requested_total")
                .description("Retiros solicitados, cualquiera sea su estado inicial")
                .register(metricas);
        this.retirosAutorizados = Counter.builder("withdrawal_approved_total")
                .description("Retiros que llegaron a AUTORIZADA, automatico o por aprobacion")
                .register(metricas);
        this.retirosRechazados = Counter.builder("withdrawal_failed_total")
                .description("Retiros rechazados: MFA, limites, proveedor, revision o aprobador")
                .register(metricas);
    }

    @Transactional
    public SalidaRetiro solicitar(EntradaRetiro entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            ordenes.bloquearIdempotencia(dsl, entrada.cuentaBilleteraId(), entrada.claveIdempotencia());
            var yaExiste = ordenes.porClaveIdempotencia(dsl, entrada.cuentaBilleteraId(), entrada.claveIdempotencia());
            if (yaExiste.isPresent()) {
                // El replay devuelve el costo ALMACENADO en la orden, no el que trae la
                // entrada repetida: si el cotizador cambio entretanto, recotizar en el
                // replay le mostraria a la persona un costo distinto del que se le cobro.
                var orden = ordenes.ver(dsl, yaExiste.get()).orElseThrow();
                return new SalidaRetiro(
                        orden.id(),
                        orden.estado(),
                        orden.costo(),
                        orden.neto(),
                        orden.retencionId().orElse(null));
            }

            var cuenta = cuentas.bloquear(dsl, entrada.cuentaBilleteraId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(11, 1), "Esa billetera no existe."));
            var instrumento = ordenes.instrumento(dsl, entrada.instrumentoDestinoId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(11, 4), "Esa cuenta de destino no existe."));

            // Las condiciones duras, todas juntas y en orden: primero lo que la
            // persona puede resolver, despues lo del sistema.
            Veredicto veredicto = CondicionesDeRetiro.evaluar(
                    new Situacion(
                            entrada.mfaVerificado(),
                            entrada.evidenciaMfaProvista(),
                            cuenta.disponible(),
                            entrada.monto(),
                            instrumento.usuarioId().equals(cuenta.usuarioId()) && instrumento.titularCoincide(),
                            instrumento.verificado(),
                            instrumento.bloqueadoHasta(),
                            ordenes.hayBloqueoDeAutoridad(dsl, cuenta.id()),
                            ordenes.encajeCumplido(dsl, cuenta.moneda().name())),
                    ahora);
            if (!veredicto.permitido()) {
                retirosRechazados.increment();
                throw new ErrorDeNegocio(codigoDe(veredicto.codigo()), veredicto.motivo());
            }

            limites.exigirDentroDe(dsl, new EntradaLimites(cuenta.id(), CONCEPTO, entrada.monto()), ctx);

            Dinero neto = CostoDeOperacion.netoDeRetiro(entrada.monto(), entrada.costo());

            // H3.S1.M2 (AMB-5, Q-02): por debajo del umbral, AUTORIZADA automatica en
            // la MISMA transaccion de creacion — nunca PENDIENTE→PAGADA directo. Por
            // encima, EN_REVISION hasta que un segundo aprobador (H3.S2) la mueva.
            EstadoDeRetiro estadoInicial =
                    entrada.requiereDobleAprobacion() ? EstadoDeRetiro.EN_REVISION : EstadoDeRetiro.AUTORIZADA;

            // El orden importa: la retencion ANTES de la orden. Al reves, entre una y
            // otra la persona podria gastar el mismo saldo en otra operacion.
            var retencion = retenciones.retenerDentroDe(
                    dsl,
                    new EntradaRetencion(
                            cuenta.id(),
                            entrada.monto(),
                            MOTIVO_RETENCION,
                            Optional.empty(),
                            Optional.of("ORDEN_RETIRO"),
                            Optional.empty(),
                            Optional.empty()),
                    ctx);

            UUID ordenId = ordenes.crear(
                    dsl,
                    cuenta.id(),
                    entrada.instrumentoDestinoId(),
                    retencion.retencionId(),
                    ctx.usuarioId(),
                    entrada.monto(),
                    entrada.costo(),
                    neto,
                    entrada.mfaVerificado(),
                    entrada.requiereDobleAprobacion(),
                    instrumento.bloqueadoHasta(),
                    entrada.claveIdempotencia(),
                    estadoInicial.name(),
                    ahora);

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "nucleo_financiero.retiro_solicitado",
                            "orden_retiro",
                            ordenId,
                            Map.of(
                                    "cuentaBilleteraId", cuenta.id().toString(),
                                    "monto", entrada.monto().toString(),
                                    "neto", neto.toString()),
                            UUID.fromString(ctx.traza().id())));

            retirosSolicitados.increment();
            if (estadoInicial == EstadoDeRetiro.AUTORIZADA) {
                retirosAutorizados.increment();
            }

            return new SalidaRetiro(ordenId, estadoInicial.name(), entrada.costo(), neto, retencion.retencionId());
        });
    }

    /**
     * AUTORIZADA → EN_PROCESO: instruye al proveedor (H3.S1.M3, base de H4.S2).
     *
     * <p>Fuera de la transaccion (invariante 6): la lectura del estado y la escritura
     * de {@code EN_PROCESO} son dos transacciones CORTAS separadas por la llamada de
     * red al proveedor en el medio, igual que {@code cotizador}/{@code segundoFactor}
     * se llaman desde fuera del caso de uso que abre la transaccion larga.
     */
    /**
     * H4.S2.M3 · las ordenes {@code EN_PROCESO} que {@code ReconciliacionDeRetiros}
     * recorre. {@code @Transactional} a proposito, a diferencia de
     * {@code instruirPago}: esta SI es una unidad de trabajo completa en si misma —
     * una lectura, sin llamada de red en el medio — asi que no hay invariante 6 que
     * respetar dividiendola.
     */
    @Transactional(readOnly = true)
    public java.util.List<OrdenRetiroRepositorio.Orden> ordenesEnProceso(ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> ordenes.enProceso(dsl));
    }

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

    /**
     * El proveedor pago: la retencion se **ejecuta** y el libro registra la salida.
     *
     * <p>Ejecutar y no liberar: el importe se consumio de verdad. Liberarlo devolveria
     * al disponible una plata que ya salio del sistema.
     */
    @Transactional
    public SalidaPago confirmarPago(UUID ordenId, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var orden = ordenes.ver(dsl, ordenId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(11, 1), "Esa orden no existe."));

            // H3.S1.M3: EN_PROCESO, no PENDIENTE — el pago solo se confirma DESPUES
            // de que instruirPago mando la orden al proveedor.
            if (!ordenes.pasarA(dsl, ordenId, EstadoDeRetiro.EN_PROCESO.name(), "PAGADA", ahora)) {
                throw new ErrorDeNegocio(
                        CodigoError.de(11, 9),
                        "Esa orden esta " + orden.estado()
                                + ": solo se puede confirmar el pago de una orden EN_PROCESO.");
            }

            orden.retencionId().ifPresent(id -> retenciones.ejecutarDentroDe(dsl, id, ctx));

            // H4.S2.M3: confirmarPago tambien lo llama ReconciliacionDeRetiros con un
            // ContextoSesion.deSistema(...) — su usuarioId es un identificador de
            // PROCESO, no una fila real de identidad.usuario, asi que escribirlo en
            // iniciada_por (FK a identidad.usuario) violaria la restriccion. Mismo
            // patron que CU24RegistrarAsiento.iniciadaPor: sistema -> sin autor humano,
            // no un autor inventado. El canal tambien lo delata: BATCH, no API.
            UUID transaccionId = libro.registrar(
                    dsl,
                    "RETIRO",
                    "ORDEN_RETIRO",
                    orden.id(),
                    ctx.esSistema() ? "BATCH" : "API",
                    orden.solicitado(),
                    "retiro:" + orden.id(),
                    ctx.esSistema() ? Optional.empty() : Optional.of(ctx.usuarioId()),
                    List.of(
                            Pata.debito(orden.cuentaId(), orden.solicitado(), "Retiro pagado"),
                            Pata.credito(cuentaPuenteDeCustodia, orden.solicitado(), "Salida hacia custodia")),
                    ahora);

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "nucleo_financiero.retiro_pagado",
                            "orden_retiro",
                            orden.id(),
                            Map.of(
                                    "cuentaBilleteraId",
                                    orden.cuentaId().toString(),
                                    "monto",
                                    orden.solicitado().toString()),
                            UUID.fromString(ctx.traza().id())));

            var despues = cuentas.ver(dsl, orden.cuentaId()).orElseThrow();
            return new SalidaPago(orden.id(), transaccionId, despues.disponible());
        });
    }

    /**
     * El proveedor rechazo en firme: la retencion se **libera** y el saldo vuelve.
     *
     * <p>No se escribe ningun movimiento: la plata nunca salio. Registrar un debito y
     * su reverso por algo que no ocurrio ensuciaria el extracto de la persona con dos
     * lineas que no explican nada.
     */
    @Transactional
    public SalidaRechazo rechazar(UUID ordenId, String motivo, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var orden = ordenes.ver(dsl, ordenId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(11, 1), "Esa orden no existe."));

            // H3.S1.M3: RECHAZADA es alcanzable desde AUTORIZADA (el proveedor
            // rechazo apenas se le instruyo) o desde EN_PROCESO (rechazo firme
            // resuelto mas tarde, via reconciliacion) — nunca desde PAGADA.
            boolean rechazada = ordenes.pasarA(dsl, ordenId, EstadoDeRetiro.AUTORIZADA.name(), "RECHAZADA", ahora)
                    || ordenes.pasarA(dsl, ordenId, EstadoDeRetiro.EN_PROCESO.name(), "RECHAZADA", ahora);
            if (!rechazada) {
                throw new ErrorDeNegocio(
                        CodigoError.de(11, 1), "Esa orden esta " + orden.estado() + ": no se puede rechazar.");
            }
            orden.retencionId().ifPresent(id -> retenciones.liberarDentroDe(dsl, id, ctx));

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "nucleo_financiero.retiro_rechazado",
                            "orden_retiro",
                            orden.id(),
                            Map.of("cuentaBilleteraId", orden.cuentaId().toString(), "motivo", motivo),
                            UUID.fromString(ctx.traza().id())));

            retirosRechazados.increment();

            var despues = cuentas.ver(dsl, orden.cuentaId()).orElseThrow();
            return new SalidaRechazo(orden.id(), motivo, despues.disponible());
        });
    }

    /**
     * EN_REVISION → AUTORIZADA (H3.S2): un aprobador con {@code RETIRO_APROBAR}
     * DISTINTO de quien pidio el retiro.
     *
     * <p>El chequeo de auto-aprobacion se hace ANTES de intentar el {@code UPDATE} a
     * proposito: si se dejara que la base lo rechazara sola (que tambien lo hace,
     * {@code ck_retiro_doble_aprobacion}), no habria forma de distinguir "sos el
     * mismo que la pidio" de "otro aprobador ya la resolvio" — dos codigos de error
     * distintos que la persona necesita distinguir.
     */
    @Transactional
    public SalidaAprobacion aprobar(UUID ordenId, ContextoSesion ctx) {
        return resolverAprobacion(ordenId, ctx, true);
    }

    /** EN_REVISION → RECHAZADA (H3.S2): el aprobador la rechaza sin autorizarla. */
    @Transactional
    public SalidaAprobacion rechazarRevision(UUID ordenId, ContextoSesion ctx) {
        return resolverAprobacion(ordenId, ctx, false);
    }

    private SalidaAprobacion resolverAprobacion(UUID ordenId, ContextoSesion ctx, boolean autorizar) {
        return datos.conContexto(ctx, dsl -> {
            var orden = ordenes.ver(dsl, ordenId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(11, 1), "Esa orden no existe."));

            if (orden.solicitadaPor().equals(ctx.usuarioId())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(11, 10),
                        "Quien solicito el retiro no puede aprobar (ni rechazar) su propia solicitud.");
            }

            boolean resuelta = autorizar
                    ? ordenes.pasarAAutorizadaPorAprobacion(dsl, ordenId, ctx.usuarioId())
                    : ordenes.pasarARechazadaPorAprobacion(dsl, ordenId, ctx.usuarioId());
            if (!resuelta) {
                throw new ErrorDeNegocio(
                        CodigoError.de(11, 11),
                        "Esa orden ya no esta en revision: alguien mas la resolvio, o ya no esta pendiente de"
                                + " aprobacion.");
            }

            String estadoFinal = autorizar ? "AUTORIZADA" : "RECHAZADA";
            if (!autorizar) {
                orden.retencionId().ifPresent(id -> retenciones.liberarDentroDe(dsl, id, ctx));
            }

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            autorizar ? "nucleo_financiero.retiro_autorizado" : "nucleo_financiero.retiro_rechazado",
                            "orden_retiro",
                            orden.id(),
                            Map.of(
                                    "cuentaBilleteraId", orden.cuentaId().toString(),
                                    "aprobadaPor", ctx.usuarioId().toString(),
                                    "solicitadaPor", orden.solicitadaPor().toString()),
                            UUID.fromString(ctx.traza().id())));

            if (autorizar) {
                retirosAutorizados.increment();
            } else {
                retirosRechazados.increment();
            }

            return new SalidaAprobacion(orden.id(), estadoFinal, ctx.usuarioId());
        });
    }

    private CodigoError codigoDe(String codigo) {
        return switch (codigo) {
            case "SALDO_INSUFICIENTE" -> CodigoError.de(11, 1);
            case "MFA_REQUERIDO" -> CodigoError.de(11, 2);
            case "INSTRUMENTO_EN_ENFRIAMIENTO" -> CodigoError.de(11, 3);
            case "TITULAR_NO_COINCIDE" -> CodigoError.de(11, 4);
            case "BLOQUEO_DE_AUTORIDAD" -> CodigoError.de(11, 5);
            case "ENCAJE_INCUMPLIDO" -> CodigoError.de(11, 6);
            default -> CodigoError.de(11, 7); // MFA_INVALIDO
        };
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
            boolean requiereDobleAprobacion) {}

    public record SalidaRetiro(
            UUID ordenRetiroId, String estado, Dinero costoRetiro, Dinero montoNeto, UUID retencionId) {}

    public record SalidaPago(UUID ordenRetiroId, UUID transaccionId, Dinero saldoDespues) {}

    public record SalidaRechazo(UUID ordenRetiroId, String motivo, Dinero saldoDespues) {}

    public record SalidaInstruccion(UUID ordenRetiroId, String estado, String referenciaProveedor) {}

    public record SalidaAprobacion(UUID ordenRetiroId, String estado, UUID aprobadaPor) {}
}
