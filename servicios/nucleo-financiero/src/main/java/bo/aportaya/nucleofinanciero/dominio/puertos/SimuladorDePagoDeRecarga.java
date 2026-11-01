package bo.aportaya.nucleofinanciero.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.UUID;

/**
 * Solo existe donde el proveedor de recargas es el doble de prueba: hace de cuenta que alguien pago el
 * QR de prueba de una orden. En {@code production} no hay ningun bean que lo implemente, y la operacion
 * que lo usa responde como una ruta inexistente.
 */
public interface SimuladorDePagoDeRecarga {

    /** El proveedor pasa a decir CONFIRMADO para esa referencia, por ese monto. Idempotente. */
    void marcarPagado(UUID referencia, Dinero monto);
}
