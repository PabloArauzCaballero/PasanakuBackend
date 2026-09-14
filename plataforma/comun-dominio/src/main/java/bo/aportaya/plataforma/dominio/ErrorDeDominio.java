package bo.aportaya.plataforma.dominio;

/**
 * Lo que el dominio rechaza por si mismo, sin preguntarle a la base ni a la red.
 *
 * <p>Que sea una sola clase raiz es lo que permite que la capa web la traduzca a un
 * {@code 422} —regla de negocio— y no la confunda con un {@code 400} de esquema.
 */
public class ErrorDeDominio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ErrorDeDominio(String mensaje) {
        super(mensaje);
    }

    /**
     * Con la causa. La necesita el borde con el mundo —guardar un archivo, hablar con
     * el almacen—: el mensaje sigue siendo para quien usa la app, y la causa queda en
     * la bitacora para quien opera. Perderla obliga a adivinar por que fallo.
     */
    public ErrorDeDominio(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
