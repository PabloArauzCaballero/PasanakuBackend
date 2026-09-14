package bo.aportaya.plataforma.archivos;

import java.io.InputStream;
import java.util.Objects;

/**
 * Lo que llega para guardar. **El nombre original nunca es la ruta** (ADR-034,
 * regla 4): se guarda como metadato y la ruta la genera el adaptador con un UUID, asi
 * no hay {@code ../}, ni colisiones, ni nombres con datos personales adentro.
 */
public record ContenidoEntrante(InputStream datos, long bytes, String nombreOriginal) {

    public ContenidoEntrante {
        Objects.requireNonNull(datos, "datos");
    }
}
