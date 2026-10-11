package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo.EntradaRetencion;
import bo.aportaya.nucleofinanciero.aplicacion.CU40EvaluarLimites.EntradaLimites;
import bo.aportaya.nucleofinanciero.dominio.CondicionesDeRetiro.Veredicto;
import bo.aportaya.nucleofinanciero.dominio.CostoDeOperacion;
import bo.aportaya.nucleofinanciero.dominio.EstadoDeRetiro;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * La solicitud del retiro: valida, retiene y deja la orden. Vive aparte de {@link CU11RetirarSaldo}
 * (que la invoca dentro de su transaccion) para que ninguna de las dos piezas pase el limite de tamano.
 *
 * <p>Retencion primero, orden despues: al reves, entre una y otra la persona podria gastar el mismo saldo.
 */
final class SolicitudDeRetiro {

    private static final String CONCEPTO = "RETIRO";
    private static final String MOTIVO_RETENCION = "COMISION_PENDIENTE";

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final OrdenRetiroRepositorio ordenes;
    private final CU13RetenerSaldo retenciones;
    private final CU40EvaluarLimites limites;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Counter retirosSolicitados;
    private final Counter retirosAutorizados;
    private final Counter retirosRechazados;

    @SuppressWarnings("checkstyle:ParameterNumber")
    SolicitudDeRetiro(
            Datos datos,
            CuentaBilleteraRepositorio cuentas,
            OrdenRetiroRepositorio ordenes,
            CU13RetenerSaldo retenciones,
            CU40EvaluarLimites limites,
            Outbox outbox,
            Reloj reloj,
            Counter retirosSolicitados,
            Counter retirosAutorizados,
            Counter retirosRechazados) {
        this.datos = datos;
        this.cuentas = cuentas;
        this.ordenes = ordenes;
        this.retenciones = retenciones;
        this.limites = limites;
        this.outbox = outbox;
        this.reloj = reloj;
        this.retirosSolicitados = retirosSolicitados;
        this.retirosAutorizados = retirosAutorizados;
        this.retirosRechazados = retirosRechazados;
    }

    CU11RetirarSaldo.SalidaRetiro solicitar(CU11RetirarSaldo.EntradaRetiro entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var cuenta = cuentas.bloquear(dsl, entrada.cuentaBilleteraId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(11, 1), "Esa billetera no existe."));
            // BOLA: el rol correcto sobre la billetera ajena sigue siendo acceso indebido. Se comprueba
            // ANTES de tomar el lock de idempotencia, para que un tercero no ocupe el de otra persona.
            if (!ctx.usuarioId().equals(cuenta.usuarioId())) {
                throw new ErrorDeNegocio(CodigoError.de(11, 4), "Solo el titular puede retirar de esta billetera.");
            }
            ordenes.bloquearIdempotencia(dsl, cuenta.id(), entrada.claveIdempotencia());
            var yaExiste = ordenes.porClaveIdempotencia(dsl, cuenta.id(), entrada.claveIdempotencia());
            if (yaExiste.isPresent()) {
                // El replay devuelve el costo ALMACENADO en la orden, no el que trae la
                // entrada repetida: si el cotizador cambio entretanto, recotizar en el
                // replay le mostraria a la persona un costo distinto del que se le cobro.
                var orden = ordenes.ver(dsl, yaExiste.get()).orElseThrow();
                ReglasDeRetiro.exigirMismaSolicitud(orden, entrada);
                return new CU11RetirarSaldo.SalidaRetiro(
                        orden.id(),
                        orden.estado(),
                        orden.costo(),
                        orden.neto(),
                        orden.retencionId().orElse(null));
            }
            var instrumento = ordenes.instrumento(dsl, entrada.instrumentoDestinoId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(11, 4), "Esa cuenta de destino no existe."));

            // Las condiciones duras, todas juntas y en orden: primero lo que la
            // persona puede resolver, despues lo del sistema.
            Veredicto veredicto = ReglasDeRetiro.evaluarCondiciones(
                    cuenta,
                    instrumento,
                    entrada,
                    ordenes.hayBloqueoDeAutoridad(dsl, cuenta.id()),
                    ordenes.encajeCumplido(dsl, cuenta.moneda().name()),
                    ahora);
            if (!veredicto.permitido()) {
                retirosRechazados.increment();
                throw new ErrorDeNegocio(CodigosDeRetiro.de(veredicto.codigo()), veredicto.motivo());
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
                    entrada.cotizacionId(),
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

            return new CU11RetirarSaldo.SalidaRetiro(
                    ordenId, estadoInicial.name(), entrada.costo(), neto, retencion.retencionId());
        });
    }
}
