package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.dominio.EstadoDeRetiro;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera.Pata;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRetiroRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import io.micrometer.core.instrument.Counter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class ResolucionDeRetiro {
    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final OrdenRetiroRepositorio ordenes;
    private final CU13RetenerSaldo retenciones;
    private final LibroDeBilletera libro;
    private final Outbox outbox;
    private final Reloj reloj;
    private final UUID cuentaPuenteDeCustodia;
    private final Counter retirosAutorizados;
    private final Counter retirosRechazados;

    @SuppressWarnings("checkstyle:ParameterNumber")
    ResolucionDeRetiro(
            Datos datos,
            CuentaBilleteraRepositorio cuentas,
            OrdenRetiroRepositorio ordenes,
            CU13RetenerSaldo retenciones,
            LibroDeBilletera libro,
            Outbox outbox,
            Reloj reloj,
            UUID cuentaPuenteDeCustodia,
            Counter retirosAutorizados,
            Counter retirosRechazados) {
        this.datos = datos;
        this.cuentas = cuentas;
        this.ordenes = ordenes;
        this.retenciones = retenciones;
        this.libro = libro;
        this.outbox = outbox;
        this.reloj = reloj;
        this.cuentaPuenteDeCustodia = cuentaPuenteDeCustodia;
        this.retirosAutorizados = retirosAutorizados;
        this.retirosRechazados = retirosRechazados;
    }

    /**
     * El proveedor pago: la retencion se **ejecuta** y el libro registra la salida.
     *
     * <p>Ejecutar y no liberar: el importe se consumio de verdad. Liberarlo devolveria
     * al disponible una plata que ya salio del sistema.
     */
    public CU11RetirarSaldo.SalidaPago confirmarPago(UUID ordenId, ContextoSesion ctx) {
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
            return new CU11RetirarSaldo.SalidaPago(orden.id(), transaccionId, despues.disponible());
        });
    }

    /**
     * El proveedor rechazo en firme: la retencion se **libera** y el saldo vuelve.
     *
     * <p>No se escribe ningun movimiento: la plata nunca salio. Registrar un debito y
     * su reverso por algo que no ocurrio ensuciaria el extracto de la persona con dos
     * lineas que no explican nada.
     */
    public CU11RetirarSaldo.SalidaRechazo rechazar(UUID ordenId, String motivo, ContextoSesion ctx) {
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
            return new CU11RetirarSaldo.SalidaRechazo(orden.id(), motivo, despues.disponible());
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
    public CU11RetirarSaldo.SalidaAprobacion aprobar(UUID ordenId, ContextoSesion ctx) {
        return resolverAprobacion(ordenId, ctx, true);
    }

    /** EN_REVISION → RECHAZADA (H3.S2): el aprobador la rechaza sin autorizarla. */
    public CU11RetirarSaldo.SalidaAprobacion rechazarRevision(UUID ordenId, ContextoSesion ctx) {
        return resolverAprobacion(ordenId, ctx, false);
    }

    private CU11RetirarSaldo.SalidaAprobacion resolverAprobacion(UUID ordenId, ContextoSesion ctx, boolean autorizar) {
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

            return new CU11RetirarSaldo.SalidaAprobacion(orden.id(), estadoFinal, ctx.usuarioId());
        });
    }
}
