package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiros;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** El proveedor: una operacion por referencia, y fallos que se arman de a uno. */
final class ProveedorDeRetirosFalso implements ProveedorDeRetiros {
    final Map<UUID, Confirmacion> operaciones = new HashMap<>();
    int envios;
    int consultas;
    boolean caido;
    /** El proximo envio se liquida pero la respuesta se pierde. */
    boolean liquidaYPierde;
    /** El proximo envio no llega al proveedor. */
    boolean pierdeAntes;

    DiscrepanciaDelProveedor hostil;

    @Override
    public synchronized void instruir(UUID referencia, Dinero neto) {
        envios++;
        if (caido || pierdeAntes) {
            pierdeAntes = false;
            throw new ErrorDeNegocio(bo.aportaya.plataforma.dominio.CodigoError.de(10, 5), "sin respuesta");
        }
        operaciones.computeIfAbsent(
                referencia,
                r -> liquidaYPierde ? confirmacion(r, neto, "CONFIRMADO") : confirmacion(r, neto, "PENDIENTE"));
        if (liquidaYPierde) {
            liquidaYPierde = false;
            throw new ErrorDeNegocio(bo.aportaya.plataforma.dominio.CodigoError.de(10, 5), "sin respuesta");
        }
    }

    @Override
    public synchronized Optional<Confirmacion> consultar(UUID referencia) {
        consultas++;
        if (hostil != null) {
            throw hostil;
        }
        if (caido) {
            throw new ErrorDeNegocio(bo.aportaya.plataforma.dominio.CodigoError.de(10, 5), "sin respuesta");
        }
        return Optional.ofNullable(operaciones.get(referencia));
    }

    synchronized void resuelve(UUID referencia, String estado) {
        var anterior = operaciones.get(referencia);
        operaciones.put(
                referencia, confirmacion(referencia, anterior.monto(), estado, anterior.transaccionProveedor()));
    }

    synchronized void dice(UUID referencia, String monto, String estado) {
        operaciones.put(referencia, confirmacion(referencia, Dinero.de(monto, Moneda.BOB), estado, UUID.randomUUID()));
    }

    private static Confirmacion confirmacion(UUID ref, Dinero monto, String estado) {
        return confirmacion(ref, monto, estado, UUID.randomUUID());
    }

    private static Confirmacion confirmacion(UUID ref, Dinero monto, String estado, UUID transaccion) {
        return new Confirmacion(
                ref,
                transaccion,
                monto,
                estado,
                "CONFIRMADO".equals(estado) ? OffsetDateTime.now(ZoneOffset.UTC) : null);
    }
}
