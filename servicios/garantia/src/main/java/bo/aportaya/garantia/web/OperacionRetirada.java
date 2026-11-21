package bo.aportaya.garantia.web;

import bo.aportaya.plataforma.dominio.CodigoError;
import java.util.Objects;

/**
 * Una operacion del contrato que el producto retiro por decision del dueno, y que sigue publicada (marcada
 * {@code deprecated}) solo mientras sus consumidores la sueltan: expandir y contraer, regla 98.1.3.
 *
 * <p>Responde {@code 410 Gone} con un codigo estable y sin efecto alguno, para que el cliente distinga «ya no existe»
 * de «fallo» y no reintente.
 */
public final class OperacionRetirada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient CodigoError codigo;

    public OperacionRetirada(CodigoError codigo, String mensaje) {
        super(mensaje);
        this.codigo = Objects.requireNonNull(codigo, "codigo");
    }

    public CodigoError codigo() {
        return codigo;
    }
}
