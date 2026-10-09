package bo.aportaya.nucleofinanciero.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Cuanto cuesta una operacion, segun quien fija los precios.
 *
 * <p>El tarifario es de {@code tarifas} y este servicio no lo lee: se lo pregunta. La
 * consulta va **fuera de la transaccion** (invariante 6), y por eso es un puerto y no
 * una llamada dentro del caso de uso.
 *
 * <p>La diferencia entre <b>cero</b> y <b>vacio</b> es la que importa y no se puede
 * confundir: cero significa que el tarifario dice que esa operacion es gratuita; vacio
 * significa que no se pudo saber. Cobrar cero cuando no se supo es regalar plata en
 * silencio, asi que quien recibe vacio rechaza (invariante 9).
 */
public interface CotizadorDeComision {

    Optional<Dinero> costoDe(String hechoGenerador, UUID referenciaId, Dinero montoBase, String claveIdempotencia);

    /**
     * La cotizacion completa —base, comision, impuesto, total y hasta cuando vale—, para
     * mostrarsela a la persona <b>antes</b> de operar. Con la misma clave devuelve la misma
     * cotizacion. Vacio significa que no se pudo saber; una operacion gratuita responde con
     * {@link Cotizacion#gratuita()} y sin identificador.
     */
    default Optional<Cotizacion> cotizar(
            String hechoGenerador, UUID referenciaId, Dinero montoBase, String claveIdempotencia) {
        return Optional.empty();
    }

    /** Deja constancia de que la persona acepto exactamente ese precio; false si no se pudo. */
    default boolean aceptar(UUID cotizacionId) {
        return false;
    }

    record Cotizacion(
            Optional<UUID> id,
            Dinero base,
            Dinero comision,
            Dinero impuesto,
            Dinero total,
            Optional<OffsetDateTime> validaHasta,
            boolean gratuita) {}
}
