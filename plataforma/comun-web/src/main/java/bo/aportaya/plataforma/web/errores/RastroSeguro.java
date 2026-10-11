package bo.aportaya.plataforma.web.errores;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lo que de una excepcion se puede escribir en la bitacora: identificadores, no contenido.
 *
 * <p>El mensaje de una excepcion es texto libre que arma quien la lanza, y el de un driver
 * arrastra lo que le dieron: la cadena de conexion, el valor que violo una unicidad
 * ({@code Key (col)=(valor)}), un fragmento de la consulta. Loguear {@code e} o
 * {@code e.getMessage()} es escribir eso en disco y en el agregador de logs (regla 90.2.1).
 *
 * <p>Se deja lo que sirve para diagnosticar —la clase de cada eslabon de la cadena, el
 * {@code SQLSTATE}, el nombre de la restriccion, y las primeras lineas del codigo propio
 * donde ocurrio— y se descarta lo que cuenta el dato. El hilo conductor con el cliente es el
 * {@code trazaId}, que el manejador pone aparte.
 */
final class RastroSeguro {

    private static final Pattern RESTRICCION = Pattern.compile("constraint \"([A-Za-z0-9_]{1,63})\"");
    private static final int PROPIAS = 4;

    private RastroSeguro() {}

    /** {@code Clase <- Causa <- Raiz [SQLSTATE 23505] en Clase.metodo:linea, ...}. */
    static String de(Throwable fallo) {
        List<String> cadena = new ArrayList<>();
        Throwable actual = fallo;
        Throwable raiz = fallo;
        while (actual != null && cadena.size() < 8) {
            cadena.add(actual.getClass().getSimpleName());
            raiz = actual;
            actual = actual.getCause() == actual ? null : actual.getCause();
        }
        StringBuilder rastro = new StringBuilder(String.join(" <- ", cadena));
        if (raiz instanceof SQLException sql && sql.getSQLState() != null) {
            rastro.append(" [SQLSTATE ").append(sql.getSQLState()).append(']');
        }
        List<String> donde = new ArrayList<>();
        for (StackTraceElement marco : fallo.getStackTrace()) {
            if (marco.getClassName().startsWith("bo.aportaya")) {
                donde.add(marco.getClassName().substring(marco.getClassName().lastIndexOf('.') + 1) + "."
                        + marco.getMethodName() + ":" + marco.getLineNumber());
                if (donde.size() == PROPIAS) {
                    break;
                }
            }
        }
        if (!donde.isEmpty()) {
            rastro.append(" en ").append(String.join(", ", donde));
        }
        return rastro.toString();
    }

    /** El nombre de la restriccion que disparo (un identificador del esquema), o {@code desconocida}. */
    static String restriccionDe(String mensajeDelDriver) {
        if (mensajeDelDriver == null) {
            return "desconocida";
        }
        Matcher m = RESTRICCION.matcher(mensajeDelDriver);
        return m.find() ? m.group(1) : "desconocida";
    }
}
