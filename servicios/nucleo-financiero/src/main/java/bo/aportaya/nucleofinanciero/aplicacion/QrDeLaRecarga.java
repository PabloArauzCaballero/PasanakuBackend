package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas.CobroQr;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRecargaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El QR dinamico de una orden de recarga: la instruccion de pago que la persona escanea. No mueve saldo.
 *
 * <p>Se guarda en una transaccion propia DESPUES de hablar con el proveedor (la red nunca va dentro de la
 * transaccion), y solo la primera vez: en un reintento queda el QR que la persona ya pudo haber escaneado.
 */
@Service
public class QrDeLaRecarga {

    private final Datos datos;
    private final OrdenRecargaRepositorio ordenes;

    public QrDeLaRecarga(Datos datos, OrdenRecargaRepositorio ordenes) {
        this.datos = datos;
        this.ordenes = ordenes;
    }

    /** Deja el QR en la orden y devuelve el que quedo guardado (en un reintento, el de la primera vez). */
    @Transactional
    public Optional<CobroQr> guardar(UUID ordenId, CobroQr cobro, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            ordenes.guardarQr(dsl, ordenId, cobro);
            return ordenes.cobroQr(dsl, ordenId);
        });
    }

    /** El QR de la orden. No autoriza: quien llama ya comprobo la titularidad ({@code consultarPropia}). */
    @Transactional(readOnly = true)
    public Optional<CobroQr> deLaOrden(UUID ordenId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> ordenes.cobroQr(dsl, ordenId));
    }
}
