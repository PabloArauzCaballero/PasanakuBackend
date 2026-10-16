package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.OrdenRetiroRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-11 · Deja la orden lista para salir hacia el proveedor, dentro de su transaccion.
 *
 * <p>Solo cambia el estado local (de AUTORIZADA a EN_PROCESO) y valida que la orden pueda
 * salir. La llamada al proveedor ocurre despues, fuera de la transaccion, en
 * {@link RetirosConProveedor}: una llamada de red dentro de la transaccion que retiene plata
 * deja el dinero bloqueado esperando a un tercero (invariante 6).
 */
@Service
public class CU11InstruirRetiro {

    private final Datos datos;
    private final OrdenRetiroRepositorio ordenes;
    private final Reloj reloj;

    public CU11InstruirRetiro(Datos datos, OrdenRetiroRepositorio ordenes, Reloj reloj) {
        this.datos = datos;
        this.ordenes = ordenes;
        this.reloj = reloj;
    }

    /** Idempotente: preparar una orden que ya esta en proceso, pagada o rechazada no cambia nada. */
    @Transactional
    public EnvioDeRetiro preparar(UUID ordenId, ContextoSesion ctx) {
        var ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            ordenes.bloquear(dsl, ordenId).orElseThrow(this::noExiste);
            var orden = ordenes.ver(dsl, ordenId).orElseThrow(this::noExiste);
            if (!ctx.esSistema() && !ctx.usuarioId().equals(orden.solicitadaPor())) {
                throw new ErrorDeNegocio(CodigoError.de(11, 4), "Solo quien pidio el retiro puede despacharlo.");
            }
            // EN_REVISION espera la segunda firma (H3.S2): no sale hasta que un aprobador la autorice.
            if ("EN_REVISION".equals(orden.estado())) {
                throw new ErrorDeNegocio(CodigoError.de(11, 8), "Falta la segunda aprobacion de este retiro.");
            }
            boolean abierta = "AUTORIZADA".equals(orden.estado()) || "EN_PROCESO".equals(orden.estado());
            if (abierta) {
                // Dos firmas: sin la segunda la orden no sale, y la base tampoco lo admitiria.
                if (orden.requiereDobleAprobacion() && orden.aprobadaPor().isEmpty()) {
                    throw new ErrorDeNegocio(CodigoError.de(11, 8), "Falta la segunda aprobacion de este retiro.");
                }
                if (orden.enfriamientoHasta()
                        .filter(hasta -> hasta.isAfter(ahora))
                        .isPresent()) {
                    throw new ErrorDeNegocio(
                            CodigoError.de(11, 3),
                            "El destino se agrego hace poco: este retiro todavia no puede salir.");
                }
                if (!ordenes.encajeCumplido(dsl, orden.neto().moneda().name())) {
                    throw new ErrorDeNegocio(
                            CodigoError.de(11, 6), "Con el encaje incumplido no salen retiros nuevos.");
                }
                ordenes.enviarAlProveedor(dsl, ordenId);
            }
            return new EnvioDeRetiro(
                    orden.id(),
                    orden.neto(),
                    abierta ? "EN_PROCESO" : orden.estado(),
                    orden.transaccionId().orElse(null));
        });
    }

    /** Lo que la orden es ahora, sin moverla. */
    @Transactional(readOnly = true)
    public EnvioDeRetiro ver(UUID ordenId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            var orden = ordenes.ver(dsl, ordenId).orElseThrow(this::noExiste);
            return new EnvioDeRetiro(
                    orden.id(),
                    orden.neto(),
                    orden.estado(),
                    orden.transaccionId().orElse(null));
        });
    }

    private ErrorDeNegocio noExiste() {
        return new ErrorDeNegocio(CodigoError.de(11, 1), "Esa orden de retiro no existe.");
    }

    /** {@code neto} es lo que se le instruye al proveedor: el monto pedido menos el costo. */
    public record EnvioDeRetiro(UUID ordenId, Dinero neto, String estado, UUID transaccionId) {}
}
