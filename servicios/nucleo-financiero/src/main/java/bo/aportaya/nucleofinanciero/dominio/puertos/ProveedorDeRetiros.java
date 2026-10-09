package bo.aportaya.nucleofinanciero.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * El proveedor que paga un retiro. Las llamadas ocurren fuera de la transaccion.
 *
 * <p>La referencia es la de la orden y es estable: reenviar la misma referencia no
 * puede pagar dos veces, y por eso <b>antes de reenviar siempre se consulta</b>. Un
 * timeout despues de liquidar es indistinguible, desde afuera, de uno antes de liquidar.
 */
public interface ProveedorDeRetiros {

    /** Instruye el pago del neto. Idempotente por referencia del lado del proveedor. */
    void instruir(UUID referencia, Dinero neto);

    /** Lo que el proveedor sabe de esa referencia, o vacio si no la conoce. */
    Optional<Confirmacion> consultar(UUID referencia);

    record Confirmacion(
            UUID referencia, UUID transaccionProveedor, Dinero monto, String estado, OffsetDateTime liquidadaEn) {}
}
