package bo.aportaya.plataforma.archivos;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import java.util.List;

/**
 * Que se acepta subir, y hasta que tamaño. **Lista blanca, no lista negra** (ADR-034,
 * regla 1): lo que no esta declarado no entra.
 *
 * <p>El tipo se decide por el contenido —los primeros bytes— y no por la extension ni
 * por el {@code Content-Type} que mando el cliente: las dos cosas las elige quien
 * sube, y quien sube puede estar atacando.
 */
public enum AmbitoArchivo {
    /** Cedula, selfie de prueba de vida. Evidencia legal con diez años de retencion. */
    IDENTIDAD(8 * 1024 * 1024, List.of("image/jpeg", "image/png", "application/pdf")),
    /** Comprobante de pago que carga quien aporta. */
    COMPROBANTE(5 * 1024 * 1024, List.of("image/jpeg", "image/png", "application/pdf")),
    /** Reportes exportados y presentados al regulador. */
    REPORTE(50 * 1024 * 1024, List.of("application/pdf", "text/csv"));

    private final long tamanoMaximo;
    private final List<String> tiposAdmitidos;

    AmbitoArchivo(long tamanoMaximo, List<String> tiposAdmitidos) {
        this.tamanoMaximo = tamanoMaximo;
        this.tiposAdmitidos = tiposAdmitidos;
    }

    public long tamanoMaximo() {
        return tamanoMaximo;
    }

    public List<String> tiposAdmitidos() {
        return tiposAdmitidos;
    }

    /**
     * El primer tramo de la ruta logica. Debajo va la carpeta del
     * {@link DestinoDeObjeto}: el ambito dice de que tramite es el archivo, y la
     * carpeta, de que expediente.
     */
    public String prefijo() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public void exigirQueQuepa(long bytes) {
        if (bytes <= 0) {
            throw new ErrorDeDominio("Un archivo vacio no es un archivo");
        }
        if (bytes > tamanoMaximo) {
            throw new ErrorDeDominio("El archivo pesa mas de lo que admite este tramite (%d MB)"
                    .formatted(tamanoMaximo / (1024 * 1024)));
        }
    }

    public void exigirTipoAdmitido(String tipoDetectado) {
        if (!tiposAdmitidos.contains(tipoDetectado)) {
            throw new ErrorDeDominio("Ese tipo de archivo no se admite aca: " + tipoDetectado);
        }
    }
}
