package bo.aportaya.identidad.dominio;

/**
 * Enmascarado de los datos de contacto que muestra la ficha del participante (CU-134). Puro.
 *
 * <p>El valor completo solo se ve con una revelacion explicita, que exige segundo factor y deja
 * rastro (R-SEG-02, R-SEG-12). Lo enmascarado no debe permitir reconstruir el dato.
 */
public final class Enmascarado {

    private Enmascarado() {}

    /** {@code +59171234512} → {@code +591 7•••••12}: prefijo de pais, primer digito y los dos ultimos. */
    public static String telefono(String e164) {
        if (e164 == null || e164.length() < 8) {
            return "•••";
        }
        String pais = e164.startsWith("+591") ? "+591" : e164.substring(0, 3);
        String resto = e164.substring(pais.length());
        return pais + " " + resto.charAt(0) + "•".repeat(Math.max(1, resto.length() - 3))
                + resto.substring(resto.length() - 2);
    }

    /** {@code juana@gmail.com} → {@code j•••@g•••.com}. */
    public static String correo(String correo) {
        if (correo == null) {
            return null;
        }
        int arroba = correo.indexOf('@');
        int punto = correo.lastIndexOf('.');
        if (arroba < 1 || punto < arroba + 2) {
            return "•••";
        }
        return correo.charAt(0) + "•••@" + correo.charAt(arroba + 1) + "•••" + correo.substring(punto);
    }

    /** {@code Juana Perez Mamani} → {@code Juana P.}: el directorio no muestra el apellido completo. */
    public static String nombreAbreviado(String nombres, String apellidos) {
        String primero = nombres == null ? "" : nombres.trim().split("\\s+")[0];
        String inicial = apellidos == null || apellidos.isBlank()
                ? ""
                : " " + apellidos.trim().charAt(0) + ".";
        return primero + inicial;
    }
}
