package bo.aportaya.identidad.infraestructura;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lo que se pide al listar personal interno (GU-17). El cursor {@code (antesDe, antesDeId)} es la fecha de alta y el
 * id de la ultima fila recibida: va completo o no va, y eso lo valida quien arma el filtro.
 */
public record FiltroDeUsuariosInternos(
        String estado, String rol, String busqueda, int limite, OffsetDateTime antesDe, UUID antesDeId) {

    /** Tamano de pagina cuando no se pide otro; igual al default de `limite` en el contrato. No es cifra regulatoria. */
    public static final int FILAS_POR_PAGINA = 50;
}
