package bo.aportaya.plataforma.dominio;

import java.util.Objects;

/**
 * Una operacion del contrato que el producto retiro por decision del dueno y que sigue publicada solo mientras sus
 * consumidores la sueltan: expandir y contraer, regla 98.1.3.
 *
 * <p>Sale como {@code 410 Gone} con su codigo {@code AP-CU<NN>-<nn>} y sin efecto alguno, para que el cliente
 * distinga «ya no existe» de «fallo» y no reintente. No es una regla de negocio incumplida (eso es
 * {@link ErrorDeNegocio}, 422): es una puerta que se cerro.
 */
public class OperacionRetirada extends ErrorDeDominio {

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
