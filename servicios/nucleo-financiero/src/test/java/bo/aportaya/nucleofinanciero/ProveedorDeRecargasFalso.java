package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** El proveedor de recargas: responde lo que el escenario dicte, o se rompe como lo haria el adaptador. */
final class ProveedorDeRecargasFalso implements ProveedorDeRecargas {
    Confirmacion respuesta;
    DiscrepanciaDelProveedor falla;

    @Override
    public void solicitar(UUID referencia, Dinero monto) {
        respuesta = new Confirmacion(referencia, UUID.randomUUID(), monto, "PENDIENTE", null);
    }

    @Override
    public Confirmacion consultar(UUID referencia) {
        if (falla != null) {
            throw falla;
        }
        return respuesta;
    }

    void dice(UUID orden, UUID transaccion, String monto, String estado) {
        boolean liquida = "CONFIRMADO".equals(estado);
        respuesta = new Confirmacion(
                orden,
                transaccion,
                Dinero.de(monto, Moneda.BOB),
                estado,
                liquida ? OffsetDateTime.now(ZoneOffset.UTC) : null);
    }
}
