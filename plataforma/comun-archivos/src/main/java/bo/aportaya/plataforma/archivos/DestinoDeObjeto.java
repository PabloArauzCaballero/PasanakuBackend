package bo.aportaya.plataforma.archivos;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Donde cae el objeto dentro de su ambito, y con que nombre.
 *
 * <p><b>Una carpeta por persona.</b> El expediente de alguien son tres archivos —el
 * anverso, el reverso y la prueba de vida— y hasta ahora caian mezclados en
 * {@code identidad/2026/09/} con nombres de UUID: para juntar el expediente de una
 * persona habia que leer tres columnas de dos tablas. Con la carpeta, el expediente
 * se ve de un vistazo listando {@code identidad/<usuarioId>/}, que es justo lo que
 * hace falta para armar un corpus y para entregarlo cuando alguien lo pide.
 *
 * <p>La carpeta es un <b>identificador opaco</b> —un UUID—, nunca un nombre ni un
 * numero de documento: la ruta no lleva datos personales adentro (ADR-034, regla 4).
 * La etiqueta dice que es el archivo (anverso, reverso, selfie) y no de quien es.
 *
 * <p>Sigue habiendo un UUID en el nombre del archivo: los objetos <b>no se
 * sobrescriben</b>, y sacarse la foto de nuevo tiene que crear un objeto nuevo al
 * lado del anterior, no pisarlo.
 */
public record DestinoDeObjeto(String carpeta, String etiqueta) {

    /** Lo unico que se admite en un tramo de ruta: sin barras, sin puntos, sin espacios. */
    private static final Pattern TRAMO = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");

    public DestinoDeObjeto {
        carpeta = normalizar(carpeta, "carpeta");
        etiqueta = normalizar(etiqueta, "etiqueta");
    }

    /**
     * El destino de una foto del expediente de alguien: {@code <usuarioId>/<cara>}.
     * Se toma el id y no el nombre a proposito — ver la nota de arriba.
     */
    public static DestinoDeObjeto deExpediente(java.util.UUID usuarioId, String cara) {
        if (usuarioId == null) {
            throw new ErrorDeDominio("Una foto de expediente sin usuario no tiene carpeta");
        }
        return new DestinoDeObjeto(usuarioId.toString(), cara);
    }

    /** El reparto viejo por fecha, para lo que no pertenece a nadie en particular. */
    public static DestinoDeObjeto porFecha() {
        var hoy = java.time.LocalDate.now();
        return new DestinoDeObjeto("%04d-%02d".formatted(hoy.getYear(), hoy.getMonthValue()), "archivo");
    }

    /** La misma validacion, para quien solo necesita el nombre de la carpeta. */
    public static String carpetaValida(String carpeta) {
        return normalizar(carpeta, "carpeta");
    }

    private static String normalizar(String tramo, String campo) {
        if (tramo == null || tramo.isBlank()) {
            throw new ErrorDeDominio("Un destino sin " + campo + " no ubica el archivo");
        }
        String limpio = tramo.trim().toLowerCase(Locale.ROOT);
        if (!TRAMO.matcher(limpio).matches()) {
            // Rechazar y no sanear: sanear una ruta invalida esconde el error y deja
            // el archivo en un lugar que nadie pidio.
            throw new ErrorDeDominio("Ese " + campo + " no sirve como tramo de ruta: " + tramo);
        }
        return limpio;
    }
}
