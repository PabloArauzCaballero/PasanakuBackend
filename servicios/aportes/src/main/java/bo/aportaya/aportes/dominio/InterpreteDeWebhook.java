package bo.aportaya.aportes.dominio;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.Optional;

/**
 * Traduce el cuerpo crudo de un webhook (formato del PROVEEDOR) al {@link EventoDePago} propio. Es el unico lugar
 * que conoce los nombres de campo del proveedor: cada adaptador trae el suyo y {@code CU100} no se entera.
 *
 * <p>Se invoca DESPUES de verificar firma y ventana: nunca se lee el cuerpo de un webhook sin firma valida.
 */
public interface InterpreteDeWebhook {

    /** @throws PayloadIlegible si el cuerpo no tiene el formato que este proveedor promete */
    EventoDePago interpretar(String cuerpoCrudo) throws PayloadIlegible;

    /**
     * {@code idEvento} y {@code referenciaProveedor} pueden venir {@code null} (el caso de uso decide que hacer);
     * {@code monto} esta vacio si falta o no se puede leer como importe con moneda valida.
     */
    record EventoDePago(String idEvento, String tipo, String referenciaProveedor, Optional<Dinero> monto) {}

    final class PayloadIlegible extends Exception {
        public PayloadIlegible(String mensaje) {
            super(mensaje);
        }
    }
}
