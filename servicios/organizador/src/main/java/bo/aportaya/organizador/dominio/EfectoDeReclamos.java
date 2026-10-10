package bo.aportaya.organizador.dominio;

import java.util.UUID;

/**
 * Lo que un reclamo en curso de una persona le permite o no le permite hacer a este servicio (H13.S1.M6, regla 91.4).
 *
 * <p>La pregunta la contesta {@code cumplimiento}
 * ({@code GET /reclamos/usuarios/{usuarioId}/efecto}, contrato en {@code evidencia/carril-A3/contrato-efecto-reclamo.md}).
 * Un reclamo en revision NO equivale a una sancion firme: una suspension o inhabilitacion que se aplica sobre quien
 * esta discutiendo el caso es castigar antes de resolver.
 *
 * <p><b>Regla del consumidor:</b> todo lo que no sea una respuesta entendible del servicio FRENA. "No se pudo saber
 * si hay un reclamo" jamas se lee como "no hay reclamo".
 */
public interface EfectoDeReclamos {

    /** Que se quiere hacer; cada uno mira su propio indicador del contrato. */
    enum Que {
        /** Ejecutar una deuda. */
        DEUDA_EJECUTABLE,
        /** Dejar firme una sancion: es lo que pregunta este servicio. */
        SANCION_FIRME
    }

    enum Decision {
        PROCEDE,
        FRENA
    }

    Decision consultar(UUID usuarioId, Que que);
}
