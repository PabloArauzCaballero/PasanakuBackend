package bo.aportaya.plataforma.archivos;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Lo que se guarda en la columna. **Nunca una URL publica** (ADR-034).
 *
 * <pre>
 *   s3://identidad/<usuarioId>/anverso-9f2c1e4a-….jpg
 * </pre>
 *
 * <p>El esquema dice <b>que adaptador</b> la escribio; el resto es la ruta logica.
 * Cambiar de adaptador no obliga a reescribir trece tablas: obliga a copiar los
 * objetos y a resolver las claves viejas con el adaptador viejo, que sigue
 * registrado.
 *
 * <p>Una URL publica en la columna seria un error de dos maneras: expone evidencia
 * personal a quien tenga el enlace, y ata el dato a un dominio y a un proveedor.
 */
public record ClaveObjeto(String esquema, String ruta) {

    private static final Pattern FORMA = Pattern.compile("^(s3|local)://(.+)$");

    public ClaveObjeto {
        Objects.requireNonNull(esquema, "esquema");
        if (ruta == null || ruta.isBlank()) {
            throw new ErrorDeDominio("Una clave de objeto sin ruta no apunta a nada");
        }
        if (ruta.contains("..")) {
            throw new ErrorDeDominio("La ruta de un objeto no sube de directorio");
        }
    }

    public static ClaveObjeto de(String texto) {
        var m = FORMA.matcher(Objects.requireNonNull(texto, "clave"));
        if (!m.matches()) {
            throw new ErrorDeDominio("Eso no es una clave de objeto: " + texto);
        }
        return new ClaveObjeto(m.group(1), m.group(2));
    }

    /** El nombre del objeto dentro del bucket, sin el esquema. */
    public String nombreEnBucket() {
        return ruta;
    }

    @Override
    public String toString() {
        return esquema + "://" + ruta;
    }
}
