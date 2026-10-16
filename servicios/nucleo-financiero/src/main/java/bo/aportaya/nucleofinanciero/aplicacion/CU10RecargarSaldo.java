package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.aplicacion.CU40EvaluarLimites.EntradaLimites;
import bo.aportaya.nucleofinanciero.dominio.CostoDeOperacion;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas.Confirmacion;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera.Pata;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRecargaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Duration;
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
 * CU-10 · Recargar saldo.
 *
 * <p>Dos actos separados y a proposito: **solicitar** crea la orden y devuelve el QR;
 * **acreditar** suma el saldo cuando el proveedor confirma. El saldo no se mueve
 * cuando la persona dice que pago, sino cuando el dinero llego. Acreditar al pedir
 * seria regalar saldo a quien abandona el pago a medias.
 *
 * <p>{@code AGENTE} no existe: [[ADR-039]] retiro el efectivo del alcance el 20 de
 * agosto de 2026, y el unico ingreso de fondos es electronico.
 */
@Service
public class CU10RecargarSaldo {

    private static final String CONCEPTO = "RECARGA";

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final OrdenRecargaRepositorio ordenes;
    private final LibroDeBilletera libro;
    private final CU40EvaluarLimites limites;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Duration vigenciaDeLaOrden;
    private final UUID cuentaPuenteDeCustodia;

    public CU10RecargarSaldo(
            Datos datos,
            CuentaBilleteraRepositorio cuentas,
            OrdenRecargaRepositorio ordenes,
            LibroDeBilletera libro,
            CU40EvaluarLimites limites,
            Outbox outbox,
            Reloj reloj,
            @Value("${aportaya.recarga.vigencia-de-la-orden}") Duration vigenciaDeLaOrden,
            @Value("${aportaya.custodia.cuenta-puente}") UUID cuentaPuenteDeCustodia) {
        this.datos = datos;
        this.cuentas = cuentas;
        this.ordenes = ordenes;
        this.libro = libro;
        this.limites = limites;
        this.outbox = outbox;
        this.reloj = reloj;
        this.vigenciaDeLaOrden = vigenciaDeLaOrden;
        this.cuentaPuenteDeCustodia = cuentaPuenteDeCustodia;
    }

    @Transactional
    public SalidaSolicitud solicitar(EntradaSolicitud entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            // La billetera se bloquea y se autoriza ANTES de tomar el lock de idempotencia: asi un tercero no
            // puede ocupar el lock de (cuenta, clave) de otra persona.
            var cuenta = cuentas.bloquear(dsl, entrada.cuentaBilleteraId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(10, 4), "Esa billetera no existe."));
            if (!ctx.usuarioId().equals(cuenta.usuarioId())) {
                throw new ErrorDeNegocio(CodigoError.de(10, 4), "Solo el titular puede solicitar una recarga.");
            }
            ordenes.bloquearIdempotencia(dsl, cuenta.id(), entrada.claveIdempotencia());
            // La clave se valida ANTES de escribir (invariante 7). Repetir el pedido
            // devuelve la misma orden, no una segunda.
            var yaExiste = ordenes.porClaveIdempotencia(dsl, cuenta.id(), entrada.claveIdempotencia());
            if (yaExiste.isPresent()) {
                var orden = ordenes.ver(dsl, yaExiste.get()).orElseThrow();
                ReglasDeRecarga.exigirMismaSolicitud(orden, entrada);
                return new SalidaSolicitud(orden.id(), orden.estado(), orden.expiraEn(), orden.acreditado());
            }

