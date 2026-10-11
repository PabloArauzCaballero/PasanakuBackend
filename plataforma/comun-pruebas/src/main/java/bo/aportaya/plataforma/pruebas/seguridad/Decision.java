package bo.aportaya.plataforma.pruebas.seguridad;

import java.nio.file.Path;

/**
 * Lo que el codigo decide sobre el acceso a una operacion.
 *
 * @param tipo permiso, publico, ninguna (el metodo existe y no declara nada) o sin
 *     metodo (el contrato lo promete y ningun controlador lo implementa)
 * @param valor el codigo de permiso, o la justificacion del {@code @Publico}
 * @param archivo el controlador, o {@code null} si no hay metodo
 * @param linea la linea de la declaracion del metodo
 * @param cuerpo el cuerpo del metodo, para las comprobaciones que miran que hace
 */
public record Decision(Tipo tipo, String valor, Path archivo, int linea, String cuerpo) {

    public enum Tipo {
        PERMISO,
        PUBLICO,
        NINGUNA,
        SIN_METODO
    }

    public static Decision sinMetodo() {
        return new Decision(Tipo.SIN_METODO, "", null, 0, "");
    }

    public boolean decide() {
        return tipo == Tipo.PERMISO || tipo == Tipo.PUBLICO;
    }

    public String ubicacion(Path raiz) {
        if (archivo == null) {
            return "(sin controlador)";
        }
        return "%s:%d".formatted(raiz.relativize(archivo).toString().replace('\\', '/'), linea);
    }
}
