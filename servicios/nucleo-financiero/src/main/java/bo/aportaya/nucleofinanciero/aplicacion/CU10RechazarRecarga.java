package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRecargaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-10 · El proveedor rechazo la recarga en firme: la orden se cierra.
 *
 * <p>No se escribe ningun movimiento: el dinero nunca entro. Cerrar la orden evita que
 * quede pendiente hasta vencer, mostrandole a la persona un QR que ya no sirve.
 */
@Service
public class CU10RechazarRecarga {

    private final Datos datos;
    private final OrdenRecargaRepositorio ordenes;
    private final CuentaBilleteraRepositorio cuentas;
    private final Outbox outbox;

    public CU10RechazarRecarga(
            Datos datos, OrdenRecargaRepositorio ordenes, CuentaBilleteraRepositorio cuentas, Outbox outbox) {
        this.datos = datos;
        this.ordenes = ordenes;
        this.cuentas = cuentas;
        this.outbox = outbox;
    }

    /** Idempotente: rechazar una orden ya rechazada no repite el evento. */
    @Transactional
    public void rechazar(UUID ordenId, ContextoSesion ctx) {
        datos.conContexto(ctx, dsl -> {
            var orden = ordenes.bloquear(dsl, ordenId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(10, 5), "Esa orden no existe."));
            var cuenta = cuentas.ver(dsl, orden.cuentaId()).orElseThrow();
            if (!ctx.usuarioId().equals(cuenta.usuarioId())) {
                throw new ErrorDeNegocio(CodigoError.de(10, 4), "Solo el titular puede cerrar esta recarga.");
            }
            if (ordenes.rechazar(dsl, ordenId)) {
                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "nucleo_financiero.recarga_rechazada",
                                "orden_recarga",
                                ordenId,
                                Map.of("cuentaBilleteraId", orden.cuentaId().toString()),
                                UUID.fromString(ctx.traza().id())));
            }
            return null;
        });
    }
}