            // AP-CU10-04.
            if (!cuenta.operativa()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(10, 4), "La billetera esta " + cuenta.estado() + ": no admite recargas.");
            }

            // AP-CU10-03.
            if (entrada.instrumentoFondeoId().isPresent()) {
                ReglasDeRecarga.exigirInstrumentoDelTitular(
                        ordenes.instrumento(dsl, entrada.instrumentoFondeoId().get()), ctx);
            }

            // AP-CU10-01 · R-LIM-01. Se evalua al SOLICITAR y no solo al acreditar,
            // para no mostrarle un QR a alguien cuya recarga va a rebotar despues.
            limites.exigirDentroDe(dsl, new EntradaLimites(cuenta.id(), CONCEPTO, entrada.monto()), ctx);

            Dinero acreditado = CostoDeOperacion.acreditacion(entrada.monto(), entrada.costoProveedor());
            OffsetDateTime expira = ahora.plus(vigenciaDeLaOrden);

            UUID ordenId = ordenes.crear(
                    dsl,
                    cuenta.id(),
                    entrada.instrumentoFondeoId(),
                    entrada.monto(),
                    entrada.costoProveedor(),
                    acreditado,
                    entrada.claveIdempotencia(),
                    entrada.medio(),
                    entrada.cotizacionId(),
                    ahora,
                    expira);

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "nucleo_financiero.recarga_solicitada",
                            "orden_recarga",
                            ordenId,
                            Map.of(
                                    "cuentaBilleteraId", cuenta.id().toString(),
                                    "monto", entrada.monto().toString(),
                                    "medio", entrada.medio()),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaSolicitud(ordenId, "PENDIENTE", expira, acreditado);
        });
    }

    /** Autoriza la lectura antes de consultar al proveedor fuera de la transaccion. */
    @Transactional(readOnly = true)
    public OrdenRecargaRepositorio.Orden consultarPropia(UUID ordenId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            var orden = ordenes.ver(dsl, ordenId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(10, 5), "Esa orden no existe."));
            var cuenta = cuentas.ver(dsl, orden.cuentaId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(10, 4), "Esa billetera no existe."));
            if (!ctx.usuarioId().equals(cuenta.usuarioId())) {
                throw new ErrorDeNegocio(CodigoError.de(10, 4), "Solo el titular puede consultar esta recarga.");
            }
            return orden;
        });
    }

    /**
     * El proveedor confirmo: ahora si se mueve el saldo.
     *
     * <p>El asiento tiene dos patas y cuadra: sale del puente de custodia —que
     * representa lo que entro desde afuera— y entra a la billetera. Sin la
     * contrapartida, R-BIL-01 rechaza la transaccion entera.
     */
    @Transactional
    public SalidaAcreditacion acreditar(UUID ordenId, Confirmacion confirmacion, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var orden = ordenes.bloquear(dsl, ordenId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(10, 5), "Esa orden no existe."));

            if (confirmacion == null
                    || !ordenId.equals(confirmacion.referencia())
                    || !"CONFIRMADO".equals(confirmacion.estado())
                    || !orden.bruto().equals(confirmacion.monto())
                    || confirmacion.transaccionProveedor() == null
                    || confirmacion.liquidadaEn() == null
                    || confirmacion.liquidadaEn().isAfter(ahora.plusSeconds(30))) {
                throw new ErrorDeNegocio(
                        CodigoError.de(10, 5), "Falta la confirmacion autentica del proveedor para esta recarga.");
            }
            var cuenta = cuentas.bloquear(dsl, orden.cuentaId()).orElseThrow();
            if (!ctx.usuarioId().equals(cuenta.usuarioId())) {
                throw new ErrorDeNegocio(CodigoError.de(10, 4), "Solo el titular puede confirmar esta recarga.");
            }

            // Repetir la confirmacion de una orden YA acreditada (B33, regla 91.6) devuelve la respuesta
            // original sin mover nada. Eso lo resuelve RecargasConProveedor (yaAcreditada / carrera perdida),
            // que primero compara la confirmacion con la referencia del proveedor que acredito; aqui un
            // segundo abono directo sigue siendo un error, nunca un segundo asiento.
            // AP-CU10-05.
            if (!"PENDIENTE".equals(orden.estado())) {
                throw new ErrorDeNegocio(CodigoError.de(10, 5), "Esa orden ya esta " + orden.estado() + ".");
            }
            if (orden.expiraEn() != null && !confirmacion.liquidadaEn().isBefore(orden.expiraEn())) {
                throw new ErrorDeNegocio(CodigoError.de(10, 5), "Esa orden de recarga ya vencio.");
            }

            UUID transaccionId = libro.registrar(
                    dsl,
                    "RECARGA",
                    "ORDEN_RECARGA",
                    orden.id(),
                    "API",
                    orden.acreditado(),
                    "recarga:" + orden.id(),
                    Optional.of(ctx.usuarioId()),
                    List.of(
                            Pata.debito(cuentaPuenteDeCustodia, orden.acreditado(), "Ingreso desde custodia"),
                            Pata.credito(cuenta.id(), orden.acreditado(), "Recarga acreditada")),
                    ahora);

            // Si otra confirmacion gano la carrera, la transaccion se revierte entera:
            // el mismo pago no puede sumar saldo dos veces.
            if (!ordenes.acreditar(
                    dsl,
                    orden.id(),
                    transaccionId,
                    confirmacion.transaccionProveedor().toString(),
                    ahora)) {
                throw new ErrorDeNegocio(CodigoError.de(10, 5), "Otra confirmacion acredito esa orden primero.");
            }

            var aplicados =
                    limites.exigirDentroDe(dsl, new EntradaLimites(cuenta.id(), CONCEPTO, orden.acreditado()), ctx);
            limites.acumularDentroDe(dsl, cuenta.id(), aplicados, orden.acreditado());

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "nucleo_financiero.recarga_acreditada",
                            "orden_recarga",
                            orden.id(),
                            Map.of(
                                    "cuentaBilleteraId", cuenta.id().toString(),
                                    "monto", orden.acreditado().toString()),
                            UUID.fromString(ctx.traza().id())));

            var despues = cuentas.ver(dsl, cuenta.id()).orElseThrow();
            return new SalidaAcreditacion(orden.id(), transaccionId, despues.disponible());
        });
    }

    /** Trabajo programado: una orden que nadie pago no queda pendiente para siempre. */
    @Transactional(readOnly = true)
    public SalidaAcreditacion resultadoConfirmado(UUID ordenId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            var orden = ordenes.ver(dsl, ordenId).orElseThrow();
            var cuenta = cuentas.ver(dsl, orden.cuentaId()).orElseThrow();
            if (!ctx.usuarioId().equals(cuenta.usuarioId()) || !"ACREDITADA".equals(orden.estado())) {
                throw new ErrorDeNegocio(CodigoError.de(10, 5), "La orden no tiene una acreditacion accesible.");
            }
            return new SalidaAcreditacion(orden.id(), orden.transaccionId(), ordenes.saldoDeLaAcreditacion(dsl, orden));
        });
    }

    @Transactional
    public int expirarVencidas(ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> ordenes.expirarVencidas(dsl, ahora));
    }

    public record EntradaSolicitud(
            String claveIdempotencia,
            UUID cuentaBilleteraId,
            Dinero monto,
            Dinero costoProveedor,
            String medio,
            Optional<UUID> instrumentoFondeoId,
            Optional<UUID> cotizacionId) {

        /** Sin cotizacion previa: el costo lo dicta el servidor y no hay aceptacion que registrar. */
        public EntradaSolicitud(
                String claveIdempotencia,
                UUID cuentaBilleteraId,
                Dinero monto,
                Dinero costoProveedor,
                String medio,
                Optional<UUID> instrumentoFondeoId) {
            this(
                    claveIdempotencia,
                    cuentaBilleteraId,
                    monto,
                    costoProveedor,
                    medio,
                    instrumentoFondeoId,
                    Optional.empty());
        }
    }

    public record SalidaSolicitud(UUID ordenRecargaId, String estado, OffsetDateTime expiraEn, Dinero acreditara) {}

    public record SalidaAcreditacion(UUID ordenRecargaId, UUID transaccionId, Dinero saldoDespues) {}
}
