package bo.aportaya.nucleofinanciero.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Confirmaciones externas autenticadas; las llamadas ocurren fuera de la transaccion. */
public interface ProveedorDeRecargas {

    /**
     * Abre el cobro en el proveedor y devuelve el QR que la persona tiene que pagar, si el proveedor
     * emite uno. Idempotente por {@code referencia}: pedirlo dos veces devuelve el MISMO QR.
     */
    Optional<CobroQr> solicitar(UUID referencia, Dinero monto);

    Confirmacion consultar(UUID referencia);

    record Confirmacion(
            UUID referencia, UUID transaccionProveedor, Dinero monto, String estado, OffsetDateTime liquidadaEn) {}

    /**
     * El QR dinamico de una orden. {@code contenido} es el texto que se codifica en el QR, tal como lo
     * entrega el proveedor; {@code origen} dice si lo emitio un banco ({@code BANCO}) o el doble de
     * prueba ({@code PRUEBA}), que no se puede pagar desde ningun banco.
     */
    record CobroQr(String contenido, String origen) {
        public static final int LARGO_MAXIMO = 512;

        public CobroQr {
            if (contenido == null || contenido.isBlank() || contenido.length() > LARGO_MAXIMO) {
                throw new IllegalArgumentException("El contenido del QR esta vacio o excede " + LARGO_MAXIMO + ".");
            }
            if (!"BANCO".equals(origen) && !"PRUEBA".equals(origen)) {
                throw new IllegalArgumentException("Origen de QR desconocido.");
            }
        }
    }
}
