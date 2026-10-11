package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.aplicacion.RegistrarDiscrepancia.EntradaDiscrepancia;
import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor;
import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor.Tipo;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas.Confirmacion;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRecargaRepositorio.Orden;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Coordina red y transacciones sin mantener locks mientras espera al proveedor.
 *
 * <p>Tres desenlaces al confirmar, y solo el primero mueve saldo: el proveedor confirma
 * una orden pendiente por el importe esperado (se acredita una vez); el proveedor
 * <b>contradice</b> lo que el libro ya sabe o lo que la orden espera (se registra una
 * discrepancia y el saldo no cambia); el proveedor todavia no sabe o no contesta (la
 * orden sigue pendiente).
 */
@Service
public class RecargasConProveedor {
    private static final String REFERENCIA = "ORDEN_RECARGA";

    private final CU10RecargarSaldo recargas;
    private final CU10RechazarRecarga rechazos;
    private final ProveedorDeRecargas proveedor;
    private final CotizarOperacion cotizaciones;
    private final OrdenesExistentes existentes;
    private final RegistrarDiscrepancia discrepancias;

    public RecargasConProveedor(
            CU10RecargarSaldo recargas,
            CU10RechazarRecarga rechazos,
            ProveedorDeRecargas proveedor,
            CotizarOperacion cotizaciones,
            OrdenesExistentes existentes,
            RegistrarDiscrepancia discrepancias) {
        this.recargas = recargas;
        this.rechazos = rechazos;
        this.proveedor = proveedor;
        this.cotizaciones = cotizaciones;
        this.existentes = existentes;
        this.discrepancias = discrepancias;
    }

    public CU10RecargarSaldo.SalidaSolicitud solicitar(CU10RecargarSaldo.EntradaSolicitud entrada, ContextoSesion ctx) {
        // El precio lo dicta tarifas y se confirma contra la cotizacion que la persona vio. Un reintento
        // de una orden ya abierta no exige una cotizacion vigente: ya se acepto la primera vez.
        var costo = cotizaciones.confirmar(
                CotizarOperacion.Operacion.RECARGA,
                entrada.cuentaBilleteraId(),
                entrada.monto(),
                entrada.claveIdempotencia(),
                entrada.cotizacionId(),
                existentes.recarga(entrada.cuentaBilleteraId(), entrada.claveIdempotencia(), ctx));
        var salida = recargas.solicitar(
                new CU10RecargarSaldo.EntradaSolicitud(
                        entrada.claveIdempotencia(),
                        entrada.cuentaBilleteraId(),
                        entrada.monto(),
                        costo.costo(),
                        entrada.medio(),
                        entrada.instrumentoFondeoId(),
                        costo.cotizacionId()),
                ctx);
        // La intencion ya esta persistida. Un timeout mantiene la misma referencia
        // para consultar/reintentar, nunca autoriza un abono.
        proveedor.solicitar(salida.ordenRecargaId(), entrada.monto());
        return salida;
    }

    public CU10RecargarSaldo.SalidaAcreditacion confirmar(UUID ordenId, ContextoSesion ctx) {
        var orden = recargas.consultarPropia(ordenId, ctx);
        Confirmacion confirmacion;
        try {
            confirmacion = proveedor.consultar(ordenId);
        } catch (DiscrepanciaDelProveedor d) {
            return discrepancia(orden, d.tipo(), d.huella(), Optional.empty(), d.getMessage(), ctx);
        }
        if (!orden.id().equals(confirmacion.referencia())) {
            return discrepancia(
                    orden,
                    Tipo.REFERENCIA_DISTINTA,
                    huellaDe(confirmacion),
                    Optional.empty(),
                    "El proveedor respondio por otra referencia.",
                    ctx);
        }
        if (!orden.bruto().equals(confirmacion.monto())) {
            return discrepancia(
                    orden,
                    Tipo.MONTO_DISTINTO,
                    huellaDe(confirmacion),
                    Optional.of(confirmacion.monto()),
                    "El proveedor informa un importe distinto del de la orden.",
                    ctx);
        }
        return switch (orden.estado()) {
            case "ACREDITADA" -> yaAcreditada(orden, confirmacion, ctx);
            case "PENDIENTE" -> pendiente(orden, confirmacion, ctx);
            default -> {
                // Cerrada en el libro (vencida, rechazada, reversada) y el proveedor dice que la cobro:
                // el dinero esta en otra parte. Es exactamente lo que alguien tiene que mirar.
                if ("CONFIRMADO".equals(confirmacion.estado())) {
                    yield discrepancia(
                            orden,
                            Tipo.ESTADO_CONTRADICTORIO,
                            huellaDe(confirmacion),
                            Optional.empty(),
                            "El proveedor confirma una orden que el libro ya cerro como " + orden.estado() + ".",
                            ctx);
                }
                throw new ErrorDeNegocio(CodigoError.de(10, 5), "Esa orden ya esta " + orden.estado() + ".");
            }
        };
    }

    private CU10RecargarSaldo.SalidaAcreditacion yaAcreditada(
            Orden orden, Confirmacion confirmacion, ContextoSesion ctx) {
        if (!"CONFIRMADO".equals(confirmacion.estado())
                || !confirmacion.transaccionProveedor().toString().equals(orden.referenciaExterna())) {
            return discrepancia(
                    orden,
                    Tipo.ESTADO_CONTRADICTORIO,
                    huellaDe(confirmacion),
                    Optional.empty(),
                    "El libro ya acredito esta orden y el proveedor ya no lo respalda.",
                    ctx);
        }
        return recargas.resultadoConfirmado(orden.id(), ctx);
    }

    private CU10RecargarSaldo.SalidaAcreditacion pendiente(Orden orden, Confirmacion confirmacion, ContextoSesion ctx) {
        switch (confirmacion.estado()) {
            case "CONFIRMADO" -> {
                try {
                    return recargas.acreditar(orden.id(), confirmacion, ctx);
                } catch (ErrorDeNegocio conflicto) {
                    // La primera confirmacion pudo ganar entre la lectura y el lock.
                    var actual = recargas.consultarPropia(orden.id(), ctx);
                    if ("ACREDITADA".equals(actual.estado())
                            && confirmacion.transaccionProveedor().toString().equals(actual.referenciaExterna())) {
                        return recargas.resultadoConfirmado(orden.id(), ctx);
                    }
                    throw conflicto;
                }
            }
            case "RECHAZADO" -> {
                rechazos.rechazar(orden.id(), ctx);
                throw new ErrorDeNegocio(CodigoError.de(10, 5), "El proveedor rechazo la recarga.");
            }
            default ->
                throw new ErrorDeNegocio(
                        CodigoError.de(10, 5), "La recarga sigue pendiente: el proveedor todavia no la confirma.");
        }
    }

    /** Registra la evidencia (sin tocar saldos) y rechaza la confirmacion. Siempre lanza. */
    private CU10RecargarSaldo.SalidaAcreditacion discrepancia(
            Orden orden, Tipo tipo, String huella, Optional<Dinero> informado, String detalle, ContextoSesion ctx) {
        discrepancias.registrar(
                new EntradaDiscrepancia(
                        REFERENCIA, orden.id(), tipo, Optional.of(orden.bruto()), informado, detalle, huella),
                ctx);
        throw new ErrorDeNegocio(
                CodigoError.de(10, 5), "La recarga no tiene una confirmacion valida por el importe esperado.");
    }

    private static String huellaDe(Confirmacion c) {
        String material = c.referencia() + "|" + c.transaccionProveedor() + "|" + c.monto() + "|" + c.estado();
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("Toda JVM trae SHA-256", imposible);
        }
    }
}
