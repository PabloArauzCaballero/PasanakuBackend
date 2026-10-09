package bo.aportaya.nucleofinanciero.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Confirmaciones externas autenticadas; las llamadas ocurren fuera de la transaccion. */
public interface ProveedorDeRecargas {
    void solicitar(UUID referencia, Dinero monto);

    Confirmacion consultar(UUID referencia);

    record Confirmacion(
            UUID referencia, UUID transaccionProveedor, Dinero monto, String estado, OffsetDateTime liquidadaEn) {}
}
