package bo.aportaya.plataforma.archivos;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;

/**
 * Decide el tipo mirando los **primeros bytes**, no la extension ni el
 * {@code Content-Type} que mando el cliente (ADR-034, regla 2).
 *
 * <p>Las dos cosas las elige quien sube, y quien sube puede estar atacando: un
 * ejecutable renombrado a {@code .jpg} pasa cualquier comprobacion de extension.
 */
public final class TipoPorContenido {

    private TipoPorContenido() {}

    /** Cuantos bytes hay que espiar para decidir. */
    public static final int BYTES_A_MIRAR = 12;

    public static String detectar(byte[] inicio) {
        if (inicio == null || inicio.length < 4) {
            throw new ErrorDeDominio("El archivo es demasiado corto para ser una imagen");
        }
        if (empiezaCon(inicio, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (empiezaCon(inicio, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        if (empiezaCon(inicio, 0x25, 0x50, 0x44, 0x46)) {
            return "application/pdf";
        }
        throw new ErrorDeDominio("No reconocemos ese archivo. Sacá la foto de nuevo.");
    }

    private static boolean empiezaCon(byte[] datos, int... firma) {
        if (datos.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((datos[i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
