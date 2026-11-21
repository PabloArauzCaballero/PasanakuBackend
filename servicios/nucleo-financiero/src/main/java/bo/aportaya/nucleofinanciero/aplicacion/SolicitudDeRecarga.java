package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.Optional;
import java.util.UUID;

/** Entrada de CU-10 · Recargar saldo: lo que pide la persona para crear la orden de recarga. */
public record SolicitudDeRecarga(
        String claveIdempotencia,
        UUID cuentaBilleteraId,
        Dinero monto,
        Dinero costoProveedor,
        String medio,
        Optional<UUID> instrumentoFondeoId,
        Optional<UUID> cotizacionId) {

    /** Sin cotizacion previa: el costo lo dicta el servidor y no hay aceptacion que registrar. */
    public SolicitudDeRecarga(
            String claveIdempotencia,
            UUID cuentaBilleteraId,
            Dinero monto,
            Dinero costoProveedor,
            String medio,
            Optional<UUID> instrumentoFondeoId) {
        this(claveIdempotencia, cuentaBilleteraId, monto, costoProveedor, medio, instrumentoFondeoId, Optional.empty());
    }
}
