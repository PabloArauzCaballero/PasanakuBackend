package bo.aportaya.plataforma.pruebas.seguridad;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Un archivo Java leido sin compilarlo: el texto original y una copia enmascarada.
 *
 * <p>La copia tiene la misma longitud que el original, pero los comentarios y el
 * contenido de los literales de texto son espacios. Buscar estructura ({@code ;},
 * {@code {}, {@code )}) sobre ella no se confunde con un punto y coma que vive dentro de
 * una justificacion de {@code @Publico}; leer el valor, en cambio, se hace sobre el
 * original, en la misma posicion. Sin la mascara, una anotacion con un parentesis en su
 * texto parte el bloque donde no es y el barrido "no encuentra" un permiso que si esta.
 */
public final class FuenteJava {

    private final Path archivo;
    private final String original;
    private final String enmascarado;

    private FuenteJava(Path archivo, String original) {
        this.archivo = archivo;
        this.original = original;
        this.enmascarado = enmascarar(original);
    }

    public static FuenteJava leer(Path archivo) {
        try {
            return new FuenteJava(archivo, Files.readString(archivo));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static FuenteJava deTexto(String texto) {
        return new FuenteJava(Path.of("memoria.java"), texto);
    }

    public Path archivo() {
        return archivo;
    }

    public String original() {
        return original;
    }

    public String enmascarado() {
        return enmascarado;
    }

    /** Linea (desde 1) de una posicion del texto. */
    public int linea(int posicion) {
        int linea = 1;
        for (int i = 0; i < posicion && i < original.length(); i++) {
            if (original.charAt(i) == '\n') {
                linea++;
            }
        }
        return linea;
    }

    /**
     * Lo que hay entre el final de la declaracion anterior y esta posicion: sus
     * anotaciones, tal cual estan escritas.
     */
    public String anotacionesAntesDe(int posicion) {
        return original.substring(inicioDelBloque(posicion), posicion);
    }

    /**
     * Lo mismo que {@link #anotacionesAntesDe}, pero sin comentarios ni contenido de
     * literales: es donde se busca la anotacion, para no tomar por tal un
     * {@code {@code @Publico}} de un Javadoc.
     */
    public String anotacionesEnmascaradasAntesDe(int posicion) {
        return enmascarado.substring(inicioDelBloque(posicion), posicion);
    }

    private int inicioDelBloque(int posicion) {
        int inicio = posicion;
        while (inicio > 0) {
            char c = enmascarado.charAt(inicio - 1);
            if (c == ';' || c == '{' || c == '}') {
                break;
            }
            inicio--;
        }
        return inicio;
    }

    /** El cuerpo de la primera llave que abre despues de la posicion, o vacio si no hay. */
    public String cuerpoDesde(int posicion) {
        int abre = enmascarado.indexOf('{', posicion);
        int termina = enmascarado.indexOf(';', posicion);
        if (abre < 0 || (termina >= 0 && termina < abre)) {
            return "";
        }
        int profundidad = 0;
        for (int i = abre; i < enmascarado.length(); i++) {
            char c = enmascarado.charAt(i);
            if (c == '{') {
                profundidad++;
            } else if (c == '}' && --profundidad == 0) {
                return original.substring(abre, i + 1);
            }
        }
        return original.substring(abre);
    }

    /**
     * Misma longitud que el texto: comentarios y contenido de literales, en blanco.
     * Los saltos de linea se conservan para no mover ninguna posicion.
     */
    static String enmascarar(String texto) {
        StringBuilder salida = new StringBuilder(texto.length());
        int i = 0;
        int n = texto.length();
        while (i < n) {
            char c = texto.charAt(i);
            if (texto.startsWith("//", i)) {
                i = tapar(texto, salida, i, siguienteSalto(texto, i));
            } else if (texto.startsWith("/*", i)) {
                int fin = texto.indexOf("*/", i + 2);
                i = tapar(texto, salida, i, fin < 0 ? n : fin + 2);
            } else if (texto.startsWith("\"\"\"", i)) {
                int fin = texto.indexOf("\"\"\"", i + 3);
                salida.append("\"\"\"");
                i = tapar(texto, salida, i + 3, fin < 0 ? n : fin);
                if (fin >= 0) {
                    salida.append("\"\"\"");
                    i = fin + 3;
                }
            } else if (c == '"' || c == '\'') {
                salida.append(c);
                int fin = cierreDeLiteral(texto, i + 1, c);
                i = tapar(texto, salida, i + 1, fin);
                if (fin < n) {
                    salida.append(c);
                    i = fin + 1;
                }
            } else {
                salida.append(c);
                i++;
            }
        }
        return salida.toString();
    }

    private static int tapar(String texto, StringBuilder salida, int desde, int hasta) {
        for (int i = desde; i < hasta; i++) {
            salida.append(texto.charAt(i) == '\n' ? '\n' : ' ');
        }
        return hasta;
    }

    private static int siguienteSalto(String texto, int desde) {
        int salto = texto.indexOf('\n', desde);
        return salto < 0 ? texto.length() : salto;
    }

    private static int cierreDeLiteral(String texto, int desde, char comilla) {
        int i = desde;
        while (i < texto.length()) {
            char c = texto.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == comilla || c == '\n') {
                return i;
            } else {
                i++;
            }
        }
        return texto.length();
    }
}
